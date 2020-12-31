package pl.merskip.keklang.compiler

import pl.merskip.keklang.ast.node.*
import pl.merskip.keklang.compiler.llvm.toReference
import pl.merskip.keklang.llvm.DebugInformationBuilder
import pl.merskip.keklang.llvm.LLVMValue
import pl.merskip.keklang.llvm.enum.EmissionKind
import pl.merskip.keklang.llvm.enum.SourceLanguage
import pl.merskip.keklang.llvm.LLVMFileMetadata as DebugFile

class Compiler(
    private val irCompiler: IRCompiler,
    private val debugBuilder: DebugInformationBuilder,
    private val typesRegister: TypesRegister
) {

    val module = irCompiler.module

    private val scopesStack = ScopesStack()
    private lateinit var builtinTypes: Builtin // placeholdere
    lateinit var debugFile: DebugFile

    init {
//        builtinTypes.registerFor(irCompiler.target)
    }

    fun compile(fileASTNode: FileASTNode) {

        val sourceFile = fileASTNode.sourceLocation.file
        debugFile = debugBuilder.createFile(sourceFile.name, ".")

        debugBuilder.createCompileUnit(
            sourceLanguage = SourceLanguage.C,
            file = debugFile,
            producer = "KeK Language Compiler",
            isOptimized = true,
            flags = "",
            runtimeVersion = 0,
            splitName = null,
            emissionKind = EmissionKind.Full,
            DWOId = 1,
            splitDebugInlining = true,
            debugInfoForProfiling = true
        )

        registerAllFunctions(fileASTNode)
        fileASTNode.nodes.forEach {
            compileFunctionBody(it)
        }

        debugBuilder.finalize()
        irCompiler.verifyModule()
    }

    private fun registerAllFunctions(fileASTNode: FileASTNode) {
//        fileASTNode.nodes.forEach { functionNode ->
//            val simpleIdentifier = functionNode.identifier
//            val parameters = functionNode.getParameters()
//            val returnType = functionNode.returnType?.identifier?.let { typesRegister.findType(it) } ?: builtinTypes.voidType
//            val identifier = TypeIdentifier.create(simpleIdentifier, parameters.map { it.type })
//
//            val (typeRef, valueRef) = irCompiler.declareFunction(identifier.uniqueIdentifier, parameters, returnType)
//            val function = Function(identifier, parameters, returnType, LLVMType.just(typeRef), valueRef)
//
//            if (function.identifier.uniqueIdentifier == "main") {
//                createEntryProgram(function)
//            }
//            typesRegister.register(function)-
//        }
    }

    private fun createEntryProgram(mainFunction: Function) {
//        FunctionBuilder.register(typesRegister, irCompiler) {
//            noOverload(true)
//            simpleIdentifier("_kek_start")
//            parameters()
//            returnType(builtInTypes.voidType)
//            implementation { irCompiler, _ ->
//
//                // Call main()
//                val returnCode = irCompiler.createCallFunction(mainFunction, emptyList())
//
//                // Call System.exit(Integer)
//                val systemExit = typesRegister.findFunction(
//                    calleeType = builtInTypes.systemType,
//                    simpleIdentifier = BuiltInTypes.EXIT_FUNCTION,
//                    parameters = listOf(builtInTypes.integerType)
//                )
//                irCompiler.createCallFunction(systemExit, listOf(returnCode))
//                irCompiler.createUnreachable()
//            }
//        }
    }

    private fun compileFunctionBody(nodeAST: FunctionDefinitionNodeAST) {
//        val parameters = nodeAST.getParameters()
//        val identifier = TypeIdentifier.create(nodeAST.identifier, parameters.map { it.type })
//        val function = typesRegister.findFunction(identifier)
//
//        scopesStack.createScope {
//            val functionParametersValues = function.value.getFunctionParametersValues()
//            (function.parameters zip functionParametersValues).forEach { (parameter, value) ->
//                scopesStack.current.addReference(parameter.name, parameter.type, LLVMValue.just(value))
//            }
//
//            val debugParameters = parameters.map { parameter ->
//                val sizeInBits = LLVM.LLVMGetIntTypeWidth(parameter.type.type.reference)
//                debugBuilder.createBasicType(parameter.name, sizeInBits.toLong(), Encoding.Signed, 0)
//            }
//            val debugSubroutineType = debugBuilder.createSubroutineType(debugFile, debugParameters, 0)
//            val debugFunction = debugBuilder.createFunction(
//                scope = debugFile,
//                name = nodeAST.identifier,
//                linkageName = null,
//                file = debugFile,
//                type = debugSubroutineType,
//                lineNumber = nodeAST.sourceLocation.startIndex.line,
//                isLocalToUnit = true,
//                isDefinition = true,
//                scopeLine = nodeAST.sourceLocation.startIndex.line,
//                flags = 0,
//                isOptimized = true
//            )
//            scopesStack.current.debugScope = debugFunction
//            irCompiler.setFunctionDebugSubprogram(function, debugFunction.reference)
//
//            val entryBlock = irCompiler.beginFunctionEntry(function)
//
//            parameters.zip(debugParameters).withIndex().forEach {
//                val (parameter, debugParameter) = it.value
//                val debugVariable = debugBuilder.createParameterVariable(
//                    scopesStack.current.debugScope!!,
//                    parameter.name,
//                    it.index,
//                    debugFile,
//                    nodeAST.sourceLocation.startIndex.line,
//                    debugParameter,
//                    true,
//                    0
//                )
//                val parameterReference = scopesStack.current.getReferenceOrNull(parameter.name)!!
//                val parameterAlloca = irCompiler.createAlloca(parameter.name + "_alloca", parameter.type.type.reference)
//                irCompiler.createStore(parameterAlloca, parameterReference.value.reference)
//
//                debugBuilder.insertDeclareAtEnd(
//                    parameterAlloca,
//                    debugVariable,
//                    debugBuilder.createExpression(),
//                    debugBuilder.createDebugLocation(
//                        nodeAST.sourceLocation.startIndex.line,
//                        nodeAST.sourceLocation.startIndex.column,
//                        scopesStack.current.debugScope!!
//                    ),
//                    entryBlock
//                )
//            }
//
//            val returnValue = compileStatement(nodeAST.body)
//
//            if (!function.returnType.isCompatibleWith(returnValue.type))
//                throw Exception("Mismatch types. Expected return ${function.returnType.identifier}, but got ${returnValue.type.identifier}")
//
//            irCompiler.createReturnValue(returnValue.value.reference)
//        }
    }

    private fun FunctionDefinitionNodeAST.getParameters(): List<Function.Parameter> =
        parameters.map {
            val type = typesRegister.findType(it.type.identifier)
            Function.Parameter(it.identifier, type)
        }

    private fun compileStatement(statement: StatementASTNode): Reference {
        irCompiler.setCurrentDebugLocation(
            debugBuilder.createDebugLocation(
                statement.sourceLocation.startIndex.line,
                statement.sourceLocation.startIndex.column,
                scopesStack.current.debugScope!!
            ).reference
        )
        return when (statement) {
            is ReferenceASTNode -> scopesStack.current.getReferenceOrNull(statement.identifier)!!
            is CodeBlockASTNode -> compileCodeBlockAndGetLastValue(statement)
            is ConstantValueNodeAST -> compileConstantValue(statement)
            is BinaryOperatorNodeAST -> compileBinaryOperator(statement)
            is StaticFunctionCallASTNode -> compileCallTypeFunction(statement)
            is FunctionCallASTNode -> compileCallFunction(statement)
            is IfElseConditionNodeAST -> compileIfElseCondition(statement)
            is ConstantStringASTNode -> compileConstantString(statement)
            else -> throw Exception("TODO: $statement")
        }
    }

    private fun compileCodeBlockAndGetLastValue(nodeAST: CodeBlockASTNode): Reference {
        var lastValue: Reference? = null
        nodeAST.statements.forEach { statement ->
            lastValue = compileStatement(statement)
        }
        return lastValue ?: error("No last value")
    }

    private fun compileConstantValue(nodeAST: ConstantValueNodeAST): Reference {
        return when (nodeAST) {
            is IntegerConstantASTNode -> {
                val type = builtinTypes.integerType
                irCompiler.createConstantIntegerValue(nodeAST.value, type)
                    .toReference(type)
            }
            else -> throw Exception("TODO: $nodeAST")
        }
    }

    private fun compileBinaryOperator(nodeAST: BinaryOperatorNodeAST): Reference {
        val lhs = compileStatement(nodeAST.lhs)
        val rhs = compileStatement(nodeAST.rhs)

        val toIdentifier = { simpleIdentifier: String ->
//            TypeIdentifier.create(simpleIdentifier, listOf(rhs.type), lhs.type)
        }

        val invokeFunction = when (nodeAST.identifier) {
//            "+" -> typesRegister.findFunction(toIdentifier(BuiltinTypes.ADD_FUNCTION))
//            "-" -> typesRegister.findFunction(toIdentifier(BuiltinTypes.SUBTRACT_FUNCTION))
//            "*" -> typesRegister.findFunction(toIdentifier(BuiltinTypes.MULTIPLE_FUNCTION))
//            "==" -> typesRegister.findFunction(toIdentifier(BuiltinTypes.IS_EQUAL_TO_FUNCTION))
            else -> throw Exception("Unknown operator: ${nodeAST.identifier}")
        }
        return compileCallFunction(invokeFunction, listOf(lhs, rhs))
    }

    private fun compileCallTypeFunction(nodeAST: StaticFunctionCallASTNode): Reference {
        val arguments = nodeAST.parameters.map { compileStatement(it) }
        val argumentsTypes = arguments.map { it.type }

//        val type = typesRegister.findType(nodeAST.typeIdentifier)
//        val function = typesRegister.findFunction(type, nodeAST.functionIdentifier, argumentsTypes)
//        return compileCallFunction(function, arguments)
        error("")
    }

    private fun compileCallFunction(nodeAST: FunctionCallASTNode): Reference {
        val arguments = nodeAST.parameters.map { compileStatement(it) }
        val argumentsTypes = arguments.map { it.type }

//        val function = typesRegister.findFunction(TypeIdentifier.function(null, nodeAST.identifier, argumentsTypes, Type.))
//        return compileCallFunction(function, arguments)
        error("")
    }

    private fun compileCallFunction(function: Function, arguments: List<Reference>): Reference {
        if (function.parameters.size != arguments.size)
            throw Exception("Mismatch numbers of parameters. Expected ${function.parameters.size}, but got ${arguments.size}")
        (function.parameters zip arguments).forEach { (functionParameter, passedArgument) ->
            if (!functionParameter.type.isCompatibleWith(passedArgument.type))
                throw Exception("Mismatch types. Expected parameter ${functionParameter.type.identifier}, but got ${passedArgument.type.identifier}")
        }

        val returnValueRef = irCompiler.createCallFunction(function, arguments.map { it.value.reference })
        return returnValueRef.toReference(function.returnType)
    }

    private fun compileIfElseCondition(node: IfElseConditionNodeAST): Reference {
        irCompiler.createIfElse(
            conditions = node.ifConditions,
            ifCondition = { compileCondition(it).value.reference },
            ifTrue = { compileStatement(it.body) },
            ifElse = if (node.elseBlock != null) {
                fun() { compileStatement(node.elseBlock) }
            } else null
        )
        return Reference(null, builtinTypes.voidType, LLVMValue.empty())
    }

    private fun compileCondition(node: IfConditionNodeAST): Reference {
        val condition = compileStatement(node.condition)
        if (!condition.type.isCompatibleWith(builtinTypes.booleanType))
            throw Exception("Conditional expression must be logic expression")
        return condition
    }

    private fun compileConstantString(node: ConstantStringASTNode): Reference {
        val stringValueRef = irCompiler.createString(node.string)
        return stringValueRef.toReference(builtinTypes.stringType, "string")
    }
}