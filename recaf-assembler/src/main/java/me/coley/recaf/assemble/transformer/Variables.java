package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.VariableReference;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * Analyzes the AST of a {@link MethodDefinition} and consolidates variable information.
 *
 * @author Matt Coley
 */
public class Variables implements Iterable<VariableInfo> {
	private final Map<Integer, VariableInfo> indexLookup = new LinkedHashMap<>();
	private final Map<String, VariableInfo> nameLookup = new LinkedHashMap<>();
	private final Set<Integer> wideSlots = new HashSet<>();
	private int nextAvailableSlot;
	private int currentSlot;

	/**
	 * Record the <i>"this"</i> variable if the definition is not static.
	 *
	 * @param selfType
	 * 		Type of the class defining the method.
	 * @param definition
	 * 		The method definition.
	 *
	 * @throws MethodCompileException
	 * 		When a variable index is already reserved by a wide variable of the prior slot.
	 * 		Since this is just the parameter step, this should not actually occur.
	 */
	public void visitDefinition(String selfType, MethodDefinition definition) throws MethodCompileException {
		// When the method is not static we need to allocate "this" to slot 0
		if (!AccessFlag.isStatic(definition.getModifiers().value())) {
			addVariableUsage(0, "this", Type.getObjectType(selfType), definition);
		}
	}

	/**
	 * Record the parameter variables.
	 *
	 * @param definition
	 * 		The method definition with parameters to pull from.
	 *
	 * @throws MethodCompileException
	 * 		When a variable index is already reserved by a wide variable of the prior slot.
	 * 		Since this is just the parameter step, this should not actually occur.
	 */
	public void visitParams(MethodDefinition definition) throws MethodCompileException {
		for (MethodParameter parameter : definition.getParams()) {
			addVariableUsage(nextAvailableSlot, parameter.getName(), Type.getType(parameter.getDesc()), parameter);
		}
	}

	/**
	 * Record variable usage in the instructions.
	 *
	 * @param code
	 * 		Instructions container.
	 *
	 * @throws MethodCompileException
	 * 		When the variable type usage is inconsistent/illegal,
	 * 		or when a variable index is already reserved by a wide variable of the prior slot.
	 */
	public void visitCode(Code code) throws MethodCompileException {
		// TODO: Similar to the AST validator, want to eventually change iteration order to be of logical flow
		//       not the linear order of ast nodes.
		for (AbstractInstruction instruction : code.getInstructions()) {
			if (instruction instanceof VariableReference) {
				VariableReference ref = (VariableReference) instruction;
				String identifier = ref.getVariableIdentifier();
				String desc = ref.getVariableDescriptor();
				// Check if variable exists
				VariableInfo info = nameLookup.get(identifier);
				if (info != null) {
					// Check if type is compatible
					//  - Store operation of any type
					//  - Load of compatible type
					if (ref.getVariableOperation() == VariableReference.OpType.ASSIGN) {
						// Assignments can be assumed to be a new scope
						addVariableUsage(info.getIndex(), identifier, Type.getType(desc), instruction);
					} else if (ref.getVariableOperation() == VariableReference.OpType.UPDATE) {
						// Only used by IINC at the moment. So the last used type should be 'int'.
						Type lastUsage = info.getLastUsedType();
						if (instruction.getOpcodeVal() == Opcodes.IINC && lastUsage.getSort() <= Type.INT) {
							addVariableUsage(info.getIndex(), identifier, Type.getType(desc), instruction);
						} else {
							throw new MethodCompileException(instruction, "Tried to update 'int' at '" + identifier +
									"', but type was: " + lastUsage.getDescriptor());
						}
					} else {
						// Loading some value from the variable. Instruction implied type must match last type.
						Type currentUsage = Type.getType(desc);
						Type lastUsage = info.getLastUsedType();
						if (currentUsage.getSort() == lastUsage.getSort()) {
							// Same type
							addVariableUsage(info.getIndex(), identifier, currentUsage, instruction);
						} else if (currentUsage.getSort() <= Type.INT && lastUsage.getSort() <= Type.INT) {
							// Any int sub-type can be used together with other int sub-types.
							addVariableUsage(info.getIndex(), identifier, currentUsage, instruction);
						} else if (currentUsage.getSort() >= Type.ARRAY && lastUsage.getSort() >= Type.ARRAY) {
							// Any object/array type can be used with another
							addVariableUsage(info.getIndex(), identifier, currentUsage, instruction);
						} else {
							String currentTypeName = Types.getSortName(currentUsage.getSort());
							String lastTypeName = Types.getSortName(lastUsage.getSort());
							throw new MethodCompileException(instruction,
									"Incompatible variable type usage [" + info.getIndex() + ":" + info.getName() +
											"] '" + currentTypeName + "' with prior '" +
											lastTypeName + "' value");
						}
					}
				} else {
					// New variable
					addVariableUsage(nextAvailableSlot, identifier, Type.getType(desc), instruction);
				}
			}
		}
	}

	/**
	 * @param index
	 * 		Target variable index.
	 * @param identifier
	 * 		Variable name.
	 * @param type
	 * 		Variable type.
	 * @param source
	 * 		Source element of the usage.
	 *
	 * @throws MethodCompileException
	 * 		When the target index is already reserved by a wide variable of the prior slot.
	 */
	public void addVariableUsage(int index, String identifier, Type type, Element source) throws MethodCompileException {
		// Ensure that the index is not reserved by a prior wide usage
		int wideCheckIndex = index - 1;
		if (wideSlots.contains(wideCheckIndex))
			throw new MethodCompileException(source, "Illegal usage of reserved wide slot!");
		// Get variable info for index and update it
		VariableInfo info = getInfo(index);
		info.addSource(source);
		info.addType(type);
		info.setName(identifier);
		nameLookup.put(identifier, info);
		if (type.getSize() > 1) {
			info.markUsesWide();
			wideSlots.add(index);
		}
		// Update next slot info
		if (currentSlot <= index) {
			currentSlot = index;
			nextAvailableSlot = Math.max(nextAvailableSlot, currentSlot + type.getSize());
		}
	}

	/**
	 * @param index
	 * 		Variable index.
	 *
	 * @return The variable information.
	 */
	public VariableInfo getInfo(int index) {
		return indexLookup.computeIfAbsent(index, VariableInfo::new);
	}

	/**
	 * Maps name to index.
	 *
	 * @param identifier
	 * 		Variable identifier/name.
	 *
	 * @return The variable's index, or {@code -1} if no such mapping exists.
	 */
	public int getIndex(String identifier) {
		VariableInfo info = nameLookup.get(identifier);
		if (info == null) return -1;
		return info.getIndex();
	}

	/**
	 * @return The number of used slots.
	 */
	public int getCurrentUsedCap() {
		return nextAvailableSlot;
	}

	/**
	 * Resets all info.
	 */
	public void clear() {
		indexLookup.clear();
		nameLookup.clear();
		wideSlots.clear();
		nextAvailableSlot = 0;
		currentSlot = 0;
	}

	/**
	 * Variables are added internally based on order of appearance.
	 * Since the internal storage retains insertion order via {@link LinkedHashMap} we can use that ordering here.
	 *
	 * @return Variables in order of their appearance.
	 */
	public Collection<VariableInfo> inAppearanceOrder() {
		return indexLookup.values();
	}

	/**
	 * @return Variables in sorted order of ascending indices, then names.
	 */
	public SortedSet<VariableInfo> inSortedOrder() {
		return new TreeSet<>(indexLookup.values());
	}

	@Override
	public Iterator<VariableInfo> iterator() {
		// Force iteration order of the lowest index, then name by using 'TreeSet'
		// If we use List while the variable map retains insertion order, then order is based on the first-occurrence.
		return inSortedOrder().iterator();
	}
}
