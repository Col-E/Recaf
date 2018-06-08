package me.coley.recaf.util;

import javafx.application.Platform;

public class Threads {

	public static void runLater(int delay, Runnable r) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {}
				r.run();
			}
		}.start();
	}

	public static void runLaterFx(int delay, Runnable r) {
		runLater(delay, () -> Platform.runLater(r));
	}

}
