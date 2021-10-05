package me.coley.recaf.code;

/**
 * Literal item that contains an absolute value. This is opposed to an item info that contains data that cannot be
 * represented literally in a raw format, such as classes of an Android dex file.
 *
 * @author Matt Coley
 */
public interface LiteralInfo {
	/**
	 * @return Item value.
	 */
	byte[] getValue();
}
