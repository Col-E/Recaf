package software.coley.recaf.info.builder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.*;
import software.coley.recaf.info.BasicInnerClassInfo;
import software.coley.recaf.info.BasicJvmClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.*;
import software.coley.recaf.info.member.*;
import software.coley.recaf.info.properties.builtin.UnknownAttributesProperty;
import software.coley.recaf.util.MultiMap;

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
	private boolean skipValidationChecks = true;
	@Nullable
	private ClassBuilderAdapter adapter;

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
	 * Creates a builder with data pulled from the given bytecode.
	 *
	 * @param reader
	 * 		ASM class reader to read bytecode from.
	 */
	public JvmClassInfoBuilder(@Nonnull ClassReader reader) {
		adaptFrom(reader);
	}

	/**
	 * Creates a builder with the given bytecode.
	 *
	 * @param bytecode
	 * 		Class bytecode to read values from.
	 */
	public JvmClassInfoBuilder(@Nonnull byte[] bytecode) {
		adaptFrom(bytecode);
	}

	/**
	 * Copies over values by reading the contents of the class file in the reader.
	 * Calls {@link #adaptFrom(byte[], int)} with {@code flags=0}.
	 * <p/>
	 * <b>IMPORTANT:</b> If {@link #skipValidationChecks(boolean)} is {@code false} and validation checks are active
	 * extra steps are taken to ensure the class is fully ASM compliant. You will want to wrap this call in a try-catch
	 * block handling {@link Throwable} to cover any potential ASM failure.
	 * <br>
	 * Validation is disabled by default.
	 * <br>
	 * If you wish to validate the input, you must use the one of the given constructors:
	 * <ul>
	 *     <li>{@link #JvmClassInfoBuilder()}</li>
	 *     <li>{@link #JvmClassInfoBuilder(JvmClassInfo)}</li>
	 * </ul>
	 *
	 * @param code
	 * 		Class bytecode to pull data from.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JvmClassInfoBuilder adaptFrom(@Nonnull byte[] code) {
		return adaptFrom(code, 0);
	}

	/**
	 * Copies over values by reading the contents of the class file in the reader.
	 * Calls {@link #adaptFrom(ClassReader, int)} with {@code flags}.
	 * <p/>
	 * <b>IMPORTANT:</b> If {@link #skipValidationChecks(boolean)} is {@code false} and validation checks are active
	 * extra steps are taken to ensure the class is fully ASM compliant. You will want to wrap this call in a try-catch
	 * block handling {@link Throwable} to cover any potential ASM failure.
	 * <br>
	 * Validation is disabled by default.
	 * <br>
	 * If you wish to validate the input, you must use the one of the given constructors:
	 * <ul>
	 *     <li>{@link #JvmClassInfoBuilder()}</li>
	 *     <li>{@link #JvmClassInfoBuilder(JvmClassInfo)}</li>
	 * </ul>
	 *
	 * @param code
	 * 		Class bytecode to pull data from.
	 * @param flags
	 * 		Reader flags to use when populating information.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JvmClassInfoBuilder adaptFrom(@Nonnull byte[] code, int flags) {
		return adaptFrom(new ClassReader(code), flags);
	}

	/**
	 * Copies over values by reading the contents of the class file in the reader.
	 * Calls {@link #adaptFrom(ClassReader, int)} with {@code flags=0}.
	 * <p/>
	 * <b>IMPORTANT:</b> If {@link #skipValidationChecks(boolean)} is {@code false} and validation checks are active
	 * extra steps are taken to ensure the class is fully ASM compliant. You will want to wrap this call in a try-catch
	 * block handling {@link Throwable} to cover any potential ASM failure.
	 * <br>
	 * Validation is disabled by default.
	 * <br>
	 * If you wish to validate the input, you must use the one of the given constructors:
	 * <ul>
	 *     <li>{@link #JvmClassInfoBuilder()}</li>
	 *     <li>{@link #JvmClassInfoBuilder(JvmClassInfo)}</li>
	 * </ul>
	 *
	 * @param reader
	 * 		ASM class reader to pull data from.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public JvmClassInfoBuilder adaptFrom(@Nonnull ClassReader reader) {
		return adaptFrom(reader, 0);
	}

	/**
	 * Copies over values by reading the contents of the class file in the reader.
	 * <p/>
	 * <b>IMPORTANT:</b> If {@link #skipValidationChecks(boolean)} is {@code false} and validation checks are active
	 * extra steps are taken to ensure the class is fully ASM compliant. You will want to wrap this call in a try-catch
	 * block handling {@link Throwable} to cover any potential ASM failure.
	 * <br>
	 * Validation is disabled by default.
	 * <br>
	 * If you wish to validate the input, you must use the one of the given constructors:
	 * <ul>
	 *     <li>{@link #JvmClassInfoBuilder()}</li>
	 *     <li>{@link #JvmClassInfoBuilder(JvmClassInfo)}</li>
	 * </ul>
	 *
	 * @param reader
	 * 		ASM class reader to pull data from.
	 * @param flags
	 * 		Reader flags to use when populating information.
	 *
	 * @return Builder.
	 */
	@Nonnull
	@SuppressWarnings(value = "deprecation")
	public JvmClassInfoBuilder adaptFrom(@Nonnull ClassReader reader, int flags) {
		// If we are doing validation checks, delegating the reader to a writer should catch most issues
		// that would normally crash ASM. It is the caller's responsibility to error handle ASM failing
		// if such failures occur.
		adapter = new ClassBuilderAdapter(skipValidationChecks ? null : new ClassWriter(reader, 0));
		reader.accept(adapter, flags);
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

	/**
	 * The default value is {@code true}. Setting to {@code false} enables class validation steps.
	 * When {@link #verify()} is run it will check if there are any custom attributes.
	 *
	 * @param skipValidationChecks
	 *        {@code false} if we want to verify the classes custom attribute
	 *
	 * @return {@code JvmClassInfoBuilder}
	 */
	@Nonnull
	public JvmClassInfoBuilder skipValidationChecks(boolean skipValidationChecks) {
		this.skipValidationChecks = skipValidationChecks;
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
		if (adapter != null && adapter.hasCustomAttributes())
			getPropertyContainer().setProperty(new UnknownAttributesProperty(adapter.getCustomAttributeNames()));
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

	/**
	 * Converts ASM visitor actions to 'with' actions in the class builder.
	 * Results in a fully reconstructed class model.
	 *
	 * @see FieldBuilderAdapter
	 * @see MethodBuilderAdapter
	 */
	private class ClassBuilderAdapter extends ClassVisitor {
		private final List<AnnotationInfo> annotations = new ArrayList<>();
		private final List<TypeAnnotationInfo> typeAnnotations = new ArrayList<>();
		private final List<InnerClassInfo> innerClasses = new ArrayList<>();
		private final List<FieldMember> fields = new ArrayList<>();
		private final List<MethodMember> methods = new ArrayList<>();
		private final List<Attribute> classCustomAttributes = new ArrayList<>();
		private final MultiMap<String, Attribute, List<Attribute>> fieldCustomAttributes;
		private final MultiMap<String, Attribute, List<Attribute>> methodCustomAttributes;

		protected ClassBuilderAdapter(@Nullable ClassVisitor cv) {
			super(getAsmVersion(), cv);
			fieldCustomAttributes = MultiMap.from(new HashMap<>(), ArrayList::new);
			methodCustomAttributes = MultiMap.from(new HashMap<>(), ArrayList::new);
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			withVersion(version & 0xFF);
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
			return new AnnotationBuilderAdapter(visible, descriptor, annotations::add);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return new AnnotationBuilderAdapter(visible, descriptor,
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
		public void visitAttribute(Attribute attribute) {
			classCustomAttributes.add(attribute);
			super.visitAttribute(attribute);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			return new FieldBuilderAdapter(access, name, descriptor, signature, value) {

				@Override
				public void visitAttribute(Attribute attribute) {
					fieldCustomAttributes.get(name).add(attribute);
					super.visitAttribute(attribute);
				}

				@Override
				public void visitEnd() {
					fields.add(getFieldMember());
				}
			};
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			return new MethodBuilderAdapter(access, name, descriptor, signature, exceptions) {

				@Override
				public void visitAttribute(Attribute attribute) {
					methodCustomAttributes.get(name).add(attribute);
					super.visitAttribute(attribute);
				}

				@Override
				public void visitEnd() {
					super.visitEnd();
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

		/**
		 * @return {@code true} when any custom attributes were found.
		 */
		public boolean hasCustomAttributes() {
			return !classCustomAttributes.isEmpty() ||
					!fieldCustomAttributes.isEmpty() ||
					!methodCustomAttributes.isEmpty();
		}

		/**
		 * @return Unique names of attributes found.
		 */
		@Nonnull
		public Collection<String> getCustomAttributeNames() {
			Set<String> names = new TreeSet<>();
			classCustomAttributes.stream()
					.map(a -> a.type)
					.forEach(names::add);
			fieldCustomAttributes.values()
					.map(a -> a.type)
					.forEach(names::add);
			methodCustomAttributes.values()
					.map(a -> a.type)
					.forEach(names::add);
			return names;
		}
	}

	private static class FieldBuilderAdapter extends FieldVisitor {
		private final BasicFieldMember fieldMember;

		public FieldBuilderAdapter(int access, String name, String descriptor,
		                           String signature, Object value) {
			super(getAsmVersion());
			fieldMember = new BasicFieldMember(name, descriptor, signature, access, value);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationBuilderAdapter(visible, descriptor, fieldMember::addAnnotation);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return new AnnotationBuilderAdapter(visible, descriptor,
					anno -> fieldMember.addTypeAnnotation(anno.withTypeInfo(typeRef, typePath)));
		}

		@Nonnull
		public BasicFieldMember getFieldMember() {
			return fieldMember;
		}
	}

	private static class MethodBuilderAdapter extends MethodVisitor {
		private final BasicMethodMember methodMember;
		private final Type methodDescriptor;
		private final List<LocalVariable> parameters;
		private int parameterIndex;
		private int parameterSlot;

		public MethodBuilderAdapter(int access, String name, String descriptor,
		                            String signature, String[] exceptions) {
			super(getAsmVersion());
			List<String> exceptionList = exceptions == null ? Collections.emptyList() : Arrays.asList(exceptions);
			methodMember = new BasicMethodMember(name, descriptor, signature, access, exceptionList, new ArrayList<>());
			methodDescriptor = Type.getMethodType(descriptor);
			parameterSlot = methodMember.hasStaticModifier() ? 0 : 1;
			parameters = new ArrayList<>(methodDescriptor.getArgumentCount());
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			return new AnnotationBuilderAdapter(visible, descriptor, methodMember::addAnnotation);
		}

		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
			return new AnnotationBuilderAdapter(visible, descriptor,
					anno -> methodMember.addTypeAnnotation(anno.withTypeInfo(typeRef, typePath)));
		}

		@Override
		public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
			methodMember.addLocalVariable(new BasicLocalVariable(index, name, descriptor, signature));
			super.visitLocalVariable(name, descriptor, signature, start, end, index);
		}

		@Override
		public void visitParameter(String name, int access) {
			super.visitParameter(name, access);

			Type[] argumentTypes = methodDescriptor.getArgumentTypes();
			if (parameterIndex < argumentTypes.length) {
				Type argumentType = argumentTypes[parameterIndex];

				// Only add when we have a name for the parameter.
				if (name != null)
					parameters.add(new BasicLocalVariable(parameterSlot, name, argumentType.getDescriptor(), null));

				parameterIndex++;
				parameterSlot += argumentType.getSize();
			}
		}

		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return new DefaultAnnotationAdapter(anno -> {
				AnnotationElement element = anno.getElements().get(DefaultAnnotationAdapter.KEY);
				if (element != null) methodMember.setAnnotationDefault(element);
			});
		}

		@Override
		public void visitEnd() {
			super.visitEnd();

			// Add local variables generated from the visited parameters if the local variable table hasn't already
			// provided variables for those indices. This assists in providing variable models for abstract methods.
			// This only works when a 'MethodParameters' attribute is present on the method. The javac compiler
			// emits this when passing '-parameters'.
			for (LocalVariable parameter : parameters)
				if (methodMember.getLocalVariable(parameter.getIndex()) == null)
					methodMember.addLocalVariable(parameter);
		}

		@Nonnull
		public BasicMethodMember getMethodMember() {
			return methodMember;
		}
	}

	private static class AnnotationBuilderAdapter extends AnnotationVisitor {
		private final Consumer<BasicAnnotationInfo> annotationConsumer;
		protected final Map<String, AnnotationElement> elements = new HashMap<>();
		private final List<Object> arrayValues = new ArrayList<>();
		private final List<BasicAnnotationInfo> subAnnotations = new ArrayList<>();
		private final boolean visible;
		private final String descriptor;

		protected AnnotationBuilderAdapter(boolean visible, String descriptor,
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
			return new AnnotationBuilderAdapter(true, descriptor, anno -> {
				if (name == null) {
					AnnotationBuilderAdapter.this.arrayValues.add(anno);
				} else {
					AnnotationBuilderAdapter.this.elements.put(name, new BasicAnnotationElement(name, anno));
				}
			}) {
				@Override
				protected void populate(@Nonnull BasicAnnotationInfo anno) {
					super.populate(anno);
					AnnotationBuilderAdapter.this.subAnnotations.add(anno);
				}
			};
		}

		@Override
		public AnnotationVisitor visitArray(String name) {
			AnnotationBuilderAdapter outer = this;
			return new AnnotationBuilderAdapter(true, "", null) {
				@Override
				public void visitEnd() {
					AnnotationBuilderAdapter inner = this;
					outer.elements.put(name, new BasicAnnotationElement(name, inner.arrayValues));
				}
			};
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			populate(new BasicAnnotationInfo(visible, descriptor));
		}

		protected void populate(@Nonnull BasicAnnotationInfo anno) {
			elements.forEach((name, value) -> anno.addElement(value));
			subAnnotations.forEach(anno::addAnnotation);
			if (annotationConsumer != null) annotationConsumer.accept(anno);
		}
	}

	private static class DefaultAnnotationAdapter extends AnnotationBuilderAdapter {
		private static final String KEY = "value";

		protected DefaultAnnotationAdapter(Consumer<BasicAnnotationInfo> annotationConsumer) {
			super(true, "Ljava/lang/Object;", annotationConsumer);
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			name = KEY;

			BasicAnnotationEnumReference enumRef = new BasicAnnotationEnumReference(descriptor, value);
			elements.put(name, new BasicAnnotationElement(name, enumRef));
		}
	}
}
