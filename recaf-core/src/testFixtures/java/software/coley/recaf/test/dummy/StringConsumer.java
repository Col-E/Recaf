package software.coley.recaf.test.dummy;

import java.util.function.Consumer;

/**
 * Class to test things pertaining to implementing methods from JDK classes <i>(as opposed to our own interfaces)</i>
 */
@SuppressWarnings("all")
public class StringConsumer implements Consumer<String> {
	@Override
	public void accept(String s) {
		System.out.println("Consumed: " + s);
	}
}
