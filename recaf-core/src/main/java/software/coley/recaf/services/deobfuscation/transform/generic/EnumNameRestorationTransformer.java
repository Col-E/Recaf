package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.objectweb.asm.Opcodes.*;
import static software.coley.recaf.util.AsmInsnUtil.getNextFollowGoto;
import static software.coley.recaf.util.AsmInsnUtil.isConstIntValue;

/**
 * A transformer that creates mappings to rename obfuscated enum constants that have been not properly obfuscated.
 *
 * @author Matt Coley
 */
@Dependent
public class EnumNameRestorationTransformer implements JvmClassTransformer {
	private static final String VALUES_ARRAY_NAME = "$values";

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		// Skip non-enum classes.
		if (!initialClassState.hasEnumModifier()) return;

		// Record mappings for enum constants where the static initializer
		// leaks their original names (assuming the code is name obfuscated).
		ClassNode node = context.getNode(bundle, initialClassState);
		String constDesc = 'L' + node.name + ";";
		String valuesDesc = '[' + constDesc;
		for (MethodNode method : node.methods) {
			// Skip abstract methods.
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;

			// Skip any method that is not the static initializer.
			if (!method.name.equals("<clinit>") || !method.desc.equals("()V"))
				continue;

			// Pattern to match for constants:
			//   new ENUM_TYPE
			//   dup
			//   ldc "NAME_OF_CONSTANT"
			//   iconst_0
			//   invokespecial ENUM_TYPE.<init> (Ljava/lang/String;I)V   (may have additional arguments, but first two should be consistent)
			//   astore v0                                               (optional)
			//   aload v0                                                (optional)
			//   putstatic ENUM_TYPE.OBF_NAME_OF_CONSTANT LENUM_TYPE;
			// Pattern to match for $values array
			//   invokestatic ENUM_TYPE.$values ()[LENUM_TYPE;
			//   putstatic Example.OBF_NAME_OF_ARRAY [LENUM_TYPE;
			for (AbstractInsnNode instruction : instructions) {
				int op = instruction.getOpcode();
				if (op == LDC)
					handleEnumConst(context, initialClassState, (LdcInsnNode) instruction, constDesc);
				else if (op == INVOKESTATIC)
					handleValuesArray(context, initialClassState, (MethodInsnNode) instruction, valuesDesc);
			}
		}
	}

	private void handleValuesArray(@Nonnull JvmTransformerContext context,
	                               @Nonnull JvmClassInfo initialClassState,
	                               @Nonnull MethodInsnNode invokeInsn,
	                               @Nonnull String valuesDesc) {
		String enumOwner = initialClassState.getName();
		String invokeOwner = invokeInsn.owner;
		String invokeName = invokeInsn.name;
		String invokeDesc = invokeInsn.desc;
		if (invokeOwner.equals(enumOwner) && invokeDesc.equals("()" + valuesDesc)) {
			AbstractInsnNode next = getNextFollowGoto(invokeInsn);
			while (next != null && next.getOpcode() != PUTSTATIC)
				next = getNextFollowGoto(next);
			if (next != null && next.getOpcode() == PUTSTATIC) {
				FieldInsnNode assignmentInsn = (FieldInsnNode) next;
				String fieldOwner = assignmentInsn.owner;
				String fieldName = assignmentInsn.name;
				String fieldDesc = assignmentInsn.desc;
				if (fieldOwner.equals(enumOwner) && fieldDesc.equals(valuesDesc) && !fieldName.equals(VALUES_ARRAY_NAME))
					context.getMappings().addField(enumOwner, valuesDesc, fieldName, VALUES_ARRAY_NAME);
			}
		}
	}

	private static void handleEnumConst(@Nonnull JvmTransformerContext context,
	                                    @Nonnull JvmClassInfo initialClassState,
	                                    @Nonnull LdcInsnNode nameInsn,
	                                    @Nonnull String constDesc) {
		if (!(nameInsn.cst instanceof String nameString) || !nameString.matches("\\w+"))
			return;

		AbstractInsnNode indexInsn = getNextFollowGoto(nameInsn);
		if (indexInsn == null || !isConstIntValue(indexInsn))
			return;

		AbstractInsnNode next = getNextFollowGoto(indexInsn);
		while (next != null && next.getOpcode() != PUTSTATIC)
			next = getNextFollowGoto(next);
		if (next != null && next.getOpcode() == PUTSTATIC) {
			FieldInsnNode assignmentInsn = (FieldInsnNode) next;
			String fieldName = assignmentInsn.name;
			String fieldDesc = assignmentInsn.desc;
			if (fieldDesc.equals(constDesc) && !fieldName.equals(nameString)) {
				FieldMember field = initialClassState.getDeclaredField(fieldName, fieldDesc);
				if (field != null && field.hasEnumModifier())
					context.getMappings().addField(initialClassState.getName(), constDesc, fieldName, nameString);
			}
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Enum name restoration";
	}
}
