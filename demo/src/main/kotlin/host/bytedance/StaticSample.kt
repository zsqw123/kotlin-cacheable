package host.bytedance

import zsu.cacheable.Cacheable

private var foo: Int = 0

object StaticSample {
    @Cacheable
    fun staticCall(a: Int, s: String) = ++foo
}

fun main() {
    assertAndPrint(1, StaticSample.staticCall(1, "s"))
    assertAndPrint(1, StaticSample.staticCall(1, "s"))
    assertAndPrint(2, StaticSample.staticCall(1, "x"))
    assertAndPrint(3, StaticSample.staticCall(2, "x"))
    assertAndPrint(3, StaticSample.staticCall(2, "x"))
}
