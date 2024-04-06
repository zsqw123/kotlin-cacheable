package zsu.cacheable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Cacheable(val cacheMode: CacheMode = CacheMode.SYNCHRONIZED)

/** refer from [kotlin.LazyThreadSafetyMode] */
enum class CacheMode {
    /**
     * Lock (containing class) is used to ensure that only a single thread can initialize the instance.
     */
    SYNCHRONIZED,

    /**
     * No locks are used to synchronize an access to the instance value; if the instance is accessed from multiple threads, its behavior is undefined.
     *
     * This mode should not be used unless the [Lazy] instance is guaranteed never to be initialized from more than one thread.
     */
    NONE,

    /**
     * Calculate value during compile time. only available for static functions with no arguments,
     * e.g. [JvmStatic] marked functions or top-level functions.
     */
    COMPILE_TIME,
}

