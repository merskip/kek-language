package pl.merskip.keklang

fun <T> List<T>.addingBegin(element: T): List<T> {
    val list = this.toMutableList()
    list.add(0, element)
    return list.toList()
}

fun <T> List<T>.addingEnd(element: T): List<T> {
    val list = this.toMutableList()
    list.add(element)
    return list.toList()
}