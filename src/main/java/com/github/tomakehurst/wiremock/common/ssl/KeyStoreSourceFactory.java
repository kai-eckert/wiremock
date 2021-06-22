package com.github.tomakehurst.wiremock.common.ssl;

import com.github.tomakehurst.wiremock.common.Exceptions;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class KeyStoreSourceFactory {

    @SuppressWarnings("unchecked")
    public static KeyStoreSource getAppropriateForJreVersion(String path, String keyStoreType, char[] keyStorePassword) {
        try {
            final Class<?extends KeyStoreSource> theClass = (Class<? extends KeyStoreSource>) Class.forName("com.github.tomakehurst.wiremock.jetty94.WritableFileOrClasspathKeyStoreSource");
            return safelyGetConstructor(theClass, String.class, String.class, char[].class).newInstance(path, keyStoreType, keyStorePassword);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            return new ReadOnlyFileOrClasspathKeyStoreSource(path, keyStoreType, keyStorePassword);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> safelyGetConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            return Exceptions.throwUnchecked(e, Constructor.class);
        }
    }
}
