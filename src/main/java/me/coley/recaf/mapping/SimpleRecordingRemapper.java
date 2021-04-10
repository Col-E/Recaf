package me.coley.recaf.mapping;

import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An extension of the SimpleRemapper that logs if a class has been modified in the renaming
 * process.
 *
 * @author Matt
 */
public class SimpleRecordingRemapper extends SimpleRemapper {
	private final boolean checkFieldHierarchy;
	private final boolean checkMethodHierarchy;
	private final boolean checkWonkyOuterRelation;
	private final Workspace workspace;
	private boolean dirty;

	/**
	 * Constructs a recording remapper.
	 *
	 * @param mapping
	 * 		Map of asm styled mappings. See
	 *        {@link SimpleRemapper#SimpleRemapper(Map)}.
	 * @param checkFieldHierarchy
	 * 		Flag for checking for field keys using super-classes.
	 * @param checkMethodHierarchy
	 * 		Flag for checking for method keys using super-classes.
	 * @param checkWonkyOuterRelation
	 * 		Flag for if outer class resolving should account for wonky renaming.
	 * @param workspace
	 * 		Workspace to pull names from when using hierarchy lookups.
	 */
	public SimpleRecordingRemapper(Map<String, String> mapping, boolean checkFieldHierarchy,
								   boolean checkMethodHierarchy, boolean checkWonkyOuterRelation,
								   Workspace workspace) {
		super(mapping);
		this.checkFieldHierarchy = checkFieldHierarchy;
		this.checkMethodHierarchy = checkMethodHierarchy;
		this.checkWonkyOuterRelation = checkWonkyOuterRelation;
		this.workspace = workspace;
	}

	/**
	 * If a class contains no references to anything in the mappings there will be no reason to
	 * update it within Recaf, so we record if any changes were made. If no changes are made we
	 * can disregard the remapped output.
	 *
	 * @return {@code true} if the class has been modified in the remapping process.
	 */
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public String mapFieldName(String owner, String name, String descriptor) {
		// Standard format
		String remappedName = map(owner + '.' + name);
		// Check if we are also using descriptors in keys, in cases where name overloading occurs
		if (remappedName == null)
			remappedName = map(owner + '.' + name + ' ' + descriptor);
		return remappedName == null ? name : remappedName;
	}

	@Override
	public String map(final String key) {
		// Don't map constructors/static-initializers
		if (key.contains("<"))
			return null;
		// Get mapped value from key
		String mapped = super.map(key);
		// No direct key mapping found?
		if (mapped == null) {
			boolean member = key.contains(".");
			// Check if the key indicates if the value is a member (field/method)
			if (member) {
				// Check if the key indicates an invoke-dynamic call
				// Don't do any parent checking if its an invoke-dynamic.
				if (key.charAt(0) == '.')
					return null;
				// No direct mapping for this member is found, perhaps it was mapped in a super-class
				boolean method = key.contains("(");
				String memberDef = key.substring(key.indexOf(".") + 1);
				// Normally we would check the whole hierarchy, but the mappings should at least be
				// correct enough for this to work.
				if ((!method && checkFieldHierarchy) || (method && checkMethodHierarchy)) {
					for (String parent : getParents(key)) {
						// Attempt to map with parent name
						mapped = map(parent + "." + memberDef);
						// If found, break so we can return the discovered mapping.
						if (mapped != null)
							break;
					}
				}
			} else {
				// Not a member, so this is a class definition.
				// Is this an inner class? If so ensure the qualified outer name is mapped
				int index = key.lastIndexOf("$");
				if(index > 1) {
					// key is an inner class
					String outer = key.substring(0, index);
					String inner = key.substring(index);
					String mappedOuter = map(outer);
					if(mappedOuter != null)
						return mappedOuter + inner;
				} else if (checkWonkyOuterRelation && workspace.getPrimary().getClasses().containsKey(key)){
					// Check if the class is just obfuscated and does not respect the "outer$inner" pattern.
					String outer = getUnmatchedOuter(key);
					if (outer != null) {
						// key is an inner class
						String inner = key.substring(key.lastIndexOf('/') + 1);
						String mappedOuter = map(outer);
						if (mappedOuter != null)
							return mappedOuter + inner;
					}
				}
			}
		}
		// Mark as dirty if mappings found
		if(mapped != null)
			dirty = true;
		return mapped;
	}

	/**
	 * Sometimes obfuscators rename inner classes and do not retain the {@code outer$inner} pattern.
	 * So we need to check for that here.
	 *
	 * @param name Class name to check for outers.
	 * @return Name of outer class or {@code null} if no outer exists.
	 */
	private String getUnmatchedOuter(String name) {
		ClassReader cr = workspace.getClassReader(name);
		if (cr == null)
			return null;
		ClassNode node = ClassUtil.getNode(cr, ClassReader.SKIP_CODE);
		// Check for outer name attr
		if (node.outerClass != null && !node.name.equals(node.outerClass))
			return node.outerClass;
		// Check if internal name of inner matches,
		// then use outer name if its not exactly the same as the given name.
		for (InnerClassNode inner : node.innerClasses) {
			if (inner.name.equals(name) && inner.outerName != null && !inner.outerName.equals(name))
				return inner.outerName;
		}
		return null;
	}

	/**
	 * @param key
	 * 		Mapping key.
	 *
	 * @return Stream of super classes.
	 */
	private Set<String> getParents(String key) {
		// Get class from key
		String className = key.contains(".") ? key.substring(0, key.indexOf(".")) : key;
		// Get parents in hierarchy
		return workspace.getHierarchyGraph().getParents(className)
				.collect(Collectors.toSet());
	}
}
