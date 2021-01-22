class GrammarGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.task("generate-grammar") {

            val mainSource = project.the<SourceSetContainer>()["main"]
            val files = mainSource.allSource.files.toList()
            val definitions = KotlinCommentBNFSearcher().findInFiles(files)

            PlainBNFWriter().write(definitions, project.file("grammar.bnf"))
            HtmlBNFWriter().write(definitions, project.file("grammar.html"))
        }
    }
}

class PlainBNFWriter {

    fun write(definitions: List<BNFDefinition>, toFile: File) {
        val source = definitions.joinToString("\n") { it.toString() }
        toFile.writeText(source)
    }
}

class HtmlBNFWriter {

    fun write(definitions: List<BNFDefinition>, toFile: File) {
        var html = "<!doctype html>\n"
        html += """
            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta1/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-giJF6kkoqNQ00vy+HMDP7azOuL0xtbfIcaT9wjKHr8RbDVddVHyTfAAsrekwKmP1" crossorigin="anonymous">
        """.trimIndent()
        html += "<div class='container'>\n"
        html += "<h1 class='display-1 mt-3'>KeK-Language Grammar Summary</h1>\n"
        html += "<div class=\"card mt-5 mb-5\"><div class=\"card-body\">"
        for (definition in definitions) {
            html += """
                <p class='font-monospace mb-1'>
                    <span>${definition.name}</span> 
                    <span style='color: #777'>:≡</span> 
                    <span>${coloredString(definition.expression)}</span>
                </p>
            """.trimIndent()
            html += "\n"
        }
        html += "</div></div>\n"
        html += "</div>\n"
        toFile.writeText(html)
    }

    private fun coloredString(expression: String): String {
        return expression.encoded().replace(Regex("\\\"(\\\\.|[^\"\\\\])*\\\"")) {
            "<span style='color: #ff5722; font-weight: bold'>${it.value.encoded()}</span>"
        }
    }

    private fun String.encoded(): String =
        replace("<", "&lt;")
            .replace(">", "&gt;")
}

class KotlinCommentBNFSearcher {

    fun findInFiles(files: List<File>): List<BNFDefinition> {
        return files.flatMap { findInFile(it) }
    }

    private fun findInFile(file: File): List<BNFDefinition> {
        return file.readLines()
            .map { it.trim() }
            .filter { it.startsWith("*") }
            .map { it.substring(1).trim() }
            .mapNotNull { parse(it) }

    }

    private fun parse(line: String): BNFDefinition? {
        return if (line.contains("::=")) {
            val (name, expression) = line.split("::=")
            BNFDefinition(name, expression)
        } else {
            null
        }
    }
}

data class BNFDefinition(
    val name: String,
    val expression: String
) {

    override fun toString() = "$name ::= $expression"
}

apply<GrammarGeneratorPlugin>()
