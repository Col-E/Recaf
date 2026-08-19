package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.objectweb.asm.Type;
import software.coley.collections.Lists;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.services.assembler.ExpressionCompileException;
import software.coley.recaf.services.assembler.ExpressionCompiler;
import software.coley.recaf.services.assembler.ExpressionResult;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.BracketMatchGraphicFactory;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.problem.Problem;
import software.coley.recaf.ui.control.richtext.problem.ProblemLevel;
import software.coley.recaf.ui.control.richtext.problem.ProblemPhase;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Component panel for the assembler which shows the variables of the currently selected method.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmExpressionCompilerPane extends AstBuildConsumerComponent {
	private static final ExecutorService compilePool = ThreadPoolFactory.newSingleThreadExecutor("expr-compile");
	protected final ProblemTracking problemTracking = new ProblemTracking();
	private final ExpressionCompiler expressionCompiler;
	private final Editor javaEditor = new Editor();
	private final Editor jasmEditor = new Editor();
	private final Button settingsButton = new Button();
	private final CheckBox classContextCheckBox = new CheckBox();
	private final CheckBox methodContextCheckBox = new CheckBox();
	private Popover popover;
	private boolean isDirty;

	@Inject
	public JvmExpressionCompilerPane(@Nonnull ExpressionCompiler expressionCompiler,
	                                 @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                                 @Nonnull Instance<SearchBar> searchBarProvider) {
		this.expressionCompiler = expressionCompiler;

		languageAssociation.configureEditorSyntax("java", javaEditor);
		javaEditor.setSelectedBracketTracking(new SelectedBracketTracking());
		javaEditor.setProblemTracking(problemTracking);
		javaEditor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
		jasmEditor.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());
		jasmEditor.setSelectedBracketTracking(new SelectedBracketTracking());
		jasmEditor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage()));
		jasmEditor.getRootLineGraphicFactory().addLineGraphicFactories(
				new BracketMatchGraphicFactory()
		);
		searchBarProvider.get().install(javaEditor);
		searchBarProvider.get().install(jasmEditor);

		// Configure the available expression context options.
		classContextCheckBox.textProperty().bind(Lang.getBinding("assembler.playground.class-context"));
		methodContextCheckBox.textProperty().bind(Lang.getBinding("assembler.playground.method-context"));
		classContextCheckBox.setSelected(true);
		methodContextCheckBox.setSelected(true);
		methodContextCheckBox.disableProperty().bind(classContextCheckBox.selectedProperty().not());
		classContextCheckBox.selectedProperty().addListener((ob, old, cur) -> onContextOptionChanged());
		methodContextCheckBox.selectedProperty().addListener((ob, old, cur) -> onContextOptionChanged());

		// Layout:
		//  - Left:  Java editor + settings/problems overlay
		//  - Right: Bytecode output
		settingsButton.setGraphic(new FontIconView(CarbonIcons.SETTINGS));
		settingsButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);
		settingsButton.setOnAction(this::showConfiguratorPopover);
		StackPane stack = new StackPane(javaEditor, settingsButton);
		SplitPane split = new SplitPane(stack, jasmEditor);
		StackPane.setAlignment(settingsButton, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(settingsButton, new Insets(7));
		setCenter(split);

		javaEditor.getTextChangeEventStream()
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.MEDIUM_DELAY_MS))
				.addObserver(unused -> scheduleCompile());
	}

	@Override
	protected void onClassSelected() {
		updateCompilerContext();
		init(ContextType.CLASS);
		scheduleCompile();
	}

	@Override
	protected void onMethodSelected() {
		updateCompilerContext();
		init(ContextType.METHOD);
		scheduleCompile();
	}

	@Override
	protected void onFieldSelected() {
		updateCompilerContext();
		init(ContextType.FIELD);
		scheduleCompile();
	}

	@Override
	protected void onPipelineOutputUpdate() {
		// no-op
	}

	@Override
	public void disable() {
		javaEditor.close();
		jasmEditor.close();
	}

	/**
	 * Populates the initial text of the expression compiler pane.
	 *
	 * @param type
	 * 		Content type in the {@link AssemblerPane}.
	 */
	private void init(@Nonnull ContextType type) {
		if (!javaEditor.getCodeArea().getText().isBlank()) return;

		// TODO: The comment should reflect what contexts are active
		//  - Should query expression compiler for this info
		String text = Lang.get("assembler.playground.comment") + '\n';

		switch (type) {
			case CLASS, FIELD -> text += "return;";
			case METHOD -> {
				Type returnType = Type.getMethodType(currentMethod.getDescriptor()).getReturnType();
				if (returnType.getSort() >= Type.ARRAY) {
					text += "return null;";
				} else {
					switch (returnType.getDescriptor().charAt(0)) {
						case 'V':
							text += "return;";
							break;
						case 'J':
							text += "return 0L;";
							break;
						case 'D':
							text += "return 0.0;";
							break;
						case 'F':
							text += "return 0F;";
							break;
						case 'I':
							text += "return 0;";
							break;
						case 'C':
							text += "return 'a';";
							break;
						case 'S':
							text += "return (short) 0;";
							break;
						case 'B':
							text += "return (byte) 0;";
							break;
						case 'Z':
							text += "return false;";
							break;
					}
				}
			}
		}
		javaEditor.setText(text);

		// Mark dirty when a user makes a change.
		javaEditor.textProperty().addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends String> ob, String old, String cur) {
				isDirty = true;
				javaEditor.textProperty().removeListener(this);
			}
		});
	}

	/**
	 * Applies each selected context independently after resetting stale compiler state.
	 */
	private void updateCompilerContext() {
		expressionCompiler.clearContext();
		if (classContextCheckBox.isSelected() && currentClass != null && canAssignClassContext())
			expressionCompiler.setClassContext(currentClass.asJvmClass());
		if (methodContextCheckBox.isSelected() && currentMethod != null && canAssignMethodContext())
			expressionCompiler.setMethodContext(currentMethod);
	}

	/**
	 * Called when a context option is changed, which will update the compiler context and schedule a recompile.
	 */
	private void onContextOptionChanged() {
		updateCompilerContext();
		scheduleCompile();
	}

	/**
	 * Shows the context settings popover, creating its controls on first use.
	 *
	 * @param e
	 * 		Button action event.
	 */
	private void showConfiguratorPopover(@Nonnull ActionEvent e) {
		if (popover == null) {
			GridPane content = createGrid();

			// Wrap in popover
			popover = new Popover(content);
			popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		}
		popover.show(settingsButton);
	}

	/**
	 * @return Grid containing the context options.
	 */
	@Nonnull
	private GridPane createGrid() {
		GridPane grid = new GridPane(8, 8);
		grid.setPadding(new Insets(8));
		grid.add(classContextCheckBox, 0, 0);
		grid.add(methodContextCheckBox, 0, 1);
		return grid;
	}

	/**
	 * Checks for things in the {@link #currentClass} which would prevent its use in the expression compiler as context.
	 *
	 * @return {@code true} when the current class can be used as context in the expression compiler.
	 */
	private boolean canAssignClassContext() {
		// We cannot have duplicate field names.
		Set<String> names = new HashSet<>();
		for (FieldMember field : currentClass.getFields()) {
			if (!names.add(field.getName()))
				return false;
		}
		return true;
	}

	/**
	 * Checks for things in the {@link #currentMethod} which would prevent its use in the expression compiler as context.
	 *
	 * @return {@code true} when the current method can be used as context in the expression compiler.
	 */
	private boolean canAssignMethodContext() {
		// If we find things that cannot be allowed as method context, add the checks here
		return true;
	}

	private void scheduleCompile() {
		if (isDirty) compilePool.submit(this::compile);
	}

	private void compile() {
		ExpressionResult result = expressionCompiler.compile(javaEditor.getText());
		FxThreadUtil.run(() -> {
			problemTracking.clear();

			// Validate no compiler errors occurred
			List<CompilerDiagnostic> diagnostics = result.getDiagnostics();
			if (!diagnostics.isEmpty()) {
				Animations.animateFailure(javaEditor, 1000);
				for (CompilerDiagnostic diagnostic : diagnostics) {
					Problem problem = Problem.fromDiagnostic(diagnostic);
					problemTracking.addItem(problem);
				}
				return;
			}

			// Validate no compile exception was thrown.
			ExpressionCompileException exception = result.getException();
			if (exception != null) {
				Animations.animateFailure(javaEditor, 1000);
				problemTracking.addItem(new Problem(-1, -1, 0,
						ProblemLevel.ERROR, ProblemPhase.BUILD, StringUtil.traceToString(exception)));
				return;
			}

			// Should have a result, but null check just to be safe.
			String assembly = result.getAssembly();
			jasmEditor.setText(Objects.requireNonNullElse(assembly, "<no-output>"));
		});
	}

	/**
	 * Type of content in the containing {@link AssemblerPane}.
	 */
	private enum ContextType {
		CLASS, FIELD, METHOD
	}
}