package software.coley.recaf.services.compile.stub;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;

/**
 * Stub generator for a nested/anonymous class emitted as a top-level declaration.
 * Facilitates calls from these nested anonymous classes to outer class members with synthetic fields.
 *
 * @author Matt Coley
 */
public class DetachedClassStubGenerator extends ClassStubGenerator {
	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 * @param info
	 * 		Class information to generate a stub for.
	 */
	public DetachedClassStubGenerator(@Nonnull Workspace workspace,
	                                  @Nonnull InheritanceGraph inheritanceGraph,
	                                  @Nonnull ClassInfo info) {
		super(workspace, inheritanceGraph, info.getAccess(),
				info.getName(), info.getSuperName(), info.getInterfaces(), info.getFields(),
				info.getMethods(), info.getInnerClasses(), info.getSignature());
	}

	private DetachedClassStubGenerator(@Nonnull Workspace workspace,
	                                   @Nonnull InheritanceGraph inheritanceGraph,
	                                   int classAccess,
	                                   @Nonnull String className,
	                                   @Nullable String superName,
	                                   @Nonnull List<String> implementing,
	                                   @Nonnull List<FieldMember> fields,
	                                   @Nonnull List<MethodMember> methods,
	                                   @Nonnull List<InnerClassInfo> innerClasses,
	                                   @Nullable String classSignature) {
		super(workspace, inheritanceGraph, classAccess, className, superName, implementing, fields, methods, innerClasses, classSignature);
	}

	@Override
	public String generate() throws ExpressionCompileException {
		StringBuilder code = new StringBuilder();
		appendClassStructure(code);
		appendDetachedOuterField(code);
		appendClassContents(code, false);
		return code.toString();
	}

	@Override
	protected boolean isNonStaticInnerClass() {
		// The '$' is part of the top-level source identifier in this representation.
		return false;
	}

	@Override
	protected boolean doSkipMethod(@Nonnull String name, @Nonnull Type type) {
		return false;
	}

	@Override
	public String getLocalModifier() {
		return "abstract";
	}

	@Override
	protected void appendConstructorBody(@Nonnull StringBuilder code, @Nonnull Type[] parameterTypes) {
		FieldMember field = getDetachedOuterField();
		if (field == null)
			return;

		// Assign the outer field if the constructor has a parameter of the outer type.
		String outerName = className.substring(0, className.lastIndexOf('$'));
		for (int i = 0; i < parameterTypes.length; i++) {
			Type parameterType = parameterTypes[i];
			if (parameterType.getSort() == Type.OBJECT && outerName.equals(parameterType.getInternalName())) {
				code.append("this.").append(field.getName()).append(" = p").append(i).append("; ");
				return;
			}
		}
	}

	private void appendDetachedOuterField(@Nonnull StringBuilder code) {
		// Add: private final Outer this$N;
		FieldMember field = getDetachedOuterField();
		if (field != null)
			code.append("private final ").append(sourceType(Type.getType(field.getDescriptor()))).append(' ')
					.append(field.getName()).append(";\n");
	}

	@Nullable
	private FieldMember getDetachedOuterField() {
		// Class name must imply nesting.
		int split = className.lastIndexOf('$');
		if (split < 0)
			return null;

		// The synthetic outer field is always a reference to the immediate outer class.
		String outerName = className.substring(0, split);
		return fields.stream()
				.filter(FieldMember::hasSyntheticModifier)
				.filter(field -> Type.getType(field.getDescriptor()).getSort() == Type.OBJECT)
				.filter(field -> outerName.equals(Type.getType(field.getDescriptor()).getInternalName()))
				.findFirst().orElse(null);
	}
}
