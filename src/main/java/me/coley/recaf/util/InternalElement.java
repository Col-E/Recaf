package me.coley.recaf.util;

/**
 * Mark an object as internal.
 *
 * @author xxDark
 */
public interface InternalElement {

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
