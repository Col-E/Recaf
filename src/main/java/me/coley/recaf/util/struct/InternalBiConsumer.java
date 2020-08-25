package me.coley.recaf.util.struct;

import me.coley.recaf.plugin.api.InternalApi;
import me.coley.recaf.util.InternalElement;

import java.util.function.BiConsumer;

/**
 * Internal listener for Recaf purposes.
 *
 * @author xxDark
 */
@InternalApi
@FunctionalInterface
public interface InternalBiConsumer<T, U> extends BiConsumer<T, U>, InternalElement {

    /**
     * Wraps bi consumer into internal listener.
     *
     * @param consumer
     *      Original bi consumer.
     * @return
     *      BiConsumer wrapped into internal listener.
     */
    static <T, U> InternalBiConsumer<T, U> internal(BiConsumer<T, U> consumer) {
        return consumer::accept;
    }
}
