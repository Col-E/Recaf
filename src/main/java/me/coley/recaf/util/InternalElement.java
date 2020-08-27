package me.coley.recaf.util;

import java.util.function.Predicate;

/**
 * Mark an object as internal.
 *
 * @author xxDark
 */
public interface InternalElement {
    Predicate<Object> INTERNAL_PREDICATE = o -> o instanceof InternalElement;
    Predicate<Object> NOT_INTERNAL_PREDICATE = INTERNAL_PREDICATE.negate();

    /**
     * Helper method to check if the object
     * is marked as internal or not.
     *
     * @param object
     *      Object to check.
     *
     * @return {@code true} if object is internal, {@code false} otherwise.
     */
    static boolean isInternal(Object object) {
        return object instanceof InternalElement;
    }
}
