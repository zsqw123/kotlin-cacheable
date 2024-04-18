package zsu.cacheable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class Cacheable(val cacheMode: CacheMode = CacheMode.TRACK_ARGS_SYNCHRONIZED)

enum class CacheMode {
    /**
     * No locks are used to synchronize an access to the instance value;
     * if the instance is accessed from multiple threads, its behavior is undefined.
     */
    NONE,

    /**
     * Lock(containing class) is used to ensure that only a single thread can initialize the instance.
     */
    SYNCHRONIZED,

    /**
     * track argument changes(comparing them using `equals` method), rebuild cache if there are any changes produced.
     */
    TRACK_ARGS,

    /**
     * [TRACK_ARGS], but compare operations will run in synchronized block.
     * @see [TRACK_ARGS]
     * @see [SYNCHRONIZED]
     */
    TRACK_ARGS_SYNCHRONIZED,
}

