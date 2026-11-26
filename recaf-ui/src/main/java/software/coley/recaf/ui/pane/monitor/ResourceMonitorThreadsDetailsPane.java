package software.coley.recaf.ui.pane.monitor;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.util.Lang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread counts and a bounded, rolling history of platform thread states.
 *
 * @author Matt Coley
 */
final class ResourceMonitorThreadsDetailsPane extends VBox {
	private static final int HISTORY_SIZE = 60;
	private static final Color RUNNABLE_COLOR = Color.LIMEGREEN;
	private static final Color BLOCKED_COLOR = Color.ORANGE;
	private static final Color WAITING_COLOR = Color.CORNFLOWERBLUE;
	private static final Color TIMED_WAITING_COLOR = Color.GOLD;
	private static final Color TERMINATED_COLOR = Color.GRAY;

	private final Label liveThreads = new Label();
	private final Label daemonThreads = new Label();
	private final Map<Long, ThreadRow> threadById = new HashMap<>();
	private final ObservableList<ThreadRow> threadRows = FXCollections.observableArrayList();
	private final TableView<ThreadRow> threadTable = new TableView<>(threadRows);
	private long previousSampleTimeNanos;

	ResourceMonitorThreadsDetailsPane(Runnable copyThreadDump) {
		setPadding(new Insets(12));
		setSpacing(8);
		setPrefWidth(1000);

		liveThreads.getStyleClass().add(Styles.TEXT_BOLD);
		daemonThreads.getStyleClass().add(Styles.TEXT_BOLD);

		configureTable();

		HBox summary = new HBox(24,
				new VBox(3, new Label(Lang.get("monitor.threads.live")), liveThreads),
				new VBox(3, new Label(Lang.get("monitor.threads.daemon")), daemonThreads),
				new ActionButton(Lang.getBinding("monitor.threads.copy-dump"), copyThreadDump));
		HBox legend = new HBox(14,
				legend("monitor.thread.state.runnable", RUNNABLE_COLOR),
				legend("monitor.thread.state.blocked", BLOCKED_COLOR),
				legend("monitor.thread.state.waiting", WAITING_COLOR),
				legend("monitor.thread.state.timed-waiting", TIMED_WAITING_COLOR),
				legend("monitor.thread.state.terminated", TERMINATED_COLOR));
		summary.setPadding(new Insets(0, 0, 4, 0));

		getChildren().addAll(summary, threadTable, legend);
	}

	void update(@Nonnull ResourceMonitorMetrics.Snapshot sample) {
		liveThreads.setText(Integer.toString(sample.threadCount()));
		daemonThreads.setText(Integer.toString(sample.daemonThreadCount()));

		long elapsed = previousSampleTimeNanos == 0 ? 0 : sample.capturedAtNanos() - previousSampleTimeNanos;
		previousSampleTimeNanos = sample.capturedAtNanos();
		for (ThreadRow row : threadRows)
			row.seenThisSample = false;
		for (ResourceMonitorMetrics.ThreadSample thread : sample.threads()) {
			ThreadRow row = threadById.get(thread.id());
			if (row == null) {
				row = new ThreadRow(thread.id(), thread.name(), thread.daemon());
				threadById.put(thread.id(), row);
				threadRows.add(row);
			}
			row.seenThisSample = true;

			double cpuLoad = -1;
			if (sample.threadCpuTimeAvailable() && row.previousCpuTimeNanos >= 0 &&
					thread.cpuTimeNanos() >= row.previousCpuTimeNanos && elapsed > 0) {
				cpuLoad = (double) (thread.cpuTimeNanos() - row.previousCpuTimeNanos) /
						elapsed / ResourceMonitorMetrics.processorCount();
			}
			row.update(thread.state(), thread.cpuTimeNanos(), cpuLoad);
		}

		for (int i = threadRows.size() - 1; i >= 0; i--) {
			ThreadRow row = threadRows.get(i);
			if (row.seenThisSample)
				continue;
			row.update(Thread.State.TERMINATED, -1, 0);
			if (row.samplesSinceSeen >= HISTORY_SIZE) {
				threadById.remove(row.id);
				threadRows.remove(i);
			}
		}
		threadTable.refresh();
	}

	private void configureTable() {
		TableColumn<ThreadRow, String> name = new TableColumn<>(Lang.get("monitor.thread.name"));
		name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
		name.setPrefWidth(250);
		TableColumn<ThreadRow, String> state = new TableColumn<>(Lang.get("monitor.thread.state"));
		state.setCellValueFactory(data -> data.getValue().state);
		state.setPrefWidth(140);
		TableColumn<ThreadRow, String> cpu = new TableColumn<>(Lang.get("monitor.thread.cpu"));
		cpu.setCellValueFactory(data -> data.getValue().cpu);
		cpu.setPrefWidth(100);
		TableColumn<ThreadRow, Number> timeline = new TableColumn<>(Lang.get("monitor.thread.timeline"));
		timeline.setCellValueFactory(data -> data.getValue().revision);
		timeline.setCellFactory(column -> new TimelineCell());
		timeline.setPrefWidth(480);
		threadTable.getColumns().setAll(List.of(name, state, cpu, timeline));
		threadTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		threadTable.setPrefHeight(470);
		threadTable.setPlaceholder(new Label(Lang.get("monitor.threads.empty")));
	}

	@Nonnull
	private static HBox legend(@Nonnull String translationKey, @Nonnull Color color) {
		Label label = new Label();
		label.textProperty().bind(Lang.getBinding(translationKey));
		return new HBox(5, new Rectangle(12, 12, color), label);
	}

	@Nonnull
	static Color colorOf(@Nonnull Thread.State state) {
		return switch (state) {
			case RUNNABLE -> RUNNABLE_COLOR;
			case BLOCKED -> BLOCKED_COLOR;
			case WAITING -> WAITING_COLOR;
			case TIMED_WAITING -> TIMED_WAITING_COLOR;
			case NEW, TERMINATED -> TERMINATED_COLOR;
		};
	}

	@Nonnull
	private static String cpuText(double cpuLoad) {
		if (cpuLoad < 0)
			return Lang.get("monitor.unavailable");
		long percentTenths = Math.round(cpuLoad * 1000);
		return percentTenths / 10 + "." + percentTenths % 10 + "%";
	}

	private static final class ThreadRow {
		private final long id;
		private final String name;
		private final SimpleStringProperty state = new SimpleStringProperty();
		private final SimpleStringProperty cpu = new SimpleStringProperty();
		private final SimpleIntegerProperty revision = new SimpleIntegerProperty();
		private final Thread.State[] history = new Thread.State[HISTORY_SIZE];
		private long previousCpuTimeNanos = -1;
		private int nextIndex;
		private int sampleCount;
		private int samplesSinceSeen;
		private boolean seenThisSample;

		private ThreadRow(long id, @Nonnull String name, boolean daemon) {
			this.id = id;
			this.name = name + (daemon ? " (daemon)" : "");
		}

		private void update(@Nonnull Thread.State nextState, long cpuTimeNanos, double cpuLoad) {
			state.set(nextState.name());
			cpu.set(cpuText(cpuLoad));
			previousCpuTimeNanos = cpuTimeNanos;
			history[nextIndex] = nextState;
			nextIndex = (nextIndex + 1) % HISTORY_SIZE;
			sampleCount = Math.min(HISTORY_SIZE, sampleCount + 1);
			samplesSinceSeen = nextState == Thread.State.TERMINATED ? samplesSinceSeen + 1 : 0;
			revision.set(revision.get() + 1);
		}

		@Nonnull
		private Thread.State stateAt(int offset) {
			int oldestIndex = (nextIndex - sampleCount + HISTORY_SIZE) % HISTORY_SIZE;
			return history[(oldestIndex + offset) % HISTORY_SIZE];
		}
	}

	private static final class TimelineCell extends TableCell<ThreadRow, Number> {
		private final Canvas canvas = new Canvas();

		private TimelineCell() {
			canvas.widthProperty().bind(widthProperty().subtract(12));
			canvas.heightProperty().bind(heightProperty().subtract(4));
		}

		@Override
		protected void updateItem(Number revision, boolean empty) {
			super.updateItem(revision, empty);
			if (empty || getTableRow() == null || getTableRow().getItem() == null) {
				setGraphic(null);
				return;
			}
			setGraphic(canvas);
			draw(getTableRow().getItem());
		}

		@Override
		protected void layoutChildren() {
			super.layoutChildren();
			if (getTableRow() != null && getTableRow().getItem() != null)
				draw(getTableRow().getItem());
		}

		private void draw(@Nonnull ThreadRow row) {
			double width = canvas.getWidth();
			double height = canvas.getHeight();
			if (width <= 0 || height <= 0)
				return;
			GraphicsContext graphics = canvas.getGraphicsContext2D();
			graphics.clearRect(0, 0, width, height);
			if (row.sampleCount == 0)
				return;
			double sampleWidth = width / HISTORY_SIZE;
			int leadingEmpty = HISTORY_SIZE - row.sampleCount;
			for (int i = 0; i < row.sampleCount; i++) {
				graphics.setFill(colorOf(row.stateAt(i)));
				graphics.fillRect((leadingEmpty + i) * sampleWidth, 0, Math.ceil(sampleWidth), height);
			}
		}
	}
}
