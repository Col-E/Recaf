package me.xdark.recaf.runtime;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.tinylog.Logger;

import java.io.Closeable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;

public final class RuntimeSimulator implements Closeable, AutoCloseable {

    private static final long THREAD_JOIN_MILLIS = 2500L;
    private final RuntimeClassLoader classLoader;
    private final ThreadGroup threadGroup;
    private final BlockingQueue<FutureTask<?>> commands;
    private final Thread runner;

    /**
     * Creates new instance of simulator
     *
     * @param scl system class loader
     */
    public RuntimeSimulator(ClassLoader scl) {
        this.classLoader = new RuntimeClassLoader(scl);
        this.threadGroup = new ThreadGroup("Simulator ThreadGroup");
        this.commands = new LinkedBlockingDeque<>();
        this.runner = bootstrap();
    }

    /**
     * Sends command to the emulator
     *
     * @param command the command to be executed
     * @param <V> type of the result
     * @return a {@link ListenableFuture} for the command
     */
    public <V> ListenableFuture<V> enqueue(Callable<V> command) {
        ListenableFutureTask<V> task = ListenableFutureTask.create(command);
        commands.add(task);
        return task;
    }

    private Thread bootstrap() {
        Thread t = new Thread(threadGroup, () -> {
            while (true) {
                FutureTask<?> task;
                try {
                    task = commands.take();
                    if (task instanceof ShutdownTask) {
                        break;
                    }
                    task.run();
                    task.get();
                } catch (Exception ex) {
                    Logger.error(ex, "Error processing command queue");
                }
            }
        }, "Simulator Runner Thread");
        t.setDaemon(true);
        t.setContextClassLoader(classLoader);
        t.start();
        return t;
    }

    @Override
    public void close() {
        // Send a signal to runner thread
        commands.add(new ShutdownTask());
        try {
            runner.join();
        } catch (InterruptedException ignored) {
            // No-op
        }
        // Attempt to release all loaded classes
        classLoader.close();
        // Now we have to shutdown thread group
        // in order to kill all threads that maybe was started
        // TODO: maybe there is a cleaner way to achieve it?
        Thread[] capture = new Thread[threadGroup.activeCount()];
        threadGroup.enumerate(capture);
        for (Thread t : capture) {
            // Let's give thread a try
            t.interrupt();
            try {
                t.join(THREAD_JOIN_MILLIS);
            } catch (InterruptedException ex) {
                Logger.warn("Thread %s did not shutdown in allotted time", t);
                // No chance here
                t.stop();
                try {
                    t.join(THREAD_JOIN_MILLIS);
                } catch (InterruptedException ignored) {
                    Logger.warn("Thread %s did not shutdown after Thread#stop(...) call!", t);
                }
            }
        }
        threadGroup.destroy();
    }

    private static class ShutdownTask extends FutureTask<Void> {

        ShutdownTask() {
            super(() -> null);
        }
    }

}
