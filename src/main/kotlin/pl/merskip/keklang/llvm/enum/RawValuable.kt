package pl.merskip.keklang.llvm.enum

interface RawValuable<V> {

    val rawValue: V


    companion object {

        inline fun <reified T, V> fromRawValue(rawValue: V): T
                where T : Enum<T>,
                      T : RawValuable<V>{
            return enumValues<T>().firstOrNull { it.rawValue == rawValue }
                ?: throw Exception("Not found rawValue=$rawValue in class ${T::class}")
        }
    }
}