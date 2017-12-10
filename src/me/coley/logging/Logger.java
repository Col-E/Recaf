package me.coley.logging;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public abstract class Logger<Stream extends OutputStream> implements Closeable {
	/**
	 * Default clock used by the {@link #FORMATTER} associated with the user's
	 * system default time-zone.
	 */
	protected final Clock zoneClock = Clock.systemDefaultZone();
	/**
	 * Default formatter to be used by children in
	 * {@link #format(Level, String)}.
	 */
	//@formatter:off
	protected final static DateTimeFormatter FORMATTER = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.SHORT)
			.withLocale(Locale.getDefault())
			.withZone(ZoneId.systemDefault());
	//@formatter:on
	/**
	 * Output stream of logging contents.
	 */
	protected final Stream out;
	/**
	 * Level of detail to log.
	 */
	protected Level level;

	public Logger(Stream out, Level level) {
		this.out = out;
		this.level = level;
	}

	/**
	 * Send the given message to the {@link #out logging destination} if
	 * 
	 * @param lvl
	 *            Message level of detail.
	 * @param message
	 *            Message contents.
	 */
	public void log(Level lvl, String message) {
		// Lower levels are for finer details
		if (level.ordinal() <= lvl.ordinal()) {
			try {
				write(format(lvl, message));
			} catch (Exception e) {}
		}
	}

	/**
	 * Writes the message to the out-stream.
	 * 
	 * @param fmtMessage
	 *            Formatted message
	 * @throws IOException
	 *             Thrown if writing or flushing fails.
	 */
	protected void write(String fmtMessage) throws IOException {
		out.write(fmtMessage.getBytes(StandardCharsets.UTF_8));
		out.flush();
	}

	/**
	 * Formats the message before it is sent to the {@link #out logging
	 * destination}.
	 * 
	 * @param lvl
	 *            Message level of detail.
	 * @param message
	 *            Message contents.
	 * @return Formatted message for output.
	 */
	protected String format(Level lvl, String message) {
		Instant currentTime = Instant.now(zoneClock);
		String time = FORMATTER.format(currentTime);
		return String.format("[%s:%s] %s\n", lvl.name(), time, message);
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	/**
	 * @return Logging detail level.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * @param level
	 *            Logging detail level to set.
	 */
	public void setLevel(Level level) {
		this.level = level;
	}
}
