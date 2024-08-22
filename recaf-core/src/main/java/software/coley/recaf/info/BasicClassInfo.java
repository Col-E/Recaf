package software.coley.recaf.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.TypeAnnotationInfo;
import software.coley.recaf.info.builder.AbstractClassInfoBuilder;
import software.coley.recaf.info.member.BasicMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.info.properties.PropertyContainer;
import software.coley.recaf.util.Types;

import java.util.*;
import java.util.stream.Stream;

/**
 * Basic implementation of class info.
 *
 * @author Matt Coley
 * @see BasicJvmClassInfo
 * @see BasicAndroidClassInfo
 */
public abstract class BasicClassInfo implements ClassInfo {
	private static final int SIGS_VALID = 1;
	private static final int SIGS_INVALID = 0;
	private static final int SIGS_UNKNOWN = -1;
	private final PropertyContainer properties;
	private final String name;
	private final String superName;
	private final List<String> interfaces;
	private final int access;
	private final String signature;
	private final String sourceFileName;
	private final List<AnnotationInfo> annotations;
	private final List<TypeAnnotationInfo> typeAnnotations;
	private final String outerClassName;
	private final String outerMethodName;
	private final String outerMethodDescriptor;
	private final List<InnerClassInfo> innerClasses;
	private final List<FieldMember> fields;
	private final List<MethodMember> methods;
	private List<String> breadcrumbs;
	private int sigCheck = SIGS_UNKNOWN;

	protected BasicClassInfo(AbstractClassInfoBuilder<?> builder) {
		this(builder.getName(),
				builder.getSuperName(),
				builder.getInterfaces(),
				builder.getAccess(),
				builder.getSignature(),
				builder.getSourceFileName(),
				builder.getAnnotations(),
				builder.getTypeAnnotations(),
				builder.getOuterClassName(),
				builder.getOuterMethodName(),
				builder.getOuterMethodDescriptor(),
				builder.getInnerClasses(),
				builder.getFields(),
				builder.getMethods(),
				builder.getPropertyContainer());
	}

	protected BasicClassInfo(@Nonnull String name, String superName, @Nonnull List<String> interfaces, int access,
	                         String signature, String sourceFileName,
	                         @Nonnull List<AnnotationInfo> annotations,
	                         @Nonnull List<TypeAnnotationInfo> typeAnnotations,
	                         String outerClassName, String outerMethodName,
	                         String outerMethodDescriptor,
	                         @Nonnull List<InnerClassInfo> innerClasses,
	                         @Nonnull List<FieldMember> fields, @Nonnull List<MethodMember> methods,
	                         @Nonnull PropertyContainer properties) {
		this.name = name;
		this.superName = superName;
		this.interfaces = interfaces;
		this.access = access;
		this.signature = signature;
		this.sourceFileName = sourceFileName;
		this.annotations = annotations;
		this.typeAnnotations = typeAnnotations;
		this.outerClassName = outerClassName;
		this.outerMethodName = outerMethodName;
		this.outerMethodDescriptor = outerMethodDescriptor;
		this.innerClasses = innerClasses;
		this.fields = fields;
		this.methods = methods;
		this.properties = properties;
		// Link fields/methods to self
		Stream.concat(fields.stream(), methods.stream())
				.filter(member -> member instanceof BasicMember)
				.map(member -> (BasicMember) member)
				.forEach(member -> member.setDeclaringClass(this));
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSuperName() {
		return superName;
	}

	@Nonnull
	@Override
	public List<String> getInterfaces() {
		return interfaces;
	}

	@Override
	public int getAccess() {
		return access;
	}

	@Override
	public String getSignature() {
		return signature;
	}

	@Override
	public boolean hasValidSignatures() {
		// Check cached value.
		if (sigCheck != SIGS_UNKNOWN) return sigCheck == SIGS_VALID;

		// Check class level signature.
		String classSignature = getSignature();
		if (classSignature != null && !Types.isValidSignature(classSignature, false)) {
			sigCheck = SIGS_INVALID;
			return false;
		}

		// Check field signatures.
		for (FieldMember field : getFields()) {
			String fieldSignature = field.getSignature();
			if (fieldSignature != null && !Types.isValidSignature(field.getSignature(), true)) {
				sigCheck = SIGS_INVALID;
				return false;
			}
		}

		// Check method signatures.
		for (MethodMember method : getMethods()) {
			String methodSignature = method.getSignature();
			if (methodSignature != null && !Types.isValidSignature(methodSignature, false)) {
				sigCheck = SIGS_INVALID;
				return false;
			}

			// And local variables.
			for (LocalVariable variable : method.getLocalVariables()) {
				String localSignature = variable.getSignature();
				if (localSignature != null && !Types.isValidSignature(localSignature, true)) {
					sigCheck = SIGS_INVALID;
					return false;
				}
			}
		}

		sigCheck = SIGS_VALID;
		return true;
	}

	@Override
	public String getSourceFileName() {
		return sourceFileName;
	}

	@Nonnull
	@Override
	public List<AnnotationInfo> getAnnotations() {
		return annotations;
	}

	@Nonnull
	@Override
	public List<TypeAnnotationInfo> getTypeAnnotations() {
		return typeAnnotations;
	}

	@Override
	public String getOuterClassName() {
		return outerClassName;
	}

	@Override
	public String getOuterMethodName() {
		return outerMethodName;
	}

	@Override
	public String getOuterMethodDescriptor() {
		return outerMethodDescriptor;
	}

	@Nonnull
	@Override
	public List<String> getOuterClassBreadcrumbs() {
		if (breadcrumbs == null) {
			String currentOuter = getOuterClassName();
			if (currentOuter == null)
				return breadcrumbs = Collections.emptyList();

			int maxOuterDepth = 10;
			breadcrumbs = new ArrayList<>();
			int counter = 0;
			while (currentOuter != null) {
				if (++counter > maxOuterDepth) {
					breadcrumbs.clear(); // assuming some obfuscator is at work, so breadcrumbs might be invalid.
					break;
				}
				breadcrumbs.addFirst(currentOuter);
				String targetOuter = currentOuter;
				currentOuter = innerClasses.stream()
						.filter(i -> i.getInnerClassName().equals(targetOuter))
						.map(InnerClassInfo::getOuterClassName)
						.findFirst().orElse(null);
			}
		}
		return breadcrumbs;
	}

	@Nonnull
	@Override
	public List<InnerClassInfo> getInnerClasses() {
		return innerClasses;
	}

	@Nonnull
	@Override
	public List<FieldMember> getFields() {
		return fields;
	}

	@Nonnull
	@Override
	public List<MethodMember> getMethods() {
		return methods;
	}

	@Override
	public <V> void setProperty(Property<V> property) {
		properties.setProperty(property);
	}

	@Override
	public void removeProperty(String key) {
		properties.removeProperty(key);
	}

	@Nonnull
	@Override
	public Map<String, Property<?>> getProperties() {
		return properties.getProperties();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;

		if (o instanceof ClassInfo other) {
			// NOTE: Do NOT consider the properties since contents of the map can point back to this instance
			//       or our containing resource, causing a cycle.
			if (access != other.getAccess()) return false;
			if (!name.equals(other.getName())) return false;
			if (!Objects.equals(superName, other.getSuperName())) return false;
			if (!interfaces.equals(other.getInterfaces())) return false;
			if (!Objects.equals(signature, other.getSignature())) return false;
			if (!Objects.equals(sourceFileName, other.getSourceFileName())) return false;
			if (!annotations.equals(other.getAnnotations())) return false;
			if (!typeAnnotations.equals(other.getTypeAnnotations())) return false;
			if (!Objects.equals(outerClassName, other.getOuterClassName())) return false;
			if (!Objects.equals(outerMethodName, other.getOuterMethodName())) return false;
			if (!Objects.equals(outerMethodDescriptor, other.getOuterMethodDescriptor())) return false;
			if (!innerClasses.equals(other.getInnerClasses())) return false;
			if (!fields.equals(other.getFields())) return false;
			return methods.equals(other.getMethods());
		}
		return false;
	}

	@Override
	public int hashCode() {
		// NOTE: Do NOT consider the properties since contents of the map can point back to this instance
		//       or our containing resource, causing a cycle.
		int result = name.hashCode();
		result = 31 * result + (superName != null ? superName.hashCode() : 0);
		result = 31 * result + interfaces.hashCode();
		result = 31 * result + access;
		result = 31 * result + (signature != null ? signature.hashCode() : 0);
		result = 31 * result + (sourceFileName != null ? sourceFileName.hashCode() : 0);
		result = 31 * result + annotations.hashCode();
		result = 31 * result + typeAnnotations.hashCode();
		result = 31 * result + (outerClassName != null ? outerClassName.hashCode() : 0);
		result = 31 * result + (outerMethodName != null ? outerMethodName.hashCode() : 0);
		result = 31 * result + (outerMethodDescriptor != null ? outerMethodDescriptor.hashCode() : 0);
		result = 31 * result + innerClasses.hashCode();
		result = 31 * result + fields.hashCode();
		result = 31 * result + methods.hashCode();
		return result;
	}
}
