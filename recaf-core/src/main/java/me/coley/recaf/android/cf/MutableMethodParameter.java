package me.coley.recaf.android.cf;

import org.jf.dexlib2.base.BaseMethodParameter;
import org.jf.dexlib2.iface.MethodParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mutable implementation of {@link MethodParameter}.
 *
 * @author Matt Coley
 */
public class MutableMethodParameter extends BaseMethodParameter implements MethodParameter {
	private Set<MutableAnnotation> annotations;
	private String name;
	private String type;

	/**
	 * @param original
	 * 		Instance to copy.
	 */
	public MutableMethodParameter(MethodParameter original) {
		this(original.getName(), original.getType(), MutableAnnotation.copyAnnotations(original.getAnnotations()));
	}

	/**
	 * @param name
	 * 		Parameter name, may be {@code null}.
	 * @param type
	 * 		Parameter type.
	 * @param annotations
	 * 		Set of annotations applied to the parameter.
	 */
	public MutableMethodParameter(@Nullable String name, @Nonnull String type,
								  @Nonnull Set<MutableAnnotation> annotations) {
		this.annotations = annotations;
		this.name = name;
		this.type = type;
	}

	@Nonnull
	@Override
	public Set<MutableAnnotation> getAnnotations() {
		return annotations;
	}

	/**
	 * @param annotations
	 * 		New set of annotations for the parameter.
	 */
	public void setAnnotations(@Nonnull Set<MutableAnnotation> annotations) {
		this.annotations = annotations;
	}

	@Nonnull
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 * 		Optional parameter name.
	 */
	public void setName(@Nullable String name) {
		this.name = name;
	}

	@Nonnull
	@Override
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * 		New parameter type.
	 */
	public void setType(@Nonnull String type) {
		this.type = type;
	}

	/**
	 * @param parameters
	 * 		Original list of parameters.
	 *
	 * @return List of mutable copies.
	 */
	public static List<MutableMethodParameter> copyParameters(List<? extends MethodParameter> parameters) {
		List<MutableMethodParameter> list = new ArrayList<>();
		for (MethodParameter parameter : parameters) {
			list.add(new MutableMethodParameter(parameter));
		}
		return list;
	}
}