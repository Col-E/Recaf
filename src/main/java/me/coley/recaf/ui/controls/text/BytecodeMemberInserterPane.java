package me.coley.recaf.ui.controls.text;

import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.parse.bytecode.parser.DefinitionParser;

/**
 * Bytecode-focused text editor for inserting new fields/methods.
 *
 * @author Matt
 */
public class BytecodeMemberInserterPane extends BytecodeEditorPane {
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
}
