package software.coley.recaf.ui.pane.monitor;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.ClipboardUtil;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Compact display of current JVM memory, CPU, and thread usage, with detailed popovers.
 *
 * @author Matt Coley
 */
@Dependent
public class ResourceMonitorPane extends HBox {
	private static final Logger logger = Logging.get(ResourceMonitorPane.class);
	private static final int HISTORY_SIZE = 60;
	private static final long MEBIBYTE = 1024L * 1024L;
	private static final long GIBIBYTE = MEBIBYTE * 1024L;
	private static final double BAR_HEIGHT = 8;
	private static final double EDGE_COLUMN_WIDTH_BONUS = 60;
	private static final Color BAR_TRACK = Color.color(0.5, 0.5, 0.5, 0.22);
	private static final Color MEMORY_FALLBACK_COLOR = Color.CORNFLOWERBLUE;
	private static final Color CPU_PROCESS_COLOR = Color.CORNFLOWERBLUE;
	private static final Color CPU_SYSTEM_COLOR = Color.color(0.3, 0.65, 0.85, 0.45);
	private static final Color[] HEAP_POOL_COLORS = {
			Color.CORNFLOWERBLUE, Color.MEDIUMSEAGREEN, Color.GOLD,
			Color.MEDIUMPURPLE, Color.TOMATO, Color.CYAN
	};

	private final ScheduledExecutorService sampler = ThreadPoolFactory.newScheduledThreadPool("resource-monitor-pane", 1, true);
	private final SummaryMetric memoryMetric;
	private final SummaryMetric cpuMetric;
	private final SummaryMetric threadMetric;
	private final double[] processCpuHistory = new double[HISTORY_SIZE];
	private final long[] gcCollectionDeltas = new long[HISTORY_SIZE];
	private final long[] gcTimeDeltas = new long[HISTORY_SIZE];
	private volatile ActiveDetail activeDetail;
	private long previousGcCollectionCount = -1;
	private long previousGcTimeMillis = -1;
	private int cpuHistoryIndex;
	private int gcHistoryIndex;

	@Inject
	public ResourceMonitorPane() {
		setAlignment(Pos.CENTER_LEFT);
		setSpacing(4);
		setPadding(new Insets(6));
		Arrays.fill(processCpuHistory, -1);
		Arrays.fill(gcCollectionDeltas, -1);
		Arrays.fill(gcTimeDeltas, -1);

		memoryMetric = new SummaryMetric("monitor.memory", this::showMemoryDetails);
		cpuMetric = new SummaryMetric("monitor.cpu", this::showCpuDetails);
		threadMetric = new SummaryMetric("monitor.threads", this::showThreadDetails);

		memoryMetric.setPrefWidth(EDGE_COLUMN_WIDTH_BONUS);
		threadMetric.setPrefWidth(EDGE_COLUMN_WIDTH_BONUS);

		getChildren().addAll(memoryMetric, new Separator(Orientation.VERTICAL), cpuMetric, new Separator(Orientation.VERTICAL), threadMetric);
		sampler.scheduleAtFixedRate(this::sample, 0, 1, TimeUnit.SECONDS);
	}

	private void showMemoryDetails(Node owner) {
		closeActiveDetail();
		ResourceMonitorMemoryDetailsPane pane = new ResourceMonitorMemoryDetailsPane(this::requestGarbageCollection);
		showDetails(owner, "monitor.memory.details-title", DetailType.MEMORY, pane::update, pane);
	}

	private void showCpuDetails(Node owner) {
		closeActiveDetail();
		ResourceMonitorMetrics.CpuTimeLease cpuTimeLease = ResourceMonitorMetrics.beginThreadCpuTime();
		ResourceMonitorCpuDetailsPane pane = new ResourceMonitorCpuDetailsPane();
		showDetails(owner, "monitor.cpu.details-title", DetailType.CPU, pane::update, pane, cpuTimeLease);
	}

	private void showThreadDetails(Node owner) {
		closeActiveDetail();
		ResourceMonitorMetrics.CpuTimeLease cpuTimeLease = ResourceMonitorMetrics.beginThreadCpuTime();
		ResourceMonitorThreadsDetailsPane pane = new ResourceMonitorThreadsDetailsPane(this::copyThreadDump);
		showDetails(owner, "monitor.threads.details-title", DetailType.THREADS, pane::update, pane, cpuTimeLease);
	}

	private void showDetails(Node owner, String titleKey, DetailType type, DetailUpdater updater, Node content) {
		showDetails(owner, titleKey, type, updater, content, null);
	}

	private void showDetails(Node owner, String titleKey, DetailType type, DetailUpdater updater, Node content,
	                         ResourceMonitorMetrics.CpuTimeLease cpuTimeLease) {
		Popover popover = new Popover(content);
		popover.titleProperty().bind(Lang.getBinding(titleKey));
		popover.setHeaderAlwaysVisible(true);
		popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		popover.setAutoHide(true);
		ActiveDetail session = new ActiveDetail(type, popover, updater, cpuTimeLease);
		activeDetail = session;
		popover.addEventHandler(WindowEvent.WINDOW_HIDDEN, event -> closeDetail(session));
		popover.show(owner);
	}

	private void closeActiveDetail() {
		ActiveDetail session = activeDetail;
		if (session != null) {
			session.popover.hide();
			closeDetail(session);
		}
	}

	private void closeDetail(ActiveDetail session) {
		if (activeDetail != session)
			return;
		activeDetail = null;
		if (session.cpuTimeLease != null)
			ResourceMonitorMetrics.endThreadCpuTime(session.cpuTimeLease);
	}

	private void sample() {
		ActiveDetail session = activeDetail;
		boolean threadDetails = session != null && session.type != DetailType.MEMORY;
		try {
			ResourceMonitorMetrics.Snapshot snapshot = ResourceMonitorMetrics.sample(threadDetails);
			FxThreadUtil.run(() -> {
				updateSummary(snapshot);
				if (session != null && activeDetail == session)
					session.updater.update(snapshot);
			});
		} catch (RuntimeException ex) {
			logger.error("Failed to sample runtime metrics", ex);
		}
	}

	private void updateSummary(ResourceMonitorMetrics.Snapshot snapshot) {
		appendCpuSample(snapshot.processCpuLoad());
		appendGcSample(snapshot.garbageCollectors());

		MemoryUsage heap = snapshot.heap();
		long heapUsed = heap.getUsed();
		long heapMax = heap.getMax();
		String heapPercent = formatPercent(heapUsed, heapMax);
		long physicalUsed = usedPhysicalMemory(snapshot.totalPhysicalMemory(), snapshot.freePhysicalMemory());
		boolean physicalMemoryGibibytes = snapshot.totalPhysicalMemory() >= GIBIBYTE;
		memoryMetric.setSummary(
				Lang.get("monitor.summary.memory.heap").formatted(formatSize(heapUsed), formatSize(heapMax), heapPercent),
				Lang.get("monitor.summary.memory.ram").formatted(
						formatSizeValue(physicalUsed, physicalMemoryGibibytes),
						formatSizeValue(snapshot.totalPhysicalMemory(), physicalMemoryGibibytes),
						physicalMemoryGibibytes ? "GB" : "MB",
						formatSizeValue(snapshot.freePhysicalMemory(), physicalMemoryGibibytes)) +
						" · " + Lang.get("monitor.summary.memory.gc").formatted(
						formatCount(sumWindow(gcCollectionDeltas)), formatMillis(sumWindow(gcTimeDeltas))));
		drawMemoryIndicator(snapshot);

		String processCpu = formatPercent(snapshot.processCpuLoad());
		cpuMetric.setSummary(
				Lang.get("monitor.summary.cpu.primary").formatted(processCpu),
				Lang.get("monitor.summary.cpu.secondary").formatted(
						formatPercent(snapshot.systemCpuLoad()), formatPercent(peakProcessCpu())));
		drawCpuIndicator(snapshot.processCpuLoad(), snapshot.systemCpuLoad());

		ResourceMonitorMetrics.ThreadStateCounts states = snapshot.threadStates();
		String stateSummary = Lang.get("monitor.summary.thread-state.runnable")
				.formatted(formatStatePercent(states.runnable(), states.total())) + " · " +
				Lang.get("monitor.summary.thread-state.blocked")
						.formatted(formatStatePercent(states.blocked(), states.total())) + " · " +
				Lang.get("monitor.summary.thread-state.waiting")
						.formatted(formatStatePercent(states.waiting(), states.total())) + " · " +
				Lang.get("monitor.summary.thread-state.timed-waiting")
						.formatted(formatStatePercent(states.timedWaiting(), states.total()));
		if (states.other() > 0)
			stateSummary += " · " + Lang.get("monitor.summary.thread-state.other")
					.formatted(formatStatePercent(states.other(), states.total()));
		threadMetric.setSummary(
				Lang.get("monitor.summary.threads.counts").formatted(
						Integer.toString(snapshot.threadCount()), Integer.toString(snapshot.daemonThreadCount()),
						Integer.toString(snapshot.peakThreadCount())),
				stateSummary);
		drawThreadIndicator(states);
	}

	private void appendCpuSample(double load) {
		processCpuHistory[cpuHistoryIndex] = load;
		cpuHistoryIndex = (cpuHistoryIndex + 1) % HISTORY_SIZE;
	}

	private double peakProcessCpu() {
		double peak = -1;
		for (double load : processCpuHistory)
			if (load > peak)
				peak = load;
		return peak;
	}

	private void appendGcSample(@Nonnull List<ResourceMonitorMetrics.GarbageCollector> collectors) {
		long collectionCount = totalGcCount(collectors);
		long collectionTime = totalGcTime(collectors);
		gcCollectionDeltas[gcHistoryIndex] = delta(collectionCount, previousGcCollectionCount);
		gcTimeDeltas[gcHistoryIndex] = delta(collectionTime, previousGcTimeMillis);
		previousGcCollectionCount = collectionCount;
		previousGcTimeMillis = collectionTime;
		gcHistoryIndex = (gcHistoryIndex + 1) % HISTORY_SIZE;
	}

	private static long totalGcCount(@Nonnull List<ResourceMonitorMetrics.GarbageCollector> collectors) {
		if (collectors.isEmpty()) return -1;
		long total = 0;
		for (ResourceMonitorMetrics.GarbageCollector collector : collectors) {
			if (collector.collectionCount() < 0) return -1;
			total += collector.collectionCount();
		}
		return total;
	}

	private static long totalGcTime(@Nonnull List<ResourceMonitorMetrics.GarbageCollector> collectors) {
		if (collectors.isEmpty()) return -1;
		long total = 0;
		for (ResourceMonitorMetrics.GarbageCollector collector : collectors) {
			if (collector.collectionTimeMillis() < 0) return -1;
			total += collector.collectionTimeMillis();
		}
		return total;
	}

	private static long delta(long current, long previous) {
		if (current < 0)
			return -1;
		return previous < 0 ? 0 : Math.max(0, current - previous);
	}

	private static long sumWindow(long[] deltas) {
		long total = 0;
		boolean available = false;
		for (long delta : deltas) {
			if (delta < 0) continue;
			total += delta;
			available = true;
		}
		return available ? total : -1;
	}

	private void drawMemoryIndicator(@Nonnull ResourceMonitorMetrics.Snapshot snapshot) {
		Canvas canvas = memoryMetric.indicator;
		GraphicsContext graphics = beginBar(canvas);
		long max = snapshot.heap().getMax();
		long used = snapshot.heap().getUsed();
		double usedWidth = canvas.getWidth() * ratio(used, max);
		if (usedWidth <= 0) {
			return;
		}

		long poolUsage = 0;
		for (ResourceMonitorMetrics.MemoryPool pool : snapshot.pools())
			if (pool.type() == MemoryType.HEAP && pool.used() > 0)
				poolUsage += pool.used();

		if (poolUsage <= 0) {
			graphics.setFill(MEMORY_FALLBACK_COLOR);
			graphics.fillRect(0, 0, usedWidth, BAR_HEIGHT);
			return;
		}

		double x = 0;
		int poolIndex = 0;
		for (ResourceMonitorMetrics.MemoryPool pool : snapshot.pools()) {
			if (pool.type() != MemoryType.HEAP || pool.used() <= 0)
				continue;
			double width = Math.min(usedWidth - x, usedWidth * pool.used() / poolUsage);
			if (width <= 0)
				break;
			graphics.setFill(HEAP_POOL_COLORS[poolIndex++ % HEAP_POOL_COLORS.length]);
			graphics.fillRect(x, 0, width, BAR_HEIGHT);
			x += width;
		}
	}

	@Nonnull
	private static GraphicsContext beginBar(@Nonnull Canvas canvas) {
		GraphicsContext graphics = canvas.getGraphicsContext2D();
		double width = canvas.getWidth();
		graphics.clearRect(0, 0, width, BAR_HEIGHT);
		graphics.setFill(BAR_TRACK);
		graphics.fillRect(0, 0, width, BAR_HEIGHT);
		return graphics;
	}

	private void drawCpuIndicator(double processLoad, double systemLoad) {
		Canvas canvas = cpuMetric.indicator;
		GraphicsContext graphics = beginBar(canvas);
		double width = canvas.getWidth();
		if (systemLoad >= 0) {
			graphics.setFill(CPU_SYSTEM_COLOR);
			graphics.fillRect(0, 0, width * loadRatio(systemLoad), BAR_HEIGHT);
		}
		if (processLoad >= 0) {
			graphics.setFill(CPU_PROCESS_COLOR);
			graphics.fillRect(0, 0, width * loadRatio(processLoad), BAR_HEIGHT);
		}
	}

	private void drawThreadIndicator(@Nonnull ResourceMonitorMetrics.ThreadStateCounts states) {
		Canvas canvas = threadMetric.indicator;
		GraphicsContext graphics = beginBar(canvas);
		if (states.total() <= 0)
			return;

		double width = canvas.getWidth();
		double x = 0;
		x = drawThreadSegment(graphics, x, width, states.runnable(), states.total(), ResourceMonitorThreadsDetailsPane.colorOf(Thread.State.RUNNABLE));
		x = drawThreadSegment(graphics, x, width, states.blocked(), states.total(), ResourceMonitorThreadsDetailsPane.colorOf(Thread.State.BLOCKED));
		x = drawThreadSegment(graphics, x, width, states.waiting(), states.total(), ResourceMonitorThreadsDetailsPane.colorOf(Thread.State.WAITING));
		x = drawThreadSegment(graphics, x, width, states.timedWaiting(), states.total(), ResourceMonitorThreadsDetailsPane.colorOf(Thread.State.TIMED_WAITING));
		drawThreadSegment(graphics, x, width, states.other(), states.total(), Color.GRAY);
	}

	private static double drawThreadSegment(@Nonnull GraphicsContext graphics, double x, double width,
	                                        int count, int total, Color color) {
		if (count <= 0)
			return x;
		double segmentWidth = width * count / total;
		graphics.setFill(color);
		graphics.fillRect(x, 0, segmentWidth, BAR_HEIGHT);
		return x + segmentWidth;
	}

	private void requestGarbageCollection() {
		sampler.execute(System::gc);
	}

	private void copyThreadDump() {
		sampler.execute(() -> {
			try {
				String dump = ResourceMonitorMetrics.threadDump();
				FxThreadUtil.run(() -> ClipboardUtil.copyString(dump));
			} catch (RuntimeException ex) {
				logger.warn("Failed to capture thread dump", ex);
			}
		});
	}

	private static long usedPhysicalMemory(long total, long free) {
		return total < 0 || free < 0 ? -1 : Math.max(0, total - free);
	}

	private static double ratio(long value, long maximum) {
		return value < 0 || maximum <= 0 ? 0 : Math.clamp((double) value / maximum, 0, 1);
	}

	private static double loadRatio(double load) {
		return load < 0 ? 0 : Math.clamp(load, 0, 1);
	}

	@Nonnull
	private static String formatSize(long bytes) {
		if (bytes < 0)
			return Lang.get("monitor.unavailable");
		boolean gibibytes = bytes >= GIBIBYTE;
		return formatSizeValue(bytes, gibibytes) + (gibibytes ? " GB" : " MB");
	}

	@Nonnull
	private static String formatSizeValue(long bytes, boolean gibibytes) {
		if (bytes < 0)
			return Lang.get("monitor.unavailable");
		if (!gibibytes)
			return Long.toString(bytes / MEBIBYTE);
		long tenths = Math.round((double) bytes / GIBIBYTE * 10);
		return tenths / 10 + "." + tenths % 10;
	}

	@Nonnull
	private static String formatPercent(long value, long maximum) {
		if (value < 0 || maximum <= 0)
			return Lang.get("monitor.unavailable");
		long tenths = Math.round((double) value / maximum * 1000);
		return tenths / 10 + "." + tenths % 10 + "%";
	}

	@Nonnull
	private static String formatPercent(double load) {
		return load < 0 ? Lang.get("monitor.unavailable") : Math.round(load * 100) + "%";
	}

	@Nonnull
	private static String formatStatePercent(int count, int total) {
		return total <= 0 ? "0%" : Math.round((double) count / total * 100) + "%";
	}

	@Nonnull
	private static String formatCount(long count) {
		return count < 0 ? Lang.get("monitor.unavailable") : Long.toString(count);
	}

	@Nonnull
	private static String formatMillis(long millis) {
		return millis < 0 ? Lang.get("monitor.unavailable") : Long.toString(millis);
	}

	private enum DetailType {
		MEMORY,
		CPU,
		THREADS
	}

	@FunctionalInterface
	private interface DetailUpdater {
		void update(ResourceMonitorMetrics.Snapshot snapshot);
	}

	private record ActiveDetail(DetailType type, Popover popover, DetailUpdater updater,
	                            ResourceMonitorMetrics.CpuTimeLease cpuTimeLease) {}

	private static final class SummaryMetric extends VBox {
		private final String titleKey;
		private final Label primary = new Label();
		private final Label secondary = new Label();
		private final Canvas indicator = new Canvas(124, BAR_HEIGHT);
		private final HBox primaryRow = new HBox(10, primary, indicator);

		private SummaryMetric(String titleKey, Consumer<Node> onClick) {
			this.titleKey = titleKey;
			Label title = new Label();
			title.textProperty().bind(Lang.getBinding(titleKey));
			title.getStyleClass().add(Styles.TEXT_SUBTLE);
			primary.getStyleClass().add(Styles.TEXT_BOLD);
			secondary.getStyleClass().add(Styles.TEXT_SUBTLE);
			primary.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
			secondary.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
			primary.setMinWidth(0);
			primary.setMaxWidth(Double.MAX_VALUE);
			secondary.setMinWidth(0);
			secondary.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(primary, Priority.ALWAYS);
			primaryRow.setAlignment(Pos.CENTER_LEFT);
			primaryRow.setMinWidth(0);
			primaryRow.setMaxWidth(Double.MAX_VALUE);
			setAlignment(Pos.CENTER_LEFT);
			setSpacing(1);
			setMinWidth(0);
			setPrefWidth(0);
			setMaxWidth(Double.MAX_VALUE);
			getChildren().addAll(title, primaryRow, secondary);
			setCursor(Cursor.HAND);
			setFocusTraversable(true);
			setAccessibleRole(AccessibleRole.BUTTON);
			setAccessibleHelp(Lang.get("monitor.open-details"));
			Tooltip.install(this, new Tooltip(Lang.get("monitor.open-details")));
			setOnMouseClicked(event -> {
				if (event.getButton() == MouseButton.PRIMARY) {
					event.consume();
					Platform.runLater(() -> onClick.accept(this));
				}
			});
			addEventHandler(KeyEvent.KEY_PRESSED, event -> {
				if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
					event.consume();
					Platform.runLater(() -> onClick.accept(this));
				}
			});
			HBox.setHgrow(this, Priority.ALWAYS);
		}

		private void setSummary(String primaryText, String secondaryText) {
			primary.setText(primaryText);
			secondary.setText(secondaryText);
			setAccessibleText(Lang.get(titleKey) + ". " + primaryText + ". " + secondaryText);
		}
	}
}
