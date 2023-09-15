package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.ClassMember;

/**
 * Simple visitor for removing a matched {@link ClassMember}.
 *
 * @author Matt Coley
 */
public class MemberRemovingVisitor extends ClassVisitor {
	private final MemberPredicate predicate;
	private boolean removed;

	/**
	 * @param cv
	 * 		Parent visitor where the removal will be applied in.
	 * @param member
	 * 		Member to remove.
	 */
	public MemberRemovingVisitor(@Nullable ClassVisitor cv, @Nonnull ClassMember member) {
		this(cv, new SingleMemberPredicate(member));
	}

	/**
	 * @param cv
	 * 		Parent visitor where the removal will be applied in.
	 * @param predicate
	 * 		Predicate to match against the members to remove.
	 */
	public MemberRemovingVisitor(@Nullable ClassVisitor cv, @Nonnull MemberPredicate predicate) {
		super(RecafConstants.getAsmVersion(), cv);
		this.predicate = predicate;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
		if (predicate.matchField(access, name, desc, sig, value)) {
			removed = true;
			return null;
		}
		return super.visitField(access, name, desc, sig, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		if (predicate.matchMethod(access, name, desc, sig, exceptions)) {
			removed = true;
			return null;
		}
		return super.visitMethod(access, name, desc, sig, exceptions);
	}

	/**
	 * @return {@code true} when a field or method was removed.
	 */
	public boolean isRemoved() {
		return removed;
	}
}
