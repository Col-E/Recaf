package software.coley.recaf.util.android;

import org.checkerframework.checker.units.qual.N;
import software.coley.recaf.util.ByteHeaderUtil;

/**
 * Hacky utils for Android chunk utils.
 *
 * @author Matt Coley
 */
public class AndroidChunkUtil {
	private static int[] EMPTY_HEADER = new int[2];
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
	public static boolean isObscuredXml(@N byte[] data) {
		return ByteHeaderUtil.match(data, EMPTY_HEADER) &&
				(ByteHeaderUtil.matchAtAnyOffset(data, ANDROID_PERMISSION) ||
						ByteHeaderUtil.matchAtAnyOffset(data, INTENT_FILTER));
	}
}
