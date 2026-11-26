package software.coley.recaf.ui.pane.monitor;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import software.coley.recaf.util.Lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Process/system CPU history and per-thread CPU usage details.
 *
 * @author Matt Coley
 */
final class ResourceMonitorCpuDetailsPane extends VBox {
	private static final int HISTORY_SIZE = 60;

	private final Label processCpu = new Label();
	private final Label systemCpu = new Label();
	private final Label processCpuShare = new Label();
	private final Label peakProcessCpu = new Label();
	private final Label activeThreadCount = new Label();
	private final Label idleThreadCount = new Label();
	private final ArrayDeque<Double> processHistory = new ArrayDeque<>(HISTORY_SIZE);
	private final ArrayDeque<Double> systemHistory = new ArrayDeque<>(HISTORY_SIZE);
	private final Map<Long, Long> previousThreadCpuTimes = new HashMap<>();
	private final List<CpuThreadUsage> threadUsageBuffer = new ArrayList<>();
	private final Set<Long> currentThreadIds = new HashSet<>();
	private final XYChart.Series<Number, Number> processSeries = new XYChart.Series<>();
	private final XYChart.Series<Number, Number> systemSeries = new XYChart.Series<>();
	private final TableView<CpuThreadUsage> topThreads = new TableView<>();
	private long previousSampleTimeNanos;

	ResourceMonitorCpuDetailsPane() {
		setPadding(new Insets(12));
		setSpacing(8);
		setPrefWidth(760);

		processCpu.getStyleClass().add(Styles.TEXT_BOLD);
		systemCpu.getStyleClass().add(Styles.TEXT_BOLD);
		processCpuShare.getStyleClass().add(Styles.TEXT_BOLD);
		peakProcessCpu.getStyleClass().add(Styles.TEXT_BOLD);
		activeThreadCount.getStyleClass().add(Styles.TEXT_BOLD);
		idleThreadCount.getStyleClass().add(Styles.TEXT_BOLD);
		Label processCpuShareTitle = new Label(Lang.get("monitor.cpu.share"));
		Tooltip.install(processCpuShareTitle, new Tooltip(Lang.get("monitor.cpu.share.help")));
		HBox current = new HBox(6,
				new VBox(3, new Label(Lang.get("monitor.cpu.process")), processCpu),
				new Separator(Orientation.VERTICAL),
				new VBox(3, new Label(Lang.get("monitor.cpu.system")), systemCpu),
				new Separator(Orientation.VERTICAL),
				new VBox(3, new Label(Lang.get("monitor.cpu.peak-60s")), peakProcessCpu),
				new Separator(Orientation.VERTICAL),
				new VBox(3, processCpuShareTitle, processCpuShare),
				new Separator(Orientation.VERTICAL),
				new VBox(3, new Label(Lang.get("monitor.cpu.active")), activeThreadCount),
				new Separator(Orientation.VERTICAL),
				new VBox(3, new Label(Lang.get("monitor.cpu.idle")), idleThreadCount));
		current.getChildren().forEach(box -> HBox.setHgrow(box, Priority.ALWAYS));

		NumberAxis secondsAxis = new NumberAxis(-HISTORY_SIZE, 0, 10);
		secondsAxis.setTickLabelFormatter(new StringConverter<>() {
			@Override
			public String toString(Number value) {
				long secondsAgo = Math.round(value.doubleValue());
				return secondsAgo == 0 ? "-0" : Long.toString(secondsAgo);
			}

			@Override
			public Number fromString(String value) {
				return Long.parseLong(value);
			}
		});
		secondsAxis.setLabel(Lang.get("monitor.history.seconds"));
		NumberAxis cpuAxis = new NumberAxis(0, 100, 25);
		cpuAxis.setLabel("%");
		LineChart<Number, Number> cpuChart = new LineChart<>(secondsAxis, cpuAxis);
		cpuChart.setVerticalZeroLineVisible(false);
		cpuChart.setTitle(Lang.get("monitor.cpu.history"));
		cpuChart.setAnimated(false);
		cpuChart.setCreateSymbols(false);
		cpuChart.setMinHeight(180);
		cpuChart.setPrefHeight(220);
		processSeries.setName(Lang.get("monitor.cpu.process"));
		systemSeries.setName(Lang.get("monitor.cpu.system"));
		cpuChart.getData().setAll(List.of(processSeries, systemSeries));

		BorderPane currentWrapper = new BorderPane();
		currentWrapper.setTop(current);
		currentWrapper.setCenter(new Separator());
		currentWrapper.setBottom(cpuChart);
		currentWrapper.getStyleClass().addAll(Styles.BORDER_DEFAULT, Styles.BG_DEFAULT);
		currentWrapper.setPadding(new Insets(8));
		configureTopThreads();
		getChildren().addAll(currentWrapper, new Label(Lang.get("monitor.cpu.top-threads")), topThreads);
	}

	void update(@Nonnull ResourceMonitorMetrics.Snapshot sample) {
		processCpu.setText(formatPercent(sample.processCpuLoad()));
		systemCpu.setText(formatPercent(sample.systemCpuLoad()));
		processCpuShare.setText(formatPercent(relativeProcessCpuShare(sample.processCpuLoad(), sample.systemCpuLoad())));
		appendHistory(processHistory, sample.processCpuLoad());
		appendHistory(systemHistory, sample.systemCpuLoad());
		peakProcessCpu.setText(formatPercent(peakLoad(processHistory)));
		ResourceMonitorMetrics.ThreadStateCounts states = sample.threadStates();
		activeThreadCount.setText(Integer.toString(states.runnable()));
		idleThreadCount.setText(Integer.toString(states.blocked() + states.waiting() + states.timedWaiting()));
		refreshSeries(processSeries, processHistory);
		refreshSeries(systemSeries, systemHistory);

		if (!sample.threadCpuTimeAvailable()) {
			topThreads.getItems().clear();
			previousThreadCpuTimes.clear();
			threadUsageBuffer.clear();
			currentThreadIds.clear();
			previousSampleTimeNanos = sample.capturedAtNanos();
			return;
		}

		long elapsed = previousSampleTimeNanos == 0 ? 0 : sample.capturedAtNanos() - previousSampleTimeNanos;
		previousSampleTimeNanos = sample.capturedAtNanos();
		List<CpuThreadUsage> usages = threadUsageBuffer;
		usages.clear();
		currentThreadIds.clear();
		for (ResourceMonitorMetrics.ThreadSample thread : sample.threads()) {
			currentThreadIds.add(thread.id());
			Long previousCpu = previousThreadCpuTimes.put(thread.id(), thread.cpuTimeNanos());
			if (previousCpu == null || previousCpu < 0 || thread.cpuTimeNanos() < 0 || elapsed <= 0)
				continue;
			long cpuDelta = thread.cpuTimeNanos() - previousCpu;
			if (cpuDelta < 0)
				continue;
			double cpuLoad = (double) cpuDelta / elapsed / ResourceMonitorMetrics.processorCount();
			usages.add(new CpuThreadUsage(thread.name(), thread.state().name(), cpuLoad, thread.topFrame()));
		}
		previousThreadCpuTimes.keySet().retainAll(currentThreadIds);
		usages.sort((left, right) -> Double.compare(right.cpuLoad(), left.cpuLoad()));
		topThreads.getItems().setAll(usages.subList(0, Math.min(10, usages.size())));
	}

	private void configureTopThreads() {
		TableColumn<CpuThreadUsage, String> name = new TableColumn<>(Lang.get("monitor.thread.name"));
		name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
		TableColumn<CpuThreadUsage, String> state = new TableColumn<>(Lang.get("monitor.thread.state"));
		state.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().state()));
		TableColumn<CpuThreadUsage, String> cpu = new TableColumn<>(Lang.get("monitor.thread.cpu"));
		cpu.setCellValueFactory(data -> new SimpleStringProperty(formatThreadPercent(data.getValue().cpuLoad())));
		TableColumn<CpuThreadUsage, String> frame = new TableColumn<>(Lang.get("monitor.cpu.top-frame"));
		frame.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().topFrame()));
		topThreads.getColumns().setAll(List.of(name, state, cpu, frame));
		topThreads.setItems(FXCollections.observableArrayList());
		topThreads.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		topThreads.setPrefHeight(210);
		topThreads.setPlaceholder(new Label(Lang.get("monitor.cpu.no-thread-data")));
	}

	private static void appendHistory(@Nonnull ArrayDeque<Double> history, double value) {
		if (history.size() == HISTORY_SIZE)
			history.removeFirst();
		history.addLast(value);
	}

	private static double peakLoad(@Nonnull ArrayDeque<Double> history) {
		double peak = -1;
		for (double load : history)
			if (load > peak)
				peak = load;
		return peak;
	}

	private static void refreshSeries(@Nonnull XYChart.Series<Number, Number> series, @Nonnull ArrayDeque<Double> history) {
		series.getData().clear();
		int index = HISTORY_SIZE - history.size();
		for (double value : history) {
			if (value >= 0)
				series.getData().add(new XYChart.Data<>(index - (HISTORY_SIZE - 1), value * 100));
			index++;
		}
	}

	private static double relativeProcessCpuShare(double processLoad, double systemLoad) {
		if (processLoad < 0 || systemLoad <= 0)
			return -1;
		return Math.clamp(processLoad / systemLoad, 0, 1);
	}

	@Nonnull
	private static String formatPercent(double load) {
		return load < 0 ? Lang.get("monitor.unavailable") : Math.round(load * 100) + "%";
	}

	@Nonnull
	private static String formatThreadPercent(double load) {
		long percentTenths = Math.round(load * 1000);
		return percentTenths / 10 + "." + percentTenths % 10 + "%";
	}

	private record CpuThreadUsage(@Nonnull String name, @Nonnull String state, double cpuLoad,
	                              @Nonnull String topFrame) {}
}
