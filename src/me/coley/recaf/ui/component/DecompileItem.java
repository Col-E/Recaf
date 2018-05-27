package me.coley.recaf.ui.component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.PropertyEditor;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.stage.Stage;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.Asm;
import me.coley.recaf.config.impl.ConfCFR;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Misc;
import me.coley.memcompiler.Compiler;

import org.benf.cfr.reader.PluginRunner;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;

/**
 * Item for decompiling classes / methods.
 * 
 * @author Matt
 */
public class DecompileItem implements Item {
	//@formatter:off
	private static final String[] KEYWORDS = new String[] { "abstract", "assert", "boolean", "break", "byte", "case", "catch",
			"char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
			"float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new",
			"package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch",
			"synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while" };
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	private static final String PAREN_PATTERN = "\\(|\\)";
	private static final String BRACE_PATTERN = "\\{|\\}";
	private static final String BRACKET_PATTERN = "\\[|\\]";
	private static final String SEMICOLON_PATTERN = "\\;";
	private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";
	private static final Pattern PATTERN = Pattern.compile(
			 "(?<KEYWORD>" + KEYWORD_PATTERN + ")" +
			"|(?<PAREN>" + PAREN_PATTERN+ ")" +
			"|(?<BRACE>" + BRACE_PATTERN + ")" +
			"|(?<BRACKET>" + BRACKET_PATTERN + ")" +
			"|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" +
			"|(?<STRING>" + STRING_PATTERN + ")" +
			"|(?<COMMENT>" + COMMENT_PATTERN + ")");
	//@formatter:on

	/**
	 * Class to decompile.
	 */
	private final ClassNode cn;
	/**
	 * Optional: Method to decompile. If not {@code null} then only this method
	 * is decompiled.
	 */
	private final MethodNode mn;

	public DecompileItem(ClassNode cn) {
		this(cn, null);
	}

	public DecompileItem(ClassNode cn, MethodNode mn) {
		this.mn = mn;
		if (mn == null) {
			this.cn = cn;
		} else {
			this.cn = strip(cn);
		}
	}

	/**
	 * @param cn
	 *            Node to extract method from.
	 * @return ClassNode containing only the {@link #mn target method}.
	 */
	private ClassNode strip(ClassNode cn) {
		ClassNode copy = new ClassNode();
		copy.visit(cn.version, cn.access, cn.name, cn.signature, cn.superName, cn.interfaces.stream().toArray(String[]::new));
		copy.methods.add(mn);
		return copy;
	}

	/**
	 * Create new stage with decompiled text.
	 */
	public void decompile() {
		CFRResourceLookup lookupHelper = new CFRResourceLookup(cn);
		Map<String, String> options = ConfCFR.instance().toStringMap();
		String text = new PluginRunner(options, new CFRSourceImpl(lookupHelper)).getDecompilationFor(cn.name);

		// I disabled the option but it seems to print regardless.
		if (text.startsWith("/")) {
			text = text.substring(text.indexOf("*/") + 3);
		}
		// Create decompilation area
		CodeArea code = new CodeArea();
		code.setEditable(mn == null);
		code.setParagraphGraphicFactory(LineNumberFactory.get(code));
		code.richChanges().filter(ch -> !ch.getInserted().equals(ch.getRemoved())).subscribe(change -> {
			code.setStyleSpans(0, computeStyle(code.getText()));
		});
		// text not passed to constructor so CSS can be applied via the change
		// listener
		code.appendText(text);
		// dont allow undo to remove text
		code.getUndoManager().forgetHistory();
		String postfix = cn.name;
		if (mn != null) {
			postfix = mn.name;
		}
		Scene scene = JavaFX.scene(new VirtualizedScrollPane<>(code), 1200, 800);
		scene.getStylesheets().add("resources/style/decompile.css");
		Stage stage = JavaFX.stage(scene, Lang.get("ui.bean.class.decompile.name") + ":" + postfix, false);
		stage.setScene(scene);
		stage.show();
		// set position
		code.selectRange(0, 0);
		code.moveTo(0);
		code.scrollToPixel(0, 0);
		// context menu
		if (mn == null && Misc.isJDK()) {
			ContextMenu ctx = new ContextMenu();
			ctx.getItems().add(new ActionMenuItem(Lang.get("ui.bean.class.recompile.name"), () -> recompile(code)));
			code.setContextMenu(ctx);
		}
	}

	/**
	 * Uses the decompiled code to recompile.
	 */
	private void recompile(CodeArea codeText) {
		try {
			String srcText = codeText.getText();
			// TODO: For dependencies in agent-mode the jar/classes should be
			// fetched from the class-path.
			Compiler compiler = new Compiler();
			if (Input.get().input != null) {
				compiler.getClassPath().add(Input.get().input.getAbsolutePath());
			} else {
				// TODO: Add instrumented classpath
			}
			compiler.getDebug().sourceName = true;
			compiler.getDebug().lineNumbers = true;
			compiler.getDebug().variables = true;
			compiler.addUnit(cn.name.replace("/", "."), srcText);
			if (mn != null) {
				// TODO: Add this back
				Logging.error("Single-method recompilation unsupported, please decompile the full class");
				return;
			}
			if (!compiler.compile()) {
				Logging.error("Could not recompile!");
			}
			// Iterate over compiled units. This will include inner classes and the like.
			// TODO: Have alternate logic for single-method replacement
			for (String unit : compiler.getUnitNames()) {
				byte[] code = compiler.getUnitCode(unit);
				ClassNode newValue = Asm.getNode(code);
				Input.get().getClasses().put(cn.name, newValue);
				Logging.info("Recompiled '" + cn.name + "' - size:" + code.length, 1);
			}
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	/**
	 * @param text
	 *            Text to apply styles to.
	 * @return Stylized regions of the text <i>(via css tags)</i>.
	 */
	private static StyleSpans<Collection<String>> computeStyle(String text) {
		Matcher matcher = PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			//@formatter:off
			String styleClass = matcher.group("KEYWORD") != null ? "keyword"
					: matcher.group("PAREN")     != null ? "paren"
					: matcher.group("BRACE")     != null ? "brace"
					: matcher.group("BRACKET")   != null ? "bracket"
					: matcher.group("SEMICOLON") != null ? "semicolon"
					: matcher.group("STRING")    != null ? "string"
					: matcher.group("COMMENT")   != null ? "comment" : "other";
			//@formatter:on
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	// boiler-plate for displaying button that opens the stage.

	@Override
	public Optional<Class<? extends PropertyEditor<?>>> getPropertyEditorClass() {
		return JavaFX.optional(DecompileButton.class);
	}

	@Override
	public Class<?> getType() {
		return Object.class;
	}

	@Override
	public String getCategory() {
		return Lang.get("ui.bean.class");
	}

	@Override
	public String getName() {
		return Lang.get("ui.bean.class.decompile.name");
	}

	@Override
	public String getDescription() {
		return Lang.get("ui.bean.class.decompile.desc");
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public void setValue(Object value) {}

	@Override
	public Optional<ObservableValue<? extends Object>> getObservableValue() {
		return JavaFX.optionalObserved(null);
	}

	/**
	 * Button to pop up decopilation window.
	 * 
	 * @author Matt
	 */
	public static class DecompileButton implements PropertyEditor<Object> {
		private final DecompileItem item;

		public DecompileButton(Item item) {
			this.item = (DecompileItem) item;
		}

		@Override
		public Node getEditor() {
			Button button = new Button(Lang.get("ui.bean.class.decompile.name"));
			button.setOnAction(e -> item.decompile());
			return button;
		}

		@Override
		public Object getValue() {
			return null;
		}

		@Override
		public void setValue(Object value) {}
	}

	private static class CFRSourceImpl implements ClassFileSource {
		/**
		 * Lookup assistor for inner classes and other references.
		 */
		private final CFRResourceLookup resources;

		private CFRSourceImpl(CFRResourceLookup resources) {
			this.resources = resources;
		}

		@Override
		public void informAnalysisRelativePathDetail(String s, String s1) {}

		@Override
		public Collection<String> addJar(String s) {
			throw new UnsupportedOperationException("Return paths of all classfiles in jar.");
		}

		@Override
		public String getPossiblyRenamedPath(String s) {
			return s;
		}

		@Override
		public Pair<byte[], String> getClassFileContent(String pathOrName) throws IOException {
			pathOrName = pathOrName.substring(0, pathOrName.length() - ".class".length());
			return Pair.make(resources.get(pathOrName), pathOrName);
		}
	}

	/**
	 * Lookup helper for CFR since it requests extra data <i>(Other classes)</i>
	 * for more accurate decompilation.
	 * 
	 * @author Matt
	 */
	private static class CFRResourceLookup {
		private final ClassNode target;

		private CFRResourceLookup(ClassNode target) {
			this.target = target;
		}

		public byte[] get(String path) {
			// Load target node from instance.
			if (target != null && path.equals(target.name)) {
				try {
					return Asm.getBytes(target);

				} catch (Exception e) {
					Logging.error(e);
				}
			}
			// Load others from the virtual file system.
			// If they don't exist return null.
			try {
				return Input.get().getFile(path);
			} catch (IOException e) {
				Logging.info("Decompile 'get' failed: " + e.getMessage());
			}
			return null;
		}

	}
}
