package zsu.cacheable

// stub for cacheable runtime
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Cacheable(val cacheMode: CacheMode = CacheMode.SYNCHRONIZED)

enum class CacheMode {
    SYNCHRONIZED,
    NONE,
    COMPILE_TIME,
}
