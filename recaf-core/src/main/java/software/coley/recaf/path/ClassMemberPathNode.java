package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.LocalVariable;

import java.util.List;
import java.util.Set;

/**
 * Path node for {@link ClassMember} types.
 *
 * @author Matt Coley
 */
public class ClassMemberPathNode extends AbstractPathNode<ClassInfo, ClassMember> {
	/**
	 * Type identifier for member nodes.
	 */
	public static final String TYPE_ID = "member";

	/**
	 * Node without parent.
	 *
	 * @param member
	 * 		Member value.
	 */
	public ClassMemberPathNode(@Nonnull ClassMember member) {
		this(null, member);
	}

	/**
	 * Node with parent.
	 *
	 * @param parent
	 * 		Parent node.
	 * @param member
	 * 		Member value.
	 *
	 * @see ClassPathNode#child(ClassMember)
	 */
	public ClassMemberPathNode(@Nullable ClassPathNode parent, @Nonnull ClassMember member) {
		super(TYPE_ID, parent, ClassMember.class, member);
	}

	/**
	 * @return {@code true} when wrapping a field.
	 */
	public boolean isField() {
		return getValue().isField();
	}

	/**
	 * @return {@code true} when wrapping a method.
	 */
	public boolean isMethod() {
		return getValue().isMethod();
	}

	/**
	 * @param thrownType
	 * 		Thrown type to wrap into node.
	 *
	 * @return Path node of thrown type, with the current member as parent.
	 */
	@Nonnull
	public ThrowsPathNode childThrows(@Nonnull String thrownType) {
		if (isMethod())
			return new ThrowsPathNode(this, thrownType);
		throw new IllegalStateException("Cannot make child for throws on non-method member");
	}

	/**
	 * @param exceptionType
	 * 		Thrown type to wrap into node.
	 *
	 * @return Path node of thrown type, with the current member as parent.
	 */
	@Nonnull
	public CatchPathNode childCatch(@Nonnull String exceptionType) {
		if (isMethod())
			return new CatchPathNode(this, exceptionType);
		throw new IllegalStateException("Cannot make child for catch on non-method member");
	}

	/**
	 * @param annotation
	 * 		Annotation to wrap into node.
	 *
	 * @return Path node of annotation, with the current member as parent.
	 */
	@Nonnull
	public AnnotationPathNode childAnnotation(@Nonnull AnnotationInfo annotation) {
		return new AnnotationPathNode(this, annotation);
	}

	/**
	 * @param variable
	 * 		Variable to wrap into node.
	 *
	 * @return Path node of local variable, with the current member as parent.
	 */
	@Nonnull
	public LocalVariablePathNode childVariable(LocalVariable variable) {
		if (isMethod())
			return new LocalVariablePathNode(this, variable);
		throw new IllegalStateException("Cannot make child for catch on non-method member");
	}

	/**
	 * @param insn
	 * 		Instruction to wrap into node.
	 * @param index
	 * 		Index of the instruction within the method code.
	 *
	 * @return Path node of instruction, with the current member as parent.
	 */
	@Nonnull
	public InstructionPathNode childInsn(@Nonnull AbstractInsnNode insn, int index) {
		if (isMethod())
			return new InstructionPathNode(this, insn, index);
		throw new IllegalStateException("Cannot make child for insn on non-method member");
	}

	@Override
	public ClassPathNode getParent() {
		return (ClassPathNode) super.getParent();
	}

	@Nonnull
	@Override
	public Set<String> directParentTypeIds() {
		return Set.of(ClassPathNode.TYPE_ID);
	}

	@Override
	public int localCompare(PathNode<?> o) {
		if (o instanceof ClassMemberPathNode classMemberNode) {
			ClassMember member = getValue();
			ClassMember otherMember = classMemberNode.getValue();

			// Show fields first
			if (member.isField() && otherMember.isMethod()) {
				return -1;
			} else if (member.isMethod() && otherMember.isField()) {
				return 1;
			}

			ClassPathNode parent = getParent();
			if (parent != null) {
				// Sort by appearance order in parent.
				ClassInfo classInfo = parent.getValue();
				List<? extends ClassMember> list = member.isField() ?
						classInfo.getFields() : classInfo.getMethods();
				return Integer.compare(list.indexOf(member), list.indexOf(otherMember));
			} else {
				// Just sort alphabetically if parent not known.
				String key = member.getName() + member.getDescriptor();
				String otherKey = otherMember.getName() + member.getDescriptor();
				return String.CASE_INSENSITIVE_ORDER.compare(key, otherKey);
			}
		}

		// Show after inner classes & annotations
		if (o instanceof InnerClassPathNode || o instanceof AnnotationPathNode)
			return 1;

		return 0;
	}
}
