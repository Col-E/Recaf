package software.coley.recaf.util.visitors;

/**
 * Predicate to use in {@link MemberFilteringVisitor} and {@link MemberRemovingVisitor}.
 *
 * @author Matt Coley
 */
public interface MemberPredicate {
	/**
	 * @param access
	 * 		Field access flags.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 * @param sig
	 * 		Field generic signature.
	 * @param value
	 * 		Field value.
	 *
	 * @return Match result.
	 */
	boolean matchField(int access, String name, String desc, String sig, Object value);

	/**
	 * @param access
	 * 		Method access flags.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param sig
	 * 		Method generic signature.
	 * @param exceptions
	 * 		Method exceptions.
	 *
	 * @return Match result.
	 */
	boolean matchMethod(int access, String name, String desc, String sig, String[] exceptions);
}
