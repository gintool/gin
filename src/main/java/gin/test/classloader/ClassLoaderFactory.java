package gin.test.classloader;

/**
 * Class that builds {@code ClassLoader} objects using the factory method
 * {@link #createClassLoader(java.lang.String) createClassLoader}.
 *
 * @author Giovani
 */
public class ClassLoaderFactory {

    /**
     * ClassLoader to build.
     */
    private final GinClassLoader classLoader;

    /**
     * Gets the enum representing the {@code ClassLoader} to be built by the
     * factory method
     * {@link #createClassLoader(java.lang.String) createClassLoader}.
     *
     * @return the enum representing the {@code ClassLoader} that is built by
     *         this factory object
     */
    public GinClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Constructs the factory object. The factory object will only create
     * {@code ClassLoaders} based on the given type.
     *
     * @param classLoader the type of {@code ClassLoader} to build. See
     *                    {@link GinClassLoader} for available types
     */
    public ClassLoaderFactory(GinClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Creates the default ClassLoaderFactory object for gin. A factory object
     * created with this method will only create objects with
     * {@link GinClassLoader.CACHE_CLASSLOADER}. See
     * {@link GinClassLoader} for available types.
     *
     * @return the default gin's {@link ClassLoader}
     */
    public static ClassLoaderFactory createDefaultGinClassLoader() {
        return new ClassLoaderFactory(GinClassLoader.CACHE_CLASSLOADER);
    }

    /**
     * Creates the {@code ClassLoader} given as argument during the
     * instantiation of the factory itself. This is the main factory method.
     *
     * @param classpath a : separated ClassPath to append to the system's
     *                  ClassPath
     * @return a {@link ClassLoader} used by gin
     */
    public CacheClassLoader createClassLoader(String classpath) {
        switch (classLoader) {
            case CACHE_CLASSLOADER:
                return new CacheClassLoader(classpath);
        }
        return null;
    }

}
