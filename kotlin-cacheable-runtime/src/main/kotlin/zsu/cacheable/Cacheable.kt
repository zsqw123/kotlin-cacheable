package zsu.cacheable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Cacheable(val cacheMode: CacheMode = CacheMode.SYNCHRONIZED)

enum class CacheMode {
    /**
     * Lock (containing class) is used to ensure that only a single thread can initialize the instance.
     */
    SYNCHRONIZED,

    /**
     * No locks are used to synchronize an access to the instance value;
     * if the instance is accessed from multiple threads, its behavior is undefined.
     */
    NONE,
}

