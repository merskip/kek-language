package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.node.*


class LLVMCompiler(
    moduleId: String,
    targetTriple: String?
) {

    private val context = LLVM.LLVMContextCreate()
    val module: LLVMModuleRef = LLVM.LLVMModuleCreateWithNameInContext(moduleId, context)
    private val builder = LLVM.LLVMCreateBuilder()

    private val variableScopeStack = VariableScopeStack()

    private var sysExitFunction: LLVMValueRef
    private var sysWriteFunction: LLVMValueRef

    private var currentBlock: LLVMBasicBlockRef? = null

    init {
        LLVM.LLVMSetTarget(module, targetTriple ?: LLVM.LLVMGetDefaultTargetTriple().string)
        sysExitFunction = declareSysExitFunction()
        sysWriteFunction = defineSysWriteFunction()
        defineBuiltinPrintFunction()
    }

    fun compile(fileNodeAST: FileNodeAST) {

        fileNodeAST.nodes.forEach { functionDefinition ->
            compileFunctionDefinition(functionDefinition)
        }
        val err = BytePointer(1024L)
        if (LLVM.LLVMVerifyModule(module, LLVM.LLVMPrintMessageAction, err) != 0) {
            println(err.string)
        }
    }

    private fun declareSysExitFunction(): LLVMValueRef {
        val parameters = arrayOf(
            LLVM.LLVMInt64TypeInContext(context)
        )
        val returnType = LLVM.LLVMVoidTypeInContext(context)
        val functionType = LLVM.LLVMFunctionType(returnType, parameters.toPointer(), parameters.size, 0)
        val functionValue = LLVM.LLVMAddFunction(module, ".kek.sys.exit", functionType)

        val entryBlock = LLVM.LLVMAppendBasicBlockInContext(context, functionValue, "entry")
        LLVM.LLVMPositionBuilderAtEnd(builder, entryBlock)

        val exitCode = functionValue.getFunctionParam(0)
        LLVM.LLVMSetValueName(exitCode, "exitCode")

        createSysCall(60, exitCode)
        LLVM.LLVMBuildUnreachable(builder)

        return functionValue
    }

    private fun defineSysWriteFunction(): LLVMValueRef {

        val returnType = LLVM.LLVMInt64TypeInContext(context)
        val parameters = arrayOf(
            LLVM.LLVMInt64Type(), // File descriptor
            LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0), // Buffer
            LLVM.LLVMInt64Type() // Buffer size
        )

        val functionType =
            LLVM.LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 0)
        val sysWriteFunctionValue = LLVM.LLVMAddFunction(module, ".kek.sys.write", functionType)

        val entryBlock = LLVM.LLVMAppendBasicBlockInContext(context, sysWriteFunctionValue, "entry")
        LLVM.LLVMPositionBuilderAtEnd(builder, entryBlock)

        val fd = sysWriteFunctionValue.getFunctionParam(0)
        LLVM.LLVMSetValueName(fd, "fd")

        val buf = sysWriteFunctionValue.getFunctionParam(1)
        LLVM.LLVMSetValueName(buf, "buf")

        val count = sysWriteFunctionValue.getFunctionParam(2)
        LLVM.LLVMSetValueName(count, "count")

        val result = createSysCall(1, fd, buf, count)
        LLVM.LLVMBuildRet(builder, result)

        if (LLVM.LLVMVerifyFunction(sysWriteFunctionValue, LLVM.LLVMPrintMessageAction) != 0) {
            val outputPointer = LLVM.LLVMPrintModuleToString(module)
            println(outputPointer.string)
            throw Exception("LLVMVerifyFunction failed")
        }

        return sysWriteFunctionValue
    }

    private fun defineBuiltinPrintFunction() {
        val returnType = LLVM.LLVMVoidTypeInContext(context)
        val parameters = arrayOf(
            LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0) // String pointer
        )

        val functionType =
            LLVM.LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 0)
        val printFunctionValue = LLVM.LLVMAddFunction(module, ".kek.builtin.print", functionType)

        val entryBlock = LLVM.LLVMAppendBasicBlockInContext(context, printFunctionValue, "entry")
        LLVM.LLVMPositionBuilderAtEnd(builder, entryBlock)

//        createSysCall(
//            60,
//            LLVM.LLVMConstInt(LLVM.LLVMInt32TypeInContext(context), 0, 0)
//        )
        LLVM.LLVMBuildRetVoid(builder)

        if (LLVM.LLVMVerifyFunction(printFunctionValue, LLVM.LLVMPrintMessageAction) != 0) {
            val outputPointer = LLVM.LLVMPrintModuleToString(module)
            println(outputPointer.string)
            throw Exception("LLVMVerifyFunction failed")
        }
    }

    private fun createSysCall(number: Long, vararg parameters: LLVMValueRef): LLVMValueRef {
        val target = LLVM.LLVMGetTarget(module).getTargetTriple()
        when (target.archType) {
            "x86_64" -> {
                val paramsRegisters = listOf("%rdi", "%rsi", "%rdx")

                val asm = mutableListOf("movq $0, %rax")
                parameters.forEachIndexed { index, _ ->
                    asm += "movq \$${index+1}, ${paramsRegisters[index]}"
                }
                asm += "syscall"
                return createAssembler(
                    asm,
                    outputConstraints = listOf("={rax}"),
                    inputConstraints = listOf(listOf("i"), parameters.map { "r" }).flatten(),
                    input = listOf(
                        listOf(LLVM.LLVMConstInt(LLVM.LLVMInt64Type(), number, 0)),
                        parameters.toList()
                    ).flatten()
                )
            }
            else -> error("Unsupported arch: ${target.archType}")
        }
    }

    private fun createAssembler(
        operations: List<String>,
        input: List<LLVMValueRef> = emptyList(),
        outputConstraints: List<String> = emptyList(),
        inputConstraints: List<String> = emptyList(),
        clobberConstraints: List<String> = emptyList()
    ): LLVMValueRef {
        val assemblerCode = operations.joinToString("; ")
        val constraints = listOf(outputConstraints, inputConstraints, clobberConstraints)
            .flatten().joinToString(",")

        val returnType = LLVM.LLVMInt64TypeInContext(context)
        val functionType = LLVM.LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(), 0, 1)
        val asmValue = LLVM.LLVMGetInlineAsm(
            functionType,
            BytePointer(assemblerCode), assemblerCode.length.toLong(),
            BytePointer(constraints), constraints.length.toLong(),
            1, 0, LLVM.LLVMInlineAsmDialectATT
        )
        return LLVM.LLVMBuildCall(builder, asmValue, PointerPointer<LLVMValueRef>(*input.toTypedArray()), input.size, "output")
    }

    private fun compileFunctionDefinition(functionDefinition: FunctionDefinitionNodeAST) {
        val functionValue = createFunction(functionDefinition)

        variableScopeStack.enterScope()

        val parametersValues = functionValue.getFunctionParams()
        val parametersIdentifiers = functionDefinition.arguments.map { it.identifier }
        (parametersIdentifiers zip parametersValues).forEach { (identifier, value) ->
            variableScopeStack.addReference(identifier, value)
            LLVM.LLVMSetValueName(value, identifier)
        }

        val entryBlock = LLVM.LLVMAppendBasicBlockInContext(context, functionValue, "entry")
        currentBlock = entryBlock
        LLVM.LLVMPositionBuilderAtEnd(builder, entryBlock)

        val returnValue = compileStatement(functionDefinition.body)
        LLVM.LLVMPositionBuilderAtEnd(builder, currentBlock)

        if (functionDefinition.identifier != "_start") {
            LLVM.LLVMBuildRet(builder, returnValue)
        } else {
            val exitParameters = listOf(
                returnValue
            ).toTypedArray()
            LLVM.LLVMBuildCall(builder, sysExitFunction, PointerPointer<LLVMValueRef>(*exitParameters), 1, "")
            LLVM.LLVMBuildUnreachable(builder)
        }

        if (LLVM.LLVMVerifyFunction(functionValue, LLVM.LLVMPrintMessageAction) != 0) {
            val outputPointer = LLVM.LLVMPrintModuleToString(module)
            println(outputPointer.string)
            throw Exception("LLVMVerifyFunction failed")
        }

        variableScopeStack.leaveScope()
    }

    private fun compileStatement(statement: StatementNodeAST): LLVMValueRef {
        return when (statement) {
            is FunctionCallNodeAST -> compileFunctionCall(statement)
            is BinaryOperatorNodeAST -> compileBinaryOperator(statement)
            is ConstantValueNodeAST -> compileConstantValue(statement)
            is IfConditionNodeAST -> compileIfCondition(statement)
            is CodeBlockNodeAST -> compileCodeBlock(statement)
            is ConstantStringNodeAST -> compileConstantStringValue(statement)
            is ReferenceNodeAST -> variableScopeStack.getReference(statement.identifier)
            else -> throw Exception("Unexpected statement: $statement")
        }
    }

    private fun compileFunctionCall(functionCall: FunctionCallNodeAST): LLVMValueRef {
        val identifier = if (functionCall.identifier == "printf") ".kek.builtin.print" else functionCall.identifier
        val functionValue = LLVM.LLVMGetNamedFunction(module, identifier)
            ?: throw Exception("Not found function: ${functionCall.identifier}")

        val parameters = functionCall.parameters.map { compileStatement(it) }.toTypedArray()

        return LLVM.LLVMBuildCall(
            builder, functionValue,
            PointerPointer<LLVMValueRef>(*parameters), parameters.size,
            if (functionCall.identifier == "printf") "" else functionCall.identifier + "_call"
        )
    }

    private fun compileBinaryOperator(binaryOperator: BinaryOperatorNodeAST): LLVMValueRef {
        val lhsValue = compileStatement(binaryOperator.lhs)
        val rhsValue = compileStatement(binaryOperator.rhs)

        return when (binaryOperator.identifier) {
            "+" -> LLVM.LLVMBuildAdd(builder, lhsValue, rhsValue, "add")
            "-" -> LLVM.LLVMBuildSub(builder, lhsValue, rhsValue, "sub")
            "*" -> LLVM.LLVMBuildMul(builder, lhsValue, rhsValue, "mul")
            "/" -> LLVM.LLVMBuildFDiv(builder, lhsValue, rhsValue, "div")
            "==" -> LLVM.LLVMBuildICmp(builder, LLVM.LLVMIntEQ, lhsValue, rhsValue, "cmp")
            else -> throw Exception("Unknown operator: ${binaryOperator.identifier}")
        }
    }

    private fun compileConstantValue(constantValue: ConstantValueNodeAST): LLVMValueRef {
        return when (constantValue) {
            is DecimalConstantValueNodeAST -> LLVM.LLVMConstReal(
                LLVM.LLVMDoubleTypeInContext(context),
                constantValue.value.toDouble()
            )
            is IntegerConstantValueNodeAST -> LLVM.LLVMConstInt(
                LLVM.LLVMInt32TypeInContext(context),
                constantValue.value.toLong(),
                1
            )
            else -> throw Exception("Unexpected statement: $constantValue")
        }
    }

    private fun compileConstantStringValue(constantStringNodeAST: ConstantStringNodeAST): LLVMValueRef {
        val hash = "%02x".format(constantStringNodeAST.string.hashCode())
        return LLVM.LLVMBuildGlobalStringPtr(builder, constantStringNodeAST.string, ".str.$hash")
    }

    private fun compileIfCondition(ifConditionNodeAST: IfConditionNodeAST): LLVMValueRef {
        val conditionValue = compileStatement(ifConditionNodeAST.condition)

        val ifTrueBlock = LLVM.LLVMCreateBasicBlockInContext(context, "ifTrue")

        val rawCurrentBlockName = LLVM.LLVMGetBasicBlockName(currentBlock!!)
        val simpleCurrentBlockNAme = rawCurrentBlockName.string.trimEnd { it.isDigit() }
        val ifAfterBlock = LLVM.LLVMCreateBasicBlockInContext(context, simpleCurrentBlockNAme)

        val ifBlockValue = LLVM.LLVMBuildCondBr(builder, conditionValue, ifTrueBlock, ifAfterBlock)

        LLVM.LLVMInsertExistingBasicBlockAfterInsertBlock(builder, ifTrueBlock)
        LLVM.LLVMPositionBuilderAtEnd(builder, ifTrueBlock)
        val ifTrueValue = compileStatement(ifConditionNodeAST.body) // TODO: Do anything with result of if?
        LLVM.LLVMBuildBr(builder, ifAfterBlock)

        LLVM.LLVMInsertExistingBasicBlockAfterInsertBlock(builder, ifAfterBlock)
        LLVM.LLVMPositionBuilderAtEnd(builder, ifAfterBlock)
        currentBlock = ifAfterBlock

        return ifBlockValue
    }

    private fun compileCodeBlock(codeBlockNodeAST: CodeBlockNodeAST): LLVMValueRef {
        var lastValue: LLVMValueRef? = null
        codeBlockNodeAST.statements.forEach { statement ->
            lastValue = compileStatement(statement)
        }
        return lastValue ?: LLVM.LLVMBuildUnreachable(builder)
    }

    private fun createFunction(functionDefinition: FunctionDefinitionNodeAST): LLVMValueRef {
        val returnType = LLVM.LLVMInt32TypeInContext(context)
        val parameters = functionDefinition.arguments.map {
            LLVM.LLVMInt32TypeInContext(context)
        }.toTypedArray()

        val functionType =
            LLVM.LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 0)
        return LLVM.LLVMAddFunction(module, functionDefinition.identifier, functionType)
    }
}