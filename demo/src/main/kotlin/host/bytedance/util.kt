package host.bytedance

fun assertAndPrint(required: Any, actual: Any) {
    println("required: $required, actual: $actual")
    assert(actual == required)
}