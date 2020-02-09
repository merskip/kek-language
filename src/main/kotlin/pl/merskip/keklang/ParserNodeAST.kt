package pl.merskip.keklang

import pl.merskip.keklang.node.*
import kotlin.Exception

class ParserNodeAST(
    tokens: List<Token>
) {

    private val tokensIter = tokens.listIterator()

    fun parse(): FileNodeAST {
        val functions = mutableListOf<FunctionDefinitionNodeAST>()
        while (true) {
            val node = parseNextTokenOrNull() ?: break
            if (node !is FunctionDefinitionNodeAST)
                throw Exception("Expected function definition at global scope")

            functions.add(node)
        }
        return FileNodeAST(functions.toList())
    }

    private fun parseNextToken(): NodeAST =
        parseNextTokenOrNull() ?: throw Exception("Unexpected end of file")

    private fun parseNextTokenOrNull(): NodeAST? {
        val token = expectNextTokenOrNull<Token>()
            ?: return null
        return when (token) {
            is Token.Func -> parseFunctionDefinition(token)
            is Token.Identifier -> parseReferenceOrFunctionCall(token)
            else -> throw Exception("Unexpected token: $token")
        }
    }

    private fun parseFunctionDefinition(token: Token.Func): FunctionDefinitionNodeAST {
        val identifierToken = getNextToken<Token.Identifier>()
        getNextToken<Token.LeftParenthesis>()
        // TODO: Parse parameter list
        getNextToken<Token.RightParenthesis>()

        val codeBlock = parseCodeBlock()

        return FunctionDefinitionNodeAST(identifierToken.text, emptyList(), codeBlock)
    }

    private fun parseCodeBlock(): CodeBlockNodeAST {
        getNextToken<Token.LeftBracket>()
        val statements = mutableListOf<StatementNodeAST>()
        while (true) {
            if (isNextToken<Token.RightBracket>())
                break
            val node = parseNextToken()
            if (node !is StatementNodeAST)
                throw Exception("Expected statement node AST, but got ${node::class}")
            getNextToken<Token.Semicolon>()
            statements.add(node)
        }
        getNextToken<Token.RightBracket>()
        return CodeBlockNodeAST(statements.toList())
    }

    private fun parseReferenceOrFunctionCall(token: Token.Identifier): NodeAST {
        return if (getAnyNextToken() is Token.LeftParenthesis) {
            // TODO: Parse call parameter list
            getNextToken<Token.RightParenthesis>()
            FunctionCallNodeAST(token.text, emptyList())
        } else {
            previousToken()
            ReferenceNodeAST(token.text)
        }
    }

    private inline fun <reified T : Token> getNextToken(): T {
        return expectNextTokenOrNull()
            ?: throw Exception("End of file, but expected token ${T::class.simpleName}")
    }

    private inline fun <reified T : Token> expectNextTokenOrNull(): T? {
        val token = getAnyNextToken() ?: return null
        if (token is T) return token
        else throw Exception("Expected next token as type ${T::class.simpleName}, but got ${token::class.simpleName}")
    }

    private inline fun <reified T : Token> isNextToken(): Boolean {
        val nextToken = getAnyNextToken()
        val isMatched = nextToken is T
        previousToken()
        return isMatched
    }

    private fun previousToken() {
        tokensIter.previous()
    }

    private fun getAnyNextToken(): Token? {
        if (!tokensIter.hasNext()) return null
        return tokensIter.next()
    }
}