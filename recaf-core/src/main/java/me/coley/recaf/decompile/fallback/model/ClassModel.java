package me.coley.recaf.decompile.fallback.model;

import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.Field;
import me.coley.cafedude.classfile.Method;
import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.cafedude.classfile.attribute.AnnotationsAttribute;
import me.coley.cafedude.classfile.constant.CpClass;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Printable;
import me.coley.recaf.decompile.fallback.print.*;
import me.coley.recaf.util.AccessFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Basic class model wrapping a {@link ClassFile}.
 *
 * @author Matt Coley
 */
public class ClassModel implements Printable {
	private final ClassFile classFile;
	private final List<String> interfaces = new ArrayList<>();
	private final List<FieldModel> fields = new ArrayList<>();
	private final List<MethodModel> methods = new ArrayList<>();
	private final ClassPrintStrategy printStrategy;

	/**
	 * Create a new model.
	 *
	 * @param classFile
	 * 		Class file to pull info from.
	 */
	public ClassModel(ClassFile classFile) {
		this.classFile = classFile;
		int access = classFile.getAccess();
		if (AccessFlag.isEnum(access)) {
			printStrategy = new EnumClassPrintStrategy();
		} else if (AccessFlag.isAnnotation(access)) {
			printStrategy = new AnnotationClassPrintStrategy();
		} else if (AccessFlag.isInterface(access)) {
			printStrategy = new InterfaceClassPrintStrategy();
		} else {
			printStrategy = new BasicClassPrintStrategy();
		}
		// Add interfaces
		for (int itf : classFile.getInterfaceIndices()) {
			CpClass cpClass = (CpClass) classFile.getPool().get(itf);
			interfaces.add(classFile.getPool().getUtf(cpClass.getIndex()));
		}
		// Add fields and methods
		for (Field field : classFile.getFields())
			fields.add(new FieldModel(this, field));
		for (Method method : classFile.getMethods())
			methods.add(new MethodModel(this, method));
	}

	/**
	 * @return {@code true} when the class contains enum flag.
	 */
	public boolean isEnum() {
		return AccessFlag.isEnum(getAccess());
	}

	/**
	 * @return {@code true} when the class contains interface flag.
	 */
	public boolean isInterface() {
		return AccessFlag.isInterface(getAccess());
	}

	/**
	 * @return {@code true} when class contains enum flag.
	 */
	public boolean isAnnotation() {
		return AccessFlag.isAnnotation(getAccess());
	}

	/**
	 * @return Class file to pull info from.
	 */
	public ClassFile getClassFile() {
		return classFile;
	}

	/**
	 * @return Constant pool of the class.
	 */
	public ConstPool getPool() {
		return getClassFile().getPool();
	}

	/**
	 * @return Class name.
	 */
	public String getName() {
		return classFile.getName();
	}

	/**
	 * @return Parent class name.
	 */
	public String getSuperName() {
		if (classFile.getSuperIndex() == 0)
			return null;
		return classFile.getSuperName();
	}

	/**
	 * @return Class access flag mask.
	 */
	public int getAccess() {
		return classFile.getAccess();
	}

	/**
	 * @return Implemented interface names.
	 */
	public List<String> getInterfaces() {
		return interfaces;
	}

	/**
	 * @return Declared fields.
	 */
	public List<FieldModel> getFields() {
		return fields;
	}

	/**
	 * @return Declared methods.
	 */
	public List<MethodModel> getMethods() {
		return methods;
	}

	/**
	 * @return All annotations from both runtime-visible and runtime-invisible attributes.
	 */
	public List<Annotation> getAnnotations() {
		Optional<AnnotationsAttribute> annotationsAttribute = classFile.getAttributes().stream()
				.filter(attribute -> attribute instanceof AnnotationsAttribute)
				.map(attribute -> ((AnnotationsAttribute) attribute))
				.findFirst();
		if (annotationsAttribute.isEmpty())
			return Collections.emptyList();
		return annotationsAttribute.get().getAnnotations();
	}

	@Override
	public String print(PrintContext context) {
		return printStrategy.print(this);
	}
}
