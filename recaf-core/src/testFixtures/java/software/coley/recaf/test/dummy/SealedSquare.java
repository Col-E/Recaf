package software.coley.recaf.test.dummy;

public record SealedSquare(double side) implements SealedShape {
	@Override
	public double area() {
		return side * side;
	}
}
