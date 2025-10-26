package software.coley.recaf.services.tutorial.content;

public class Chapter2 extends Chapter3 {
	// The class should be filtered to only show this field.
	//  - No super-type
	//  - No methods
	// We will tell the user to use the 'fields & methods' tab to find the method.
	private int findTheHiddenMessage;

	private static void hiddenMethod() {
		// Once this method is revealed, tell them to use the 'inheritance' tab to
		// see that this extends the class representing the next class
		System.out.println("You found me!");
		System.out.println("Now go to the next chapter in the 'Inheritance' tab!");
	}
}
