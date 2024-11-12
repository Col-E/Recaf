package software.coley.recaf.services.compile.stub;

import dev.xdark.blw.type.MethodType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;

/**
 * Class stub generator which emits classes under the assumption they are inner classes of an outer class.
 *
 * @author Matt Coley
 */
public class InnerClassStubGenerator extends ClassStubGenerator {
	/**
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * @param classAccess
	 * 		Host class access modifiers.
	 * @param className
	 * 		Host class name.
	 * @param superName
	 * 		Host class super name.
	 * @param implementing
	 * 		Host class interfaces implemented.
	 * @param fields
	 * 		Host class declared fields.
	 * @param methods
	 * 		Host class declared methods.
	 * @param innerClasses
	 * 		Host class declared inner classes.
	 */
	public InnerClassStubGenerator(@Nonnull Workspace workspace,
	                               int classAccess,
	                               @Nonnull String className,
	                               @Nullable String superName,
	                               @Nonnull List<String> implementing,
	                               @Nonnull List<FieldMember> fields,
	                               @Nonnull List<MethodMember> methods,
	                               @Nonnull List<InnerClassInfo> innerClasses) {
		super(workspace, classAccess, className, superName, implementing, fields, methods, innerClasses);
	}

	@Nonnull
	@Override
	protected String getLocalName() {
		// Will be "OuterClass$TheInner"
		String localName = super.getLocalName();

		// We just want "TheInner"
		int innerSplit = localName.indexOf('$');
		if (innerSplit > 0)
			localName = localName.substring(innerSplit + 1);

		return localName;
	}

	@Nonnull
	@Override
	public String getLocalModifier() {
		if (AccessFlag.isAbstract(classAccess))
			return "abstract";

		// If the inner class (this context) is not abstract, we do not want to force
		// it to be abstract in order to allow expressions to do "new Inner()" and stuff.
		return "";
	}

	@Override
	public String generate() throws ExpressionCompileException {
		StringBuilder code = new StringBuilder();

		appendClassStructure(code);
		appendEnumConsts(code);
		appendFields(code);
		appendMethods(code);
		appendInnerClasses(code);
		appendClassEnd(code);

		return code.toString();
	}

	@Override
	protected boolean doSkipMethod(@Nonnull String name, @Nonnull MethodType type) {
		// Do not skip any methods
		return false;
	}
}
