package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.*;
import software.coley.recaf.info.BasicInnerClassInfo;
import software.coley.recaf.info.BasicJvmClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.*;
import software.coley.recaf.info.member.*;

import java.util.*;
import java.util.function.Consumer;

import static software.coley.recaf.RecafConstants.getAsmVersion;

/**
 * Builder for {@link JvmClassInfo}.
 *
 * @author Matt Coley
 */
public class JvmClassInfoBuilder extends AbstractClassInfoBuilder<JvmClassInfoBuilder> {
	private byte[] bytecode;
	private int version = JvmClassInfo.BASE_VERSION + 8; // Java 8

	/**
	 * Create empty builder.
	 */
	public JvmClassInfoBuilder() {
		super();
	}

	/**
	 * Create a builder with data pulled from the given class.
	 *
	 * @param classInfo
	 * 		Class to pull data from.
	 */
	public JvmClassInfoBuilder(@Nonnull JvmClassInfo classInfo) {
		super(classInfo);
		withBytecode(classInfo.getBytecode());
		withVersion(classInfo.getVersion());
	}

	/**
	 * @param reader
	 * 		ASM class reader to pull data from.
	 */
	public JvmClassInfoBuilder(@Nonnull ClassReader reader) {
		adaptFrom(reader);
	}

	/**
	 * Copies over values by reading the contents of the class file in the reader.
	 *
	 * @param reader
	 * 		ASM class reader to pull data from.
	 *
	 * @return Builder.
	 */
	@Nonnull
	@SuppressWarnings("deprecation")
	public JvmClassInfoBuilder adaptFrom(@Nonnull ClassReader reader) {
		reader.accept(new ClassBuilderAppender(), 0);
		return withBytecode(reader.b);
	}

	@Nonnull
	public JvmClassInfoBuilder withBytecode(byte[] bytecode) {
		this.bytecode = bytecode;
		return this;
	}

	@Nonnull
	public JvmClassInfoBuilder withVersion(int version) {
		this.version = version;
		return this;
	}

	public byte[] getBytecode() {
		return bytecode;
	}

	public int getVersion() {
		return version;
	}

	@Override
	public JvmClassInfo build() {
		verify();
		return new BasicJvmClassInfo(this);
	}

	@Override
	protected void verify() {
		super.verify();
		if (bytecode == null)
			throw new IllegalStateException("Bytecode required");
		if (version < JvmClassInfo.BASE_VERSION)
			throw new IllegalStateException("Version cannot be lower than 44 (v1)");
	}

	private class ClassBuilderAppender extends ClassVisitor {
		private final List<AnnotationInfo> annotations = new ArrayList<>();
		private final List<TypeAnnotationInfo> typeAnnotations = new ArrayList<>();
		private final List<InnerClassInfo> innerClasses = new ArrayList<>();
		private final List<FieldMember> fields = new ArrayList<>();
		private final List<MethodMember> methods = new ArrayList<>();

		protected ClassBuilderAppender() {
			super(getAsmVersion());
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			withVersion(version);
			withAccess(access);
			withName(name);
			withSignature(signature);
			withSuperName(superName);
			withInterfaces(Arrays.asList(interfaces));
		}

		@Override
		public void visitSource(String source, String debug) {
			super.visitSource(source, debug);
			withSourceFileName(source);
		}

		@Override
		public void visitOuterClass(String owner, String name, String descriptor) {
			super.visitOuterClass(owner, name, descriptor);
			withOuterClassName(owner);
			withOuterMethodName(name);
			withOuterMethodDescriptor(descriptor);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationBuilderAppender(visible, descriptor, annotations::add);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return new AnnotationBuilderAppender(visible, descriptor,
					anno -> typeAnnotations.add(anno.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			super.visitInnerClass(name, outerName, innerName, access);

			// Get the name of the class being visited
			String currentClassName = getName();

			// Add the inner data
			innerClasses.add(new BasicInnerClassInfo(currentClassName, name, outerName, innerName, access));

			// If the local 'name' is the current class name, then we are visiting an inner class entry
			// that most likely is a representation of the current class. If this entry has data about
			// the outer class, we want to grab it.
			if (name.equals(currentClassName)) {
				// Only need to do this once, and some entries may not have data.
				// Because they can be in any order we need to protect against re-assigning null.
				if (getOuterClassName() == null &&
						outerName != null &&
						currentClassName.startsWith(outerName)) {
					withOuterClassName(outerName);
				}
			}

		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			return new FieldBuilderAppender(access, name, descriptor, signature, value) {
				@Override
				public void visitEnd() {
					fields.add(getFieldMember());
				}
			};
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			return new MethodBuilderAppender(access, name, descriptor, signature, exceptions) {
				@Override
				public void visitEnd() {
					methods.add(getMethodMember());
				}
			};
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			if (!annotations.isEmpty()) {
				withAnnotations(annotations);
			}
			withFields(fields);
			withMethods(methods);
			withInnerClasses(innerClasses);
			withAnnotations(annotations);
			withTypeAnnotations(typeAnnotations);
		}
	}

	private static class FieldBuilderAppender extends FieldVisitor {
		private final BasicFieldMember fieldMember;

		public FieldBuilderAppender(int access, String name, String descriptor,
									String signature, Object value) {
			super(getAsmVersion());
			fieldMember = new BasicFieldMember(name, descriptor, signature, access, value);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationBuilderAppender(visible, descriptor, fieldMember::addAnnotation);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return new AnnotationBuilderAppender(visible, descriptor,
					anno -> fieldMember.addTypeAnnotation(anno.withTypeInfo(typeRef, typePath)));
		}

		@Nonnull
		public BasicFieldMember getFieldMember() {
			return fieldMember;
		}
	}

	private static class MethodBuilderAppender extends MethodVisitor {
		private final BasicMethodMember methodMember;

		public MethodBuilderAppender(int access, String name, String descriptor,
									 String signature, String[] exceptions) {
			super(getAsmVersion());
			List<String> exceptionList = exceptions == null ? Collections.emptyList() : Arrays.asList(exceptions);
			methodMember = new BasicMethodMember(name, descriptor, signature, access, exceptionList, new ArrayList<>());
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationBuilderAppender(visible, descriptor, methodMember::addAnnotation);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return new AnnotationBuilderAppender(visible, descriptor,
					anno -> methodMember.addTypeAnnotation(anno.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
			methodMember.addLocalVariable(new BasicLocalVariable(index, name, descriptor, signature));
			super.visitLocalVariable(name, descriptor, signature, start, end, index);
		}

		@Nonnull
		public BasicMethodMember getMethodMember() {
			return methodMember;
		}
	}

	private static class AnnotationBuilderAppender extends AnnotationVisitor {
		private final Consumer<BasicAnnotationInfo> annotationConsumer;
		private final Map<String, AnnotationElement> elements = new HashMap<>();
		private final List<Object> arrayValues = new ArrayList<>();
		private final boolean visible;
		private final String descriptor;

		protected AnnotationBuilderAppender(boolean visible, String descriptor,
											Consumer<BasicAnnotationInfo> annotationConsumer) {
			super(getAsmVersion());
			this.visible = visible;
			this.descriptor = descriptor;
			this.annotationConsumer = annotationConsumer;
		}

		@Override
		public void visit(String name, Object value) {
			super.visit(name, value);
			// The 'value' can technically be a primitive array, but it doesn't really matter
			// how we capture it.
			if (name == null) {
				arrayValues.add(value);
			} else {
				elements.put(name, new BasicAnnotationElement(name, value));
			}
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			super.visitEnum(name, descriptor, value);
			BasicAnnotationEnumReference enumRef = new BasicAnnotationEnumReference(descriptor, value);
			if (name == null) {
				arrayValues.add(enumRef);
			} else {
				elements.put(name, new BasicAnnotationElement(name, enumRef));
			}
		}

		@Override
		public AnnotationVisitor visitAnnotation(String name, String descriptor) {
			return new AnnotationBuilderAppender(true, descriptor, anno -> {
				if (name == null) {
					arrayValues.add(anno);
				} else {
					elements.put(name, new BasicAnnotationElement(name, anno));
				}
			});
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationBuilderAppender outer = this;
			return new AnnotationBuilderAppender(true, "", null) {
				@Override
				public void visitEnd() {
					AnnotationBuilderAppender inner = this;
					outer.elements.put(name, new BasicAnnotationElement(name, inner.arrayValues));
				}
			};
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			BasicAnnotationInfo anno = new BasicAnnotationInfo(visible, descriptor);
			elements.forEach((name, value) -> anno.addElement(value));
			if (annotationConsumer != null) annotationConsumer.accept(anno);
		}
	}
}
