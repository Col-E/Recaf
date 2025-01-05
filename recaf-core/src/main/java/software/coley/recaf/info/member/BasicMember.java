package software.coley.recaf.info.member;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.TypeAnnotationInfo;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.info.properties.PropertyContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Common base for basic member implementations.
 *
 * @author Matt Coley
 */
public abstract class BasicMember implements ClassMember {
	private final String name;
	private final String desc;
	private final String signature;
	private final int access;
	private PropertyContainer properties;
	private List<AnnotationInfo> annotations;
	private List<TypeAnnotationInfo> typeAnnotations;
	private ClassInfo declaringClass;

	protected BasicMember(@Nonnull String name, @Nonnull String desc, @Nullable String signature, int access) {
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
	public void addAnnotation(@Nonnull AnnotationInfo annotation) {
		if (annotations == null)
			annotations = new ArrayList<>(2);
		annotations.add(annotation);
	}

	/**
	 * For internal use when populating the model.
	 * Adding an annotation here does not change the bytecode of the class.
	 *
	 * @param typeAnnotation
	 * 		Annotation to add.
	 */
	public void addTypeAnnotation(@Nonnull TypeAnnotationInfo typeAnnotation) {
		if (typeAnnotations == null)
			typeAnnotations = new ArrayList<>(2);
		typeAnnotations.add(typeAnnotation);
	}

	/**
	 * For internal use when populating the model.
	 *
	 * @param declaringClass
	 * 		Declaring class to assign.
	 */
	public void setDeclaringClass(@Nonnull ClassInfo declaringClass) {
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
		if (annotations == null)
			return Collections.emptyList();
		return annotations;
	}

	@Nonnull
	@Override
	public List<TypeAnnotationInfo> getTypeAnnotations() {
		if (typeAnnotations == null)
			return Collections.emptyList();
		return typeAnnotations;
	}

	@Override
	public <V> void setProperty(Property<V> property) {
		if (properties == null)
			properties = new BasicPropertyContainer();
		properties.setProperty(property);
	}

	@Override
	public void removeProperty(String key) {
		if (properties != null)
			properties.removeProperty(key);
	}

	@Nonnull
	@Override
	public Map<String, Property<?>> getProperties() {
		if (properties == null)
			return Collections.emptyMap();
		return properties.getProperties();
	}
}
