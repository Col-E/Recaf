package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.benf.cfr.reader.PluginRunner;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.Recaf;
import me.coley.recaf.cfr.CFRResourceLookup;
import me.coley.recaf.cfr.CFRSetting;
import me.coley.recaf.cfr.CFRSourceImpl;

@SuppressWarnings("serial")
public class DecompilePanel extends JPanel {
	private final RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
	private final RTextScrollPane scrollText = new RTextScrollPane(textArea);
	private final ClassNode classNode;
	private final MethodNode methodNode;
	private final CFRResourceLookup lookupHelper;

	public DecompilePanel(ClassNode cn) {
		this(cn, null);
	}

	public DecompilePanel(ClassNode cn, MethodNode mn) {
		this.classNode = cn;
		this.methodNode = mn;
		if (mn != null) {
			this.lookupHelper = new CFRResourceLookup(Recaf.getInstance(), getIsolatedMethodClass());
		} else {
			this.lookupHelper = new CFRResourceLookup(Recaf.getInstance());
		}
		//
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(false);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setComponentPopupMenu(null);
		textArea.setPopupMenu(null);
		//
		setLayout(new BorderLayout());
		add(scrollText, BorderLayout.CENTER);
		//
		decompile();
	}

	/**
	 * Returns a title for the containing parent to access (Groupbox /
	 * InternalWindow).
	 *
	 * @return The title.
	 */
	public String getTitle() {
		String s = "CFR: " + classNode.name;
		if (methodNode != null) {
			s += "#" + methodNode.name + methodNode.desc;
		}
		return s;
	}

	private ClassNode getIsolatedMethodClass() {
		ClassNode copy = new ClassNode();
		copy.visit(classNode.version, classNode.access, classNode.name, classNode.signature, classNode.superName,
				   classNode.interfaces.stream().toArray(String[]::new));
		// I initially though sharing method nodes would be bad,
		// but copying it is even more of a pain.
		copy.methods.add(methodNode);
		return copy;
	}

	/**
	 * Decompiled the class.
	 */
	private void decompile() {
		String name = classNode.name;
		String text = new PluginRunner(CFRSetting.toStringMap(), new CFRSourceImpl(lookupHelper)).getDecompilationFor(name);
		// Hack to substring the first indent (Where the isolated method begins)
		// to the end of the class, minus one (so it substrings to the method's
		// closing brace)
		if (methodNode != null) {
			text = text.substring(text.indexOf("    "), text.lastIndexOf("}") - 1);
		}
		textArea.setText(text);
		textArea.moveCaretPosition(0);
	}
}
