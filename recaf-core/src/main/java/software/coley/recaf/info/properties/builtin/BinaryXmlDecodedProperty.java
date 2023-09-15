package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;

/**
 * Built in property for caching the decoded contents of {@link BinaryXmlFileInfo} items.
 *
 * @author Matt Coley
 */
public class BinaryXmlDecodedProperty extends BasicProperty<String> {
	public static final String KEY = "decoded-bin-xml";

	/**
	 * @param value
	 * 		Decoded content.
	 */
	public BinaryXmlDecodedProperty(@Nonnull String value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Decoded XML associated with instance, or {@code null} when no association exists.
	 */
	@Nullable
	public static String get(@Nonnull Info info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param xml
	 * 		Decoded XML to associate with the item.
	 */
	public static void set(@Nonnull Info info, @Nonnull String xml) {
		info.setProperty(new BinaryXmlDecodedProperty(xml));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
