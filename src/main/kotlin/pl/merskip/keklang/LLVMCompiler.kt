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
    val module = LLVM.LLVMModuleCreateWithNameInContext(moduleId, context)
    private val builder = LLVM.LLVMCreateBuilder()

    private lateinit var exitFunction: LLVMValueRef

    fun compile(fileNodeAST: FileNodeAST) {
        exitFunction = declareExitFunction()
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
        return LLVM.LLVMAddFunction(module, "exit", functionType)
    }

    private fun compileFunctionDefinition(functionDefinition: FunctionDefinitionNodeAST) {
        val functionValue = createFunction(functionDefinition)

        val entryBlock = LLVM.LLVMAppendBasicBlockInContext(context, functionValue, "entry")
        LLVM.LLVMPositionBuilderAtEnd(builder, entryBlock)

        var lastValue: LLVMValueRef? = null
        functionDefinition.codeBlockNodeAST.statements.forEach { statement ->
            lastValue = compileStatement(statement)
        }

        val returnValue = lastValue
            ?: throw Exception("Block of function is empty")

        LLVM.LLVMPositionBuilderAtEnd(builder, entryBlock)

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
    }

    private fun compileStatement(statement: StatementNodeAST): LLVMValueRef {
        return when (statement) {
            is FunctionCallNodeAST -> compileFunctionCall(statement)
            is BinaryOperatorNodeAST -> compileBinaryOperator(statement)
            is ConstantValueNodeAST -> compileConstantValue(statement)
            else -> throw Exception("Unexpected statement: $statement")
        }
    }

    private fun compileFunctionCall(functionCall: FunctionCallNodeAST): LLVMValueRef {
        val functionValue = LLVM.LLVMGetNamedFunction(module, functionCall.identifier)
            ?: throw Exception("Not found function: ${functionCall.identifier}")

        val parameters = functionCall.parameters.map { parameter ->
            when (parameter) {
                is ConstantValueNodeAST -> compileConstantValue(parameter)
                is FunctionCallNodeAST -> compileFunctionCall(parameter)
                else -> throw Exception("Unexpected statement: $parameter")
            }
        }.toTypedArray()

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