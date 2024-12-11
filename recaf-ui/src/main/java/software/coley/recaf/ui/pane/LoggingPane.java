package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.event.Level;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.LogConsumer;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.ui.control.richtext.linegraphics.LineGraphicFactory;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Pane for displaying logger calls.
 *
 * @author Matt Coley
 */
@Dependent
public class LoggingPane extends BorderPane implements LogConsumer<String> {
	private final List<LogCallInfo> infos = Collections.synchronizedList(new ArrayList<>());
	private final Queue<String> messageQueue = new ArrayDeque<>();
	private final Editor editor = new Editor();
	private final CodeArea codeArea = editor.getCodeArea();

	@Inject
	public LoggingPane(@Nonnull RecafDirectoriesConfig config, @Nonnull SearchBar searchBar) {
		Logging.addLogConsumer(this);
		codeArea.setEditable(false);
		searchBar.install(editor);
		editor.getRootLineGraphicFactory().addLineGraphicFactory(new LoggingLineFactory());
		setCenter(editor);

		// Initial line
		infos.add(new LogCallInfo(Level.TRACE, null));
		codeArea.appendText("Current log will write to: " + StringUtil.pathToAbsoluteString(config.getCurrentLogPath()));

		// We want to reduce the calls to the FX thread, so we will chunk log-appends into groups
		// occurring every 500ms, which shouldn't be too noticeable, and save us some CPU time.
		ThreadPoolFactory.newScheduledThreadPool("logging-pane", 1, true)
				.scheduleAtFixedRate(() -> {
					try {
						StringBuilder messageBuilder = new StringBuilder();
						synchronized (messageQueue) {
							if (messageQueue.isEmpty())
								return;
							String message;
							while ((message = messageQueue.poll()) != null)
								messageBuilder.append(message).append('\n');
						}
						if (messageBuilder.length() > 1) {
							String collectedMessage = messageBuilder.substring(0, messageBuilder.length() - 1);
							FxThreadUtil.run(() -> {
								codeArea.appendText("\n" + collectedMessage);
								codeArea.showParagraphAtBottom(codeArea.getParagraphs().size() - 1);
							});
						}
					} catch (Throwable t) {
						// We don't want to cause infinite loops by causing uncaught exceptions to trigger another
						// logger call, so we will just print the trace here and move on.
						t.printStackTrace();
					}
				}, 100, 500, TimeUnit.MILLISECONDS);
	}

	@Override
	public void accept(@Nonnull String loggerName, @Nonnull Level level, @Nullable String messageContent) {
		accept(loggerName, level, messageContent, null);
	}

	@Override
	public void accept(@Nonnull String loggerName, @Nonnull Level level, @Nullable String messageContent, @Nullable Throwable throwable) {
		if (messageContent == null) {
			if (throwable == null)
				return;
			messageContent = Objects.requireNonNullElse(throwable.getMessage(), throwable.getClass().getSimpleName());
		}
		infos.add(new LogCallInfo(level, throwable));
		synchronized (messageQueue) {
			messageQueue.add(messageContent);
		}
	}

	private record LogCallInfo(
			@Nonnull Level level,
			@Nullable Throwable throwable) {}

	private class LoggingLineFactory implements LineGraphicFactory {
		private static final Insets PADDING = new Insets(0, 10, 0, 0);
		private static final double SIZE = 4;
		private static final double[] TRIANGLE = {
				SIZE, 0, // Size used for circles is radius, so for triangles we want to double positions based on it.
				SIZE * 2, SIZE * 2,
				0, SIZE * 2
		};

		@Override
		public int priority() {
			return -1;
		}

		@Override
		public void apply(@Nonnull LineContainer container, int paragraph) {
			if (paragraph >= infos.size())
				return;
			LogCallInfo info = infos.get(paragraph);
			Shape shape;
			switch (info.level) {
				case ERROR -> {
					if (info.throwable == null)
						shape = new Circle(SIZE, Color.RED);
					else {
						shape = new Polygon(TRIANGLE);
						shape.setFill(Color.RED);
					}
				}
				case WARN -> shape = new Circle(SIZE, Color.YELLOW);
				case INFO -> shape = new Circle(SIZE, Color.LIGHTBLUE);
				case DEBUG -> shape = new Circle(SIZE, Color.CORNFLOWERBLUE);
				case TRACE -> shape = new Circle(SIZE, Color.DODGERBLUE);
				default -> throw new IllegalArgumentException("Unsupported logging level");
			}
			shape.setOpacity(0.65);

			// Wrap and provide right-side padding to give the indicator space between it and the line no.
			HBox wrapper = new HBox(shape);
			wrapper.setAlignment(Pos.CENTER);
			wrapper.setPadding(PADDING);
			wrapper.setCursor(Cursor.HAND);
			if (info.throwable != null) {
				Tooltip tooltip = new Tooltip(StringUtil.traceToString(info.throwable));
				tooltip.setShowDelay(Duration.ZERO);
				Tooltip.install(wrapper, tooltip);
			}
			container.addHorizontal(wrapper);
		}

		@Override
		public void install(@Nonnull Editor editor) {
			// no-op
		}

		@Override
		public void uninstall(@Nonnull Editor editor) {
			// no-op
		}
	}

	private static class PruneError extends RuntimeException {
		private PruneError() {
			super(null, null, false, false);
		}
	}
}
