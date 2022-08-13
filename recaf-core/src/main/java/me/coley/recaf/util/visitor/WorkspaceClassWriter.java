package me.coley.recaf.util.visitor;

import me.coley.recaf.Controller;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Map;

/**
 * Writer that uses workspace class inheritance graph to compute common parents for frame generation.
 *
 * @author Matt Coley
 */
public class WorkspaceClassWriter extends ClassWriter {
	private final Controller controller;
	private Map<String, String> mappings;
	private Map<String, String> reverseMappings;

	/**
	 * @param controller
	 * 		Controller to pull workspace inheritance graph from.
	 * @param cr
	 * 		Reader used to read the original class data.
	 * @param flags
	 * 		Writer flags.
	 */
	public WorkspaceClassWriter(Controller controller, ClassReader cr, int flags) {
		super(cr, flags);
		this.controller = controller;
	}

	/**
	 * @param controller
	 * 		Controller to pull workspace inheritance graph from.
	 * @param flags
	 * 		Writer flags.
	 */
	public WorkspaceClassWriter(Controller controller, int flags) {
		super(flags);
		this.controller = controller;
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) throws TypeNotPresentException {
		// Default assumption if a type isn't given.
		if (type1 == null || type2 == null)
			return "java/lang/Object";
		// Apply mappings if they exist.
		if (reverseMappings != null) {
			// We're likely looking at bytecode that is not reflected in the workspace yet.
			// So we have to "undo" the mappings for the given types.
			type1 = reverseMappings.getOrDefault(type1, type1);
			type2 = reverseMappings.getOrDefault(type2, type2);
		}
		// Find common parent in workspace
		String common = controller.getServices().getInheritanceGraph().getCommon(type1, type2);
		if (common != null && !common.equals("java/lang/Object")) {
			// Assuming we have mappings we want to make sure the common name is using the mapped name.
			if (mappings != null)
				common = mappings.getOrDefault(common, common);
			return common;
		}
		// Fallback: Use base common parent lookup.
		try {
			return super.getCommonSuperClass(type1, type2);
		} catch (Throwable ex) {
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
		// TODO: This isn't called and should be.
		//  - check how aggregate mappings reverse mappings to generate proper reverse mappings
		this.mappings = mappings;
		this.reverseMappings = reverseMappings;
	}
}
