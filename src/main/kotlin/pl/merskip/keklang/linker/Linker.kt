package pl.merskip.keklang.linker

import java.io.File

interface Linker {

    fun compile(inputFiles: List<File>, entryPoint: String?, outputFile: File)
}