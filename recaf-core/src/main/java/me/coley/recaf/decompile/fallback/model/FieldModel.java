package me.coley.recaf.decompile.fallback.model;

import me.coley.cafedude.classfile.ConstPool;
import me.coley.cafedude.classfile.ConstantPoolConstants;
import me.coley.cafedude.classfile.Field;
import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.cafedude.classfile.attribute.AnnotationsAttribute;
import me.coley.cafedude.classfile.attribute.ConstantValueAttribute;
import me.coley.cafedude.classfile.constant.*;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Printable;
import me.coley.recaf.decompile.fallback.print.BasicFieldPrintStrategy;
import me.coley.recaf.decompile.fallback.print.EnumConstFieldPrintStrategy;
import me.coley.recaf.decompile.fallback.print.FieldPrintStrategy;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Basic field model wrapping a {@link Field}.
 *
 * @author Matt Coley
 */
public class FieldModel implements Printable, ConstantPoolConstants {
	private final FieldPrintStrategy printStrategy;
	private final ClassModel owner;
	private final Field field;
	private final ConstPool pool;

	/**
	 * @param owner
	 * 		Declaring class.
	 * @param field
	 * 		Field wrapped.
	 */
	public FieldModel(ClassModel owner, Field field) {
		this.owner = owner;
		this.field = field;
		pool = owner.getClassFile().getPool();
		// Determine print strategy
		if (owner.isEnum() && isEnumConst()) {
			printStrategy = new EnumConstFieldPrintStrategy();
		} else {
			printStrategy = new BasicFieldPrintStrategy();
		}
	}

	/**
	 * @return {@code true} when the field should be treated as an enum const.
	 */
	public boolean isEnumConst() {
		// Quick way to check if the print strategy is already applied.
		if (printStrategy instanceof EnumConstFieldPrintStrategy)
			return true;
		// Type must be the declaring class
		String p = Type.getType(getDesc()).getInternalName();
		if (!p.equals(owner.getName()))
			return false;
		// Must have const flags
		if (!AccessFlag.hasAll(field.getAccess(), AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL))
			return false;
		// All enum constants must be adjacent
		List<FieldModel> fields = owner.getFields();
		if (fields.isEmpty())
			return true;
		int index = fields.indexOf(this);
		if (index > 0) {
			return fields.get(index - 1).isEnumConst();
		} else {
			return true;
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
	 * @return Field name.
	 */
	public String getName() {
		return pool.getUtf(field.getNameIndex());
	}

	/**
	 * @return Field descriptor.
	 */
	public String getDesc() {
		return pool.getUtf(field.getTypeIndex());
	}

	/**
	 * @return Field modifiers.
	 */
	public int getAccess() {
		return field.getAccess();
	}

	/**
	 * @return All annotations from both runtime-visible and runtime-invisible attributes.
	 */
	public List<Annotation> getAnnotations() {
		Optional<AnnotationsAttribute> annotationsAttribute = field.getAttributes().stream()
				.filter(attribute -> attribute instanceof AnnotationsAttribute)
				.map(attribute -> ((AnnotationsAttribute) attribute))
				.findFirst();
		if (annotationsAttribute.isEmpty())
			return Collections.emptyList();
		return annotationsAttribute.get().getAnnotations();
	}

	/**
	 * @return Field constant value, or {@code null} for no constant value.
	 */
	public Object getConstValue() {
		Optional<ConstantValueAttribute> valueAttribute = field.getAttributes().stream()
				.filter(attribute -> attribute instanceof ConstantValueAttribute)
				.map(attribute -> ((ConstantValueAttribute) attribute))
				.findFirst();
		if (valueAttribute.isEmpty())
			return null;
		int cpValueIndex = valueAttribute.get().getConstantValueIndex();
		ConstPoolEntry entry = pool.get(cpValueIndex);
		switch (entry.getTag()) {
			case INTEGER:
				return ((CpInt) entry).getValue();
			case FLOAT:
				return ((CpFloat) entry).getValue();
			case LONG:
				return ((CpLong) entry).getValue();
			case DOUBLE:
				return ((CpDouble) entry).getValue();
			case CLASS:
				int classNameIndex = ((CpClass) entry).getIndex();
				return pool.getUtf(classNameIndex);
			case STRING:
				int utfIndex = ((CpString) entry).getIndex();
				return pool.getUtf(utfIndex);
			default:
				// Unsupported value
				return null;
		}
	}

	@Override
	public String print(PrintContext context) {
		return printStrategy.print(owner, this);
	}
}
