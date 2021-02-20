package me.coley.recaf.android.cf;

import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Mutable implementation of {@link Field}.
 *
 * @author Matt Coley
 */
public class MutableField extends BaseFieldReference implements Field {
	private String definingClass;
	private String name;
	private String type;
	private int accessFlags;
	private EncodedValue initialValue;
	private Set<MutableAnnotation> annotations;
	private Set<HiddenApiRestriction> hiddenApiRestrictions;

	/**
	 * @param original
	 * 		Instance to copy.
	 */
	public MutableField(Field original) {
		definingClass = original.getDefiningClass();
		name = original.getName();
		type = original.getType();
		accessFlags = original.getAccessFlags();
		initialValue = original.getInitialValue();
		annotations = MutableAnnotation.copyAnnotations(original.getAnnotations());
		hiddenApiRestrictions = original.getHiddenApiRestrictions();
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
	 * 		New field name.
	 */
	public void setName(@Nonnull String name) {
		this.name = name;
	}

	@Nonnull
	@Override
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * 		New field type.
	 */
	public void setType(@Nonnull String type) {
		this.type = type;
	}

	@Override
	public int getAccessFlags() {
		return accessFlags;
	}

	/**
	 * @param accessFlags
	 * 		New field access modifiers.
	 */
	public void setAccessFlags(int accessFlags) {
		this.accessFlags = accessFlags;
	}

	@Nullable
	@Override
	public EncodedValue getInitialValue() {
		return initialValue;
	}

	/**
	 * @param initialValue
	 * 		New initial value.
	 */
	public void setInitialValue(@Nonnull EncodedValue initialValue) {
		this.initialValue = initialValue;
	}

	@Nonnull
	@Override
	public Set<MutableAnnotation> getAnnotations() {
		return annotations;
	}

	/**
	 * @param annotations
	 * 		New set of annotations to apply to field.
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

	/**
	 * @param fields
	 * 		Original set of fields.
	 *
	 * @return Set of mutable copies.
	 */
	public static Set<MutableField> copyFields(Iterable<? extends Field> fields) {
		Set<MutableField> list = new HashSet<>();
		for (Field field : fields) {
			list.add(new MutableField(field));
		}
		return list;
	}
}
