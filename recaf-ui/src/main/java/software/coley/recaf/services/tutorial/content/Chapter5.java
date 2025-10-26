package software.coley.recaf.services.tutorial.content;

// We will now ask the user to use the 'Java to bytecode' tool in the assembler.
// The decrypt method is a stub, and they will need to implement it.
// In the tutorial builder we will obfuscate this class so that they can't just recompile it.
public class Chapter5 {
	// This method will get replaced when the user succeeds and point the user to the next chapter.
	public static void main(String[] args) {
		System.out.println(decrypt());
	}

	public static String decrypt() {
		return decrypt("쭴쭸쭹쭡쭲쭥쭣쭲쭳쬺쭠쭾쭣쭿쬺쭲쭶쭤쭲"); // converted-with-ease
	}

	// This method will be renamed to 'this whole sentence is the name of the method'
	@SuppressWarnings("all")
	private static String decrypt(String text) {
//		// "this whole sentence is the name of the method".hashCode();
//		int key = Thread.currentThread().getStackTrace()[1].getMethodName().hashCode();
//		char[] chars = text.toCharArray();
//		for (int i = 0; i < chars.length; i++)
//			chars[i] ^= key;
//		return String.valueOf(chars);
		return null;
	}
}
