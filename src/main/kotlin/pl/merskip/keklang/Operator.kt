package pl.merskip.keklang

data class Operator(
    val identifier: String,
    val precedence: Int
) {

    init {
        assert(precedence > 0)
    }
}