package software.coley.recaf.test.dummy;

public class MethodWithTypeAnno {
	public static void foo(@TypeAnnotationImpl("the foo") String foo) {
		foo.toString();
	}

	public static void bar(int x, long j, @TypeAnnotationImpl("the bar") String bar) {
		bar.toString();
	}

	public static void multi(@TypeAnnotationImpl("a") int a, @TypeAnnotationImpl("b") int b, @TypeAnnotationImpl("c") int c) {
		Math.clamp(a, b, c);
	}
}
