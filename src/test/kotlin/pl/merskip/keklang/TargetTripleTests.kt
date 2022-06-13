package pl.merskip.keklang

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import pl.merskip.keklang.llvm.LLVMTargetTriple
import pl.merskip.keklang.llvm.enum.ArchType
import pl.merskip.keklang.llvm.enum.EnvironmentType
import pl.merskip.keklang.llvm.enum.OperatingSystem
import pl.merskip.keklang.llvm.enum.VendorType

class TargetTripleTests {

    @Test
    fun `parse full string`() {
        val targetTriple = LLVMTargetTriple.from("x86_64-pc-linux-gnu")

        assertEquals(ArchType.X86_64, targetTriple.archType)
        assertEquals(VendorType.PC, targetTriple.vendor)
        assertEquals(OperatingSystem.Linux, targetTriple.operatingSystem)
        assertEquals(EnvironmentType.GNU, targetTriple.environment)
    }

    @Test
    fun `parse without environment`() {
        val targetTriple = LLVMTargetTriple.from("x86_64-pc-linux")

        assertEquals(ArchType.X86_64, targetTriple.archType)
        assertEquals(VendorType.PC, targetTriple.vendor)
        assertEquals(OperatingSystem.Linux, targetTriple.operatingSystem)
        assertEquals(null, targetTriple.environment)
    }

    @Test
    fun `parse without vendor`() {
        val targetTriple = LLVMTargetTriple.from("x86_64-linux-gnu")

        assertEquals(ArchType.X86_64, targetTriple.archType)
        assertEquals(null, targetTriple.vendor)
        assertEquals(OperatingSystem.Linux, targetTriple.operatingSystem)
        assertEquals(EnvironmentType.GNU, targetTriple.environment)
    }

    @Test
    fun `parse without vendor and environment`() {
        val targetTriple = LLVMTargetTriple.from("x86_64-linux")

        assertEquals(ArchType.X86_64, targetTriple.archType)
        assertEquals(null, targetTriple.vendor)
        assertEquals(OperatingSystem.Linux, targetTriple.operatingSystem)
        assertEquals(null, targetTriple.environment)
    }

    @Test
    fun `parse full with unknown vendor`() {
        val targetTriple = LLVMTargetTriple.from("x86_64-unknown-linux-gnu")

        assertEquals(ArchType.X86_64, targetTriple.archType)
        assertEquals(null, targetTriple.vendor)
        assertEquals(OperatingSystem.Linux, targetTriple.operatingSystem)
        assertEquals(EnvironmentType.GNU, targetTriple.environment)
    }
}