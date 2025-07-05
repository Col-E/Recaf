package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.VarInsnNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Replaces all local variables with basic patterns.
 *
 * @author Matt Coley
 */
@Dependent
public class VariableTableNormalizingTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			Type[] argumentTypes = Type.getMethodType(method.desc).getArgumentTypes();
			boolean isStatic = AccessFlag.isStatic(method.access);
			int slot = isStatic ? 0 : 1;

			InsnList instructions = method.instructions;
			if (instructions == null) {
				List<ParameterNode> parameters = new ArrayList<>(argumentTypes.length);
				for (Type argumentType : argumentTypes) {
					parameters.add(new ParameterNode("param" + slot, 0));
					slot += argumentType.getSize();
				}

				if (!Objects.equals(parameters, method.parameters)) {
					method.parameters = parameters;
					method.localVariables = null;
					dirty = true;
				}
			} else {
				// Its easier just to add labels than to trust that each method
				// has them in valid locations to span the whole method.
				LabelNode start = new LabelNode();
				LabelNode end = new LabelNode();
				method.instructions.insert(start);
				method.instructions.add(end);

				// Populate map of:
				//  variable index ---> variable name & type
				Map<Integer, NameType> slotToTempVariable = new TreeMap<>();
				if (!isStatic) {
					slotToTempVariable.put(0, new NameType("this", Type.getObjectType(initialClassState.getName())));
				}
				for (Type argumentType : argumentTypes) {
					slotToTempVariable.put(slot, new NameType("param" + slot, argumentType));
					slot += argumentType.getSize();
				}
				for (AbstractInsnNode insn : method.instructions) {
					if (insn instanceof VarInsnNode varInsn) {
						int varSlot = varInsn.var;
						slotToTempVariable.computeIfAbsent(varSlot, v -> {
							Type varType = AsmInsnUtil.getTypeForVarInsn(varInsn);
							return new NameType("v" + v, varType);
						});
					}
				}

				// Flatten map to list, check if we already have matching variables.
				List<NameType> newNameTypes = slotToTempVariable.values().stream().toList();
				List<NameType> existingNameTypes = method.localVariables == null ? Collections.emptyList() : method.localVariables.stream()
						.filter(l -> Types.isValidDesc(l.desc))
						.map(l -> new NameType(l.name, Type.getType(l.desc)))
						.toList();
				if (!Objects.equals(newNameTypes, existingNameTypes)) {
					// Not a match, replace what was found.
					List<LocalVariableNode> variables = slotToTempVariable.entrySet().stream()
							.map(e -> {
								int varSlot = e.getKey();
								NameType nameType = e.getValue();
								return new LocalVariableNode(nameType.name(), nameType.type().getDescriptor(), null, start, end, varSlot);
							}).toList();
					method.parameters = null;
					method.localVariables = variables;
					dirty = true;
				}
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	@Nonnull
	@Override
	public String name() {
		return "Variable table normalization";
	}

	private record NameType(@Nonnull String name, @Nonnull Type type) {}
}
