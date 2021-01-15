


class GrammarGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.task("generate-grammar") {
            var source = ""
            val mainSource = project.the<SourceSetContainer>()["main"]
            val files = mainSource.allSource.files.toList()
            for (file in files) {
                for (line in file.readLines()) {
                    if (line.contains("::=")) {
                        val ebnf = line.trim().removePrefix("* ")
                        source += ebnf + "\n"
                    }
                }
            }
            project.file("grammar.bnf").writeText(source)
        }
    }

}

apply<GrammarGeneratorPlugin>()
