package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
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
	public static final String IDENTIFIER = "cleanup.vartable";

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		boolean slotsChanged = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			Type[] argumentTypes = Type.getMethodType(method.desc).getArgumentTypes();
			boolean isStatic = AccessFlag.isStatic(method.access);
			int slot = isStatic ? 0 : 1;

			InsnList instructions = method.instructions;
			if (instructions == null) {
				// No instructions, so no local variable usage.
				// Just rebuild the parameter table to match the method signature.
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
				// Reduce the number of used local slots to the minimum required by the method.
				boolean compacted = compactLocalSlots(method, argumentTypes, isStatic);
				dirty |= compacted;
				slotsChanged |= compacted;
				if (compacted)
					method.localVariables = null;

				// Recompute max locals to match the number of used local slots.
				int normalizedMaxLocals = normalizedMaxLocals(method, argumentTypes, isStatic);
				if (method.maxLocals != normalizedMaxLocals) {
					method.maxLocals = normalizedMaxLocals;
					dirty = true;
					slotsChanged = true;
				}

				// If the method only uses parameter variables, but has local variable metadata for non-parameter variables,
				// We will nuke the table and rebuild it to match the parameter variables.
				if (!hasNonParameterUsage(method, isStatic, argumentTypes)
						&& hasNonParameterMetadata(method, isStatic, argumentTypes)) {
					method.localVariables = null;
					dirty = true;
				}

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
		if (dirty) {
			// Renumbered slots and a smaller max locals leave existing frames naming slots that no longer exist.
			if (slotsChanged)
				context.setRecomputeFrames(className);
			context.setNode(bundle, initialClassState, node);
		}
	}

	/**
	 * @param method
	 * 		Method to check for non-parameter variable usage.
	 * @param isStatic
	 *        {@code true} if the method is static, {@code false} otherwise.
	 * @param argumentTypes
	 * 		Method argument types.
	 *
	 * @return {@code true} if the method has non-parameter variable slot reads/writes.
	 */
	private static boolean hasNonParameterUsage(@Nonnull MethodNode method, boolean isStatic,
	                                            @Nonnull Type[] argumentTypes) {
		int parameterEnd = Types.parameterEndSlot(isStatic, argumentTypes);
		for (AbstractInsnNode instruction : method.instructions) {
			if (instruction instanceof VarInsnNode variable && variable.var >= parameterEnd)
				return true;
			if (instruction instanceof IincInsnNode increment && increment.var >= parameterEnd)
				return true;
		}
		return false;
	}

	/**
	 * @param method
	 * 		Method to check for non-parameter variable metadata.
	 * @param isStatic
	 *        {@code true} if the method is static, {@code false} otherwise.
	 * @param argumentTypes
	 * 		Method argument types.
	 *
	 * @return {@code true} if the method has non-parameter variable entries.
	 */
	private static boolean hasNonParameterMetadata(@Nonnull MethodNode method, boolean isStatic,
	                                               @Nonnull Type[] argumentTypes) {
		if (method.localVariables == null)
			return false;
		int parameterEnd = Types.parameterEndSlot(isStatic, argumentTypes);
		for (LocalVariableNode variable : method.localVariables)
			if (variable.index >= parameterEnd)
				return true;
		return false;
	}

	/**
	 * Assumed to be used after {@link #compactLocalSlots(MethodNode, Type[], boolean)}.
	 *
	 * @param method
	 * 		Method to compute the normalized max locals for.
	 * @param argumentTypes
	 * 		Method argument types.
	 * @param isStatic
	 *        {@code true} if the method is static, {@code false} otherwise.
	 *
	 * @return Max local slot count for the method.
	 */
	private static int normalizedMaxLocals(@Nonnull MethodNode method, @Nonnull Type[] argumentTypes, boolean isStatic) {
		int maxLocals = Types.parameterEndSlot(isStatic, argumentTypes);
		for (AbstractInsnNode instruction : method.instructions) {
			if (instruction instanceof VarInsnNode variable) {
				maxLocals = Math.max(maxLocals, variable.var + AsmInsnUtil.getTypeForVarInsn(variable).getSize());
			} else if (instruction instanceof IincInsnNode increment) {
				maxLocals = Math.max(maxLocals, increment.var + 1);
			}
		}
		return maxLocals;
	}

	/**
	 * Compacts the local variable slots used by the method to the minimum required.
	 *
	 * @param method
	 * 		Method to compact local variable slots for.
	 * @param argumentTypes
	 * 		Method argument types.
	 * @param isStatic
	 *        {@code true} if the method is static, {@code false} otherwise.
	 *
	 * @return {@code true} if the method was modified, {@code false} otherwise.
	 */
	private static boolean compactLocalSlots(@Nonnull MethodNode method, @Nonnull Type[] argumentTypes, boolean isStatic) {
		// Keep parameters fixed and collect every referenced slot that the normalized output must compact.
		int parameterEnd = Types.parameterEndSlot(isStatic, argumentTypes);

		// Map of original slot -> new slot.
		Map<Integer, Integer> remapping = new TreeMap<>();
		for (AbstractInsnNode instruction : method.instructions) {
			int slot;
			int width;
			if (instruction instanceof VarInsnNode variable) {
				slot = variable.var;
				width = AsmInsnUtil.getTypeForVarInsn(variable).getSize();
			} else if (instruction instanceof IincInsnNode increment) {
				slot = increment.var;
				width = 1;
			} else {
				continue;
			}

			// Fill slots used by locals (not parameters) with a placeholder to be remapped later.
			for (int offset = 0; offset < width; offset++) {
				int originalSlot = slot + offset;
				if (originalSlot >= parameterEnd)
					remapping.putIfAbsent(originalSlot, 0);
			}
		}
		if (remapping.isEmpty())
			return false;

		// Linear remapping of all non-parameter slots to the next available slot after the parameters.
		// Reserved slots are in this map too, so wide variables are implicitly handled too.
		int nextSlot = parameterEnd;
		for (Integer originalSlot : remapping.keySet())
			remapping.put(originalSlot, nextSlot++);

		// Rewrite all variable referencing instructions to use the new slots.
		boolean dirty = false;
		for (AbstractInsnNode instruction : method.instructions) {
			if (instruction instanceof VarInsnNode variable && variable.var >= parameterEnd) {
				Integer replacement = remapping.get(variable.var);
				if (replacement != null && replacement != variable.var) {
					variable.var = replacement;
					dirty = true;
				}
			} else if (instruction instanceof IincInsnNode increment && increment.var >= parameterEnd) {
				Integer replacement = remapping.get(increment.var);
				if (replacement != null && replacement != increment.var) {
					increment.var = replacement;
					dirty = true;
				}
			}
		}

		return dirty;
	}

	@Nonnull
	@Override
	public String identifier() {
		return IDENTIFIER;
	}

	private record NameType(@Nonnull String name, @Nonnull Type type) {}
}
