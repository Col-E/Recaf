package software.coley.recaf.test.dummy;

/**
 * Dummy class with a inheritance examples.
 */
@SuppressWarnings("all")
public class Inheritance {
	public interface Edible {
	}

	public interface Red {
	}

	public class Apple implements Edible, Red {
	}

	public class AppleWithWorm extends Apple {
	}

	public class Grape implements Edible {
	}

	public class NotFoodException extends Exception {
	}
}
