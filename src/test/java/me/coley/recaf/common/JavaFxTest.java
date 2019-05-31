package me.coley.recaf.common;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base start hook to allow testing of JavaFx dependent code.
 *
 * @author <a href="https://stackoverflow.com/a/28501560">fge</a>
 */
public interface JavaFxTest {
	@BeforeAll
	static void initToolkit() {
		try {
			final CountDownLatch latch = new CountDownLatch(1);
			SwingUtilities.invokeLater(() -> {
				// initializes JavaFX environment
				new JFXPanel();
				latch.countDown();
			});
			// That's a pretty reasonable delay... Right?
			if(!latch.await(5L, TimeUnit.SECONDS))
				throw new ExceptionInInitializerError();
		} catch(InterruptedException e) {
			fail("Failed to setup JavaFx.", e);
		}
	}
}