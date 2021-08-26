package me.coley.recaf.ui.util;

import me.coley.recaf.util.Threads;
import org.fxmisc.flowless.Virtualized;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for scrollable components such as {@link javafx.scene.control.ScrollPane} and {@link Virtualized}.
 * Most of the things in here are terrible hacks, but they're terrible hacks that improve the UX.
 *
 * @author Matt Coley
 */
public class ScrollUtils {
	/**
	 * Queue up a thread that forces the virtualized container to scroll to the given Y position.
	 * Will attempt to scroll to the position multiple times over a short period in case the content of the
	 * virtualized container is not yet ready at the time of calling this method.
	 *
	 * @param virtualized
	 * 		Container to update scroll position of.
	 * @param targetY
	 * 		Position to scroll to.
	 */
	public static void forceScroll(Virtualized virtualized, double targetY) {
		Threads.run(() -> {
			// Limit the number of tries, ensuring we don't keep the thread around forever if it isn't possible
			// to scroll to the target position.
			int tries = 0;
			int maxTries = 10;
			while (tries < maxTries && virtualized.getEstimatedScrollY() != targetY) {
				// Use a latch so we don't create a back-up when queueing up FX threads.
				CountDownLatch latch = new CountDownLatch(1);
				Threads.runFx(() -> {
					virtualized.estimatedScrollYProperty().setValue(targetY);
					latch.countDown();
				});
				tries++;
				// Wait on the latch to complete, or continue anyways after a few milliseconds and try again.
				// Should give the app time to update the estimated Y property so we don't loop more times than
				// we need to.
				try {
					latch.await(15, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					// Ignore
				}
			}
			// If we've timed out, check some edge cases.
			//
			// Case 1: We can't scroll to the target position because it is beyond the allowed bounds
			if (virtualized.totalHeightEstimateProperty() != null && tries == maxTries
					&& targetY >= virtualized.totalHeightEstimateProperty().getValue()) {
				virtualized.scrollYBy(Integer.MAX_VALUE);
			}
		});
	}
}
