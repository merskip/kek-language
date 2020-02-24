package pl.merskip.keklang

import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.PointerPointer
import org.bytedeco.llvm.LLVM.*
import org.bytedeco.llvm.global.LLVM
import pl.merskip.keklang.node.*


class LLVMCompiler(
    moduleId: String
) {

    private val context = LLVM.LLVMContextCreate()
    val module: LLVMModuleRef = LLVM.LLVMModuleCreateWithNameInContext(moduleId, context)
    private val builder = LLVM.LLVMCreateBuilder()

    private val variableScopeStack = VariableScopeStack()

    private var exitFunction: LLVMValueRef = declareExitFunction()

    private var currentBlock: LLVMBasicBlockRef? = null

    init {
        declarePrintfFunction()
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

    private fun declareExitFunction(): LLVMValueRef {
        val exitFunction = LLVM.LLVMGetNamedFunction(module, "exit")
        if (exitFunction != null) return exitFunction

        val parameters = listOf(
            LLVM.LLVMInt32TypeInContext(context)
        ).toTypedArray()
        val returnType = LLVM.LLVMVoidTypeInContext(context)

        val functionType =
            LLVM.LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 0)

        val functionValue = LLVM.LLVMAddFunction(module, "exit", functionType)
        LLVM.LLVMSetFunctionCallConv(functionValue, LLVM.LLVMCCallConv)
        return functionValue
    }

    private fun declarePrintfFunction(): LLVMValueRef {
        val printfFunction = LLVM.LLVMGetNamedFunction(module, "printf")
        if (printfFunction != null) return printfFunction

        val parameters = listOf(
            LLVM.LLVMPointerType(LLVM.LLVMInt8Type(), 0)
        ).toTypedArray()
        val returnType = LLVM.LLVMInt32TypeInContext(context)

        val functionType =
            LLVM.LLVMFunctionType(returnType, PointerPointer<LLVMTypeRef>(*parameters), parameters.size, 1)

        val functionValue = LLVM.LLVMAddFunction(module, "printf", functionType)
        LLVM.LLVMSetFunctionCallConv(functionValue, LLVM.LLVMCCallConv)
        return functionValue
    }

    private fun compileFunctionDefinition(functionDefinition: FunctionDefinitionNodeAST) {
        val functionValue = createFunction(functionDefinition)

        variableScopeStack.enterScope()

        val parametersValues = FunctionGetParams(functionValue)
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
            LLVM.LLVMBuildCall(builder, exitFunction, PointerPointer<LLVMValueRef>(*exitParameters), 1, "")
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
        val functionValue = LLVM.LLVMGetNamedFunction(module, functionCall.identifier)
            ?: throw Exception("Not found function: ${functionCall.identifier}")

        val parameters = functionCall.parameters.map { compileStatement(it) }.toTypedArray()

        return LLVM.LLVMBuildCall(
            builder, functionValue,
            PointerPointer<LLVMValueRef>(*parameters), parameters.size,
            functionCall.identifier + "_call"
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