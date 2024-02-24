package software.coley.recaf.util.io;

import dev.xdark.ssvm.filesystem.NullOutputStream;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A print stream that sends content to a string consumer, {@link #setLineHandler(Consumer)}.
 *
 * @author Matt Coley
 */
public class BufferDelegatingPrintStream extends PrintStream {
	private final StringBuilder buffer = new StringBuilder();
	private final Charset charset;
	private Consumer<String> lineHandler;

	/**
	 * New stream with UTF-8 encoding.
	 */
	public BufferDelegatingPrintStream() {
		this(StandardCharsets.UTF_8);
	}

	/**
	 * New stream with a specified encoding.
	 *
	 * @param charset
	 * 		Encoding to use.
	 */
	public BufferDelegatingPrintStream(@Nonnull Charset charset) {
		super(new NullOutputStream(), true, charset);
		this.charset = charset;
	}

	private void sendToHandler() {
		int end;
		while ((end = buffer.indexOf("\n")) > -1) {
			if (lineHandler != null) {
				String line = buffer.substring(0, end);
				lineHandler.accept(line);
			}
			buffer.delete(0, end + 1);
		}
	}

	/**
	 * @return Current consumer handling per-line strings sent to this print stream.
	 */
	@Nullable
	public Consumer<String> getLineHandler() {
		return lineHandler;
	}

	/**
	 * @param lineHandler
	 * 		Consumer to handle per-line strings sent to this print stream.
	 */
	public void setLineHandler(@Nullable Consumer<String> lineHandler) {
		this.lineHandler = lineHandler;
	}

	@Override
	public void write(int b) {
		buffer.append((char) b);
		sendToHandler();
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		ByteBuffer bb = ByteBuffer.wrap(buf, off, len);
		buffer.append(charset.decode(bb));
		sendToHandler();
	}

	@Override
	public void print(boolean b) {
		buffer.append(b);
		sendToHandler();
	}

	@Override
	public void print(char c) {
		buffer.append(c);
		sendToHandler();
	}

	@Override
	public void print(int i) {
		buffer.append(i);
		sendToHandler();
	}

	@Override
	public void print(long l) {
		buffer.append(l);
		sendToHandler();
	}

	@Override
	public void print(float f) {
		buffer.append(f);
		sendToHandler();
	}

	@Override
	public void print(double d) {
		buffer.append(d);
		sendToHandler();
	}

	@Override
	public void print(char[] s) {
		buffer.append(s);
		sendToHandler();
	}

	@Override
	public void print(String s) {
		buffer.append(s);
		sendToHandler();
	}

	@Override
	public void print(Object obj) {
		buffer.append(obj);
		sendToHandler();
	}

	@Override
	public void println() {
		buffer.append('\n');
		sendToHandler();
	}

	@Override
	public void println(boolean x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(char x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(int x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(long x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(float x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(double x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(char[] x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(String x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public void println(Object x) {
		buffer.append(x).append('\n');
		sendToHandler();
	}

	@Override
	public PrintStream printf(@Nonnull String format, Object... args) {
		buffer.append(String.format(format, args));
		sendToHandler();
		return this;
	}

	@Override
	public PrintStream printf(Locale l, @Nonnull String format, Object... args) {
		buffer.append(String.format(format, args));
		sendToHandler();
		return this;
	}

	@Override
	public PrintStream format(@Nonnull String format, Object... args) {
		buffer.append(String.format(format, args));
		sendToHandler();
		return this;
	}

	@Override
	public PrintStream format(Locale l, @Nonnull String format, Object... args) {
		buffer.append(String.format(format, args));
		sendToHandler();
		return this;
	}

	@Override
	public PrintStream append(CharSequence csq) {
		buffer.append(csq);
		sendToHandler();
		return this;
	}

	@Override
	public PrintStream append(CharSequence csq, int start, int end) {
		buffer.append(csq, start, end);
		sendToHandler();
		return this;
	}

	@Override
	public PrintStream append(char c) {
		buffer.append(c);
		sendToHandler();
		return this;
	}
}
