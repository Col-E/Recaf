package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import org.objectweb.asm.*;

/**
 * An empty visitor that provides empty visitor instances,
 * This will allow ASM to attempt to interpret all portions of some input bytecode, allowing detection of
 * any code that causes ASM to fail to parse a class.
 *
 * @author Matt Coley
 */
public class ValidationVisitor extends ClassVisitor {
	private static final int API = RecafConstants.getAsmVersion();

	/**
	 * Create visitor.
	 */
	public ValidationVisitor() {
		super(API);
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desd, boolean visible) {
		return anno();
	}

	@Override
	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desd, boolean visible) {
		return anno();
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desd, String signature, Object value) {
		return field();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return method();
	}

	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		return record();
	}

	private static AnnotationVisitor anno() {
		return new AnnotationVisitor(API) {
			@Override
			public AnnotationVisitor visitAnnotation(String name, String desd) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitArray(String name) {
				return anno();
			}
		};
	}

	private static FieldVisitor field() {
		return new FieldVisitor(API) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desd, boolean visible) {
				return anno();
			}
		};
	}

	private static MethodVisitor method() {
		return new MethodVisitor(API) {
			@Override
			public AnnotationVisitor visitAnnotationDefault() {
				return anno();
			}

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath path, String desd, boolean visible) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath path, String desd, boolean visible) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath path, String desd, boolean visible) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath path,
																  Label[] start, Label[] end, int[] index,
																  String desc, boolean visible) {
				return anno();
			}
		};
	}

	private static RecordComponentVisitor record() {
		return new RecordComponentVisitor(API) {
			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				return anno();
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath,
														 String descriptor, boolean visible) {
				return anno();
			}
		};
	}
}
