package software.coley.recaf.services.tutorial.content;

// This class shows some math operations.
// The goal is to find the value of what gets passed to 'consume'.
// The way this math is set up is such that it can be entirely simulated by the JASM analysis engine.
//
// When the user opens the 'Analysis' tab in the assembler and looks at the final assignment to 'c'
// they should find the answer. This chapter teaches them how to open the assembler and use the analysis tab.
public class Chapter4 {
	// 25565
	private static final int answer = 0;

	public static void main(String[] args) {
		int a = "a".repeat(16).length();
		int b = Integer.parseInt("FF", a);
		int c = Integer.parseInt("FF", a << 1);
		a = Integer.min(a, b);
		b = (c / a) << 10;
		a = (int) Math.floor(b * 0.83);
		c = b - ((c * 10) - (a / 1000));
		a = -100 + (a - b) / 40;
		c += a;
		consume(c);
	}

	private static void consume(int value) {
		System.out.println(value);
	}
}
