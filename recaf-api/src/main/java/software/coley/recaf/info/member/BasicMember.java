package software.coley.recaf.info.member;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.TypeAnnotationInfo;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.info.properties.PropertyContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Common base for basic member implementations.
 *
 * @author Matt Coley
 */
public abstract class BasicMember implements ClassMember {
	private final PropertyContainer properties = new BasicPropertyContainer();
	private final List<AnnotationInfo> annotations = new ArrayList<>();
	private final List<TypeAnnotationInfo> typeAnnotations = new ArrayList<>();
	private final String name;
	private final String desc;
	private final String signature;
	private final int access;
	private ClassInfo declaringClass;

	protected BasicMember(String name, String desc, String signature, int access) {
		this.name = name;
		this.desc = desc;
		this.signature = signature;
		this.access = access;
	}

	/**
	 * For internal use when populating the model.
	 * Adding an annotation here does not change the bytecode of the class.
	 *
	 * @param annotation
	 * 		Annotation to add.
	 */
	public void addAnnotation(AnnotationInfo annotation) {
		annotations.add(annotation);
	}

	/**
	 * For internal use when populating the model.
	 * Adding an annotation here does not change the bytecode of the class.
	 *
	 * @param typeAnnotation
	 * 		Annotation to add.
	 */
	public void addTypeAnnotation(TypeAnnotationInfo typeAnnotation) {
		typeAnnotations.add(typeAnnotation);
	}

	/**
	 * For internal use when populating the model.
	 *
	 * @param declaringClass
	 * 		Declaring class to assign.
	 */
	public void setDeclaringClass(ClassInfo declaringClass) {
		this.declaringClass = declaringClass;
	}

	@Override
	public ClassInfo getDeclaringClass() {
		return declaringClass;
	}

	@Override
	public int getAccess() {
		return access;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public String getDescriptor() {
		return desc;
	}

	@Override
	public String getSignature() {
		return signature;
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
}
