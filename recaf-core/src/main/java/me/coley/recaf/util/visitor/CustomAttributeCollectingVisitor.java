package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.util.Multimap;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

/**
 * Collects custom attributes in a class.
 *
 * @author Matt Coley
 */
public class CustomAttributeCollectingVisitor extends ClassVisitor {
	private final List<Attribute> classCustomAttributes = new ArrayList<>();
	private final Multimap<String, Attribute, List<Attribute>> fieldCustomAttributes;
	private final Multimap<String, Attribute, List<Attribute>> methodCustomAttributes;

	/**
	 * @param cv
	 * 		Parent visitor.
	 */
	public CustomAttributeCollectingVisitor(ClassVisitor cv) {
		super(RecafConstants.getAsmVersion(), cv);
		fieldCustomAttributes
				= Multimap.from(new HashMap<>(), ArrayList::new);
		methodCustomAttributes
				= Multimap.from(new HashMap<>(), ArrayList::new);
	}

	/**
	 * @return {@code true} when any custom attributes were found.
	 */
	public boolean hasCustomAttributes() {
		return classCustomAttributes.size() > 0 ||
				fieldCustomAttributes.size() > 0 ||
				methodCustomAttributes.size() > 0;
	}

	/**
	 * @return Unique names of attributes found.
	 */
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

	/**
	 * @return Class level custom attributes.
	 */
	public List<Attribute> getClassCustomAttributes() {
		return classCustomAttributes;
	}

	/**
	 * @return Field level custom attributes. Keys are field names.
	 */
	public Multimap<String, Attribute, List<Attribute>> getFieldCustomAttributes() {
		return fieldCustomAttributes;
	}

	/**
	 * @return Method level custom attributes. Keys are method names.
	 */
	public Multimap<String, Attribute, List<Attribute>> getMethodCustomAttributes() {
		return methodCustomAttributes;
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		classCustomAttributes.add(attribute);
		super.visitAttribute(attribute);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
		FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
		return new FieldVisitor(RecafConstants.getAsmVersion(), fv) {
			@Override
			public void visitAttribute(Attribute attribute) {
				fieldCustomAttributes.get(name).add(attribute);
				super.visitAttribute(attribute);
			}
		};
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
		return new MethodVisitor(RecafConstants.getAsmVersion(), mv) {
			@Override
			public void visitAttribute(Attribute attribute) {
				methodCustomAttributes.get(name).add(attribute);
				super.visitAttribute(attribute);
			}
		};
	}
}
