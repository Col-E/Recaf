package software.coley.recaf.ui.pane.monitor;

import com.sun.management.OperatingSystemMXBean;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.CancelSignal;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures JVM and host metrics for the resource monitor and its details panes.
 *
 * @author Matt Coley
 */
final class ResourceMonitorMetrics {
	private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
	private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
	private static final OperatingSystemMXBean operatingSystemBean = getOperatingSystemBean();
	private static final List<GarbageCollectorMXBean> garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
	private static final List<MemoryPoolMXBean> memoryPools = ManagementFactory.getMemoryPoolMXBeans();
	private static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();

	private ResourceMonitorMetrics() {}

	@Nonnull
	static Snapshot sample(boolean includeThreadDetails) {
		MemoryUsage heap = memoryBean.getHeapMemoryUsage();
		MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
		long totalPhysicalMemory = safeTotalPhysicalMemory();
		long freePhysicalMemory = safeFreePhysicalMemory();
		long processCommittedVirtualMemory = safeCommittedVirtualMemorySize();
		double processCpuLoad = safeProcessCpuLoad();
		double systemCpuLoad = safeSystemCpuLoad();
		int threadCount = threadBean.getThreadCount();
		int daemonThreadCount = threadBean.getDaemonThreadCount();
		int peakThreadCount = threadBean.getPeakThreadCount();
		boolean threadCpuTimeAvailable = isThreadCpuTimeAvailable();

		// Memory pools, GC counters, and thread states are inexpensive to sample at 1 Hz. Stack traces and
		// per-thread CPU times are only collected while a detail popover is open.
		List<MemoryPool> pools = capturePools();
		List<GarbageCollector> gc = captureGarbageCollectors();
		ThreadCapture threadCapture = captureThreads(includeThreadDetails, threadCpuTimeAvailable);
		return new Snapshot(System.nanoTime(), heap, nonHeap, totalPhysicalMemory, freePhysicalMemory,
				processCommittedVirtualMemory, processCpuLoad, systemCpuLoad, threadCount, daemonThreadCount, peakThreadCount, threadCpuTimeAvailable,
				threadCapture.states, pools, gc, threadCapture.threads);
	}

	@Nonnull
	static CpuTimeLease beginThreadCpuTime() {
		if (!threadBean.isThreadCpuTimeSupported())
			return new CpuTimeLease(false);
		try {
			boolean wasEnabled = threadBean.isThreadCpuTimeEnabled();
			if (!wasEnabled)
				threadBean.setThreadCpuTimeEnabled(true);
			return new CpuTimeLease(!wasEnabled && threadBean.isThreadCpuTimeEnabled());
		} catch (SecurityException | UnsupportedOperationException ex) {
			return new CpuTimeLease(false);
		}
	}

	static void endThreadCpuTime(@Nonnull CpuTimeLease lease) {
		if (!lease.enabledByMonitor)
			return;
		try {
			if (threadBean.isThreadCpuTimeEnabled())
				threadBean.setThreadCpuTimeEnabled(false);
		} catch (Throwable ignored) {
			// If the bean fails, it's whatever...
		}
	}

	@Nonnull
	static String threadDump() {
		ThreadInfo[] threadInfos = threadBean.dumpAllThreads(
				threadBean.isObjectMonitorUsageSupported(), threadBean.isSynchronizerUsageSupported());
		StringBuilder builder = new StringBuilder();
		for (ThreadInfo threadInfo : threadInfos) {
			if (threadInfo != null)
				builder.append(threadInfo).append('\n');
		}
		return builder.toString();
	}

	@Nullable
	private static OperatingSystemMXBean getOperatingSystemBean() {
		java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
		return bean instanceof OperatingSystemMXBean extendedBean ? extendedBean : null;
	}

	@Nonnull
	private static OperatingSystemMXBean operatingSystemBean() {
		if (operatingSystemBean == null)
			throw CancelSignal.get();
		return operatingSystemBean;
	}

	private static long safeTotalPhysicalMemory() {
		try {
			return operatingSystemBean().getTotalMemorySize();
		} catch (Throwable ex) {
			return -1;
		}
	}

	private static long safeFreePhysicalMemory() {
		try {
			return operatingSystemBean().getFreeMemorySize();
		} catch (Throwable ex) {
			return -1;
		}
	}

	private static long safeCommittedVirtualMemorySize() {
		try {
			return operatingSystemBean().getCommittedVirtualMemorySize();
		} catch (Throwable ex) {
			return -1;
		}
	}

	private static double safeProcessCpuLoad() {
		try {
			return operatingSystemBean().getProcessCpuLoad();
		} catch (Throwable ex) {
			return -1;
		}
	}

	private static double safeSystemCpuLoad() {
		try {
			return operatingSystemBean().getCpuLoad();
		} catch (Throwable ex) {
			return -1;
		}
	}

	private static boolean isThreadCpuTimeAvailable() {
		try {
			return threadBean.isThreadCpuTimeSupported() && threadBean.isThreadCpuTimeEnabled();
		} catch (SecurityException | UnsupportedOperationException ex) {
			return false;
		}
	}

	@Nonnull
	private static List<MemoryPool> capturePools() {
		List<MemoryPool> pools = new ArrayList<>(memoryPools.size());
		for (MemoryPoolMXBean pool : memoryPools) {
			MemoryUsage usage = pool.getUsage();
			if (usage != null)
				pools.add(new MemoryPool(pool.getName(), pool.getType(), usage.getUsed(), usage.getCommitted(), usage.getMax()));
		}
		return pools;
	}

	@Nonnull
	private static List<GarbageCollector> captureGarbageCollectors() {
		List<GarbageCollector> collectors = new ArrayList<>(garbageCollectors.size());
		for (GarbageCollectorMXBean collector : garbageCollectors)
			collectors.add(new GarbageCollector(collector.getName(), collector.getCollectionCount(), collector.getCollectionTime()));
		return collectors;
	}

	@Nonnull
	private static ThreadCapture captureThreads(boolean includeDetails, boolean cpuTimeAvailable) {
		long[] ids = threadBean.getAllThreadIds();
		ThreadInfo[] infos = threadBean.getThreadInfo(ids, includeDetails ? 1 : 0);
		List<ThreadSample> threads = includeDetails ? new ArrayList<>(infos.length) : List.of();
		int runnable = 0;
		int blocked = 0;
		int waiting = 0;
		int timedWaiting = 0;
		int other = 0;
		int total = 0;
		for (ThreadInfo info : infos) {
			if (info == null)
				continue;
			Thread.State state = info.getThreadState();
			switch (state) {
				case RUNNABLE -> runnable++;
				case BLOCKED -> blocked++;
				case WAITING -> waiting++;
				case TIMED_WAITING -> timedWaiting++;
				case NEW, TERMINATED -> other++;
			}
			total++;
			if (includeDetails) {
				long cpuTime = cpuTimeAvailable ? safeThreadCpuTime(info.getThreadId()) : -1;
				StackTraceElement[] stack = info.getStackTrace();
				String topFrame = stack.length == 0 ? "" : stack[0].toString();
				threads.add(new ThreadSample(info.getThreadId(), info.getThreadName(), info.isDaemon(),
						state, cpuTime, topFrame));
			}
		}
		ThreadStateCounts states = new ThreadStateCounts(runnable, blocked, waiting, timedWaiting, other, total);
		return new ThreadCapture(threads, states);
	}

	private static long safeThreadCpuTime(long threadId) {
		try {
			return threadBean.getThreadCpuTime(threadId);
		} catch (UnsupportedOperationException | SecurityException ex) {
			return -1;
		}
	}

	static int processorCount() {
		return PROCESSOR_COUNT;
	}

	record Snapshot(long capturedAtNanos, @Nonnull MemoryUsage heap, @Nonnull MemoryUsage nonHeap,
	                long totalPhysicalMemory, long freePhysicalMemory, long processCommittedVirtualMemory,
	                double processCpuLoad, double systemCpuLoad,
	                int threadCount, int daemonThreadCount, int peakThreadCount, boolean threadCpuTimeAvailable,
	                @Nonnull ThreadStateCounts threadStates, @Nonnull List<MemoryPool> pools,
	                @Nonnull List<GarbageCollector> garbageCollectors, @Nonnull List<ThreadSample> threads) {}

	record MemoryPool(@Nonnull String name, @Nonnull MemoryType type, long used, long committed, long max) {}

	record GarbageCollector(@Nonnull String name, long collectionCount, long collectionTimeMillis) {}

	record ThreadStateCounts(int runnable, int blocked, int waiting, int timedWaiting, int other, int total) {}

	record ThreadCapture(@Nonnull List<ThreadSample> threads, @Nonnull ThreadStateCounts states) {}

	record ThreadSample(long id, @Nonnull String name, boolean daemon, @Nonnull Thread.State state, long cpuTimeNanos,
	                    @Nonnull String topFrame) {}

	record CpuTimeLease(boolean enabledByMonitor) {}
}
