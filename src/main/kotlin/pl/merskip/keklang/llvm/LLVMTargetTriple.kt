package pl.merskip.keklang.llvm

import org.bytedeco.llvm.global.LLVM
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

    fun isMatch(archType: ArchType? = null,
                vendor: VendorType? = null,
                operatingSystem: OperatingSystem? = null,
                environment: EnvironmentType? = null
    ): Boolean {
        if (archType != null && this.archType != archType)
            return false
        if (vendor != null && this.vendor != vendor)
            return false
        if (operatingSystem != null && this.operatingSystem != operatingSystem)
            return false
        if (environment != null && this.environment != operatingSystem)
            return false
        return true
    }

    fun toSimpleString(): String =
        listOfNotNull(operatingSystem, archType).joinToString("-") { it.name }.toLowerCase()

    override fun toString(): String {
        return listOfNotNull(archType, vendor, operatingSystem, environment).joinToString("-") { it.name }.toLowerCase()
    }

    companion object {
        fun host(): LLVMTargetTriple {
            val targetTriple = LLVM.LLVMGetDefaultTargetTriple().disposable.string
            return from(targetTriple)
        }

        fun from(string: String): LLVMTargetTriple {
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