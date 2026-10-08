package software.coley.recaf.info.properties.builtin;

import com.google.common.hash.Hashing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.BasicProperty;

import java.util.Arrays;

/**
 * Built in property to retain hashes for original file content.
 *
 * @author Matt Coley
 */
public class OriginalFileHashesProperty extends BasicProperty<OriginalFileHashesProperty.Hashes> {
	public static final String KEY = "original-file-hashes";

	/**
	 * @param hashes
	 * 		Hashes of the original file content.
	 */
	public OriginalFileHashesProperty(@Nonnull Hashes hashes) {
		super(KEY, hashes);
	}

	/**
	 * @param content
	 * 		Original file content.
	 *
	 * @return Hashes of the content.
	 */
	@Nonnull
	@SuppressWarnings("deprecation") // Don't care that MD5/SHA1 are deprecated, people still use them frequently.
	public static Hashes compute(@Nonnull byte[] content) {
		return new Hashes(Arrays.hashCode(content),
				Hashing.md5().hashBytes(content).toString(),
				Hashing.sha1().hashBytes(content).toString(),
				Hashing.sha256().hashBytes(content).toString(),
				Hashing.sha512().hashBytes(content).toString());
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Hashes of the original file content, or {@code null} when absent.
	 */
	@Nullable
	public static Hashes get(@Nonnull Info info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param content
	 * 		Original file content.
	 */
	public static void set(@Nonnull Info info, @Nonnull byte[] content) {
		info.setProperty(new OriginalFileHashesProperty(compute(content)));
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Hash code of the original file content, or {@code null} when absent.
	 */
	public static int hash(@Nonnull Info info) {
		OriginalFileHashesProperty.Hashes hashes = get(info);
		if (hashes != null)
			return hashes.rawContentHashCode();

		byte[] content;
		if (info instanceof FileInfo fileInfo)
			content = fileInfo.getRawContent();
		else if (info instanceof JvmClassInfo classInfo)
			content = classInfo.getBytecode();
		else
			throw new IllegalArgumentException("Info type does not have content: " + info.getClass().getName());

		hashes = compute(content);
		info.setProperty(new OriginalFileHashesProperty(hashes));
		return hashes.rawContentHashCode();
	}

	/**
	 * Immutable hashes of original file content.
	 *
	 * @param rawContentHashCode
	 * 		Hash code compatible with {@link java.util.Arrays#hashCode(byte[])}.
	 * @param md5
	 * 		MD5 digest.
	 * @param sha1
	 * 		SHA-1 digest.
	 * @param sha256
	 * 		SHA-256 digest.
	 * @param sha512
	 * 		SHA-512 digest.
	 */
	public record Hashes(int rawContentHashCode,
	                     @Nonnull String md5,
	                     @Nonnull String sha1,
	                     @Nonnull String sha256,
	                     @Nonnull String sha512) {}
}
