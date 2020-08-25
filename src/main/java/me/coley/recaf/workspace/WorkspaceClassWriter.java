package me.coley.recaf.workspace;

import org.objectweb.asm.ClassWriter;

import java.util.Map;

/**
 * Writer that uses workspace hierarchy to compute common parents for frame generation.
 *
 * @author Matt
 */
public class WorkspaceClassWriter extends ClassWriter {
	private final Workspace workspace;
	private Map<String, String> mappings;
	private Map<String, String> reverseMappings;

	/**
	 * @param workspace
	 * 		Workspace to use for hierarchy lookups.
	 * @param flags
	 * 		Writer flags.
	 */
	WorkspaceClassWriter(Workspace workspace, int flags) {
		super(flags);
		this.workspace = workspace;
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) throws TypeNotPresentException {
		// Default assumption if a type isn't given
		if(type1 == null || type2 == null)
			return "java/lang/Object";
		// Apply mappings if they exist
		if (mappings != null) {
			// We're likely looking at bytecode that is not reflected in the workspace yet.
			// So we have to "undo" the mappings for the given types.
			type1 = reverseMappings.getOrDefault(type1, type1);
			type2 = reverseMappings.getOrDefault(type2, type2);
		}
		// Find common parent in workspace
		String common = workspace.getHierarchyGraph().getCommon(type1, type2);
		if (common != null && !common.equals("java/lang/Object")) {
			// Assuming we have mappings we want to make sure the common name is using the mapped name.
			if (mappings != null)
				common = mappings.getOrDefault(common, common);
			return common;
		}
		// Fallback: Use base common parent lookup
		try {
			return super.getCommonSuperClass(type1, type2);
		} catch(Throwable ex) {
			return "java/lang/Object";
		}
	}

	/**
	 * When remapping classes, they're not in the workspace. So providing the
	 * <i>base-to-renamed</i> mappings will allow us to do a lookup.
	 *
	 * @param mappings
	 * 		<i>base-to-renamed</i> map.
	 * @param reverseMappings
	 * 		<i>renamed-to-base</i> map.
	 */
	public void setMappings(Map<String, String> mappings, Map<String, String> reverseMappings) {
		this.mappings = mappings;
		this.reverseMappings = reverseMappings;
	}
}
