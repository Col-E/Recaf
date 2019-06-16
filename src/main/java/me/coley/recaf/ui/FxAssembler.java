package me.coley.recaf.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Popup;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.Logging;
import me.coley.recaf.bytecode.AccessFlag;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.Assembly;
import me.coley.recaf.parse.assembly.exception.AssemblyParseException;
import me.coley.recaf.parse.assembly.exception.ExceptionWrapper;
import me.coley.recaf.parse.assembly.util.LineData;
import me.coley.recaf.ui.component.AccessButton;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.util.*;
import org.fxmisc.richtext.LineNumberFactory;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCode.*;

/**
 * Window for bytecode assembling from text.
 *
 * @author Matt
 */
public class FxAssembler extends FxCode {
	//@formatter:off
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", OpcodeUtil.getInsnNames()) + ")\\b";
	private static final String LABEL_PATTERN = "\\b(LABEL[ ]+[-\\w]+)\\b";
	private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String CONST_HEX_PATTERN = "(0[xX][0-9a-fA-F]+)+";
	private static final String CONST_VAL_PATTERN = "\\b[\\d_]+[\\._]?[\\d]?[dfljf]?\\b";
	private static final String COMMENT_SINGLE_PATTERN = "//[^\n]*";
	private static final String CONST_PATTERN = CONST_HEX_PATTERN + "|" + CONST_VAL_PATTERN;
	private static final Pattern PATTERN = new Pattern(
			"({LABEL}" + LABEL_PATTERN + ")" +
			"|({COMMENTLINE}" + COMMENT_SINGLE_PATTERN + ")" +
			"|({KEYWORD}" + KEYWORD_PATTERN + ")" +
			"|({STRING}" + STRING_PATTERN + ")" +
			"|({CONSTPATTERN}" + CONST_PATTERN + ")");
	//@formatter:on
	// Method attributes
	private String methodName, methodDesc;
	private int methodAcc;
	// Assembler & options
	private final Assembly asm = new Assembly();
	private boolean locals = true, verify = true;
	// Used to prevent pre-mature parsing in constructor
	private boolean doParse;
	// UI attributes
	private static final int ROW_HEIGHT = 24;
	private final SimpleListProperty<ExceptionWrapper> exceptions
			= new SimpleListProperty<>(FXCollections.observableArrayList());
	private final Popup popAuto = new Popup();
	private final Consumer<MethodNode> onSave;
	private final TextField txtName = new TextField(methodName);
	private final TextField txtDesc = new TextField(methodDesc);
	private final AccessButton btnAcc = new AccessButton(AccessFlag.Type.METHOD, methodAcc);
	private final CheckBox chkVerify = new CheckBox(Lang.get("asm.edit.verify.name"));
	private final CheckBox chkLocals = new CheckBox(Lang.get("ui.bean.method.localvariables.name"));
	private ActionButton btnSave;
	private Label lblErrors = new Label();
	private Label lblFirstErrorLoc = new Label();
	private Label lblFirstError = new Label();

	private FxAssembler(MethodNode method, Consumer<MethodNode> onSave) {
		super();
		this.onSave = onSave;
		methodAcc = method.access;
		methodName = method.name;
		methodDesc = method.desc;
		asm.setMethodDeclaration(methodAcc, methodName, methodDesc);
		setupControls();
	}

	/**
	 * @param owner
	 * @param method
	 * @param onSave
	 *
	 * @return FxAssembler for editing methods.
	 */
	public static FxAssembler method(ClassNode owner, MethodNode method, Consumer<MethodNode> onSave) {
		FxAssembler fx = new FxAssembler(method, onSave);
		// setup text for existing instructions
		fx.asm.setHostType(owner.name);
		String[] lines = fx.asm.generateInstructions(method);
		String disassembly = String.join("\n", lines);
		fx.setInitialText(disassembly);
		fx.doParse = true;
		return fx;
	}

	/**
	 * @param method
	 * @param onSave
	 *
	 * @return FxAssembler for editing instructions.
	 */
	public static FxAssembler insns(ClassNode owner, MethodNode method, Consumer<MethodNode> onSave) {
		FxAssembler fx = new FxAssembler(method, onSave);
		fx.asm.setHostType(owner.name);
		fx.doParse = true;
		fx.verify = false;
		fx.chkVerify.setSelected(false);
		return fx;
	}

	@Override
	protected String createTitle() {
		return "Assembler";
	}

	@Override
	protected Image createIcon() {
		return Icons.LOGO;
	}

	@Override
	protected Pattern createPattern() {
		return PATTERN;
	}

	@Override
	protected String getStyleClass(Matcher matcher) {
		//@formatter:off
			return 	  matcher.group("STRING")       != null ? "string"
					: matcher.group("LABEL")        != null ? "annotation"
					: matcher.group("COMMENTLINE")  != null ? "comment-line"
					: matcher.group("KEYWORD")      != null ? "keyword"
					: matcher.group("CONSTPATTERN") != null ? "const" : null;
		//@formatter:on
	}

	@Override
	protected void setupCodePane() {
		super.setupCodePane();
		code.setEditable(true);
		// Add line numbers.
		IntFunction<Node> lineFactory = LineNumberFactory.get(code);
		IntFunction<Node> errorFactory = new ErrorIndicatorFactory();
		IntFunction<Node> decorationFactory = line -> {
			HBox hbox = new HBox(
					lineFactory.apply(line),
					errorFactory.apply(line));
			hbox.setAlignment(Pos.CENTER_LEFT);
			return hbox;
		};
		code.setParagraphGraphicFactory(decorationFactory);
		// Setup auto-complete
		code.setOnKeyReleased(e -> {
			// Update auto-complete on key-release except for certain non-modifying keys.
			KeyCode k = e.getCode();
			if (k == ESCAPE){
				// Remove popup if escape is pressed.
				popAuto.hide();
				return;
			}
			if (k == BACK_SPACE){
				// Don't show popup if backspacing onto some text.
				popAuto.hide();
				return;
			}
			if (k != PERIOD && (k.isArrowKey() || k.isModifierKey() || k.isWhitespaceKey())) {
				// Pressing period and most normal keyboard characters will allow auto-completion
				// to be run. Arrow keys and misc. keys will not update it.
				if (k == SPACE)
					popAuto.hide();
				return;
			}
			// Hide popup and then update auto-completion.
			// This may re-show it if there are suggestions.
			popAuto.hide();
			updateAutoComplete();
		});
		code.setOnKeyTyped(e -> {
			// Ensure directional / input keys are sent to the popup.
			KeyCode k = e.getCode();
			if (popAuto.isShowing() && (k == UP || k == DOWN || k == ENTER || k == TAB)) {
				popAuto.requestFocus();
			}
		});
	}

	/**
	 * Set up the other controls.
	 */
	private void setupControls() {
		GridPane gridTop = new GridPane();
		GridPane gridBottom = new GridPane();
		gridTop.getStyleClass().addAll("code-controls", "top");
		gridBottom.getStyleClass().addAll("code-controls", "bottom");
		// Top grid
		Label lblAccess = new Label(Lang.get("ui.bean.method.access.name"));
		Label lblName = new Label(Lang.get("ui.bean.method.name.name"));
		Label lblDesc = new Label(Lang.get("ui.bean.method.desc.name"));
		chkVerify.setSelected(true);
		chkLocals.setSelected(true);
		chkVerify.selectedProperty().addListener((o, a, b) -> {
			verify = b.booleanValue();
			onCodeChange(code.getText());
		});
		chkLocals.selectedProperty().addListener((o, a, b) -> {
			locals = b.booleanValue();
			onCodeChange(code.getText());
		});
		btnAcc.setAccess(methodAcc);
		txtName.setText(methodName);
		txtDesc.setText(methodDesc);
		code.deleteText(0, code.getLength());
		btnAcc.setUpdateTask(i -> {
			methodAcc = i;
			onCodeChange(code.getText());
		});
		txtName.setOnKeyReleased(e -> {
			KeyCode k = e.getCode();
			if (k.isModifierKey() || k.isArrowKey())
				return;
			methodName = txtName.getText();
			onCodeChange(code.getText());
		});
		txtDesc.setOnKeyReleased(e -> {
			KeyCode k = e.getCode();
			if (k.isModifierKey() || k.isArrowKey())
				return;
			methodDesc = txtDesc.getText();
			onCodeChange(code.getText());
		});
		gridTop.add(lblAccess, 0, 0);
		gridTop.add(btnAcc, 0, 1);
		gridTop.add(lblName, 1, 0);
		gridTop.add(txtName, 1, 1);
		gridTop.add(lblDesc, 2, 0);
		gridTop.add(txtDesc, 2, 1);
		gridTop.add(chkVerify, 3, 0);
		gridTop.add(chkLocals, 3, 1);
		ColumnConstraints cSmall1 = new ColumnConstraints();
		ColumnConstraints cSmall2 = new ColumnConstraints();
		ColumnConstraints cLarge = new ColumnConstraints();
		cSmall1.setHalignment(HPos.CENTER);
		cSmall1.setPercentWidth(18);
		cSmall2.setHalignment(HPos.LEFT);
		cSmall2.setPercentWidth(18);
		cLarge.setPercentWidth(32);
		gridTop.getColumnConstraints().addAll(cSmall1, cLarge, cLarge, cSmall2);
		// Bottom grid
		btnSave = new ActionButton(Lang.get("misc.save"), () -> {
			onSave.accept(asm.getMethod());
			btnSave.setDisable(true);
		});
		btnSave.setDisable(true);
		gridBottom.add(btnSave, 0 , 0);
		gridBottom.add(lblErrors, 1 , 0);
		gridBottom.add(lblFirstErrorLoc, 2 , 0);
		gridBottom.add(lblFirstError, 3, 0);
		gridBottom.setHgap(10);
		wrapper.setTop(gridTop);
		wrapper.setBottom(gridBottom);
	}

	@Override
	protected void setupSearch() {
		super.setupSearch();
		// Disable the auto-display by mouse proximity
		pane.setTriggerDistance(-1);
	}

	@Override
	protected void onCodeChange(String code) {
		if (doParse) {
			parseInstructions(code);
		}
	}

	/**
	 * Updates the interpreted instructions.
	 *
	 * @param code
	 * 		Current updated text.
	 */
	private void parseInstructions(String code) {
		ThreadAction.<Boolean>create().supplier(() -> {
			// Attempt to assemble instructions
			String[] lines = code.split("\n");
			asm.setDoGenerateLocals(locals);
			asm.setDoVerify(verify);
			asm.setMethodDeclaration(methodAcc, methodName, methodDesc);
			boolean parseSuccess = false;
			try {
				parseSuccess = asm.parseInstructions(lines);
			} catch(AssemblyParseException e) {
				// Expected exception
			} catch(Exception e) {
				// Unknown exception
				Logging.error(e);
			}
			return parseSuccess;
		}).consumer(success -> {
			// Reset exceptions
			exceptions.clear();
			if(success) {
				// Success
				btnSave.setDisable(false);
				lblErrors.setText("");
				lblFirstErrorLoc.setText("");
				lblFirstError.setText("");
			} else {
				// Failure
				exceptions.addAll(asm.getExceptions());
				btnSave.setDisable(true);
				// Display the number of errors and where the first occurring error is
				lblErrors.setText("Errors: " + asm.getExceptions().size());
				ExceptionWrapper err = asm.getExceptions().get(0);
				lblFirstErrorLoc.setText(Lang.get("ui.edit.method.assemblyfirsterror") + " " + err.line);
				lblFirstError.setText("(" + err.exception.getMessage() + ")");
			}
			btnSave.requestLayout();
			lblErrors.requestLayout();
			// Update exceptions to reset displayed exception icons
			forceUpdate();
		}).onUi().run();
	}

	/**
	 * Update code-completion prompt.
	 */
	private void updateAutoComplete() {
		if(popAuto == null)
			return;
		int position = code.getCaretPosition();
		int line = code.getCurrentParagraph();
		String lineText = code.getParagraph(line).getText();
		// Ensure that the caret is at the end of the line.
		if (code.getCaretColumn() < lineText.length()) {
			return;
		}
		ThreadAction.<List<String>>create().supplier(() -> {
			List<String> suggestions = null;
			// Check for opcode matching
			if (lineText.matches("^\\w+$")) {
				// Populate by matching opcode names
				suggestions = OpcodeUtil.getInsnNames().stream()
						.filter(op -> op.toUpperCase().startsWith(lineText.toUpperCase()) && !op.equalsIgnoreCase(lineText))
						.sorted(Comparator.naturalOrder())
						.collect(Collectors.toList());
			} else {
				// Instruction arg matching
				try {
					LineData lineData = LineData.from(lineText);
					if (lineData == null)
						return null;
					String opText = lineData.optext;
					int opcode = lineData.opcode;
					int type = lineData.type;
					AbstractAssembler assembler = Assembly.getAssembler(type, opcode);
					if(assembler != null) {
						String args = lineText.substring(opText.length()).trim();
						suggestions = assembler.suggest(args);
					}
				} catch(Exception e) {
					// If we fail, don't suggest anything
					return null;
				}
			}
			// Limit capacity
			suggestions = suggestions.stream().limit(7).collect(Collectors.toList());
			return suggestions;
		}).consumer(suggestions -> {
			// No suggestions? Do nothing
			if (suggestions == null || suggestions.isEmpty()) {
				return;
			}
			ListView<String> listSuggestions = new ListView<>(FXCollections.observableArrayList(suggestions));
			listSuggestions.getStyleClass().add("tab-complete");
			listSuggestions.getSelectionModel().select(0);
			listSuggestions.setPrefHeight(suggestions.size() * ROW_HEIGHT + 2);
			// Get current word
			Matcher m = new Pattern("([\\/\\w]+|(?!\\.))$").matcher(lineText);
			if(!m.find()) {
				return;
			}
			String curWord = m.group(0);
			// Action to replace the word with some given replacement word.
			Runnable replace = () -> {
				String selected = listSuggestions.getSelectionModel().getSelectedItem();
				Platform.runLater(() -> {
					code.replaceText(position - curWord.length(), position, selected);
					code.moveTo(position + selected.length() - curWord.length());
				});
				popAuto.hide();
			};
			Optional<Bounds> val = code.caretBoundsProperty().getValue();
			if (!val.isPresent())
				return;
			Bounds pointer = val.get();
			popAuto.getContent().clear();
			popAuto.getContent().add(listSuggestions);
			listSuggestions.setOnMouseClicked(e -> replace.run());
			listSuggestions.setOnKeyPressed(e -> {
				if(e.getCode() == ENTER || e.getCode() == TAB) {
					replace.run();
				}
			});
			popAuto.show(code, pointer.getMaxX(), pointer.getMinY());
		}).onUi().run();
	}

	/**
	 * RichTextFX hack to redraw line graphics.
	 */
	private void forceUpdate() {
		IntFunction<Node> va = (IntFunction<Node>) code.getParagraphGraphicFactory();
		code.setParagraphGraphicFactory(null);
		code.setParagraphGraphicFactory(va);
	}

	/**
	 * Decorator factory for building error indicators.
	 */
	class ErrorIndicatorFactory implements IntFunction<Node> {
		private final double[] shape = new double[] {0, 5, 2.5, 0, 7.5, 0, 10, 5, 7.5, 10, 2.5, 10};

		@Override
		public Node apply(int lineNo) {
			Polygon poly = new Polygon(shape);
			poly.getStyleClass().add("cursor-pointer");
			poly.setFill(Color.RED);
			if(exceptions == null || exceptions.isEmpty()) {
				poly.setVisible(false);
				return poly;
			}
			Optional<ExceptionWrapper> exx = exceptions.stream()
					.filter(ex -> ex.line == (lineNo + 1)).findFirst();
			poly.visibleProperty().setValue(exx.isPresent());
			if(exx.isPresent()) {
				Tooltip t = new Tooltip(exx.get().exception.getMessage());
				Tooltip.install(poly, t);
			}
			return poly;
		}
	}
}
