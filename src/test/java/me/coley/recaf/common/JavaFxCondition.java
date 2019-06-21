package me.coley.recaf.common;

import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.extension.*;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JUnit 5 condition for successful loading of JavaFx environments.
 *
 * @author Matt
 */
public class JavaFxCondition implements ExecutionCondition {
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		try {
			// From: https://stackoverflow.com/a/28501560
			final CountDownLatch latch = new CountDownLatch(1);
			SwingUtilities.invokeLater(() -> {
				// initializes JavaFX environment
				new JFXPanel();
				latch.countDown();
			});
			// That's a pretty reasonable delay... Right?
			if(!latch.await(5L, TimeUnit.SECONDS))
				throw new ExceptionInInitializerError();
			// Success! JavaFX is loaded
			return ConditionEvaluationResult.enabled("JavaFX is supported");
		} catch(Throwable t) {
			return ConditionEvaluationResult.disabled("JavaFX could not be loaded");
		}
	}
}
