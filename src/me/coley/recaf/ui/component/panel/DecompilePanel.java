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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import me.coley.recaf.cfr.CFRResourceLookup;
import me.coley.recaf.Recaf;
import me.coley.recaf.asm.Access;
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
	// Items being decompiled
	private final ClassNode classNode;
	private final MethodNode methodNode;
	/**
	 * Util to help CFR find references.
	 */
	private final CFRResourceLookup lookupHelper;
	/**
	 * Last decompilation text.
	 */
	private String fullText;

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
		boolean canEdit = !Access.isEnum(cn.access);
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(canEdit);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setComponentPopupMenu(null);
		if (canEdit) {
			textArea.getPopupMenu().add(new ActionMenuItem("Recompile", () -> recompile()));
		}
		//
		setLayout(new BorderLayout());
		add(scrollText, BorderLayout.CENTER);
		//
		decompile();
	}

	/**
	 * Uses the decompiled code to recompile.
	 */
	private void recompile() {
		try {
			String name = classNode.name.replace("/", ".");
			// TODO: For dependencies in agent-mode the jar/classes should be
			// fetched from the class-path.
			InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
			compiler.ignoreWarnings();
			compiler.useOptions("-cp", Recaf.INSTANCE.jarData.jar.getAbsolutePath());
			String srcText = textArea.getText();
			if (methodNode != null) {
				// Add remaining text for src to be proper class instead of
				// single method floating out in space.
				// Really hacky, but should work for everything that isn't an
				// enum type.
				//
				// Worked on basic interfaces w/ default methods and typical
				// classes.
				//
				// Can't exactly extend an enum so not sure how that'll be
				// addressed unless I do have it depend on being a complete
				// decompilation.
				srcText = addExtra(srcText);
				name = srcText.substring(2, srcText.indexOf(";"));
			}
			Class<?> clazz = null;
			try {
				clazz = compiler.compile(name, srcText);
			} catch (NullPointerException e) {
				Recaf.INSTANCE.logging.error("Could not recompile, user attempted recompile from JRE process (not JDK).");
				Recaf.INSTANCE.logging.error(new RuntimeException(Lang.get("window.compile.failjdk")));
				return;
			}
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
			if (methodNode == null) {
				Recaf.INSTANCE.jarData.classes.put(classNode.name, newValue);
				Recaf.INSTANCE.logging.info("Recompiled '" + classNode.name + "' - size:" + compiled.getByteCode().length, 1);
				Recaf.INSTANCE.ui.setTempTile(Lang.get("window.compile.full.msg"), 3000);
			} else {
				MethodNode mn = getMethod(newValue, methodNode.name);
				methodNode.instructions.clear();
				methodNode.instructions.add(mn.instructions);
				Recaf.INSTANCE.logging.info("Recompiled '" + methodNode.name + "' - size:" + compiled.getByteCode().length, 1);
				Recaf.INSTANCE.ui.setTempTile(Lang.get("window.compile.single.msg"), 3000);
			}

		} catch (Exception e) {
			Recaf.INSTANCE.logging.error(e);
		}
	}
	
	/**
	 * Really ugly hack for adding constructor to the extended class. Bypasses
	 * dependency on having all other methods in the class decompiling
	 * correctly.
	 * 
	 * @param srcText
	 * @return
	 */
	private String addExtra(String srcText) {
		String name = classNode.name.replace("/", ".");
		String imports = "";
		int impStart = this.fullText.indexOf("import ");
		int impLast = this.fullText.lastIndexOf("import ");
		if (impStart != -1) {
			int impEnd = this.fullText.indexOf(";", impLast);
			imports = this.fullText.substring(impStart, impEnd + 1);
		}
		String nameSub = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : name;
		String newName = nameSub + "_" + methodNode.name;
		String clzType = Access.isInterface(classNode.access) ? "interface " : "abstract class ";
		String clz = clzType + newName + " extends " + name;
		StringBuilder constructor = new StringBuilder();

		MethodNode con = getMethod(classNode, "<init>");
		if (con != null) {
			constructor.append("\tpublic " + newName + "(");
			Type conT = Type.getType(con.desc);
			int i = 0;
			for (Type arg : conT.getArgumentTypes()) {
				constructor.append(arg.getClassName() + " arg" + (i++) + ",");
			}
			if (constructor.toString().endsWith(",")) {
				constructor.deleteCharAt(constructor.length() - 1);
			}
			constructor.append(") {\n\t\tsuper(");
			for (Type arg : conT.getArgumentTypes()) {
				if (arg.getDescriptor().length() == 1) {
					constructor.append("0,");
				} else {
					constructor.append("null,");
				}
			}
			if (constructor.toString().endsWith(",")) {
				constructor.deleteCharAt(constructor.length() - 1);
			}
			constructor.append(");\n\t}");
		}

		return "//" + newName + ";\n" + imports + "\n" + clz + " {\n" + constructor + "\n" + srcText + "\n}";
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
		fullText = text;
		if (methodNode != null) {
			text = text.substring(text.indexOf("    "), text.lastIndexOf("}") - 1);
		}
		textArea.setText(text);
		textArea.moveCaretPosition(0);
		textArea.setCaretPosition(0);
	}

	private static MethodNode getMethod(ClassNode cn, String name) {
		for (MethodNode m : cn.methods) {
			if (m.name.equals(name)) {
				return m;
			}
		}
		return null;
	}
}