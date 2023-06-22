package software.coley.recaf.test.dummy;

import java.util.TimerTask;

/**
 * Class to test basic inner-outer relation, for an anonymous class.
 */
@SuppressWarnings("all")
public class ClassWithAnonymousInner {
	void foo() {
		// Use timer-task so IntelliJ doesn't suggest lambda-izing a runnable
		new Thread(new TimerTask() {
			@Override
			public void run() {
				System.out.println("Hello");
			}
		}).start();
	}
}
