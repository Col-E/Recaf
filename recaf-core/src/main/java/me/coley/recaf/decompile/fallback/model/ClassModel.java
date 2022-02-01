package me.coley.recaf.decompile.fallback.model;

import me.coley.cafedude.ClassFile;
import me.coley.cafedude.Field;
import me.coley.cafedude.Method;
import me.coley.cafedude.constant.CpClass;
import me.coley.recaf.assemble.ast.Printable;
import me.coley.recaf.decompile.fallback.print.*;
import me.coley.recaf.util.AccessFlag;

import java.util.ArrayList;
import java.util.List;

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
	 * @return Class name.
	 */
	public String getName() {
		return classFile.getName();
	}

	/**
	 * @return Parent class name.
	 */
	public String getSuperName() {
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

	@Override
	public String print() {
		return printStrategy.print(this);
	}
}
