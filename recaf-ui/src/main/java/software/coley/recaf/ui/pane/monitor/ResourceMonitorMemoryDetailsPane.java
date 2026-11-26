package software.coley.recaf.ui.pane.monitor;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.util.Lang;

import java.util.List;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Detailed host and JVM memory information.
 *
 * @author Matt Coley
 */
public final class ResourceMonitorMemoryDetailsPane extends VBox {
	private static final long BYTES_PER_MEBIBYTE = 1024L * 1024L;
	private static final long BYTES_PER_GIBIBYTE = BYTES_PER_MEBIBYTE * 1024L;

	private final Label hostMemory = new Label();
	private final Label heapMemory = new Label();
	private final Label nonHeapMemory = new Label();
	private final ProgressBar hostMemoryBar = new ProgressBar(0);
	private final ProgressBar processMemoryOverlayBar = new ProgressBar(0);
	private final ProgressBar heapMemoryBar = new ProgressBar(0);
	private final ProgressBar nonHeapMemoryBar = new ProgressBar(0);
	private final TableView<ResourceMonitorMetrics.MemoryPool> poolTable = new TableView<>();
	private final TableView<ResourceMonitorMetrics.GarbageCollector> gcTable = new TableView<>();

	ResourceMonitorMemoryDetailsPane(@Nonnull Runnable requestGc) {
		setPadding(new Insets(12));
		setSpacing(8);
		setPrefWidth(560);

		Label hostMemoryTitle = new BoundLabel(getBinding("monitor.memory.system"));
		Label heapMemoryTitle = new BoundLabel(getBinding("monitor.memory.heap"));
		Label nonHeapMemoryTitle = new BoundLabel(getBinding("monitor.memory.non-heap"));
		HBox hostMemoryText = new HBox(8, hostMemoryTitle, hostMemory);
		HBox heapMemoryText = new HBox(8, heapMemoryTitle, heapMemory);
		HBox nonHeapMemoryText = new HBox(8, nonHeapMemoryTitle, nonHeapMemory);
		configureValueRow(hostMemoryText, hostMemory);
		configureValueRow(heapMemoryText, heapMemory);
		configureValueRow(nonHeapMemoryText, nonHeapMemory);
		StackPane.setAlignment(hostMemoryText, Pos.CENTER_LEFT);
		StackPane.setAlignment(heapMemoryText, Pos.CENTER_LEFT);
		StackPane.setAlignment(nonHeapMemoryText, Pos.CENTER_LEFT);
		StackPane.setMargin(hostMemoryText, new Insets(0, 8, 0, 8));
		StackPane.setMargin(heapMemoryText, new Insets(0, 8, 0, 8));
		StackPane.setMargin(nonHeapMemoryText, new Insets(0, 8, 0, 8));
		hostMemoryBar.setMaxWidth(Double.MAX_VALUE);
		heapMemoryBar.setMaxWidth(Double.MAX_VALUE);
		nonHeapMemoryBar.setMaxWidth(Double.MAX_VALUE);
		processMemoryOverlayBar.setMaxWidth(Double.MAX_VALUE);
		processMemoryOverlayBar.setMinHeight(24);
		processMemoryOverlayBar.setMouseTransparent(true);
		processMemoryOverlayBar.getStyleClass().add("process-memory-overlay");
		hostMemoryBar.setMinHeight(24);
		heapMemoryBar.setMinHeight(24);
		nonHeapMemoryBar.setMinHeight(24);

		StackPane hostMemoryDisplay = new StackPane(hostMemoryBar, processMemoryOverlayBar, hostMemoryText);
		StackPane heapMemoryDisplay = new StackPane(heapMemoryBar, heapMemoryText);
		StackPane nonHeapMemoryDisplay = new StackPane(nonHeapMemoryBar, nonHeapMemoryText);
		VBox summary = new VBox(6, hostMemoryDisplay, heapMemoryDisplay, nonHeapMemoryDisplay);
		summary.setPadding(new Insets(6));
		summary.getStyleClass().addAll(Styles.BORDER_DEFAULT, Styles.BG_INSET);
		poolTable.getStyleClass().add(Tweaks.EDGE_TO_EDGE);
		gcTable.getStyleClass().add(Tweaks.EDGE_TO_EDGE);

		configurePoolTable();
		configureGcTable();
		TabPane tables = new TabPane(
				new BoundTab(getBinding("monitor.memory.pools"), CarbonIcons.DATA_CLASS, poolTable),
				new BoundTab(getBinding("monitor.memory.garbage-collectors"), CarbonIcons.CLEAN, gcTable));
		tables.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		tables.getStyleClass().addAll(Styles.BORDER_DEFAULT);
		VBox.setVgrow(tables, Priority.ALWAYS);

		ActionButton requestGcButton = new ActionButton(getBinding("monitor.memory.request-gc"), requestGc);
		requestGcButton.getStyleClass().addAll(Styles.ACCENT, Styles.BUTTON_OUTLINED);

		getChildren().addAll(summary, tables, requestGcButton);
	}

	void update(@Nonnull ResourceMonitorMetrics.Snapshot sample) {
		long total = sample.totalPhysicalMemory();
		long free = sample.freePhysicalMemory();
		long used = total >= 0 && free >= 0 ? Math.max(0, total - free) : -1;
		hostMemory.setText(used < 0 ? Lang.get("monitor.unavailable") :
				formatMemorySize(used) + " / " + formatMemorySize(total) + " (" + formatPercent(used, total) + "%)"
						+ " - " + Lang.get("monitor.memory.free") + " " + formatMemorySize(free));
		hostMemoryBar.setProgress(ratio(used, total));

		long heapUsed = sample.heap().getUsed();
		long heapMax = sample.heap().getMax();
		heapMemory.setText(formatMemorySize(heapUsed) + " / " + formatMemorySize(sample.heap().getCommitted()) +
				" / " + formatMemorySize(heapMax) + " (" + formatPercent(heapUsed, heapMax) + "%)");
		heapMemoryBar.setProgress(ratio(heapUsed, heapMax));
		long nonHeapUsed = sample.nonHeap().getUsed();
		long nonHeapCommitted = sample.nonHeap().getCommitted();
		nonHeapMemory.setText(formatMemorySize(nonHeapUsed) + " / " + formatMemorySize(nonHeapCommitted));
		nonHeapMemoryBar.setProgress(ratio(nonHeapUsed, nonHeapCommitted));

		long processMemory = sample.processCommittedVirtualMemory();
		processMemoryOverlayBar.setProgress(processMemory < 0 || total <= 0 ? 0 : ratio(processMemory, total));
		String processMemoryPercent = processMemory < 0 || total <= 0 ? Lang.get("monitor.unavailable") :
				formatPercent(processMemory, total) + "%";

		poolTable.getItems().setAll(sample.pools());
		gcTable.getItems().setAll(sample.garbageCollectors());
	}

	private void configurePoolTable() {
		TableColumn<ResourceMonitorMetrics.MemoryPool, String> name = new TableColumn<>(Lang.get("monitor.memory.pool"));
		TableColumn<ResourceMonitorMetrics.MemoryPool, String> used = new TableColumn<>(Lang.get("monitor.memory.used"));
		TableColumn<ResourceMonitorMetrics.MemoryPool, String> max = new TableColumn<>(Lang.get("monitor.memory.max"));
		name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
		used.setCellValueFactory(data -> new SimpleStringProperty(formatMemorySize(data.getValue().used())));
		max.setCellValueFactory(data -> new SimpleStringProperty(formatMemorySize(data.getValue().max())));
		poolTable.getColumns().setAll(List.of(name, used, max));
		poolTable.setItems(FXCollections.observableArrayList());
		poolTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		poolTable.setPrefHeight(270);
		poolTable.setPlaceholder(new Label(Lang.get("monitor.memory.pools.empty")));
	}

	private void configureGcTable() {
		TableColumn<ResourceMonitorMetrics.GarbageCollector, String> name = new TableColumn<>(Lang.get("monitor.memory.collector"));
		TableColumn<ResourceMonitorMetrics.GarbageCollector, String> count = new TableColumn<>(Lang.get("monitor.memory.collections"));
		TableColumn<ResourceMonitorMetrics.GarbageCollector, String> time = new TableColumn<>(Lang.get("monitor.memory.collection-time"));
		name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name()));
		count.setCellValueFactory(data -> new SimpleStringProperty(formatCount(data.getValue().collectionCount())));
		time.setCellValueFactory(data -> new SimpleStringProperty(formatMillis(data.getValue().collectionTimeMillis())));
		gcTable.getColumns().setAll(List.of(name, count, time));
		gcTable.setItems(FXCollections.observableArrayList());
		gcTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		gcTable.setPrefHeight(270);
		gcTable.setPlaceholder(new Label(Lang.get("monitor.memory.collectors.empty")));
	}

	private static void configureValueRow(@Nonnull HBox row, @Nonnull Label value) {
		row.setMinWidth(0);
		row.setMaxWidth(Double.MAX_VALUE);
		row.setAlignment(Pos.CENTER_LEFT);
		value.setMinWidth(0);
		value.setMaxWidth(Double.MAX_VALUE);
		value.setAlignment(Pos.CENTER_RIGHT);
		value.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
		HBox.setHgrow(value, Priority.ALWAYS);
	}

	@Nonnull
	private static String formatMemorySize(long bytes) {
		if (bytes < 0)
			return Lang.get("monitor.unavailable");
		if (bytes < BYTES_PER_GIBIBYTE)
			return bytes / BYTES_PER_MEBIBYTE + " MB";
		long tenths = Math.round((double) bytes / BYTES_PER_GIBIBYTE * 10);
		return tenths / 10 + "." + tenths % 10 + " GB";
	}

	@Nonnull
	private static String formatCount(long count) {
		return count < 0 ? Lang.get("monitor.unavailable") : Long.toString(count);
	}

	@Nonnull
	private static String formatMillis(long millis) {
		return millis < 0 ? Lang.get("monitor.unavailable") : millis + " ms";
	}

	@Nonnull
	private static String formatPercent(long used, long max) {
		return max <= 0 || used < 0 ? Lang.get("monitor.unavailable") : Long.toString(Math.round((double) used / max * 100));
	}

	private static double ratio(long used, long max) {
		return max <= 0 || used < 0 ? ProgressBar.INDETERMINATE_PROGRESS : Math.clamp((double) used / max, 0, 1);
	}
}
