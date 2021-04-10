package me.coley.recaf.mapping;

import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ClassVisitorPlugin;
import me.coley.recaf.workspace.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Base for mapppings.
 *
 * @author Matt
 */
public class Mappings {
	private Map<String, String> mappings;
	private Map<String, String> reverseClassMappings;
	private Workspace workspace;
	private boolean checkFieldHierarchy;
	private boolean checkMethodHierarchy;
	private boolean checkWonkyOuterRelation;
	private boolean clearDebugInfo;

	/**
	 * @param workspace
	 * 		Workspace to pull names from when using hierarchy lookups.
	 */
	public Mappings(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * See the
	 * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)} docs for more
	 * information.
	 *
	 * @return ASM formatted mappings.
	 */
	public Map<String, String> getMappings() {
		return mappings;
	}

	/**
	 * Set the {@link #getMappings() mappings}.
	 * @param mappings Mappings to use.
	 */
	public void setMappings(Map<String, String> mappings) {
		this.mappings = mappings;
		// Save inverted class name mappings for class-writing (requires ancestor analysis)
		// - Allows us to not have to recompile in ancestral order
		reverseClassMappings = mappings.entrySet()
				.stream()
				.filter(e -> !e.getKey().contains("."))
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	}

	/**
	 * The inverted mappings of {@link #getMappings()}.
	 *
	 * @return Inverted ASM formatted mappings.
	 */
	public Map<String, String> getReverseClassMappings() {
		return reverseClassMappings;
	}

	/**
	 * In some cases automated mappings can reference fields in super-classes but specify the
	 * implementing class as the field's owner. Enabling this flag will allow the mapper to check
	 * against super-classes when mapping these cases.
	 *
	 * @return Flag for if parent classes should be checked for containing fields.
	 */
	public boolean doCheckFieldHierarchy() {
		return checkFieldHierarchy;
	}

	/**
	 * In many cases automated mappings can reference methods declared in super-classes but
	 * specify the implementing class as the method's owner. Enabling this flag will allow the
	 * mapper to check against super-classes when mapping these cases.
	 *
	 * @return Flag for if parent classes should be checked for containing methods.
	 */
	public boolean doCheckMethodHierarchy() {
		return checkMethodHierarchy;
	}

	/**
	 * In some obfuscated classes, an inner class can have a name that shows no relationship to the outer class.
	 * When such classes still contain a reference to the outer class in the {@code InnerClassesAttribute} but
	 * no useful data in the {@link org.objectweb.asm.tree.ClassNode#outerClass}, we can sometimes use the inner
	 * class attribute to resolve what the true outer class name is.
	 *
	 * @return Flag for if outer class resolving should account for wonky renaming.
	 */
	public boolean doCheckWonkyOuterRelation() {
		return checkWonkyOuterRelation;
	}

	/**
	 * @param checkFieldHierarchy Flag for if parent classes should be checked for containing fields.
	 */
	public void setCheckFieldHierarchy(boolean checkFieldHierarchy) {
		this.checkFieldHierarchy = checkFieldHierarchy;
	}

	/**
	 * @param checkMethodHierarchy Flag for if parent classes should be checked for containing methods.
	 */
	public void setCheckMethodHierarchy(boolean checkMethodHierarchy) {
		this.checkMethodHierarchy = checkMethodHierarchy;
	}

	/**
	 * @param checkWonkyOuterRelation Flag for if outer class resolving should account for wonky renaming.
	 */
	public void setCheckWonkyOuterRelation(boolean checkWonkyOuterRelation) {
		this.checkWonkyOuterRelation = checkWonkyOuterRelation;
	}

	/**
	 * Useful for clearing intentionally bad debug info like bad variable names &amp; signatures.
	 *
	 * @return Flag for removing debug information.
	 */
	public boolean doClearDebugInfo() {
		return clearDebugInfo;
	}

	/**
	 * @param clearDebugInfo
	 * 		Flag for removing debug information.
	 */
	public void setClearDebugInfo(boolean clearDebugInfo) {
		this.clearDebugInfo = clearDebugInfo;
	}

	/**
	 * Applies mappings to all classes in the given resource. Return value is the map of updated
	 * classes.
	 *
	 * @param resource
	 * 		Resource containing classes.
	 *
	 * @return Map of updated classes. Keys of the old names, values of the updated code.
	 */
	public Map<String, byte[]> accept(JavaResource resource) {
		// Collect: <OldName, NewBytecode>
		Map<String, byte[]> updated = new HashMap<>();
		for(Map.Entry<String, byte[]> e : resource.getClasses().entrySet()) {
			byte[] old = e.getValue();
			ClassReader cr = new ClassReader(old);
			accept(updated, cr);
		}
		// Update the resource's classes map
		for(Map.Entry<String, byte[]> e : updated.entrySet()) {
			String oldKey = e.getKey();
			String newKey = new ClassReader(e.getValue()).getClassName();
			if (!oldKey.equals(newKey))
				resource.getClasses().remove(oldKey);
			resource.getClasses().put(newKey, e.getValue());
		}
		// Tell the workspace we've finished renaming classes
		workspace.onPrimaryDefinitionChanges(updated.keySet());
		// Update hierarchy graph
		workspace.getHierarchyGraph().refresh();
		// Update saved mappings
		workspace.updateAggregateMappings(getMappings(), updated.keySet());
		return updated;
	}

	/**
	 * Applies mappings to the given class and puts the modified bytecode in the map.
	 *
	 * @param updated
	 * 		Map to collect updated values in.
	 * @param cr
	 * 		Class bytecode reader.
	 */
	private void accept(Map<String, byte[]> updated, ClassReader cr) {
		try {
			accept(updated, cr, ClassReader.SKIP_FRAMES, ClassWriter.COMPUTE_FRAMES);
		} catch(IllegalArgumentException ex) {
			// ASM throws: "JSR/RET are not supported with computeFrames option"
			if (ex.getMessage() != null && ex.getMessage().contains("JSR/RET")) {
				accept(updated, cr, ClassReader.EXPAND_FRAMES, ClassWriter.COMPUTE_MAXS);
			}
		}
	}

	private void accept(Map<String, byte[]> updated, ClassReader cr, int readFlags, int writeFlags) {
		String name = cr.getClassName();
		// Apply with mapper
		SimpleRecordingRemapper mapper = new SimpleRecordingRemapper(getMappings(),
				checkFieldHierarchy, checkMethodHierarchy, checkWonkyOuterRelation, workspace);
		WorkspaceClassWriter cw = workspace.createWriter(writeFlags);
		cw.setMappings(getMappings(), reverseClassMappings);
		ClassVisitor visitor = cw;
		for (ClassVisitorPlugin visitorPlugin : PluginsManager.getInstance()
				.ofType(ClassVisitorPlugin.class)) {
			visitor = visitorPlugin.intercept(visitor);
		}
		ClassRemapper adapter = new LenientClassRemapper(visitor, mapper);
		if (clearDebugInfo)
			readFlags |= ClassReader.SKIP_DEBUG;
		cr.accept(adapter, readFlags);
		// Only return the modified class if any references to the mappings were found.
		if (mapper.isDirty())
			updated.put(name, cw.toByteArray());
	}
}
