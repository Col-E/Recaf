package me.coley.recaf.util;

/**
 * Special type of {@link Error} that signals
 * that further execution must be stpooed.
 *
 * @author xDark
 */
public final class CancelSignal extends Error {

    private static final CancelSignal INSTANCE = new CancelSignal();

    /**
     * Deny all constructions.
     */
    private CancelSignal() {}

    /**
     * @return a cancellation signal.
     */
    public static CancelSignal get() {
        return INSTANCE;
    }
}
