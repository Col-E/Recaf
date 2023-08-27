package software.coley.recaf.util.android;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.ByteHeaderUtil;

/**
 * Hacky utils for Android chunk utils.
 *
 * @author Matt Coley
 */
public class AndroidChunkUtil {
	private static int[] EMPTY_HEADER = new int[2];
	private static int[] ANDROID_SCHEMA_URL = new int[]{
			's', 'c', 'h', 'e', 'm', 'a', 's', '.',
			'a', 'n', 'd', 'r', 'o', 'i', 'd', '.',
			'c', 'o', 'm'
	};
	private static int[] ANDROID_SCHEMA_URL_ALT = new int[]{
			's', 0, 'c', 0, 'h', 0, 'e', 0, 'm', 0, 'a', 0, 's', 0, '.', 0,
			'a', 0, 'n', 0, 'd', 0, 'r', 0, 'o', 0, 'i', 0, 'd', 0, '.', 0,
			'c', 0, 'o', 0, 'm', 0
	};
	private static int[] ANDROID_PERMISSION = new int[]{
			'a', 'n', 'd', 'r', 'o', 'i', 'd', '.',
			'p', 'e', 'r', 'm', 'i', 's', 's', 'i', 'o', 'n'
	};
	private static int[] INTENT_FILTER = new int[]{
			'i', 'n', 't', 'e', 'n', 't', '-',
			'f', 'i', 'l', 't', 'e', 'r'
	};

	/**
	 * @param data
	 * 		Data to check.
	 *
	 * @return {@code true} if likely obscured XML chunk model.
	 */
	public static boolean isObscuredXml(@Nonnull byte[] data) {
		return ByteHeaderUtil.match(data, EMPTY_HEADER) &&
				(ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_PERMISSION) ||
						ByteHeaderUtil.matchAtAnyOffset(data, INTENT_FILTER) ||
						ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_SCHEMA_URL) ||
						ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_SCHEMA_URL_ALT));
	}
}
