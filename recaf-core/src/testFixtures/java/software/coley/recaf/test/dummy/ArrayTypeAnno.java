package software.coley.recaf.test.dummy;

public class ArrayTypeAnno {
	public static void foo(String[][]@TypeAnnotationImpl("the foo") [][] foo) {
		System.out.println(foo.length);
	}
}
