package me.coley.recaf.decompile.fallback.model;

import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.Method;
import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.cafedude.classfile.annotation.ElementValue;
import me.coley.cafedude.classfile.attribute.*;
import me.coley.cafedude.classfile.constant.CpClass;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Printable;
import me.coley.recaf.decompile.fallback.print.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Basic method model wrapping a {@link Method}.
 *
 * @author Matt Coley
 */
public class MethodModel implements Printable {
	private final MethodPrintStrategy printStrategy;
	private final ClassModel owner;
	private final Method method;
	private final ConstPool pool;

	/**
	 * @param owner
	 * 		Declaring class.
	 * @param method
	 * 		Method wrapped.
	 */
	public MethodModel(ClassModel owner, Method method) {
		this.owner = owner;
		this.method = method;
		pool = owner.getClassFile().getPool();
		// Determine print strategy
		if (getName().equals("<init>")) {
			printStrategy = new ConstructorMethodPrintStrategy();
		} else if (getName().equals("<clinit>")) {
			printStrategy = new StaticInitMethodPrintStrategy();
		} else if (owner.isAnnotation()) {
			printStrategy = new AnnotationMethodPrintStrategy();
		} else if (owner.isInterface()) {
			printStrategy = new InterfaceMethodPrintStrategy();
		} else {
			printStrategy = new BasicMethodPrintStrategy();
		}
	}

	/**
	 * @return Const pool of the {@link #getOwner() declaring class}.
	 */
	public ConstPool getPool() {
		return pool;
	}

	/**
	 * @return Declaring class.
	 */
	public ClassModel getOwner() {
		return owner;
	}

	/**
	 * @return Method name.
	 */
	public String getName() {
		return pool.getUtf(method.getNameIndex());
	}

	/**
	 * @return Method descriptor.
	 */
	public String getDesc() {
		return pool.getUtf(method.getTypeIndex());
	}

	/**
	 * @return Method modifiers.
	 */
	public int getAccess() {
		return method.getAccess();
	}

	/**
	 * @return Method thrown exception types.
	 */
	public List<String> getThrownTypes() {
		Optional<ExceptionsAttribute> exceptionsAttribute = method.getAttributes().stream()
				.filter(attribute -> attribute instanceof ExceptionsAttribute)
				.map(attribute -> ((ExceptionsAttribute) attribute))
				.findFirst();
		if (exceptionsAttribute.isEmpty())
			return Collections.emptyList();
		else
			return exceptionsAttribute.get().getExceptionIndexTable().stream()
					.map(classIndex -> (CpClass) pool.get(classIndex))
					.map(cpClass -> pool.getUtf(cpClass.getIndex()))
					.collect(Collectors.toList());
	}

	/**
	 * The code attribute contains sub-attributes.
	 * Some data should iterate over {@link CodeAttribute#getAttributes()} instead of {@link Method#getAttributes()}.
	 *
	 * @return Code attribute.
	 */
	public CodeAttribute getCodeAttribute() {
		Optional<CodeAttribute> codeAttribute = method.getAttributes().stream()
				.filter(attribute -> attribute instanceof CodeAttribute)
				.map(attribute -> ((CodeAttribute) attribute))
				.findFirst();
		if (codeAttribute.isEmpty())
			return null;
		return codeAttribute.get();
	}

	/**
	 * @return Variable table attribute, or {@code null} if no variable data is present.
	 */
	public LocalVariableTableAttribute getLocalVariableTable() {
		CodeAttribute code = getCodeAttribute();
		if (code == null)
			return null;
		Optional<LocalVariableTableAttribute> localsAttribute = code.getAttributes().stream()
				.filter(attribute -> attribute instanceof LocalVariableTableAttribute)
				.map(attribute -> ((LocalVariableTableAttribute) attribute))
				.findFirst();
		if (localsAttribute.isEmpty())
			return null;
		return localsAttribute.get();
	}

	/**
	 * @return All annotations from both runtime-visible and runtime-invisible attributes.
	 */
	public List<Annotation> getAnnotations() {
		Optional<AnnotationsAttribute> annotationsAttribute = method.getAttributes().stream()
				.filter(attribute -> attribute instanceof AnnotationsAttribute)
				.map(attribute -> ((AnnotationsAttribute) attribute))
				.findFirst();
		if (annotationsAttribute.isEmpty())
			return Collections.emptyList();
		return annotationsAttribute.get().getAnnotations();
	}

	/**
	 * @return Annotation value.
	 */
	public ElementValue getAnnotationDefaultValue() {
		// Skip if parent is not an annotation
		if (!owner.isAnnotation())
			return null;
		Optional<AnnotationDefaultAttribute> annotationDefaultAttribute = method.getAttributes().stream()
				.filter(attribute -> attribute instanceof AnnotationDefaultAttribute)
				.map(attribute -> ((AnnotationDefaultAttribute) attribute))
				.findFirst();
		if (annotationDefaultAttribute.isEmpty())
			return null;
		return annotationDefaultAttribute.get().getElementValue();
	}

	@Override
	public String print(PrintContext context) {
		return printStrategy.print(owner, this);
	}
}
