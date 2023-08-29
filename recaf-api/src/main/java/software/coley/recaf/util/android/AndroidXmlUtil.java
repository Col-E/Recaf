package software.coley.recaf.util.android;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.ByteHeaderUtil;

/**
 * Hacky utils for Android xml utils.
 *
 * @author Matt Coley
 */
public class AndroidXmlUtil {
	private static final int[] ANDROID_SCHEMA_URL = new int[]{
			's', 'c', 'h', 'e', 'm', 'a', 's', '.',
			'a', 'n', 'd', 'r', 'o', 'i', 'd', '.',
			'c', 'o', 'm'
	};
	private static final int[] ANDROID_SCHEMA_URL_UTF16 = new int[]{
			's', 0, 'c', 0, 'h', 0, 'e', 0, 'm', 0, 'a', 0, 's', 0, '.', 0,
			'a', 0, 'n', 0, 'd', 0, 'r', 0, 'o', 0, 'i', 0, 'd', 0, '.', 0,
			'c', 0, 'o', 0, 'm', 0
	};
	private static final int[] ANDROID_PERMISSION = new int[]{
			'a', 'n', 'd', 'r', 'o', 'i', 'd', '.',
			'p', 'e', 'r', 'm', 'i', 's', 's', 'i', 'o', 'n'
	};
	private static final int[] ANDROID_PERMISSION_UTF16 = new int[]{
			'a', 0, 'n', 0, 'd', 0, 'r', 0, 'o', 0, 'i', 0, 'd', 0, '.', 0,
			'p', 0, 'e', 0, 'r', 0, 'm', 0, 'i', 0, 's', 0, 's', 0, 'i', 0, 'o', 0, 'n'
	};
	private static final int[] INTENT_FILTER = new int[]{
			'i', 'n', 't', 'e', 'n', 't', '-',
			'f', 'i', 'l', 't', 'e', 'r'
	};
	private static final int[] INTENT_FILTER_UTF16 = new int[]{
			'i', 0, 'n', 0, 't', 0, 'e', 0, 'n', 0, 't', 0, '-', 0,
			'f', 0, 'i', 0, 'l', 0, 't', 0, 'e', 0, 'r'
	};

	/**
	 * @param data
	 * 		Data to check.
	 *
	 * @return {@code true} if likely obscured XML chunk model, where the header does not match.
	 */
	public static boolean hasXmlIndicators(@Nonnull byte[] data) {
		return ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_PERMISSION) ||
				ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_PERMISSION_UTF16) ||
				ByteHeaderUtil.matchAtAnyOffset(data, INTENT_FILTER) ||
				ByteHeaderUtil.matchAtAnyOffset(data, INTENT_FILTER_UTF16) ||
				ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_SCHEMA_URL) ||
				ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_SCHEMA_URL_UTF16);
	}
}
