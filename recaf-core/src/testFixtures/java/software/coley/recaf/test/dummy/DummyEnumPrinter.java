package software.coley.recaf.test.dummy;

/**
 * Dummy class to show that enum's {@code $VALUES} field and {@code values()} methods are not remapped.
 */
@SuppressWarnings("all")
public class DummyEnumPrinter {
	public static void main(String[] args) {
		run1();
		run2();
	}

	public static String run1() {
		StringBuilder sb = new StringBuilder(DummyEnum.class.getName() + ":");
		for (DummyEnum enumConstant : DummyEnum.values()) {
			String name = enumConstant.name();
			System.out.println(name);
			sb.append("[" + name + "]");
			DummyEnum valueOf = Enum.valueOf(DummyEnum.class, name);
			if (enumConstant != valueOf) {
				System.err.println("Mismatch: " + name);
			}
		}
		return sb.toString();
	}

	public static String run2() {
		StringBuilder sb = new StringBuilder(DummyEnum.class.getName() + ":");
		for (DummyEnum enumConstant : DummyEnum.class.getEnumConstants()) {
			String name = enumConstant.name();
			System.out.println(name);
			sb.append("[" + name + "]");
		}
		return sb.toString();
	}
}
