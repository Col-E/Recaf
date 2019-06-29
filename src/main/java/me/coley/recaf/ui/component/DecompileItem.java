package me.coley.recaf.ui.component;

import java.util.Collections;
import java.util.Optional;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.PropertyEditor;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.model.TwoDimensional.Position;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.ClassUtil;
import me.coley.recaf.bytecode.search.Parameter;
import me.coley.recaf.event.*;
import me.coley.recaf.parse.source.CodeInfo;
import me.coley.recaf.parse.source.MemberNode;
import me.coley.recaf.ui.*;
import me.coley.recaf.util.*;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.memcompiler.Compiler;
import me.coley.memcompiler.JavaXCompiler;

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
	private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String CONST_HEX_PATTERN = "(0[xX][0-9a-fA-F]+)+";
	private static final String CONST_VAL_PATTERN = "(\\b([\\d._]*[\\d])\\b)+|(true|false|null)";
	private static final String CONST_PATTERN = CONST_HEX_PATTERN + "|" + CONST_VAL_PATTERN;
	private static final String COMMENT_SINGLE_PATTERN = "//[^\n]*";
	private static final String COMMENT_MULTI_SINGLE_PATTERN = "/[*](.|\n|\r)+?\\*/";
	private static final String COMMENT_MULTI_JAVADOC_PATTERN = "/[*]{2}(.|\n|\r)+?\\*/";
	private static final String ANNOTATION_PATTERN = "\\B(@[\\w]+)\\b";
	private static final Pattern PATTERN = new Pattern(
			"({COMMENTDOC}" + COMMENT_MULTI_JAVADOC_PATTERN + ")" +
			"|({COMMENTMULTI}" + COMMENT_MULTI_SINGLE_PATTERN + ")" +
			"|({COMMENTLINE}" + COMMENT_SINGLE_PATTERN + ")" +
			"|({KEYWORD}" + KEYWORD_PATTERN + ")" +
			"|({STRING}" + STRING_PATTERN + ")" +
			"|({ANNOTATION}" + ANNOTATION_PATTERN + ")" +
			"|({CONSTPATTERN}" + CONST_PATTERN + ")");
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
		this.cn = cn;
	}

	/**
	 * Create new stage with decompiled text.
	 */
	public void decompile() {
		// Create decompilation area
		CFRPipeline pipe = new CFRPipeline(cn, mn);
		FxDecompile code = new FxDecompile(pipe);
		code.show();
	}

	/**
	 * Currently unused since recompiling from method-only decompile is already
	 * unsupported <i>(for now)</i>.
	 * 
	 * @param cn
	 *            Node to extract method from.
	 * @return ClassNode containing only the {@link #mn target method}.
	 */
	@SuppressWarnings("unused")
	private ClassNode strip(ClassNode cn) {
		ClassNode copy = new ClassNode();
		copy.visit(cn.version, cn.access, cn.name, cn.signature, cn.superName, cn.interfaces.stream().toArray(String[]::new));
		copy.methods.add(mn);
		return copy;
	}

	/* boiler-plate for displaying button that opens the stage. */

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

	public class FxDecompile extends FxCode {
		/**
		 * Code analysis results of the current decompiled code.
		 */
		private CodeInfo info;
		/**
		 * Title postfix.
		 */
		private final String postfix;
		/**
		 * Flag marking ability to use the recompilation functions.
		 */
		private boolean canCompile;
		/**
		 * Current context menu instance.
		 */
		private ContextMenu ctx;
		/**
		 * Selected item.
		 */
		private Object selection;
		/**
		 * CFR decompiler pipeline.
		 */
		private CFRPipeline cfrPipe;

		public FxDecompile(CFRPipeline pipe) {
			super();
			this.postfix = pipe.getTitlePostfix();
			this.cfrPipe = pipe;
			setInitialText(pipe.decompile());
		}

		@Override
		protected void setupCodePane() {
			// Setup info BEFORE anything else.
			this.info = new CodeInfo(cn, this);
			// setup code-pane
			super.setupCodePane();
			// Add line numbers.
			code.setParagraphGraphicFactory(LineNumberFactory.get(code));
			// Setup context menu
			ctx = new ContextMenu();
			ctx.getStyleClass().add("code-context-menu");
			code.setContextMenu(ctx);
			// Add caret listener, updates what is currently selected.
			// This data will be used to populate the context menu.
			code.caretPositionProperty().addListener((obs, old, cur) -> {
				if (info.hasRegions()) {
					Position pos = code.offsetToPosition(cur, Bias.Backward);
					int line = pos.getMajor() + 1;
					int column = pos.getMinor() + 1;
					info.updateContextMenu(line, column);
				}
			});
			code.setOnMouseClicked(value -> {
				if (value.getButton() == MouseButton.SECONDARY) {
					// Context menu's code is UGLY, but it will look nice...
					// Worth it.
					ctx.getItems().clear();
					HBox box = new HBox();
					if (canCompile) {
						// Add recompile option if supported
						box.getChildren().add(FormatFactory.raw(Lang.get("ui.bean.class.recompile.name")));
						ctx.getItems().add(new ActionMenuItem("", box, () -> recompile(code)));
					}
					// Add other options if there is a selected item.
					if (selection == null) return;
					box = new HBox();
					// Verify that the selection is in the Input.
					// If so, allow the user to quickly jump to its definition.
					boolean allowEdit = true;
					if (selection instanceof ClassNode) {
						String owner = ((ClassNode) selection).name;
						allowEdit = Input.get().classes.contains(owner);
					} else if (selection instanceof MemberNode) {
						String owner = ((MemberNode) selection).getOwner().name;
						allowEdit = Input.get().classes.contains(owner);
					}
					if (allowEdit) {
						if (selection instanceof ClassNode) {
							ClassNode cn = (ClassNode) selection;
							box.getChildren().add(Icons.getClass(cn.access));
							box.getChildren().add(FormatFactory.raw(" " + Lang.get("misc.edit") + " "));
							box.getChildren().add(FormatFactory.type(Type.getType("L" + cn.name + ";")));
							ctx.getItems().add(new ActionMenuItem("", box, () -> Bus.post(new ClassOpenEvent(cn))));
						} else if (selection instanceof MemberNode) {
							MemberNode mn = (MemberNode) selection;
							Type type = Type.getType(mn.getDesc());
							box.getChildren().add(Icons.getMember(mn.getAccess(), mn.isMethod()));
							box.getChildren().add(FormatFactory.raw(" " + Lang.get("misc.edit") + " "));
							if (mn.isMethod()) {
								HBox typeBox = FormatFactory.typeMethod(type);
								Node retTypeNode = typeBox.getChildren().remove(typeBox.getChildren().size() - 1);
								box.getChildren().add(retTypeNode);
								box.getChildren().add(FormatFactory.raw(" "));
								box.getChildren().add(FormatFactory.name(mn.getName()));
								box.getChildren().add(typeBox);
								ctx.getItems().add(new ActionMenuItem("", box, () -> Bus.post(new MethodOpenEvent(mn.getOwner(),
										mn.getMethod(), null))));
							} else {
								box.getChildren().add(FormatFactory.type(type));
								box.getChildren().add(FormatFactory.raw(" "));
								box.getChildren().add(FormatFactory.name(mn.getName()));
								ctx.getItems().add(new ActionMenuItem("", box, () -> Bus.post(new FieldOpenEvent(mn.getOwner(), mn
										.getField(), null))));
							}
						}
					}
					// Reference search
					box = new HBox();
					box.getChildren().add(FormatFactory.raw(Lang.get("ui.edit.method.search")));
					if (selection instanceof ClassNode) {
						ClassNode owner = (ClassNode) selection;
						ctx.getItems().add(new ActionMenuItem("", box, () -> FxSearch.open(Parameter.references(owner.name, null,
								null))));
					} else if (selection instanceof MemberNode) {
						MemberNode mn = (MemberNode) selection;
						ClassNode owner = mn.getOwner();
						ctx.getItems().add(new ActionMenuItem("", box, () -> FxSearch.open(Parameter.references(owner.name, mn
								.getName(), mn.getDesc()))));
					}
				}
			});
			// Different functionality depending on if an entire class is being
			// analyzed or if its just a single method.
			if (mn == null) {
				// Allow recompilation if user is running on a JDK and is
				// working on
				// an entire class.
				if (Misc.canCompile()) {
					code.setEditable(true);
					canCompile = true;
				} else {
					Logging.info("Recompilation unsupported. Did not detect proper JDK classes.");
				}
			} else {
				// If we're focusing on a single method then allow the code to
				// be updated as the user redefines the code.
				Bus.subscribe(this);
				setOnCloseRequest(e -> {
					// Remove subscriptions on close
					Bus.unsubscribe(this);
				});
			}
		}

		@Listener
		private void onClassDirty(ClassDirtyEvent event) {
			// Skip over class changes that aren't of the current class
			if (!event.getNode().name.equals(cn.name))
				return;
			// While this will cause decompilation to be executed even on edits
			// that may not affect the output, making a MethodDirtyEvent will
			// require introducing a large amount of ugly boilerplate code. So
			// for the sake of keeping the spaghetti down, this minor
			// inefficiency is fine.
			Threads.run(() -> {
				String text = cfrPipe.decompile();
				Threads.runFx(() -> {
					code.clear();
					code.appendText(text);
				});
			});
		}

		@Override
		protected String createTitle() {
			return Lang.get("ui.bean.class.decompile.name") + ": " + postfix;
		}

		@Override
		protected Image createIcon() {
			return Icons.CL_CLASS;
		}

		@Override
		protected Pattern createPattern() {
			return PATTERN;
		}

		@Override
		protected String getStyleClass(Matcher matcher) {
			//@formatter:off
			return 	  matcher.group("STRING")       != null ? "string"
					: matcher.group("KEYWORD")      != null ? "keyword"
					: matcher.group("COMMENTDOC")   != null ? "comment-javadoc"
					: matcher.group("COMMENTMULTI") != null ? "comment-multi"
					: matcher.group("COMMENTLINE")  != null ? "comment-line"
					: matcher.group("CONSTPATTERN") != null ? "const"
					: matcher.group("ANNOTATION")   != null ? "annotation" : null;
			//@formatter:on
		}

		/**
		 * Uses the decompiled code to recompile.
		 */
		private void recompile(CodeArea codeText) {
			try {
				String srcText = codeText.getText();
				// TODO: For dependencies in agent-mode the jar/classes
				// should be fetched from the class-path.
				Compiler compiler = new JavaXCompiler();
				if (Input.get().input != null) {
					compiler.setClassPath(Collections.singletonList(Input.get().input.getAbsolutePath()));
				} else {
					// TODO: Add instrumented classpath
				}
				compiler.getDebug().sourceName = true;
				compiler.getDebug().lineNumbers = true;
				compiler.getDebug().variables = true;
				compiler.addUnit(cn.name.replace("/", "."), srcText);
				if (mn != null) {
					Logging.error("Single-method recompilation unsupported, please decompile the full class");
					return;
				}
				compiler.setCompileListener(msg -> Logging.error(msg.toString()));
				if (!compiler.compile()) {
					Logging.error("Could not recompile!");
					return;
				}
				// Iterate over compiled units. This will include inner classes
				// and the like.
				for (String unit : compiler.getUnitNames()) {
					byte[] code = compiler.getUnitCode(unit);
					ClassNode newValue = ClassUtil.getNode(code);
					Input.get().getClasses().put(cn.name, newValue);
					Logging.info("Recompiled '" + cn.name + "' - size:" + code.length, 1);
					Bus.post(new ClassRecompileEvent(cn, newValue));
					Bus.post(new ClassReloadEvent(cn.name));
				}
			} catch (Exception e) {
				Logging.error(e);
			}
		}

		@Override
		protected void onCodeChange(String code) {
			if (mn == null) {
				info.update(code);
			}
		}

		/**
		 * Called when the user's caret moves into the range of a recognized
		 * type.
		 * 
		 * @param cn
		 *            Type recognized.
		 */
		public void updateSelection(ClassNode cn) {
			selection = cn;
		}

		/**
		 * Called when the user's caret moves into the range of a recognized
		 * member.
		 * 
		 * @param mn
		 *            Member recognized.
		 */
		public void updateSelection(MemberNode mn) {
			selection = mn;
		}

		/**
		 * Reset selected item.
		 */
		public void resetSelection() {
			selection = null;
			if (!canCompile) {
				code.setContextMenu(null);
			}
		}
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
}
