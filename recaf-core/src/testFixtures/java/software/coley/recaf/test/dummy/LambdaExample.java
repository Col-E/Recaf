package software.coley.recaf.test.dummy;

public class LambdaExample {
	public static void main(String[] args) {
		Runnable runnable = () -> System.out.println("lambda");
		runnable.run();
	}
}
