package software.coley.recaf.services.tutorial.content;

// A hidden value will be inserted into the constant pool of this class, but never referenced.
// We will instruct the user to use the low level view to find it.
public class Chapter3 implements Runnable {
	// Answer is the 'LowLevelView'
	@SuppressWarnings("all")
	private static final String answer = "Put_Your_Answer_Here";

	@Override
	public void run() {
		impl();
	}

	private static void impl() {
		try {
			Object theAnswer = Chapter3.class.getDeclaredFields()[0].get(null);
			System.out.println("Finding secrets is easy with: " + theAnswer);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		}
	}
}
