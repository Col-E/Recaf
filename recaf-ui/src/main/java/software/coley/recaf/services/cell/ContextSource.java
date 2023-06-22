package software.coley.recaf.services.cell;

/**
 * Allows the {@link ContextMenuProviderFactory} types to know additional information about the context of the inputs.
 * For instance, if the request to provide a context menu for some data is based on <b>the declaration</b> of the data
 * or <b>a reference to</b> the data.
 *
 * @author Matt Coley
 */
public interface ContextSource {
	/**
	 * Constant describing declaration context sources.
	 */
	ContextSource DECLARATION = new ContextSource() {
		@Override
		public boolean isDeclaration() {
			return true;
		}

		@Override
		public boolean isReference() {
			return false;
		}
	};

	/**
	 * Constant describing reference context sources.
	 */
	ContextSource REFERENCE = new ContextSource() {
		@Override
		public boolean isDeclaration() {
			return false;
		}

		@Override
		public boolean isReference() {
			return true;
		}
	};

	/**
	 * @return {@code true} if the context is of a declaration.
	 */
	boolean isDeclaration();

	/**
	 * @return {@code true} if the context is of a reference.
	 */
	boolean isReference();
}
