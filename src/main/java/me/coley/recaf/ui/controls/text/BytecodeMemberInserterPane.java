package me.coley.recaf.ui.controls.text;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.parser.DefinitionParser;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ClassVisitorPlugin;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.Log;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Bytecode-focused text editor for inserting new fields/methods.
 *
 * @author Matt
 */
public class BytecodeMemberInserterPane extends BytecodeEditorPane {
	private String lastMemberName;
	private String lastMemberDesc;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param className
	 * 		Name of class containing the method.
	 * @param isMethod
	 * 		Target member type flag.
	 */
	public BytecodeMemberInserterPane(GuiController controller, String className, boolean isMethod) {
		super(controller, className, isMethod ? "methodName" : "fieldName", isMethod ? "()V" : "Ljava/lang/Object;");
	}

	@Override
	public boolean disassemble() {
		if (isMethod) {
			setText(DefinitionParser.DEFINE + " " + memberName + memberDesc +"\n" +
					"START:\n" +
					"// Method code here\n" +
					"RETURN\n" +
					"END:\n");
		} else {
			setText(DefinitionParser.DEFINE + " " + memberDesc + " " + memberName);
		}
		forgetHistory();
		return true;
	}

	@Override
	public byte[] assemble() {
		if((isMethod && currentMethod == null) || (!isMethod && currentField == null)) {
			// Skip of not saved
			return null;
		}
		// Don't use the final member name/desc, use whatever has been assembled
		String newMemberName = null;
		String newMemberDesc = null;
		if (isMethod) {
			newMemberName = currentMethod.name;
			newMemberDesc = currentMethod.desc;
		} else {
			newMemberName = currentField.name;
			newMemberDesc = currentField.desc;
		}
		// Check if user changed the name
		ClassReader cr  = controller.getWorkspace().getClassReader(className);
		ClassNode existingNode = ClassUtil.getNode(cr, ClassReader.EXPAND_FRAMES);
		removeIfRenamed(newMemberName, newMemberDesc, existingNode);
		// Update last used name
		lastMemberName = newMemberName;
		lastMemberDesc = newMemberDesc;
		updateOrInsert(newMemberName, newMemberDesc, existingNode);
		// Compile changes
		ClassWriter cw = controller.getWorkspace().createWriter(ClassWriter.COMPUTE_FRAMES);
		ClassVisitor visitor = cw;
		for (ClassVisitorPlugin visitorPlugin : PluginsManager.getInstance()
				.ofType(ClassVisitorPlugin.class)) {
			visitor = visitorPlugin.intercept(visitor);
		}
		existingNode.accept(visitor);
		return cw.toByteArray();
	}

	private void removeIfRenamed(String newMemberName, String newMemberDesc, ClassNode existingNode) {
		if ((lastMemberName != null && !lastMemberName.equals(newMemberName)) ||
				(lastMemberDesc != null && !lastMemberDesc.equals(newMemberDesc))) {
			// Remove the old member
			Log.debug("User changed member definition name or desc when inserting a new member");
			if (isMethod) {
				for(int i = 0; i < existingNode.methods.size(); i++) {
					MethodNode existingMethod = existingNode.methods.get(i);
					if(existingMethod.name.equals(lastMemberName) && existingMethod.desc.equals(lastMemberDesc)) {
						existingNode.methods.remove(i);
						break;
					}
				}
			} else {
				for(int i = 0; i < existingNode.fields.size(); i++) {
					FieldNode existingField = existingNode.fields.get(i);
					if(existingField.name.equals(lastMemberName) && existingField.desc.equals(lastMemberDesc)) {
						existingNode.fields.remove(i);
						break;
					}
				}
			}
		}
	}

	private void updateOrInsert(String newMemberName, String newMemberDesc, ClassNode existingNode) {
		boolean found = false;
		if (isMethod) {
			// Overwrite if its been added and we're making an change
			for(int i = 0; i < existingNode.methods.size(); i++) {
				MethodNode existingMethod = existingNode.methods.get(i);
				if(existingMethod.name.equals(newMemberName) && existingMethod.desc.equals(newMemberDesc)) {
					ClassUtil.copyMethodMetadata(existingMethod, currentMethod);
					existingNode.methods.set(i, currentMethod);
					found = true;
					break;
				}
			}
			// Add if no method match
			if(!found) {
				existingNode.methods.add(currentMethod);
			}
		} else {
			// Overwrite if its been added and we're making an change
			for(int i = 0; i < existingNode.fields.size(); i++) {
				FieldNode existingField = existingNode.fields.get(i);
				if(existingField.name.equals(newMemberName) && existingField.desc.equals(newMemberDesc)) {
					ClassUtil.copyFieldMetadata(currentField, existingField);
					existingNode.fields.set(i, currentField);
					found = true;
					break;
				}
			}
			// Add if no field match
			if(!found) {
				existingNode.fields.add(currentField);
			}
		}
	}
}
