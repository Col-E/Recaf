package me.coley.recaf.ui.component.panel;

import java.awt.BorderLayout;
import java.util.Map;

import javax.swing.JPanel;

import org.benf.cfr.reader.PluginRunner;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.mdkt.compiler.CompiledCode;
import org.mdkt.compiler.DynamicClassLoader;
import org.mdkt.compiler.InMemoryJavaCompiler;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.cfr.CFRResourceLookup;
import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Asm;
import me.coley.recaf.cfr.CFRParameter;
import me.coley.recaf.cfr.CFRSourceImpl;
import me.coley.recaf.ui.Lang;
import me.coley.recaf.ui.component.action.ActionMenuItem;
import me.coley.recaf.util.Reflect;

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
			this.lookupHelper = new CFRResourceLookup(getIsolatedMethodClass());
		} else {
			this.lookupHelper = new CFRResourceLookup();
		}
		//
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setComponentPopupMenu(null);
		// textArea.setPopupMenu(menu);
		textArea.getPopupMenu().add(new ActionMenuItem("Recompile", () -> {
			recompile();
		}));
		//
		setLayout(new BorderLayout());
		add(scrollText, BorderLayout.CENTER);
		//
		decompile();
	}

	private void recompile() {
		try {
			String name = classNode.name.replace("/", ".");
			Class<?> clazz = InMemoryJavaCompiler.newInstance().compile(name, textArea.getText());
			DynamicClassLoader loader = ((DynamicClassLoader) clazz.getClassLoader());
			Map<String, CompiledCode> code = Reflect.get(loader, "customCompiledCode");
			if (code == null) {
				Recaf.INSTANCE.logging.error("Could not recompile, could not fetch compiled code.");
				return;
			}
			CompiledCode compiled = code.get(name);
			if (compiled == null) {
				Recaf.INSTANCE.logging.error("Could not recompile, mismatched class names.");
				return;
			}
			ClassNode newValue = Asm.getNode(compiled.getByteCode());
			Recaf.INSTANCE.jarData.classes.put(classNode.name, newValue);
			Recaf.INSTANCE.logging.info("Recompiled '" + classNode.name + "' - size:" + compiled.getByteCode().length, 1);			
			Recaf.INSTANCE.ui.setTempTile(Lang.get("window.compile.msg"), 3000);
		} catch (Exception e) {
			Recaf.INSTANCE.logging.error(e);
		}
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

	/**
	 * Isolates the {@link #methodNode} in a wrapper class-node.
	 * 
	 * @return New ClassNode containing only {@link #methodNode}.
	 */
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
		String text = new PluginRunner(CFRParameter.toStringMap(), new CFRSourceImpl(lookupHelper)).getDecompilationFor(name);
		// Hack to substring the first indent (Where the isolated method begins)
		// to the end of the class, minus one (so it substrings to the method's
		// closing brace)
		if (methodNode != null) {
			text = text.substring(text.indexOf("    "), text.lastIndexOf("}") - 1);
		}
		textArea.setText(text);
		textArea.moveCaretPosition(0);
		textArea.setCaretPosition(0);
	}
}