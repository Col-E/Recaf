package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.arch.module.Module;

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
	private String sourceFile;
	private String nestHost;
	private int version;
	private String superClass;
	private Module module;
	private final List<String> interfaces;
	private final List<String> permittedSubclasses = new ArrayList<>();
	private final List<InnerClass> innerClasses = new ArrayList<>();
	private final List<String> nestMembers = new ArrayList<>();
	private final List<FieldDefinition> definedFields = new ArrayList<>();
	private final List<MethodDefinition> definedMethods = new ArrayList<>();

	public ClassDefinition(Modifiers modifiers, String name) {
		this.name = name;
		this.superClass = null;
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

	public void addInnerClass(InnerClass innerClass) {
		innerClasses.add(innerClass);
	}

	public void addNestMember(String nestMember) {
		nestMembers.add(nestMember);
	}

	public void addPermittedSubclass(String permittedSubclass) {
		permittedSubclasses.add(permittedSubclass);
	}

	/**
	 * @param superClass
	 * 		New super-type name.
	 */
	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public void setNestHost(String nestHost) {
		this.nestHost = nestHost;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void setModule(Module module) {
		this.module = module;
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

	public String getSourceFile() {
		return sourceFile;
	}

	public String getNestHost() {
		return nestHost;
	}

	public List<String> getPermittedSubclasses() {
		return permittedSubclasses;
	}

	public int getVersion() {
		return version;
	}

	public Module getModule() {
		return module;
	}

	public List<String> getNestMembers() {
		return nestMembers;
	}

	public List<InnerClass> getInnerClasses() {
		return innerClasses;
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
		sb.append(context.fmtKeyword("version")).append(' ').append(version - 44).append('\n');
		if(sourceFile != null) {
			sb.append(context.fmtKeyword("sourcefile")).append(' ').append(sourceFile).append('\n');
		}
		if(nestHost != null) {
			sb.append(context.fmtKeyword("nesthost")).append(' ').append(nestHost).append('\n');
		}
		for (String permittedSubclass : permittedSubclasses) {
			sb.append(context.fmtKeyword("permittedsubclass")).append(' ').append(permittedSubclass).append('\n');
		}
		for (String nestMember : nestMembers) {
			sb.append(context.fmtKeyword("nestmember")).append(' ').append(nestMember).append('\n');
		}
		for (InnerClass innerClass : innerClasses) {
			sb.append(innerClass.print(context)).append('\n');
		}
		if(module != null) {
			sb.append(module.print(context)).append('\n');
		}
		sb.append(buildDefString(context, context.fmtKeyword("class"))).append(name);
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
