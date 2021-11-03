package me.coley.recaf.util;

import me.coley.recaf.decompile.DecompileResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ExceptionUtil {
    /**
     * Attempts at writing back the exception as a comment type string
     * @param e Exception thrown
     * @return String value of the exception
     */
    public static String writeExceptionAsComment(final Throwable e) {
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final PrintStream outputPrintStream = new PrintStream(outputStream);

            outputPrintStream.close();
            outputStream.close();

            final String var = outputStream.toString(StandardCharsets.UTF_8.name());
            final StringBuilder error = new StringBuilder();

            for (String s : var.split("\n")) {
                error.append("//");
                error.append(" ");
                error.append(s);
                error.append("\n");
            }

            return error.toString();
        } catch (IOException ex) {
            return "// Failed to parse exception";
        }
    }
}
