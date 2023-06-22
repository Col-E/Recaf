package software.coley.recaf.test.dummy;

/**
 * Class to test interactions with a class that depends on another <i>(In the same package in this case)</i>.
 *
 * @see StringConsumer
 */
@SuppressWarnings("all")
public class StringConsumerUser {
	public static void main(String[] args) {
		for (String arg : args) {
			new StringConsumer().accept(arg);
		}
	}
}
