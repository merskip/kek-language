package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.LLVMBasicBlockRef
import org.bytedeco.llvm.LLVM.LLVMModuleRef
import org.bytedeco.llvm.LLVM.LLVMTypeRef
import org.bytedeco.llvm.LLVM.LLVMValueRef
import org.bytedeco.llvm.global.LLVM.*
import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.llvm.LLVMModule
import pl.merskip.keklang.llvm.enum.ArchType


@Deprecated("", replaceWith = ReplaceWith("pl.merskip.keklang.compiler.Compiler"))
class LLVMCompiler(
    moduleId: String,
    targetTriple: String?
) {

    private val context = LLVMContextCreate()
    val module: LLVMModuleRef = LLVMModuleCreateWithNameInContext(moduleId, context)
    private val builder = LLVMCreateBuilder()

    private val variableScopeStack = VariableScopeStack()

    private var sysExitFunction: LLVMValueRef
    private var sysWriteFunction: LLVMValueRef

    private var currentBlock: LLVMBasicBlockRef? = null

    init {
        LLVMSetTarget(module, targetTriple ?: LLVMGetDefaultTargetTriple().string)
        sysExitFunction = declareSysExitFunction()
        sysWriteFunction = defineSysWriteFunction()
        defineBuiltinPrintFunction()
    }

    fun compile(fileASTNode: FileASTNode) {

        fileASTNode.nodes.forEach { functionDefinition ->
            compileFunctionDefinition(functionDefinition)
        }
        val err = BytePointer(1024L)
        if (LLVMVerifyModule(module, LLVMPrintMessageAction, err) != 0) {
            println(err.string)
        }
    }

    private fun declareSysExitFunction(): LLVMValueRef {
        val parameters = arrayOf(
            LLVMInt64TypeInContext(context)
        )
        val returnType = LLVMVoidTypeInContext(context)
        val functionType = LLVMFunctionType(returnType, parameters.toPointerPointer(), parameters.size, 0)
        val functionValue = LLVMAddFunction(module, ".kek.sys.exit", functionType)

        val entryBlock = LLVMAppendBasicBlockInContext(context, functionValue, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        val exitCode = functionValue.getFunctionParameterValue(0)
        LLVMSetValueName(exitCode, "exitCode")

        createSysCall(60, exitCode)
        LLVMBuildUnreachable(builder)

        return functionValue
    }

    private fun defineSysWriteFunction(): LLVMValueRef {

        val returnType = LLVMInt64TypeInContext(context)
        val parameters = arrayOf(
            LLVMInt64Type(), // File descriptor
            LLVMPointerType(LLVMInt8Type(), 0), // Buffer
            LLVMInt64Type() // Buffer size
        )

        val functionType =
            LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 0)
        val sysWriteFunctionValue = LLVMAddFunction(module, ".kek.sys.write", functionType)

        val entryBlock = LLVMAppendBasicBlockInContext(context, sysWriteFunctionValue, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        val fd = sysWriteFunctionValue.getFunctionParameterValue(0)
        LLVMSetValueName(fd, "fd")

        val buf = sysWriteFunctionValue.getFunctionParameterValue(1)
        LLVMSetValueName(buf, "buf")

        val count = sysWriteFunctionValue.getFunctionParameterValue(2)
        LLVMSetValueName(count, "count")

        val result = createSysCall(1, fd, buf, count)
        LLVMBuildRet(builder, result)

        if (LLVMVerifyFunction(sysWriteFunctionValue, LLVMPrintMessageAction) != 0) {
            val outputPointer = LLVMPrintModuleToString(module)
            println(outputPointer.string)
            throw Exception("LLVMVerifyFunction failed")
        }

        return sysWriteFunctionValue
    }

    private fun defineBuiltinPrintFunction() {
        val returnType = LLVMVoidTypeInContext(context)
        val parameters = arrayOf(
            LLVMPointerType(LLVMInt8Type(), 0) // String pointer
        )

        val functionType =
            LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 0)
        val printFunctionValue = LLVMAddFunction(module, ".kek.builtin.print", functionType)

        val entryBlock = LLVMAppendBasicBlockInContext(context, printFunctionValue, "entry")
        LLVMPositionBuilderAtEnd(builder, entryBlock)

//        createSysCall(
//            60,
//            LLVMConstInt(LLVMInt32TypeInContext(context), 0, 0)
//        )
        LLVMBuildRetVoid(builder)

        if (LLVMVerifyFunction(printFunctionValue, LLVMPrintMessageAction) != 0) {
            val outputPointer = LLVMPrintModuleToString(module)
            println(outputPointer.string)
            throw Exception("LLVMVerifyFunction failed")
        }
    }

    private fun createSysCall(number: Long, vararg parameters: LLVMValueRef): LLVMValueRef {
        val target = LLVMModule(module).getTargetTriple()
        when (target.archType) {
            ArchType.X86_64 -> {
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
                        listOf(LLVMConstInt(LLVMInt64Type(), number, 0)),
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

        val returnType = LLVMInt64TypeInContext(context)
        val functionType = LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(), 0, 1)
        val asmValue = LLVMGetInlineAsm(
            functionType,
            BytePointer(assemblerCode), assemblerCode.length.toLong(),
            BytePointer(constraints), constraints.length.toLong(),
            1, 0, LLVMInlineAsmDialectATT
        )
        return LLVMBuildCall(builder, asmValue, PointerPointer<LLVMValueRef>(*input.toTypedArray()), input.size, "output")
    }

    private fun compileFunctionDefinition(functionDefinition: FunctionDefinitionNodeAST) {
        val functionValue = createFunction(functionDefinition)

        variableScopeStack.enterScope()

        val parametersValues = functionValue.getFunctionParametersValues()
        val parametersIdentifiers = functionDefinition.parameters.map { it.identifier }
        (parametersIdentifiers zip parametersValues).forEach { (identifier, value) ->
            variableScopeStack.addReference(identifier, value)
            LLVMSetValueName(value, identifier)
        }

        val entryBlock = LLVMAppendBasicBlockInContext(context, functionValue, "entry")
        currentBlock = entryBlock
        LLVMPositionBuilderAtEnd(builder, entryBlock)

        val returnValue = compileStatement(functionDefinition.body)
        LLVMPositionBuilderAtEnd(builder, currentBlock)

        if (functionDefinition.identifier != "_start") {
            LLVMBuildRet(builder, returnValue)
        } else {
            val exitParameters = listOf(
                returnValue
            ).toTypedArray()
            LLVMBuildCall(builder, sysExitFunction, PointerPointer<LLVMValueRef>(*exitParameters), 1, "")
            LLVMBuildUnreachable(builder)
        }

        if (LLVMVerifyFunction(functionValue, LLVMPrintMessageAction) != 0) {
            val outputPointer = LLVMPrintModuleToString(module)
            println(outputPointer.string)
            throw Exception("LLVMVerifyFunction failed")
        }

        variableScopeStack.leaveScope()
    }

    private fun compileStatement(statement: StatementASTNode): LLVMValueRef {
        return when (statement) {
            is FunctionCallASTNode -> compileFunctionCall(statement)
            is BinaryOperatorNodeAST -> compileBinaryOperator(statement)
            is ConstantValueNodeAST -> compileConstantValue(statement)
            is IfConditionNodeAST -> compileIfCondition(statement)
            is CodeBlockASTNode -> compileCodeBlock(statement)
            is ConstantStringASTNode -> compileConstantStringValue(statement)
            is ReferenceASTNode -> variableScopeStack.getReference(statement.identifier)
            else -> throw Exception("Unexpected statement: $statement")
        }
    }

    private fun compileFunctionCall(functionCall: FunctionCallASTNode): LLVMValueRef {
        val identifier = if (functionCall.identifier == "printf") ".kek.builtin.print" else functionCall.identifier
        val functionValue = LLVMGetNamedFunction(module, identifier)
            ?: throw Exception("Not found function: ${functionCall.identifier}")

        val parameters = functionCall.parameters.map { compileStatement(it) }.toTypedArray()

        return LLVMBuildCall(
            builder, functionValue,
            PointerPointer<LLVMValueRef>(*parameters), parameters.size,
            if (functionCall.identifier == "printf") "" else functionCall.identifier + "_call"
        )
    }

    private fun compileBinaryOperator(binaryOperator: BinaryOperatorNodeAST): LLVMValueRef {
        val lhsValue = compileStatement(binaryOperator.lhs)
        val rhsValue = compileStatement(binaryOperator.rhs)

        return when (binaryOperator.identifier) {
            "+" -> LLVMBuildAdd(builder, lhsValue, rhsValue, "add")
            "-" -> LLVMBuildSub(builder, lhsValue, rhsValue, "sub")
            "*" -> LLVMBuildMul(builder, lhsValue, rhsValue, "mul")
            "/" -> LLVMBuildFDiv(builder, lhsValue, rhsValue, "div")
            "==" -> LLVMBuildICmp(builder, LLVMIntEQ, lhsValue, rhsValue, "cmp")
            else -> throw Exception("Unknown operator: ${binaryOperator.identifier}")
        }
    }

    private fun compileConstantValue(constantValue: ConstantValueNodeAST): LLVMValueRef {
        return when (constantValue) {
            is DecimalConstantValueNodeAST -> LLVMConstReal(
                LLVMDoubleTypeInContext(context),
                constantValue.value.toDouble()
            )
            is IntegerConstantASTNode -> LLVMConstInt(
                LLVMInt32TypeInContext(context),
                constantValue.value.toLong(),
                1
            )
            else -> throw Exception("Unexpected statement: $constantValue")
        }
    }

    private fun compileConstantStringValue(constantStringNodeAST: ConstantStringASTNode): LLVMValueRef {
        val hash = "%02x".format(constantStringNodeAST.string.hashCode())
        return LLVMBuildGlobalStringPtr(builder, constantStringNodeAST.string, ".str.$hash")
    }

    private fun compileIfCondition(ifConditionNodeAST: IfConditionNodeAST): LLVMValueRef {
        val conditionValue = compileStatement(ifConditionNodeAST.condition)

        val ifTrueBlock = LLVMCreateBasicBlockInContext(context, "ifTrue")

        val rawCurrentBlockName = LLVMGetBasicBlockName(currentBlock!!)
        val simpleCurrentBlockNAme = rawCurrentBlockName.string.trimEnd { it.isDigit() }
        val ifAfterBlock = LLVMCreateBasicBlockInContext(context, simpleCurrentBlockNAme)

        val ifBlockValue = LLVMBuildCondBr(builder, conditionValue, ifTrueBlock, ifAfterBlock)

        LLVMInsertExistingBasicBlockAfterInsertBlock(builder, ifTrueBlock)
        LLVMPositionBuilderAtEnd(builder, ifTrueBlock)
        val ifTrueValue = compileStatement(ifConditionNodeAST.body) // TODO: Do anything with result of if?
        LLVMBuildBr(builder, ifAfterBlock)

        LLVMInsertExistingBasicBlockAfterInsertBlock(builder, ifAfterBlock)
        LLVMPositionBuilderAtEnd(builder, ifAfterBlock)
        currentBlock = ifAfterBlock

        return ifBlockValue
    }

    private fun compileCodeBlock(codeBlockNodeAST: CodeBlockASTNode): LLVMValueRef {
        var lastValue: LLVMValueRef? = null
        codeBlockNodeAST.statements.forEach { statement ->
            lastValue = compileStatement(statement)
        }
        return lastValue ?: LLVMBuildUnreachable(builder)
    }

    private fun createFunction(functionDefinition: FunctionDefinitionNodeAST): LLVMValueRef {
        val returnType = LLVMInt32TypeInContext(context)
        val parameters = functionDefinition.parameters.map {
            LLVMInt32TypeInContext(context)
        }.toTypedArray()

        val functionType =
            LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 0)
        return LLVMAddFunction(module, functionDefinition.identifier, functionType)
    }
}