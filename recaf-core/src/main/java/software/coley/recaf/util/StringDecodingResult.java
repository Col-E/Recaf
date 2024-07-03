package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;

/**
 * Model of attempted string decoding.
 *
 * @param raw
 * 		Bytes that were attempted to be decoded.
 * @param charset
 * 		Determined encoding of the text bytes. {@code null} if the bytes did not get interpreted as text.
 * @param text
 * 		Determined string representation of the text bytes. {@code null} if the bytes did not get interpreted as text.
 *
 * @author Matt Coley
 * @see StringUtil#decodeString(byte[]) Utility method to guess the encoding of some arbitrary input.
 */
public record StringDecodingResult(@Nonnull byte[] raw, @Nullable Charset charset, @Nullable String text) {
	/**
	 * @return {@code true} when the {@link #raw() raw bytes} could be decoded into a known {@link #charset() charset}.
	 */
	public boolean couldDecode() {
		return charset != null;
	}
}
