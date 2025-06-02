package software.coley.recaf.util.visitors;

import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import software.coley.recaf.RecafConstants;

/**
 * A visitor that strips signature data from classes.
 *
 * @author Matt Coley
 */
public class SignatureRemovingVisitor extends ClassVisitor {
	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public SignatureRemovingVisitor(@Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
	}

	@Override
	public void visit(int version, int access, String name, String s, String superName, String[] interfaces) {
		super.visit(version, access, name, null, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String s, Object value) {
		return super.visitField(access, name, desc, null, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String s, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, null, exceptions);
		return new MethodVisitor(RecafConstants.getAsmVersion(), mv) {
			@Override
			public void visitLocalVariable(String name, String desc, String s, Label start, Label end, int index) {
				super.visitLocalVariable(name, desc, null, start, end, index);
			}
		};
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String s) {
		return super.visitRecordComponent(name, descriptor, null);
	}
}
