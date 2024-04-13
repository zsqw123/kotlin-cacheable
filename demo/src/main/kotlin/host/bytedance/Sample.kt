package host.bytedance

import zsu.cacheable.Cacheable

class Sample {
    private var foo: Int = 0

    @Cacheable
    fun call() = ++foo
}

fun main() {
    val sample = Sample()
    println(sample.call())
    println(sample.call())
    println(sample.call())
}
