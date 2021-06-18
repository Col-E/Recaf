package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.DefinitionArgAST;
import me.coley.recaf.parse.bytecode.ast.LabelAST;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.parse.bytecode.ast.VariableReference;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;

import java.util.*;

/**
 * Variable name/index handling.
 *
 * @author Matt
 */
public class VariableNameCache {
	private final Map<String, Integer> nameToIndex = new HashMap<>();
	private final Set<Integer> usedRawIndices = new HashSet<>();
	private final boolean isStatic;
	private int next;
	private int maxIndex;

	/**
	 * @param isStatic
	 * 		Is the method static or not.
	 * @param currentType
	 * 		Internal name of declaring class.
	 */
	VariableNameCache(boolean isStatic, String currentType) {
		// Add "this" for instance methods
		this.isStatic = isStatic;
		if (!isStatic) {
			nameToIndex.put("this", 0);
			nameToIndex.put("0", 0);
			setNext(next + 1);
		}
	}

	/**
	 * @param root
	 * 		AST root.
	 *
	 * @throws AssemblerException
	 * 		When fetching type-information from an instruction fails.<br>
	 * 		Or when variables cannot fetch label information.
	 */
	void visit(RootAST root) throws AssemblerException {
		// Method descriptor
		// - contains explicit types & names
		// - highest priority due to being part of the method definition
		int argNext = isStatic ? 0 : 1;
		List<LabelAST> labels = root.search(LabelAST.class);
		for (DefinitionArgAST arg : root.search(DefinitionArgAST.class)) {
			String name = arg.getVariableName().getName();
			Type type = Type.getType(arg.getDesc().getDesc());
			// Get index from name
			int index = -1;
			if (name.matches("\\d+"))
				index = Integer.parseInt(name);
			else
				index = nameToIndex.getOrDefault(name, argNext);
			// Update for next arg index
			argNext += type.getSize();
			// Populate
			nameToIndex.put(String.valueOf(index), index);
			nameToIndex.put(name, index);
			// Update next index
			setNext(index + getNextVarIncrement(index, type.getSize()));
		}
		// Update next to be the minimum index following the last argument index
		setNext(argNext);
		// Add data for raw-indexed variables
		// We must track what indices they use so named variables do not conflict.
		for (VariableReference ast : root.search(VariableReference.class)) {
			String name = ast.getVariableName().getName();
			if (name.matches("\\d+")) {
				int index = Integer.parseInt(name);
				int size = TypeUtil.sortToSize(ast.getVariableSort());
				nameToIndex.put(name, index);
				usedRawIndices.add(index);
				// Doubles/longs take two spaces
				if (size > 1)
					usedRawIndices.add(index + 1);
			}
		}
		// Fit next into the next index that is not used by a raw-indexed variable
		next += getNextVarIncrement(next, 0);
		// Add data for named variables
		for (VariableReference ast : root.search(VariableReference.class)) {
			String name = ast.getVariableName().getName();
			// Skip raw-index variables
			if (name.matches("\\d+"))
				continue;
			// Add index mapping if it does not exist
			if (!nameToIndex.containsKey(name)) {
				int index = next;
				int size = TypeUtil.sortToSize(ast.getVariableSort());
				nameToIndex.put(name, index);
				setNext(index + getNextVarIncrement(index, size));
				// Track used indices
				usedRawIndices.add(index);
				if (size > 1)
					usedRawIndices.add(index + 1);
			}
		}
	}

	/**
	 * Adds a variable to the cache.
	 *
	 * @param name
	 * 		Variable name.
	 * @param type
	 * 		Variable type.
	 *
	 * @return Assigned index of the new variable.
	 */
	public int getAndIncrementNext(String name, Type type) {
		int size = type.getSize();
		int ret = getNextFreeVar(next, size);
		// Update used indices
		usedRawIndices.add(ret);
		if (size > 1)
			usedRawIndices.add(ret + 1);
		nameToIndex.put(name, ret);
		// Update next and max values
		next = getNextFreeVar(next, 1);
		maxIndex = next;
		return ret;
	}

	/**
	 * Finds the next free variable.
	 *
	 * @param start
	 * 		Starting position.
	 * @param size
	 * 		Size of variable to check for free space.
	 *
	 * @return Next free index.
	 */
	public int getNextFreeVar(int start, int size) {
		int temp = start;
		if (size == 1)
			while (isIndexUsed(temp))
				temp++;
		else
			while (isIndexUsed(temp) && isIndexUsed(temp + 1))
				temp++;
		return temp;
	}

	/**
	 * Finds the increment needed to fit the next variable slot. Will skip already used values.
	 *
	 * @param current
	 * 		Current variable index, without increment.
	 * @param size
	 * 		Size of variable just discovered.
	 *
	 * @return Variable increment amount.
	 */
	private int getNextVarIncrement(int current, int size) {
		int temp = current + size;
		while (isIndexUsed(temp))
			temp++;
		return temp - current;
	}

	/**
	 * @param index
	 * 		Index to check.
	 *
	 * @return {@code true} if the index is used.
	 */
	public boolean isIndexUsed(int index) {
		return usedRawIndices.contains(index) || nameToIndex.containsValue(index);
	}

	/**
	 * @param name
	 * 		Variable name.
	 *
	 * @return Index.
	 *
	 * @throws AssemblerException
	 * 		When index lookup fails.
	 */
	public int getIndex(String name) throws AssemblerException {
		try {
			return nameToIndex.get(name);
		} catch (Exception ex) {
			throw new AssemblerException("Failed to fetch index of: " + name);
		}
	}

	/**
	 * @return Max used locals.
	 */
	public int getMax() {
		// We offset by one because this needs to be 1-based, not 0-based in the contexts
		// that this getter method is used in.
		int value = maxIndex + 1;
		// Check if a reserved index occupies a higher space
		OptionalInt maxUsed = usedRawIndices.stream().mapToInt(i -> i).max();
		if (maxUsed.isPresent()) {
			int maxUsedInt = maxUsed.getAsInt() + 1;
			if (value < maxUsedInt)
				value = maxUsedInt;
		}
		return value;
	}

	/**
	 * @param variables
	 * 		Map of default indices to use for variable names.
	 */
	public void populateDefaults(Collection<LocalVariableNode> variables) {
		variables.forEach(variable -> {
			Type type;
			try {
				type = Type.getType(variable.desc);
			} catch (Exception ex) {
				// Sometimes obfuscators will put in garbage into the local variable debug info.
				// ASM doesn't like that, so we will just skip the variable if it has bogus info.
				return;
			}
			// Populate
			String name = variable.name;
			int index = variable.index;
			nameToIndex.put(name, index);
			// Update next index
			setNext(index + getNextVarIncrement(index, type.getSize()));
		});
	}

	// Internal use only
	Map<String, Integer> getNameToIndex() {
		return nameToIndex;
	}

	private void setNext(int next) {
		this.next = next;
		if (next > maxIndex)
			maxIndex = next;
	}
}
