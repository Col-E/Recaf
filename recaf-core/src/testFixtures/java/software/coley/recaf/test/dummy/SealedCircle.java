package software.coley.recaf.test.dummy;

public record SealedCircle(double radius) implements SealedShape {
	@Override
	public double area() {
		return Math.PI * radius * radius;
	}
}
