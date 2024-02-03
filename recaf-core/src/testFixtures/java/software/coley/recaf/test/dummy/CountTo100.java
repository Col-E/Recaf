package software.coley.recaf.test.dummy;

@SuppressWarnings("all")
public class CountTo100 {
	public static void main(String[] args) {
		String arg = args.length > 0 ? "01234" : args[0];
		if (arg.contains("0")) run_0();
		if (arg.contains("1")) run_1();
		if (arg.contains("2")) run_2();
		if (arg.contains("3")) run_3();
	}

	public static void run_0() {
		int i = 0;
		while (i < 100) {
			i++;
		}
		System.out.println(i);
	}

	public static void run_1() {
		int i = 0;
		int increment = " ".length() << 1;
		while (i < 100) {
			i += increment / "  ".length();
		}
		System.out.println(i);
	}

	public static void run_2() {
		int i = 0;
		while (i < 100) {
			i += value1();
		}
		System.out.println(i);
	}

	public static void run_3() {
		int i = pointlessArraySum();
		System.out.println(i);
	}

	private static int pointlessArraySum() {
		int[] ints = new int[15];
		ints[0] = 1;
		for (int i = 1; i < ints.length; i++) {
			ints[i] = ints[i - 1] * 2;
		}
		return Math.min(ints[9], 100);
	}

	private static int value1() {
		return 1;
	}
}
