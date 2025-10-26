package software.coley.recaf.services.tutorial.content;

// Whatever the last chapter is, when it is completed toggle a value in the Recaf config so that users aren't
// always perma-prompted to redo the tutorial. It will remain in the help menu, but the message on the welcome
// panel won't be in your face.
public class Chapter7 {
	public static void main(String[] args) {
		System.out.println(Chapter6.whereIsThisUsed());
	}
}
