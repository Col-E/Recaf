package me.coley.recaf.android.cf;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mutable implementation of {@link Method}.
 *
 * @author Matt Coley
 */
public class MutableMethod extends BaseMethodReference implements Method {
	private String definingClass;
	private String name;
	private String returnType;
	private List<MutableMethodParameter> parameters;
	private Set<MutableAnnotation> annotations;
	private Set<HiddenApiRestriction> hiddenApiRestrictions;
	private MutableMethodImplementation impl;
	private int accessFlags;

	/**
	 * @param original
	 * 		Instance to copy.
	 */
	public MutableMethod(Method original) {
		this(original.getDefiningClass(), original.getName(), original.getReturnType(),
				MutableMethodParameter.copyParameters(original.getParameters()),
				original.getAccessFlags(), MutableAnnotation.copyAnnotations(original.getAnnotations()),
				original.getHiddenApiRestrictions(), copyImpl(original.getImplementation()));
	}

	/**
	 * @param definingClass
	 * 		Type that declares the method.
	 * @param name
	 * 		Method's name.
	 * @param returnType
	 * 		Method's return type, as a type descriptor.
	 * @param parameters
	 * 		List of parameters.
	 * @param accessFlags
	 * 		Access flags/modifiers.
	 * @param annotations
	 * 		Set of applied annotations.
	 * @param hiddenApiRestrictions
	 * 		Set of API restrictions.
	 * @param impl
	 * 		Method implementation. May be {@code null} if the method is {@code abstract}.
	 */
	public MutableMethod(@Nonnull String definingClass, @Nonnull String name, @Nonnull String returnType,
						 @Nonnull List<MutableMethodParameter> parameters, int accessFlags,
						 @Nonnull Set<MutableAnnotation> annotations,
						 @Nonnull Set<HiddenApiRestriction> hiddenApiRestrictions,
						 MutableMethodImplementation impl) {
		this.definingClass = definingClass;
		this.name = name;
		this.returnType = returnType;
		this.parameters = parameters;
		this.accessFlags = accessFlags;
		this.annotations = annotations;
		this.hiddenApiRestrictions = hiddenApiRestrictions;
		this.impl = impl;
	}

	@Nonnull
	@Override
	public String getDefiningClass() {
		return definingClass;
	}

	/**
	 * @param definingClass
	 * 		New defining class type.
	 */
	public void setDefiningClass(@Nonnull String definingClass) {
		this.definingClass = definingClass;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * 		New method name.
	 */
	public void setName(@Nonnull String name) {
		this.name = name;
	}

	@Override
	public int getAccessFlags() {
		return accessFlags;
	}

	/**
	 * @param accessFlags
	 * 		New method access modifiers.
	 */
	public void setAccessFlags(int accessFlags) {
		this.accessFlags = accessFlags;
	}

	@Nonnull
	@Override
	public Set<MutableAnnotation> getAnnotations() {
		return annotations;
	}

	/**
	 * @param annotations
	 * 		New set of annotations to apply to method.
	 */
	public void setAnnotations(@Nonnull Set<MutableAnnotation> annotations) {
		this.annotations = annotations;
	}

	@Nonnull
	@Override
	public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
		return hiddenApiRestrictions;
	}

	/**
	 * @param hiddenApiRestrictions
	 * 		New API restriction set.
	 */
	public void setHiddenApiRestrictions(@Nonnull Set<HiddenApiRestriction> hiddenApiRestrictions) {
		this.hiddenApiRestrictions = hiddenApiRestrictions;
	}

	@Nonnull
	@Override
	public String getReturnType() {
		return returnType;
	}

	/**
	 * @param returnType
	 * 		New return type.
	 */
	public void setReturnType(@Nonnull String returnType) {
		this.returnType = returnType;
	}

	@Nonnull
	@Override
	public List<MutableMethodParameter> getParameters() {
		return parameters;
	}

	/**
	 * @param parameters
	 * 		New parameters.
	 */
	public void setParameters(@Nonnull List<MutableMethodParameter> parameters) {
		this.parameters = parameters;
	}

	@Nullable
	@Override
	public MutableMethodImplementation getImplementation() {
		return impl;
	}

	@Nonnull
	@Override
	public List<String> getParameterTypes() {
		return parameters.stream()
				.map(MutableMethodParameter::getType)
				.collect(Collectors.toList());
	}

	/**
	 * Note: Direct methods are constructors, static, and private methods.
	 *
	 * @param method
	 * 		Some method.
	 *
	 * @return {@code true} when it is a direct method. {@code false} otherwise.
	 */
	public static boolean isDirect(Method method) {
		if (method.getName().equals("<init>"))
			return true;
		int mask = AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue();
		return (method.getAccessFlags() & mask) > 0;
	}

	/**
	 * @param implementation
	 * 		Some method impl.
	 *
	 * @return Mutable copy of the impl.
	 */
	public static MutableMethodImplementation copyImpl(MethodImplementation implementation) {
		if (implementation == null)
			return null;
		return new MutableMethodImplementationExt(implementation);
	}

	/**
	 * @param methods
	 * 		Original set of methods.
	 *
	 * @return Set of mutable copies.
	 */
	public static Set<MutableMethod> copyMethods(Iterable<? extends Method> methods) {
		Set<MutableMethod> set = new HashSet<>();
		for (Method method : methods) {
			set.add(new MutableMethod(method));
		}
		return set;
	}
}
