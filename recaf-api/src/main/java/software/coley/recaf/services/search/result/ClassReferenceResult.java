package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Result of a class reference match.
 *
 * @author Matt Coley
 */
public class ClassReferenceResult extends Result<ClassReferenceResult.ClassReference> {
	private final ClassReference ref;

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param name
	 * 		Class name.
	 */
	public ClassReferenceResult(@Nonnull PathNode<?> path, @Nonnull String name) {
		this(path, new ClassReference(name));
	}

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param ref
	 * 		Class reference.
	 */
	public ClassReferenceResult(@Nonnull PathNode<?> path, @Nonnull ClassReference ref) {
		super(path);
		this.ref = ref;
	}

	@Nonnull
	@Override
	protected ClassReference getValue() {
		return ref;
	}

	public static class ClassReference {
		private final String name;

		public ClassReference(@Nonnull String name) {
			this.name = name;
		}

		/**
		 * @return Class name.
		 */
		@Nonnull
		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return "ClassReference{" +
					"name='" + name + '\'' +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ClassReference that = (ClassReference) o;

			return name.equals(that.name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}
}
