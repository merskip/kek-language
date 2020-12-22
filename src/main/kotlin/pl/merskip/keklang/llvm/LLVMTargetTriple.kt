package pl.merskip.keklang.llvm

import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.EnvironmentType
import pl.merskip.keklang.llvm.enum.OperatingSystem
import pl.merskip.keklang.llvm.enum.VendorType

data class LLVMTargetTriple(
    val archType: ArchType?,
    val vendor: VendorType?,
    val operatingSystem: OperatingSystem?,
    val environment: EnvironmentType?
) {

    override fun toString(): String {
        return listOfNotNull(archType, vendor, operatingSystem, environment).joinToString("-") { it.name }.toLowerCase()
    }

    companion object {
        fun fromString(string: String): LLVMTargetTriple {
            val chunks = string.split("-")
            try {
                return LLVMTargetTriple(
                    findValueWithIgnoreCase<ArchType>(chunks[0]),
                    findValueWithIgnoreCase<VendorType>(chunks[1]),
                    findValueWithIgnoreCase<OperatingSystem>(chunks[2]),
                    findValueWithIgnoreCase<EnvironmentType>(chunks.getOrNull(3))
                )
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed parse \"$string\" into target triple", e)
            }
        }

        private inline fun <reified E : Enum<E>> findValueWithIgnoreCase(value: String?): E? =
            enumValues<E>().firstOrNull { it.name.equals(value, true) }

    }
}