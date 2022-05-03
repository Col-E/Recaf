package me.coley.recaf.ui.control;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.logging.LogConsumer;
import me.coley.recaf.util.logging.Logging;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.Caret;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.event.Level;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * Text area that displays log messages.
 *
 * @author Matt Coley
 */
public class LoggingTextArea extends BorderPane implements LogConsumer<String> {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
			.withZone(ZoneId.systemDefault());
	private static LoggingTextArea instance;
	private final CodeArea codeArea = new CodeArea();

	private LoggingTextArea() {
		Logging.addLogConsumer(this);
		codeArea.setEditable(false);
		codeArea.setShowCaret(Caret.CaretVisibility.OFF);
		setCenter(new VirtualizedScrollPane<>(codeArea));
	}

	@Override
	public void accept(String loggerName, Level level, String messageContent) {
		addLog(loggerName, level, messageContent);
	}

	@Override
	public void accept(String loggerName, Level level, String messageContent, Throwable throwable) {
		if (!Platform.isFxApplicationThread()) {
			FxThreadUtil.run(() -> accept(loggerName, level, messageContent, throwable));
			return;
		}
		// Throwable to string
		StringWriter writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		// Add logging + error
		addLog(loggerName, level, messageContent);
		writer.append("\n");
		synchronized (codeArea) {
			codeArea.append(writer.toString(), "log-error");
		}
		scrollToBottom();

	}

	private void addLog(String loggerName, Level level, String messageContent) {
		if (!Platform.isFxApplicationThread()) {
			FxThreadUtil.run(() -> addLog(loggerName, level, messageContent));
			return;
		}
		synchronized (codeArea) {
			codeArea.append(TIME_FORMATTER.format(Instant.now()), "log-time");
			codeArea.append(" [", Collections.emptyList());
			codeArea.append(minify(loggerName), "log-name");
			codeArea.append(":", Collections.emptyList());
			codeArea.append(level.name(), "log-level");
			codeArea.append("] ", Collections.emptyList());
			codeArea.append(messageContent + "\n", "log-content");
			codeArea.requestFollowCaret();
		}
	}

	/**
	 * Scroll to bottom and move caret position to match.
	 */
	private void scrollToBottom() {
		synchronized (codeArea) {
			codeArea.moveTo(codeArea.getLength());
			// Option may not be set initially
			if (codeArea.totalHeightEstimateProperty().isPresent())
				codeArea.scrollYToPixel(codeArea.getTotalHeightEstimate());
		}
	}

	/**
	 * @param loggerName
	 * 		Original logger name.
	 *
	 * @return Logger name with any package name prefix removed.
	 */
	private static String minify(String loggerName) {
		int index = loggerName.lastIndexOf('.');
		return loggerName.substring(index + 1);
	}

	/**
	 * @return Instance of the logging text area.
	 */
	public static LoggingTextArea getInstance() {
		if (instance == null) {
			instance = new LoggingTextArea();
		}
		return instance;
	}
}
