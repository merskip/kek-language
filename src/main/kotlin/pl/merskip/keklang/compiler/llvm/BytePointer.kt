package pl.merskip.keklang.compiler.llvm

import org.bytedeco.javacpp.BytePointer
import pl.merskip.keklang.compiler.TargetTriple
import pl.merskip.keklang.compiler.TargetTriple.EnvironmentType

fun BytePointer.getTargetTriple(): TargetTriple {
    val chunks = string.split("-")
    try {
        return TargetTriple(
            findValueWithIgnoreCase(chunks[0]),
            findValueWithIgnoreCase(chunks[1]),
            findValueWithIgnoreCase(chunks[2]),
            chunks.getOrNull(3)?.let { findValueWithIgnoreCase<EnvironmentType>(it) }
        )
    } catch (e: Exception) {
        throw RuntimeException("Failed parse \"$string\" into target triple", e)
    }
}

private inline fun <reified E: Enum<E>> findValueWithIgnoreCase(value: String): E =
    enumValues<E>().firstOrNull { it.name.equals(value, true) }
        ?: throw Exception("Not found value '$value' in '${E::class.simpleName}'")
