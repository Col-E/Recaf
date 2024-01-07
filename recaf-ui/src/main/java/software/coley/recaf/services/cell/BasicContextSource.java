package software.coley.recaf.services.cell;

/**
 * Basic context source. Either a declaration or a reference.
 *
 * @author Matt Coley
 */
public class BasicContextSource implements ContextSource {
	private final boolean isDeclaration;

	/**
	 * @param isDeclaration
	 *        {@code true} for the source to model a declaration.
	 *        {@code false} for the source to model a reference.
	 */
	public BasicContextSource(boolean isDeclaration) {
		this.isDeclaration = isDeclaration;
	}

	@Override
	public boolean isDeclaration() {
		return isDeclaration;
	}

	@Override
	public boolean isReference() {
		return !isDeclaration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicContextSource that = (BasicContextSource) o;

		return isDeclaration == that.isDeclaration;
	}

	@Override
	public int hashCode() {
		return (isDeclaration ? 1 : 0);
	}

	@Override
	public String toString() {
		return "BasicContextSource{" +
				"isDeclaration=" + isDeclaration +
				'}';
	}
}
