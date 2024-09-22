package software.coley.recaf.ui.pane;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.script.ScriptEngine;
import software.coley.recaf.services.script.ScriptFile;
import software.coley.recaf.services.script.ScriptManager;
import software.coley.recaf.services.script.ScriptManagerConfig;
import software.coley.recaf.services.window.WindowFactory;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.ScrollbarPaddingUtil;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.problem.Problem;
import software.coley.recaf.ui.control.richtext.problem.ProblemPhase;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

import static software.coley.recaf.util.Lang.getBinding;

/**
 * Pane to display available scripts.
 *
 * @author Matt Coley
 * @author yapht
 * @see ScriptManager Source of scripts to pull from.
 */
@Dependent
public class ScriptManagerPane extends BorderPane {
	private static final Logger logger = Logging.get(ScriptManagerPane.class);
	private final VBox scriptList = new VBox();
	private final ScriptManager scriptManager;
	private final ScriptManagerConfig config;
	private final ScriptEngine engine;
	private final FileTypeSyntaxAssociationService languageAssociation;
	private final WindowFactory windowFactory;
	private final RecafDirectoriesConfig directories;
	private final KeybindingConfig keys;
	private final Instance<SearchBar> searchBarProvider;

	@Inject
	public ScriptManagerPane(@Nonnull ScriptManagerConfig config,
	                         @Nonnull ScriptManager scriptManager,
	                         @Nonnull ScriptEngine engine,
	                         @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                         @Nonnull WindowFactory windowFactory,
	                         @Nonnull RecafDirectoriesConfig directories,
	                         @Nonnull KeybindingConfig keys,
	                         @Nonnull Instance<SearchBar> searchBarProvider) {
		this.windowFactory = windowFactory;
		this.scriptManager = scriptManager;
		this.config = config;
		this.engine = engine;
		this.languageAssociation = languageAssociation;
		this.directories = directories;
		this.keys = keys;
		this.searchBarProvider = searchBarProvider;

		scriptManager.getScriptFiles().addChangeListener((ob, old, cur) -> refreshScripts());
		refreshScripts();

		scriptList.setFillWidth(true);
		scriptList.setSpacing(10);
		scriptList.setPadding(new Insets(10));

		ScrollPane scroll = new ScrollPane(scriptList);
		scroll.getStyleClass().add("dark-scroll-pane");
		scroll.setFitToWidth(true);
		setCenter(scroll);

		HBox controls = new HBox();
		controls.setStyle("""
				-fx-background-color: -color-bg-default;
				-fx-border-color: -color-border-default;
				-fx-border-width: 1 0 0 0;
				""");
		controls.setPadding(new Insets(10));
		controls.setSpacing(10);
		controls.getChildren().addAll(
				new ActionButton(CarbonIcons.EDIT, getBinding("menu.scripting.new"), this::newScript),
				new ActionButton(CarbonIcons.FOLDER, getBinding("menu.scripting.browse"), this::browse)
		);
		controls.getChildren().forEach(b -> b.getStyleClass().add("muted"));
		setBottom(controls);
	}

	/**
	 * Repopulate the script list.
	 */
	private void refreshScripts() {
		FxThreadUtil.run(() -> {
			scriptList.getChildren().clear();
			for (ScriptFile scriptFile : new TreeSet<>(scriptManager.getScriptFiles())) {
				scriptList.getChildren().add(new ScriptEntry(scriptFile));
			}
		});
	}

	/**
	 * @param script
	 * 		Script to edit.
	 */
	private void editScript(@Nonnull ScriptFile script) {
		ScriptEditor scriptEditor = new ScriptEditor(languageAssociation, script.source(), searchBarProvider.get())
				.withPath(script.path());
		Scene scene = new RecafScene(scriptEditor, 750, 400);
		windowFactory.createAnonymousStage(scene, getBinding("menu.scripting.editor"), 750, 400).show();
	}

	/**
	 * Opens a new script editor.
	 */
	public void newScript() {
		// TODO: Editor save prompts file location to save to in scripts dir
		//  - Add toggle in manager button list to create 'advanced' script using class model
		String template = """
				// ==Metadata==
				// @name Name
				// @description Description
				// @version 1.0.0
				// @author Author
				// ==/Metadata==
								
				System.out.println("Hello world");
				""";
		ScriptEditor scriptEditor = new ScriptEditor(languageAssociation, template, searchBarProvider.get());
		Scene scene = new RecafScene(scriptEditor, 750, 400);
		windowFactory.createAnonymousStage(scene, getBinding("menu.scripting.editor"), 750, 400).show();
	}

	/**
	 * Opens the local scripts directory.
	 */
	private void browse() {
		try {
			DesktopUtil.showDocument(config.getScriptsDirectory().toUri());
		} catch (IOException ex) {
			logger.error("Failed to show scripts directory", ex);
		}
	}

	/**
	 * Editor for scripts.
	 */
	private class ScriptEditor extends BorderPane {
		private final ProblemTracking problemTracking = new ProblemTracking();
		private final Editor editor = new Editor();
		private Path scriptPath;

		private ScriptEditor(@Nonnull FileTypeSyntaxAssociationService associationService, @Nonnull String initialText, @Nonnull SearchBar searchBar) {
			editor.setText(initialText);
			editor.getCodeArea().getUndoManager().forgetHistory();
			associationService.configureEditorSyntax("java", editor);
			editor.setSelectedBracketTracking(new SelectedBracketTracking());
			editor.setProblemTracking(problemTracking);
			editor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();

			// Add extra components
			searchBar.install(editor);
			editor.getPrimaryStack().getChildren().add(new RunScriptComponent());

			// Setup keybindings
			setOnKeyPressed(e -> {
				if (keys.getSave().match(e))
					save();
			});

			// Layout
			setCenter(editor);
		}

		/**
		 * @param scriptPath
		 * 		Location on disk to save to path to.
		 *
		 * @return Self.
		 */
		@Nonnull
		public ScriptEditor withPath(@Nonnull Path scriptPath) {
			this.scriptPath = scriptPath;
			return this;
		}

		/**
		 * Save the script to {@link #scriptPath}.
		 */
		private void save() {
			// Clear old errors emitted by compilation.
			problemTracking.removeByPhase(ProblemPhase.BUILD);

			// Invoke compiler with data.
			engine.compile(editor.getText()).whenCompleteAsync((result, throwable) -> {
				if (result != null && result.wasSuccess()) {
					// Don't care about compilation, just wanted to validate it was valid semantics.
					Animations.animateSuccess(this, 1000);
				} else {
					// Handle compile-result failure, or uncaught thrown exception.
					if (result != null) {
						for (CompilerDiagnostic diagnostic : result.diagnostics())
							problemTracking.add(Problem.fromDiagnostic(diagnostic));
					} else {
						logger.error("Compilation encountered an error", throwable);
					}
					Animations.animateFailure(this, 1000);
				}

				// Write to disk regardless, if path is given. If not given, prompt user for it.
				if (scriptPath == null) {
					FileChooser chooser = new FileChooserBuilder()
							.setInitialDirectory(directories.getScriptsDirectory())
							.setTitle(Lang.get("dialog.file.save"))
							.build();

					File selected = chooser.showSaveDialog(getScene().getWindow());
					if (selected != null)
						scriptPath = selected.toPath();
				}
				if (scriptPath != null) {
					try {
						Files.writeString(scriptPath, editor.getText());
					} catch (IOException ex) {
						logger.error("Failed to write to script file", ex);
					}
				}

				// Redraw paragraph graphics to update things like in-line problem graphics.
				editor.redrawParagraphGraphics();
			}, FxThreadUtil.executor());
		}

		/**
		 * Editor component to call {@link ScriptEngine#run(String)}.
		 */
		private class RunScriptComponent extends ActionButton {
			private RunScriptComponent() {
				super(new FontIconView(CarbonIcons.PLAY_FILLED, Color.LIME), Lang.getBinding("menu.scripting.execute"),
						() -> {
							problemTracking.removeByPhase(ProblemPhase.BUILD);
							engine.run(editor.getText()).whenCompleteAsync((result, throwable) -> {
								if (result != null && result.wasSuccess()) {
									// Don't care about compilation, just wanted to validate it was valid semantics.
									Animations.animateSuccess(editor, 1000);
								} else {
									// Handle compile-result failure, or uncaught thrown exception.
									if (result != null) {
										for (CompilerDiagnostic diagnostic : result.getCompileDiagnostics())
											problemTracking.add(Problem.fromDiagnostic(diagnostic));

										// Display runtime error if given.
										Throwable runtimeThrowable = result.getRuntimeThrowable();
										if (runtimeThrowable != null) {
											Label traceString = new Label(StringUtil.traceToString(runtimeThrowable));
											traceString.setGraphic(new FontIconView(CarbonIcons.ERROR, Color.RED));

											Popover popover = new Popover();
											popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
											popover.setContentNode(traceString);

											// Hack to get self
											ObservableList<Node> children = editor.getPrimaryStack().getChildrenUnmodifiable();
											popover.show(children.get(children.size() - 1));
										}
									} else {
										logger.error("Compilation encountered an error", throwable);
									}
									Animations.animateFailure(editor, 1000);
								}

								// Redraw paragraph graphics to update things like in-line problem graphics.
								editor.redrawParagraphGraphics();
							}, FxThreadUtil.executor());
						});

				// Layout tweaks
				StackPane.setAlignment(this, Pos.BOTTOM_RIGHT);
				StackPane.setMargin(this, new Insets(7));
				editor.getVerticalScrollbar().visibleProperty()
						.addListener((ob, old, cur) -> ScrollbarPaddingUtil.handleScrollbarVisibility(this, cur));
			}
		}
	}

	/**
	 * Entry showing the script details + run/edit action buttons.
	 */
	private class ScriptEntry extends BorderPane {
		private ScriptEntry(ScriptFile script) {
			setPadding(new Insets(10));
			getStyleClass().add("tooltip");

			Label nameLabel = new Label(script.name());
			nameLabel.setWrapText(true);
			nameLabel.setMinSize(350, 20);
			nameLabel.setMaxWidth(550);
			nameLabel.getStyleClass().add(Styles.TITLE_3);

			VBox info = new VBox();
			info.getChildren().add(nameLabel);

			String description = script.description();
			String author = script.author();
			String version = script.version();
			String url = script.getTagValue("url");

			if (!description.isBlank())
				info.getChildren().add(makeAttribLabel(null, EscapeUtil.unescapeStandard(description)));
			if (!author.isBlank())
				info.getChildren().add(makeAttribLabel(getBinding("menu.scripting.author"), author));
			if (!version.isBlank())
				info.getChildren().add(makeAttribLabel(getBinding("menu.scripting.version"), version));
			if (!url.isBlank()) {
				info.getChildren().add(makeAttribLabel(new StringBinding() {
					@Override
					protected String computeValue() {
						return "URL";
					}
				}, url));
			}

			VBox actions = new VBox();
			actions.setSpacing(4);
			actions.setAlignment(Pos.CENTER_RIGHT);


			ScriptEntry entry = this;
			Button executeButton = new ActionButton(CarbonIcons.PLAY_FILLED_ALT, getBinding("menu.scripting.execute"), () -> {
				script.execute(engine)
						.whenComplete((result, error) -> {
							if (result != null && result.wasSuccess()) {
								Animations.animateSuccess(entry, 1000);
							} else {
								Animations.animateFailure(entry, 1000);
							}
						});
			});
			executeButton.setAlignment(Pos.CENTER_LEFT);
			executeButton.setPrefSize(130, 30);

			Button editButton = new ActionButton(CarbonIcons.EDIT, getBinding("menu.scripting.edit"), () -> editScript(script));
			editButton.setAlignment(Pos.CENTER_LEFT);
			editButton.setPrefSize(130, 30);

			actions.getChildren().addAll(executeButton, editButton);

			Separator separator = new Separator(Orientation.HORIZONTAL);
			separator.prefWidthProperty().bind(scriptList.widthProperty());

			setLeft(info);
			setRight(actions);

			prefWidthProperty().bind(widthProperty());
		}

		/**
		 * Used to display bullet point format.
		 *
		 * @param langBinding
		 * 		Language binding for label display.
		 * @param secondaryText
		 * 		Text to appear after the initial binding text.
		 *
		 * @return Label bound to translatable text.
		 */
		private static Label makeAttribLabel(StringBinding langBinding, String secondaryText) {
			Label label = new Label(secondaryText);
			label.setWrapText(true);
			label.setMaxWidth(550);
			if (langBinding != null) {
				label.textProperty().bind(new StringBinding() {
					{
						bind(langBinding);
					}

					@Override
					protected String computeValue() {
						return String.format("  • %s: %s", langBinding.get(), secondaryText);
					}
				});
			}
			return label;
		}
	}
}
