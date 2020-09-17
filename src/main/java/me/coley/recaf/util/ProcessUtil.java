package me.coley.recaf.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Set of utilities for {@link Process}.
 *
 * @author xxDark
 */
public final class ProcessUtil {

    /**
     * Deny all constructions.
     */
    private ProcessUtil() { }

    /**
     * Waits for process to finish.
     *
     * @param process
     *      Process to wait for.
     * @param timeout
     *      The maximum time to wait.
     * @param unit
     *      The time unit of the {@code timeout} argument.
     *
     * @return {@code true} if process was terminated.
     */
    public static boolean waitFor(Process process, long timeout, TimeUnit unit) {
        long now = System.currentTimeMillis();
        while (timeout > 0L) {
            try {
                return process.waitFor(timeout, unit);
            } catch (InterruptedException ex) {
                timeout -= (System.currentTimeMillis() - now);
            }
        }
        return false;
    }

    /**
     * Reads <i>stderr</i> of the process.
     *
     * @param process
     *      Process to read error from.
     * @return
     *      Content of process's <i>stderr</i>.
     *
     * @throws IOException
     *      When any I/O error occur.
     */
    public static List<String> readProcessError(Process process) throws IOException {
        List<String> result = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        }
        return result;
    }
}
