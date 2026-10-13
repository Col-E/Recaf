package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;

/**
 * A requirement that generated phantoms must satisfy. Hints come from evidence outside the analyzed bytecode, such as
 * a compiler reporting that a type is missing, that a phantom cannot be converted to a known type, or that a member
 * does not exist.
 * <p>
 * There are three forms:
 * <ul>
 *     <li>{@link TypeOnly}: the type must exist as a phantom, with no relation to any other type.</li>
 *     <li>{@link Supertype}: the phantom must extend or implement another type.</li>
 *     <li>{@link Member}: the phantom must declare a field or method.</li>
 * </ul>
 * A hint is honored only when {@link #phantomName()} names a type that the generator is free to create or shape.
 * Known types are never changed, so hints naming them are ignored.
 *
 * @author jus7n
 * @see PhantomGenerator#createPhantomsForClasses(Workspace, java.util.Collection, int, java.util.Collection)
 */
public sealed interface PhantomHint {
	/**
	 * @return Internal name of the phantom that this hint requires or shapes.
	 */
	@Nonnull
	String phantomName();

	/**
	 * Requires a type to exist as a phantom.
	 *
	 * @param phantomName
	 * 		Internal name of the type that must exist.
	 */
	record TypeOnly(@Nonnull String phantomName) implements PhantomHint {}

	/**
	 * Requires a phantom to be a subtype of another type.
	 * <p>
	 * The kind of {@code supertype} decides the relation that is required.
	 * A supertype class implies extension, while a supertype interface implies implementation.
	 * A superclass hint on a phantom that is an interface is ignored, since an interface cannot have a superclass.
	 *
	 * @param phantomName
	 * 		Internal name of the phantom that must extend or implement {@code supertype}.
	 * @param supertype
	 * 		Internal name of the required superclass or interface.
	 */
	record Supertype(@Nonnull String phantomName, @Nonnull String supertype) implements PhantomHint {}

	/**
	 * Requires a phantom to declare a field or method.
	 *
	 * @param phantomName
	 * 		Internal name of the phantom that must declare the member.
	 * @param name
	 * 		Member name.
	 * @param descriptor
	 * 		Field descriptor, or method descriptor.
	 * @param field
	 *        {@code true} for a field, {@code false} for a method.
	 * @param staticMember
	 *        {@code true} when the member is static.
	 * @param exceptions
	 * 		Internal names of exceptions the method declares. Empty for fields.
	 */
	record Member(@Nonnull String phantomName, @Nonnull String name, @Nonnull String descriptor,
	              boolean field, boolean staticMember, @Nonnull List<String> exceptions) implements PhantomHint {
		/**
		 * Copies the exceptions so that the hint cannot change after creation.
		 */
		public Member {
			exceptions = List.copyOf(exceptions);
		}

		/**
		 * Creates a member hint that declares no exceptions.
		 *
		 * @param phantomName
		 * 		Internal name of the phantom that must declare the member.
		 * @param name
		 * 		Member name.
		 * @param descriptor
		 * 		Field descriptor, or method descriptor.
		 * @param field
		 *        {@code true} for a field, {@code false} for a method.
		 * @param staticMember
		 *        {@code true} when the member is static.
		 */
		public Member(@Nonnull String phantomName, @Nonnull String name, @Nonnull String descriptor,
		              boolean field, boolean staticMember) {
			this(phantomName, name, descriptor, field, staticMember, List.of());
		}

		/**
		 * @return Parameter part of a method descriptor, including both parentheses, or an empty string for a field.
		 * Two method hints with the same name and parameters describe the same method, whatever they return.
		 */
		@Nonnull
		public String parameterDescriptor() {
			return field ? "" : descriptor.substring(0, descriptor.indexOf(')') + 1);
		}
	}
}
