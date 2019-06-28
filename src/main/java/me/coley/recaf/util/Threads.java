package me.coley.recaf.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import me.coley.recaf.Logging;
import me.coley.recaf.config.impl.ConfOther;

public class Threads {
	public static ExecutorService pool() {
		// http://tempusfugitlibrary.org/recipes/2012/07/12/optimise-the-number-of-threads/
		// threads = number of cores + 1
		int cores = Runtime.getRuntime().availableProcessors();
		return Executors.newFixedThreadPool(cores + 1);
	}

	public static void waitForCompletion(ExecutorService pool) {
		try {
			pool.shutdown();
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Logging.error(e);
		}
	}

	public static void run(Runnable r) {
		runLater(0, r);
	}

	public static void runLater(int delay, Runnable r) {
		new Thread(() -> {
			try {
				if (delay > 0)
					Thread.sleep(delay);
				r.run();
			} catch (Exception e) {
				Logging.error(e);
			}
		}).start();
	}

	public static void runFx(Runnable r) {
		Platform.runLater(r);
	}

	public static void runLaterFx(int delay, Runnable r) {
		runLater(delay, () -> Platform.runLater(r));
	}
}
