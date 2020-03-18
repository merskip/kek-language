package pl.merskip.keklang.compiler.llvm

import org.bytedeco.javacpp.BytePointer
import pl.merskip.keklang.compiler.TargetTriple
import pl.merskip.keklang.compiler.TargetTriple.*

fun BytePointer.getTargetTriple(): TargetTriple {
    val chunks = string.split("-")
    try {
        return TargetTriple(
            findValueWithIgnoreCase<ArchType>(chunks[0]),
            findValueWithIgnoreCase<VendorType>(chunks[1]),
            findValueWithIgnoreCase<OSType>(chunks[2]),
            findValueWithIgnoreCase<EnvironmentType>(chunks.getOrNull(3))
        )
    } catch (e: Exception) {
        throw RuntimeException("Failed parse \"$string\" into target triple", e)
    }
}

private inline fun <reified E: Enum<E>> findValueWithIgnoreCase(value: String?): E? =
    enumValues<E>().firstOrNull { it.name.equals(value, true) }
