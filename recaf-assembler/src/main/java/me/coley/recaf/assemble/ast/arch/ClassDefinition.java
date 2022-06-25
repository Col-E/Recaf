package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.PrintContext;

import java.util.List;

/**
 * Definition of a class.
 *
 * @author Nowilltolife
 */
public class ClassDefinition extends AbstractDefinition implements Definition {
	// TODO: Finish implementing the class
	String name;
	String superClass;
	List<String> interfaces;
	List<FieldDefinition> definedFields;
	List<MethodDefinition> definedMethods;

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
		// Not relevant here
		return "";
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
			for (int i = 0; i < interfaces.size(); i++) {
				sb.append(context.fmtKeyword("implements "));
				sb.append(interfaces.get(i));
				if (i < interfaces.size() - 1) {
					sb.append("\n");
				}
			}
		}
		sb.append("\n");
		for (FieldDefinition field : definedFields) {
			sb.append(field.print(context));
		}
		for (MethodDefinition method : definedMethods) {
			sb.append(method.print(context));
		}
		return sb.toString();
	}
}
