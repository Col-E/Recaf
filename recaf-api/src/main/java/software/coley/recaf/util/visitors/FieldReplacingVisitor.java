package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.FieldNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.FieldMember;

/**
 * Simple visitor for replacing a matched {@link FieldMember}.
 *
 * @author Matt Coley
 */
public class FieldReplacingVisitor extends ClassVisitor {
	private final FieldMember fieldMember;
	private final FieldNode replacementField;
	private boolean replaced;

	/**
	 * @param cv
	 * 		Parent visitor where the removal will be applied in.
	 * @param fieldMember
	 * 		Details of the field to replace.
	 * @param replacementField
	 * 		Field to replace with.
	 */
	public FieldReplacingVisitor(@Nullable ClassVisitor cv,
								 @Nonnull FieldMember fieldMember,
								 @Nonnull FieldNode replacementField) {
		super(RecafConstants.getAsmVersion(), cv);
		this.fieldMember = fieldMember;
		this.replacementField = replacementField;
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
		if (fieldMember.getName().equals(name) && fieldMember.getDescriptor().equals(desc)) {
			replaced = true;
			replacementField.accept(cv);
			return null;
		}
		return super.visitField(access, name, desc, sig, value);
	}

	/**
	 * @return {@code true} when the field was replaced.
	 */
	public boolean isReplaced() {
		return replaced;
	}
}