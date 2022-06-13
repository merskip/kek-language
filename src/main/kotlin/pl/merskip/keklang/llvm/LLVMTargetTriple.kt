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
            var index = 0
            try {
                val arch = findValueWithIgnoreCase<ArchType>(chunks.getOrNull(index))
                if (arch != null || chunks.getOrNull(index) == "unknown") index += 1

                val vendor = findValueWithIgnoreCase<VendorType>(chunks.getOrNull(index))
                if (vendor != null || chunks.getOrNull(index) == "unknown") index += 1

                val operatingSystem = findValueWithIgnoreCase<OperatingSystem>(chunks.getOrNull(index))
                if (operatingSystem != null || chunks.getOrNull(index) == "unknown") index += 1

                val environment = findValueWithIgnoreCase<EnvironmentType>(chunks.getOrNull(index))

                return LLVMTargetTriple(arch, vendor, operatingSystem, environment)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed parse \"$string\" into target triple", e)
            }
        }

        private inline fun <reified E : Enum<E>> findValueWithIgnoreCase(value: String?): E? =
            enumValues<E>().firstOrNull { it.name.equals(value, true) }

    }
}