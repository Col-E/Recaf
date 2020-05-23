package me.coley.recaf.util;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Faster I/O utils
 *
 * @author xxDark
 */
public final class IOUtil {
    /**
     * Indicates that we can read as many bytes
     * as we want.
     */
    public static final int ANY = Integer.MIN_VALUE;
    /**
     * Default buffer size.
     */
    private static final int BUFFER_SIZE = 4096;

    private IOUtil() {
    }

    /**
     * Transfers data from input to output stream.
     *
     * @param in     an input stream
     * @param out    an output stream
     * @param buffer data buffer
     * @param max    maximum amount of bytes to transfer
     * @return amount of bytes read
     * @throws IOException if any I/O error occurs
     */
    public static int transfer(InputStream in, OutputStream out, byte[] buffer, int max) throws IOException {
        int transferred = 0;
        int r;
        while ((max == ANY || max > 0) && (r = in.read(buffer, 0, buffer.length)) != -1) {
            transferred += r;
            out.write(buffer, 0, r);
            if (max != ANY) {
                max -= r;
            }
        }
        return transferred;
    }

    /**
     * Transfers data from input to output stream.
     * No limits.
     *
     * @param in     an input stream
     * @param out    an output stream
     * @param buffer data buffer
     * @return amount of bytes read
     * @throws IOException if any I/O error occurs
     */
    public static int transfer(InputStream in, OutputStream out, byte[] buffer) throws IOException {
        return transfer(in, out, buffer, ANY);
    }

    /**
     * Transfers data from input to output stream.
     *
     * @param in  an input stream
     * @param out an output stream
     * @param max maximum amount of bytes to transfer
     * @return amount of bytes read
     * @throws IOException if any I/O error occurs
     */
    public static int transfer(InputStream in, OutputStream out, int max) throws IOException {
        return transfer(in, out, new byte[BUFFER_SIZE], max);
    }

    /**
     * Transfers data from url to output stream.
     * No limits.
     *
     * @param url    the url
     * @param out    an output stream
     * @param buffer data buffer
     * @return amount of bytes read
     * @throws IOException if any I/O error occurs
     */
    public static int transfer(URL url, OutputStream out, byte[] buffer) throws IOException {
        try (InputStream in = url.openStream()) {
            return transfer(in, out, buffer, ANY);
        }
    }

    /**
     * Transfers data from url to output stream.
     *
     * @param url the url
     * @param out an output stream
     * @param max maximum amount of bytes to transfer
     * @return amount of bytes read
     * @throws IOException if any I/O error occurs
     */
    public static int transfer(URL url, OutputStream out, int max) throws IOException {
        try (InputStream in = url.openStream()) {
            return transfer(in, out, ANY);
        }
    }

    /**
     * Transfers data from url to output stream.
     * No limits.
     *
     * @param url the url
     * @param out an output stream
     * @return amount of bytes read
     * @throws IOException if any I/O error occurs
     */
    public static int transfer(URL url, OutputStream out) throws IOException {
        try (InputStream in = url.openStream()) {
            return transfer(in, out, ANY);
        }
    }

    /**
     * Reads data from input stream to byte array.
     *
     * @param url    the url
     * @param out    an output stream
     * @param buffer data buffer
     * @param max    maximum amount of bytes to transfer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url, ByteArrayOutputStream out, byte[] buffer, int max)
            throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in, out, buffer, max);
        }
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param url    the url
     * @param out    an output stream
     * @param length data buffer length
     * @param max    maximum amount of bytes to transfer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url, ByteArrayOutputStream out, int length, int max)
            throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in, out, length, max);
        }
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param url    the url
     * @param out    an output stream
     * @param buffer data buffer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url, ByteArrayOutputStream out, byte[] buffer) throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in, out, buffer);
        }
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param url    the url
     * @param out    an output stream
     * @param length data buffer length
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url, ByteArrayOutputStream out, int length) throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in, out, length);
        }
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param url the url
     * @param out an output stream
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url, ByteArrayOutputStream out) throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in, out);
        }
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param in an input stream
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in) throws IOException {
        return toByteArray(in, new ByteArrayOutputStream(in.available()));
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param in     an input stream
     * @param buffer data buffer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in, byte[] buffer) throws IOException {
        return toByteArray(in, new ByteArrayOutputStream(in.available()), buffer);
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param in     an input stream
     * @param length data buffer length
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in, int length) throws IOException {
        return toByteArray(in, new ByteArrayOutputStream(in.available()), length);
    }


    /**
     * Reads data from input stream to byte array.
     *
     * @param in     an input stream
     * @param out    an output stream
     * @param buffer data buffer
     * @param max    maximum amount of bytes to transfer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, byte[] buffer, int max)
            throws IOException {
        transfer(in, out, buffer, max);
        return out.toByteArray();
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param in     an input stream
     * @param out    an output stream
     * @param length data buffer length
     * @param max    maximum amount of bytes to transfer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, int length, int max)
            throws IOException {
        return toByteArray(in, out, new byte[length], max);
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param in     an input stream
     * @param out    an output stream
     * @param buffer data buffer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, byte[] buffer) throws IOException {
        return toByteArray(in, out, buffer, ANY);
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param in     an input stream
     * @param out    an output stream
     * @param length data buffer length
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out, int length) throws IOException {
        return toByteArray(in, out, new byte[length]);
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param in  an input stream
     * @param out an output stream
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(InputStream in, ByteArrayOutputStream out) throws IOException {
        return toByteArray(in, out, new byte[BUFFER_SIZE]);
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param url the url
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in);
        }
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param url    the url
     * @param buffer data buffer
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url, byte[] buffer) throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in, buffer);
        }
    }

    /**
     * Reads data from input stream to byte array.
     * No limits.
     *
     * @param url    an input stream
     * @param length data buffer length
     * @return array of bytes
     * @throws IOException if any I/O error occurs
     */
    public static byte[] toByteArray(URL url, int length) throws IOException {
        try (InputStream in = url.openStream()) {
            return toByteArray(in, new ByteArrayOutputStream(in.available()), length);
        }
    }

    /**
     * @param path The path to get extension from.
     * @return path extension.
     */
    public static String getExtension(Path path) {
        String name = path.getFileName().toString();
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * @param prefix path prefix
     * @param suffix path suffix
     * @return new created temp path.
     * @throws IOException if any I/O error occur.
     */
    public static Path createTempFile(String prefix, String suffix) throws IOException {
        Path path = Files.createTempFile(prefix, suffix);
        path.toFile().deleteOnExit();
        return path;
    }

    /**
     * @param file
     * the file to convert to path
     * @return path from file
     */
    public static Path toPath(File file) {
        return file.toPath().normalize();
    }

    /**
     * @param path the path
     * @return normalized and absolute path as string
     */
    public static String toString(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
