package me.coley.recaf.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import me.coley.recaf.Logging;
import me.coley.recaf.config.impl.ConfOther;

public class Threads {
	private final static ConfOther conf = ConfOther.instance();

	public static ExecutorService pool(PoolKind kind) {
		switch (kind) {
		case IO:
			// There is more of a benefit to throwing threads at IO tasks than
			// at a computational problem. So defining caps for both allow a
			// higher cap to be set for these IO tasks.
			return Executors.newFixedThreadPool(conf.maxThreadsIO);
		case LOGIC:
		default:
			return Executors.newFixedThreadPool(conf.maxThreadsLogic);
		}
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
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
					r.run();
				} catch (Exception e) {
					Logging.error(e);
				}

			}
		}.start();
	}

	public static void runFx(Runnable r) {
		runLaterFx(0, r);
	}

	public static void runLaterFx(int delay, Runnable r) {
		runLater(delay, () -> Platform.runLater(r));
	}

	public enum PoolKind {
		IO, LOGIC
	}
}
