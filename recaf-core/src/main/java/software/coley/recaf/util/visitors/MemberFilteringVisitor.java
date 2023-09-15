package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.ClassMember;

/**
 * A visitor that keeps only matched members. Everything else is removed.
 *
 * @author Matt Coley
 */
public class MemberFilteringVisitor extends ClassVisitor {
	private final MemberPredicate predicate;

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param member
	 * 		Target member to visit.
	 */
	public MemberFilteringVisitor(@Nullable ClassVisitor cv, @Nonnull ClassMember member) {
		this(cv, new SingleMemberPredicate(member));
	}

	/**
	 * @param cv
	 * 		Parent visitor.
	 * @param predicate
	 * 		Predicate to match against the members to include.
	 */
	public MemberFilteringVisitor(@Nullable ClassVisitor cv, @Nonnull MemberPredicate predicate) {
		super(RecafConstants.getAsmVersion(), cv);
		this.predicate = predicate;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
		if (predicate.matchField(access, name, desc, sig, value))
			return super.visitField(access, name, desc, sig, value);
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
		if (predicate.matchMethod(access, name, desc, sig, exceptions))
			return super.visitMethod(access, name, desc, sig, exceptions);
		return null;
	}

	@Override
	public ModuleVisitor visitModule(String name, int access, String version) {
		// Skip
		return null;
	}

	@Override
	public void visitNestHost(String nestHost) {
		// Skip
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		// Skip
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// Skip
		return null;
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
		// Skip
		return null;
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		// Skip
	}

	@Override
	public void visitNestMember(String nestMember) {
		// Skip
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		// Skip
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// Skip
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String desc, String sig) {
		// Skip
		return null;
	}

}
