package me.coley.recaf.util;

import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Stream utilities.
 *
 * @author xDark
 */
public final class Streams {

	private Streams() {
	}

	public static <T> void forEachOn(Stream<T> stream, Consumer<? super T> c, Executor executor) {
		Phaser phaser = new Phaser(1);
		AtomicReference<Throwable> throwable = new AtomicReference<>();
		stream.forEach(x -> {
			if (throwable.get() != null) {
				return;
			}
			phaser.register();
			executor.execute(() -> {
				try {
					if (throwable.get() == null) {
						try {
							c.accept(x);
						} catch(Throwable t) {
							throwable.compareAndSet(null, t);
						}
					}
				} finally {
					phaser.arriveAndDeregister();
				}
			});
		});
		phaser.arriveAndAwaitAdvance();
		Throwable thrown = throwable.get();
		if (thrown != null) {
			ReflectUtil.propagate(thrown);
		}
	}
}
