package software.coley.recaf.util.visitors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import software.coley.recaf.RecafConstants;

import java.util.function.Predicate;

/**
 * A visitor that strips unrecognized attributes from classes.
 *
 * @author Matt Coley
 */
public class UnknownAttributeRemovingVisitor extends ClassVisitor {
	private final Predicate<Attribute> whitelist;

	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public UnknownAttributeRemovingVisitor(@Nullable ClassVisitor cv) {
		this(attr -> false, cv);
	}

	/**
	 * @param whitelist
	 * 		Attribute whitelist function.
	 * @param cv
	 * 		Parent visitor.
	 */
	public UnknownAttributeRemovingVisitor(@Nonnull Predicate<Attribute> whitelist, @Nullable ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
		this.whitelist = whitelist;
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		if (whitelist.test(attribute))
			super.visitAttribute(attribute);
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		RecordComponentVisitor rv = super.visitRecordComponent(name, descriptor, signature);
		return new RecordComponentVisitor(RecafConstants.getAsmVersion(), rv) {
			@Override
			public void visitAttribute(Attribute attribute) {
				if (whitelist.test(attribute))
					super.visitAttribute(attribute);
			}
		};
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
		return new FieldVisitor(RecafConstants.getAsmVersion(), fv) {
			@Override
			public void visitAttribute(Attribute attribute) {
				if (whitelist.test(attribute))
					super.visitAttribute(attribute);
			}
		};
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodVisitor(RecafConstants.getAsmVersion(), mv) {
			@Override
			public void visitAttribute(Attribute attribute) {
				if (whitelist.test(attribute))
					super.visitAttribute(attribute);
			}
		};
	}
}
