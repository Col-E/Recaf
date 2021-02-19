package me.coley.recaf.dex;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

// TODO: Write test case to validate equality with a DexBackedClassDef
// TODO: Replace interface types of field/method/annotation with mutable instances
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
	private List<Field> fields;
	private List<Method> methods;
	private List<Annotation> annotations;

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
		this.fields = copyFields(original.getFields());
		this.methods = copyMethods(original.getMethods());
		this.annotations = copyAnnotations(original.getAnnotations());
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
	public void setType(String type) {
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
	public void setSuperClass(String superClass) {
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
	public void setInterfaces(List<String> interfaces) {
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
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	/**
	 * @return List view of annotations.
	 */
	@Nonnull
	public List<Annotation> getAnnotationsAsList() {
		return annotations;
	}

	@Nonnull
	@Override
	public Set<? extends Annotation> getAnnotations() {
		return new HashSet<>(annotations);
	}

	/**
	 * @param annotations
	 * 		New annotation list.
	 */
	public void setAnnotations(List<Annotation> annotations) {
		this.annotations = annotations;
	}

	@Nonnull
	@Override
	public Iterable<? extends Field> getStaticFields() {
		return fields.stream()
				.filter(f -> (f.getAccessFlags() & AccessFlags.STATIC.getValue()) > 0)
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<? extends Field> getInstanceFields() {
		return fields.stream()
				.filter(f -> (f.getAccessFlags() & AccessFlags.STATIC.getValue()) == 0)
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<? extends Field> getFields() {
		return fields;
	}

	/**
	 * @param fields
	 * 		New field list.
	 */
	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	@Nonnull
	@Override
	public Iterable<? extends Method> getDirectMethods() {
		// Direct: static, private, or constructor
		return methods.stream()
				.filter(MutableClassDef::isDirect)
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<? extends Method> getVirtualMethods() {
		// Direct: non-static, non-private, and not-constructor
		return methods.stream()
				.filter(m -> !isDirect(m))
				.collect(Collectors.toList());
	}

	@Nonnull
	@Override
	public Iterable<? extends Method> getMethods() {
		// Combination of direct/virtual methods
		return methods;
	}

	/**
	 * @param methods
	 * 		New method list.
	 */
	public void setMethods(List<Method> methods) {
		this.methods = methods;
	}

	private static boolean isDirect(Method m) {
		if (m.getName().equals("<init>"))
			return true;
		int mask = AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue();
		return (m.getAccessFlags() & mask) == 0;
	}

	private static List<Field> copyFields(Iterable<? extends Field> fields) {
		// TODO
		return Collections.emptyList();
	}

	private static List<Method> copyMethods(Iterable<? extends Method> methods) {
		// TODO
		return Collections.emptyList();
	}

	private static List<Annotation> copyAnnotations(Set<? extends Annotation> annotations) {
		// TODO
		return Collections.emptyList();
	}
}
