package software.coley.recaf.test.dummy;

public class LambdaCalcExample {
	public static void main(String[] args) {
		MathOperation addition = (a, b) -> a + b;
		MathOperation multiplication = (a, b) -> a * b;
		addition.operate(5, 3);
		multiplication.operate(5, 3);
	}

	interface MathOperation {
		int operate(int a, int b);
	}
}
