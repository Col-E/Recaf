package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.PrintContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Definition of a class.
 *
 * @author Nowilltolife
 */
public class ClassDefinition extends AbstractDefinition implements Definition {
	private final String name;
	private String superClass;
	private final List<String> interfaces;
	private final List<FieldDefinition> definedFields = new ArrayList<>();
	private final List<MethodDefinition> definedMethods = new ArrayList<>();

	public ClassDefinition(Modifiers modifiers, String name) {
		this.name = name;
		this.superClass = "java/lang/Object";
		this.interfaces = new ArrayList<>();
		setModifiers(modifiers);
	}

	public ClassDefinition(Modifiers modifiers, String name, String superClass, List<String> interfaces) {
		this.name = name;
		this.superClass = superClass;
		this.interfaces = interfaces;
		setModifiers(modifiers);
	}

	public ClassDefinition(Modifiers modifiers, String name, String superClass, String... interfaces) {
		this(modifiers, name, superClass, Arrays.asList(interfaces));
	}

	/**
	 * Adds a new method to the class.
	 *
	 * @param method
	 * 		Method to add.
	 */
	public void addMethod(MethodDefinition method) {
		definedMethods.add(method);
		child(method);
	}

	/**
	 * Adds a new field to the class.
	 *
	 * @param field
	 * 		Field to add.
	 */
	public void addField(FieldDefinition field) {
		definedFields.add(field);
		child(field);
	}

	/**
	 * @param interfaceName
	 * 		Interface to add.
	 */
	public void addInterface(String interfaceName) {
		interfaces.add(interfaceName);
	}

	/**
	 * @param superClass
	 * 		New super-type name.
	 */
	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	/**
	 * @return Fields defined in the class.
	 */
	public List<FieldDefinition> getDefinedFields() {
		return definedFields;
	}

	/**
	 * @return Methods defined in the class.
	 */
	public List<MethodDefinition> getDefinedMethods() {
		return definedMethods;
	}

	/**
	 * @return Super type of the class.
	 */
	public String getSuperClass() {
		return superClass;
	}

	/**
	 * @return Interface types the class implements.
	 */
	public List<String> getInterfaces() {
		return interfaces;
	}

	@Override
	public boolean isClass() {
		return true;
	}

	@Override
	public boolean isField() {
		return false;
	}

	@Override
	public boolean isMethod() {
		return false;
	}

	@Override
	public String getDesc() {
		return "L" + getName() + ";";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String print(PrintContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append(context.fmtKeyword("class ")).append(name);
		if (superClass != null) {
			sb.append("\n").append(context.fmtKeyword("extends ")).append(superClass);
		}
		if (interfaces != null && !interfaces.isEmpty()) {
			sb.append("\n");
			for (int i = 0; i < interfaces.size(); i++) {
				sb.append(context.fmtKeyword("implements "));
				sb.append(interfaces.get(i));
				if (i < interfaces.size() - 1) {
					sb.append("\n");
				}
			}
		}
		sb.append("\n\n");
		for (FieldDefinition field : definedFields) {
			sb.append(field.print(context)).append("\n");
		}
		sb.append("\n");
		for (MethodDefinition method : definedMethods) {
			sb.append(method.print(context)).append("\n\n");
		}
		return sb.toString();
	}
}
