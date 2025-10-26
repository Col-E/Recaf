package software.coley.recaf.services.tutorial.content;

@SuppressWarnings("all")
public class Chapter1 {
	// Just ask the user to change this string and save the changes
	public static final String message = "Hello World!";

	public static void main(String[] args) {
		run();
	}

	private static void run() {
		try {
			// The tutorial will use field 'DefaultValue' attributes in a few cases and
			// that means making the fields 'static final' which will inline primitives.
			// This has the unfortunate downside of inlining at compile time, which we
			// want to avoid to prevent confusion.
			Object theMessage = Chapter1.class.getDeclaredFields()[0].get(null);
			System.out.println(theMessage);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
