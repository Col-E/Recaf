package software.coley.recaf.test.dummy;

/**
 * Class to test array things.
 */
@SuppressWarnings("all")
public class ArrayStuff {
	public static int[] ceil(int[] a, int[] b) {
		int minLen = Math.min(a.length, b.length);
		int maxLen = Math.max(a.length, b.length);
		int[] c = new int[maxLen];
		for (int i = 0; i < minLen; i++)
			c[i] = Math.max(a[i], b[i]);
		if (minLen != maxLen) {
			int[] longer = a.length > b.length ? a : b;
			System.arraycopy(a, minLen, c, minLen, maxLen - minLen);
		}
		return c;
	}

	public static int sum(boolean[] values) {
		int sum = 0;
		for (boolean value : values)
			if (value) sum++;
		return sum;
	}

	public static int sum(byte[] values) {
		int sum = 0;
		for (int value : values)
			sum += value;
		return sum;
	}

	public static int sum(short[] values) {
		int sum = 0;
		for (int value : values)
			sum += value;
		return sum;
	}

	public static int sum(char[] values) {
		int sum = 0;
		for (int value : values)
			sum += value;
		return sum;
	}

	public static int sum(int[] values) {
		int sum = 0;
		for (int value : values)
			sum += value;
		return sum;
	}

	public static double sum(double[] values) {
		double sum = 0;
		for (double value : values)
			sum += value;
		return sum;
	}
}
