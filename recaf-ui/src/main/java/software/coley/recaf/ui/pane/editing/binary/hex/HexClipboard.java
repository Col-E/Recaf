package software.coley.recaf.ui.pane.editing.binary.hex;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Clipboard bridge for the hex editor.
 */
public final class HexClipboard {
	/** Internal clipboard format used to preserve arbitrary bytes between hex-editor operations. */
	@Nonnull
	public static final DataFormat HEX_BYTES_FORMAT = new DataFormat("application/x-recaf-hex-bytes");

	private HexClipboard() {}

	/**
	 * Writes a spaced hexadecimal string for other applications handling string content, and detached raw bytes for Recaf.
	 *
	 * @param source
	 * 		Source bytes.
	 * @param start
	 * 		Inclusive byte offset.
	 * @param end
	 * 		Exclusive byte offset.
	 *
	 * @return {@code true} when the system clipboard accepted the content.
	 */
	public static boolean copy(@Nonnull byte[] source, int start, int end) {
		byte[] bytes = Arrays.copyOfRange(source, start, end);
		ClipboardContent content = new ClipboardContent();
		content.putString(formatHex(bytes));
		content.put(HEX_BYTES_FORMAT, ByteBuffer.wrap(bytes));
		return Clipboard.getSystemClipboard().setContent(content);
	}

	/**
	 * Reads bytes from the system clipboard.
	 *
	 * @param hexColumnActive
	 * 		Whether external text should be decoded as hexadecimal rather than US-ASCII.
	 *
	 * @return Detached clipboard bytes, or {@code null} when the clipboard has no supported content.
	 *
	 * @throws IllegalArgumentException
	 * 		When clipboard text cannot be decoded in the active column's format, or Recaf's raw format is malformed.
	 */
	@Nullable
	public static byte[] readBytes(boolean hexColumnActive) {
		Clipboard clipboard = Clipboard.getSystemClipboard();
		if (clipboard.hasContent(HEX_BYTES_FORMAT)) {
			Object raw = clipboard.getContent(HEX_BYTES_FORMAT);
			if (!(raw instanceof ByteBuffer buffer))
				throw new IllegalArgumentException("Clipboard contains an unsupported raw byte format");

			ByteBuffer copy = buffer.duplicate();
			byte[] bytes = new byte[copy.remaining()];
			copy.get(bytes);
			return bytes;
		}

		if (!clipboard.hasString())
			return null;

		String text = clipboard.getString();
		if (text == null)
			return null;

		return hexColumnActive ? decodeHex(text) : encodeAscii(text);
	}

	@Nonnull
	private static String formatHex(byte[] bytes) {
		long capacity = bytes.length == 0 ? 0 : (long) bytes.length * 3 - 1;
		StringBuilder text = new StringBuilder((int) Math.min(Integer.MAX_VALUE - 8L, capacity));
		for (int i = 0; i < bytes.length; i++) {
			if (i > 0)
				text.append(' ');
			text.append(HexUtil.strFormat00(bytes[i]));
		}
		return text.toString();
	}

	@Nonnull
	private static byte[] decodeHex(@Nonnull String text) {
		int byteCount = countHexBytes(text);
		byte[] bytes = new byte[byteCount];
		int output = 0;
		int high = -1;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (high < 0 && isHexPrefix(text, i)) {
				i++;
				continue;
			}
			if (isSeparator(c))
				continue;
			int digit = HexUtil.hexDigit(c);
			if (high < 0)
				high = digit;
			else {
				bytes[output++] = (byte) ((high << 4) | digit);
				high = -1;
			}
		}
		return bytes;
	}

	private static int countHexBytes(@Nonnull String text) {
		int digits = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if ((digits & 1) == 0 && isHexPrefix(text, i)) {
				if (i + 3 >= text.length() || HexUtil.hexDigit(text.charAt(i + 2)) < 0
						|| HexUtil.hexDigit(text.charAt(i + 3)) < 0)
					throw malformedHex();
				i++;
				continue;
			}
			if (isSeparator(c)) {
				if ((digits & 1) != 0)
					throw malformedHex();
				continue;
			}
			if (HexUtil.hexDigit(c) < 0)
				throw malformedHex();
			digits++;
		}
		if ((digits & 1) != 0)
			throw malformedHex();
		return digits / 2;
	}

	private static boolean isHexPrefix(@Nonnull String text, int index) {
		return index + 1 < text.length() && text.charAt(index) == '0'
				&& (text.charAt(index + 1) == 'x' || text.charAt(index + 1) == 'X');
	}

	private static boolean isSeparator(char c) {
		return Character.isWhitespace(c) || c == ',' || c == ':' || c == ';';
	}

	@Nonnull
	private static byte[] encodeAscii(@Nonnull String text) {
		try {
			ByteBuffer buffer = StandardCharsets.US_ASCII.newEncoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.encode(CharBuffer.wrap(text));
			byte[] bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
			return bytes;
		} catch (CharacterCodingException ex) {
			throw new IllegalArgumentException("Clipboard text cannot be represented as US-ASCII", ex);
		}
	}

	@Nonnull
	private static IllegalArgumentException malformedHex() {
		return new IllegalArgumentException("Clipboard text is not valid hexadecimal byte input");
	}
}
