package software.coley.recaf.test.dummy;

public sealed interface SealedShape permits SealedCircle, SealedOtherShape, SealedSquare {
	double area();
}
