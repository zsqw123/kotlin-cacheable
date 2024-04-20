package host.bytedance

import zsu.cacheable.Cacheable

class Sample {
    private var foo: Int = 0

    @Cacheable
    fun call() = ++foo
}

fun main() {
    val sample = Sample()
    assertAndPrint(1, sample.call())
    assertAndPrint(1, sample.call())
    assertAndPrint(1, sample.call())
}

