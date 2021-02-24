package me.coley.recaf.android.cf;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.ClassDef;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mutable implementation of {@link ClassDef}.
 *
 * @author Matt Coley
 */
public class MutableClassDef extends BaseTypeReference implements ClassDef {
	private String superClass;
	private String type;
	private String sourceFile;
	private int accessFlags;
	private List<String> interfaces;
	private Set<MutableField> fields;
	private Set<MutableMethod> methods;
	private Set<MutableAnnotation> annotations;

	/**
	 * Create a class definition from an existing class.
	 *
	 * @param original
	 * 		Original class definition to copy.
	 */
	public MutableClassDef(ClassDef original) {
		this.type = original.getType();
		this.superClass = original.getSuperclass();
		this.sourceFile = original.getSourceFile();
		this.accessFlags = original.getAccessFlags();
		this.interfaces = new ArrayList<>(original.getInterfaces());
		this.fields = MutableField.copyFields(original.getFields());
		this.methods = MutableMethod.copyMethods(original.getMethods());
		this.annotations = MutableAnnotation.copyAnnotations(original.getAnnotations());
	}

	@Nonnull
	@Override
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * 		New class type.
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
	 * 		New access flags.
	 */
	public void setAccessFlags(int accessFlags) {
		this.accessFlags = accessFlags;
	}

	@Nonnull
	@Override
	public String getSuperclass() {
		return superClass;
	}

	/**
	 * @param superClass
	 * 		New super-class type.
	 */
	public void setSuperClass(@Nonnull String superClass) {
		this.superClass = superClass;
	}


	@Nonnull
	@Override
	public List<String> getInterfaces() {
		return interfaces;
	}

	/**
	 * @param interfaces
	 * 		New interface type list.
	 */
	public void setInterfaces(@Nonnull List<String> interfaces) {
		this.interfaces = interfaces;
	}

	@Nonnull
	@Override
	public String getSourceFile() {
		return sourceFile;
	}

	/**
	 * @param sourceFile
	 * 		New source name.
	 */
	public void setSourceFile(@Nonnull String sourceFile) {
		this.sourceFile = sourceFile;
	}

	@Nonnull
	@Override
	public Set<MutableAnnotation> getAnnotations() {
		return new HashSet<>(annotations);
	}

	/**
	 * @param annotations
	 * 		New annotation set.
	 */
	public void setAnnotations(@Nonnull Set<MutableAnnotation> annotations) {
		this.annotations = annotations;
	}

	@Nonnull
	@Override
	public Iterable<MutableField> getStaticFields() {
		return fields.stream()
				.filter(f -> (f.getAccessFlags() & AccessFlags.STATIC.getValue()) > 0)
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<MutableField> getInstanceFields() {
		return fields.stream()
				.filter(f -> (f.getAccessFlags() & AccessFlags.STATIC.getValue()) == 0)
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<MutableField> getFields() {
		return fields;
	}

	/**
	 * @param fields
	 * 		New field set.
	 */
	public void setFields(@Nonnull Set<MutableField> fields) {
		this.fields = fields;
	}

	@Nonnull
	@Override
	public Iterable<MutableMethod> getDirectMethods() {
		// Direct: static, private, or constructor
		return methods.stream()
				.filter(MutableMethod::isDirect)
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<MutableMethod> getVirtualMethods() {
		// Direct: non-static, non-private, and not-constructor
		return methods.stream()
				.filter(m -> !MutableMethod.isDirect(m))
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<MutableMethod> getMethods() {
		// Combination of direct/virtual methods
		return methods;
	}

	/**
	 * @param methods
	 * 		New method set.
	 */
	public void setMethods(@Nonnull Set<MutableMethod> methods) {
		this.methods = methods;
	}
}
