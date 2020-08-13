package gin.test.classloader;

/**
 * The multiple available {@link ClassLoader} in gin. Used by the factory method
 * {@link #createClassLoader(java.lang.String)} to decide which
 * {@link ClassLoader} to build.
 */
public enum GinClassLoader {
    /**
     * Default cache gin {@link ClassLoader}.
     *
     * @see CacheClassLoader
     */
    CACHE_CLASSLOADER

}
