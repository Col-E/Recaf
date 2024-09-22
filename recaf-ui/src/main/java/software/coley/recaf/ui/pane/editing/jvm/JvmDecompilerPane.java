package software.coley.recaf.ui.pane.editing.jvm;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.animation.Transition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.properties.builtin.CachedDecompileProperty;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.compile.*;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.phantom.GeneratedPhantomWorkspaceResource;
import software.coley.recaf.services.phantom.PhantomGenerationException;
import software.coley.recaf.services.phantom.PhantomGenerator;
import software.coley.recaf.services.source.AstResolveResult;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.ModalPaneComponent;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.problem.Problem;
import software.coley.recaf.ui.control.richtext.problem.ProblemLevel;
import software.coley.recaf.ui.control.richtext.problem.ProblemPhase;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.source.JavaContextActionSupport;
import software.coley.recaf.ui.pane.editing.AbstractDecompilePane;
import software.coley.recaf.ui.pane.editing.ToolsContainerComponent;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Displays a {@link JvmClassInfo} via a configured {@link Editor} as decompiled by {@link DecompilerManager}.
 *
 * @author Matt Coley
 */
@Dependent
public class JvmDecompilerPane extends AbstractDecompilePane {
	private static final Logger logger = Logging.get(JvmDecompilerPane.class);
	private static final ExecutorService compilePool = ThreadPoolFactory.newSingleThreadExecutor("recompile");
	private final ObservableInteger javacTarget;
	private final ObservableInteger javacDownsampleTarget;
	private final ObservableBoolean javacDebug;
	private final ModalPaneComponent overlayModal = new ModalPaneComponent();
	private final PhantomGenerator phantomGenerator;
	private final JavacCompilerConfig javacConfig;
	private final JavacCompiler javac;

	@Inject
	public JvmDecompilerPane(@Nonnull DecompilerPaneConfig config,
	                         @Nonnull KeybindingConfig keys,
	                         @Nonnull SearchBar searchBar,
	                         @Nonnull ToolsContainerComponent toolsContainer,
	                         @Nonnull JavaContextActionSupport contextActionSupport,
	                         @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                         @Nonnull DecompilerManager decompilerManager,
	                         @Nonnull JavacCompiler javac,
	                         @Nonnull JavacCompilerConfig javacConfig,
	                         @Nonnull PhantomGenerator phantomGenerator,
	                         @Nonnull Actions actions) {
		super(config, searchBar, contextActionSupport, languageAssociation, decompilerManager);
		this.phantomGenerator = phantomGenerator;
		this.javacDebug = new ObservableBoolean(javacConfig.getDefaultEmitDebug().getValue());
		this.javacTarget = new ObservableInteger(javacConfig.getDefaultTargetVersion().getValue());
		this.javacDownsampleTarget = new ObservableInteger(javacConfig.getDefaultDownsampleTargetVersion().getValue());
		this.javacConfig = javacConfig;
		this.javac = javac;

		// Install tools container with configurator
		new JvmDecompilerPaneConfigurator(toolsContainer, config, decompiler, javacTarget, javacDownsampleTarget, javacDebug, decompilerManager);
		new JvmClassInfoProvider(toolsContainer, this);
		installToolsContainer(toolsContainer);

		// Setup keybindings
		setOnKeyPressed(e -> {
			if (keys.getSave().match(e))
				save();
			if (keys.getRename().match(e)) {
				// Resolve what the caret position has, then handle renaming on the generic result.
				AstResolveResult result = contextActionSupport.resolvePosition(editor.getCodeArea().getCaretPosition());
				if (result != null)
					actions.rename(result.path());
			}
		});

		// Install overlay modal
		overlayModal.setPersistent(true);
		overlayModal.install(editor);
	}

	/**
	 * Called when {@link KeybindingConfig#getSave()} is pressed.
	 * <br>
	 * Compiles the current Java code in the {@link #editor} and updates the workspace
	 * with the newly compiled {@link JvmClassInfo}.
	 */
	private void save() {
		// Pull data from path.
		JvmClassInfo info = path.getValue().asJvmClass();
		Workspace workspace = path.getValueOfType(Workspace.class);
		if (workspace == null)
			throw new IllegalStateException("Workspace missing from class path node");
		JvmClassBundle bundle = (JvmClassBundle) path.getValueOfType(Bundle.class);
		if (bundle == null)
			throw new IllegalStateException("Bundle missing from class path node");

		// Clear old errors emitted by compilation.
		problemTracking.removeByPhase(ProblemPhase.BUILD);

		// Invoke compiler with data.
		String infoName = info.getName();
		CompletableFuture.supplyAsync(() -> {
			// Generate phantoms for missing references in this class, if enabled and only if
			// there are no generated phantom resources already in the workspace.
			// This should be time-capped by 'completeOnTimeout' to prevent lock-ups.
			List<WorkspaceResource> phantomResources;
			if (javacConfig.getGeneratePhantoms().getValue() && workspace.getSupportingResources().stream()
					.noneMatch(resource -> resource instanceof GeneratedPhantomWorkspaceResource)) {
				ClassInfo currentInfo = path.getValue();
				Collection<JvmClassInfo> classesToScan;
				if (currentInfo.getInnerClasses().isEmpty()) {
					classesToScan = Collections.singleton(currentInfo.asJvmClass());
				} else {
					classesToScan = workspace.findJvmClasses(c -> c.getName().startsWith(currentInfo.getName())).stream()
							.map(p -> p.getValue().asJvmClass())
							.collect(Collectors.toList());
				}
				try {
					WorkspaceResource resource = phantomGenerator.createPhantomsForClasses(workspace, classesToScan);
					phantomResources = Collections.singletonList(resource);
					int generatedCount = resource.getJvmClassBundle().size();
					if (generatedCount > 0)
						logger.debug("Generated {} phantoms for pre-compile", generatedCount);
				} catch (PhantomGenerationException ex) {
					logger.warn("Failed to generate phantoms for compilation against '{}'", currentInfo.getName(), ex);
					phantomResources = null;
				}
			} else {
				phantomResources = null;
			}
			return phantomResources;
		}, compilePool).completeOnTimeout(null, 2, TimeUnit.SECONDS).thenApplyAsync(phantomResources -> {
			// Populate javac args
			boolean debug = javacDebug.getValue();
			JavacArgumentsBuilder builder = new JavacArgumentsBuilder()
					.withVersionTarget(useConfiguredVersion(info))
					.withDownsampleTarget(javacDownsampleTarget.getValue())
					.withDebugVariables(debug)
					.withDebugSourceName(debug)
					.withDebugLineNumbers(debug)
					.withClassSource(editor.getText())
					.withClassName(infoName);

			// Run javac with args + phantoms
			return javac.compile(builder.build(), workspace, phantomResources, null);
		}, compilePool).whenCompleteAsync((result, throwable) -> {
			// Handle results.
			//  - Success --> Update content in the containing bundle
			//  - Failure --> Show error + diagnostics to user
			if (result != null && result.wasSuccess()) {
				// Renaming is not allowed. Tell the user to use mapping operations.
				// This should usually be caught by javac, but we're double-checking here.
				// We *could* have some hacky code to work around the rename being done outside the dedicated API,
				// but it would be ugly. Find the new name for the class and any inners, copy over properties from
				// the old names, apply mapping operations to patch broken references, etc.
				CompileMap compilations = result.getCompilations();

				// Check if the target name still exists.
				if (!compilations.containsKey(infoName)) {
					logger.warn("Please only rename classes via mapping operations.");
					return;
				}

				// Check if any non-external-reference inner class entry no longer exists.
				//  - Removal/updating/insertion is OK, renaming is not.
				//  - Because inners may have other inners we need to recursively collect inner classes
				Map<String, InnerClassInfo> realInners = info.getInnerClasses().stream()
						.filter(inner -> !inner.isExternalReference())
						.collect(Collectors.toMap(InnerClassInfo::getInnerClassName, Function.identity()));
				Set<String> names = new HashSet<>();
				boolean recurseAddInners;
				do {
					// Reset the recurse flag each iteration. We'll enable it only if we need to.
					recurseAddInners = false;
					for (String type : new HashSet<>(realInners.keySet())) {
						// Skip if we already checked this type for further inner classes.
						if (!names.add(type)) continue;

						// Lookup inner class in workspace and add its inner classes to the map.
						ClassPathNode typePath = workspace.findClass(type);
						if (typePath != null) {
							List<InnerClassInfo> innerClasses = typePath.getValue().getInnerClasses();
							for (InnerClassInfo inner : innerClasses) {
								if (inner.isExternalReference()) continue;

								// Enable another recursive pass if a new inner class was found.
								recurseAddInners |= realInners.putIfAbsent(inner.getInnerClassName(), inner) == null;
							}
						}
					}
				} while (recurseAddInners);

				// Ensure all names in the compilation exist in the previous inner classes info
				if (!realInners.isEmpty() && compilations.keySet().stream().anyMatch(name -> !name.equals(infoName) && !realInners.containsKey(name))) {
					logger.warn("Please only rename inner classes via mapping operations.");
					Animations.animateWarn(this, 1000);
					return;
				}

				// Compilation map has contents, update the workspace.
				Animations.animateSuccess(this, 1000);
				updateLock.set(true);
				compilations.forEach((name, bytecode) -> {
					JvmClassInfo newInfo;
					if (infoName.equals(name)) {
						// Adapt from existing.
						newInfo = info.toJvmClassBuilder()
								.adaptFrom(bytecode)
								.build();
					} else {
						// Handle inner classes.
						JvmClassInfo originalClass = bundle.get(name);
						if (originalClass != null) {
							// Adapt from existing.
							newInfo = originalClass
									.toJvmClassBuilder()
									.adaptFrom(bytecode)
									.build();
						} else {
							// Class is new.
							newInfo = new JvmClassInfoBuilder(bytecode).build();
						}
					}

					// Update cached decompile property to current editor text.
					// If the class is opened later, we can use the code we compiled with.
					JvmDecompiler currentDecompiler = decompiler.getValue();
					int configHash = currentDecompiler.getConfig().getHash();
					CachedDecompileProperty.set(newInfo, currentDecompiler,
							new DecompileResult(editor.getText(), configHash));

					// Update the class in the bundle.
					bundle.put(newInfo);
				});
				updateLock.set(false);
			} else {
				// Handle compile-result failure, or uncaught thrown exception.
				if (result != null) {
					if (result.getDiagnostics().isEmpty() && result.getException() != null)
						problemTracking.add(new Problem(-1, -1, 0,
								ProblemLevel.ERROR, ProblemPhase.BUILD, result.getException().toString()));

					for (CompilerDiagnostic diagnostic : result.getDiagnostics())
						problemTracking.add(Problem.fromDiagnostic(diagnostic));

					// For first-timers, tell them you cannot save with errors.
					if (!config.getAcknowledgedSaveWithErrors().getValue())
						showFirstTimeSaveWithErrors();
				} else {
					logger.error("Compilation encountered an error on class '{}'", infoName, throwable);
				}
				Animations.animateFailure(this, 1000);
			}

			// Redraw paragraph graphics to update things like in-line problem graphics.
			editor.redrawParagraphGraphics();
		}, FxThreadUtil.executor());
	}

	/**
	 * @param info
	 * 		Class to recompile.
	 *
	 * @return Target Java version <i>(Standard versioning, not the internal one)</i>.
	 */
	private int useConfiguredVersion(@Nonnull JvmClassInfo info) {
		int version = javacTarget.getValue();

		// Negative: Match class file's version
		if (version < 0)
			return JavaVersion.adaptFromClassFileVersion(info.getVersion());

		// Use provided version
		return version;
	}

	/**
	 * Show popup telling user they cannot save with errors.
	 */
	private void showFirstTimeSaveWithErrors() {
		Button acknowledge = new Button();
		acknowledge.setGraphic(new FontIconView(CarbonIcons.TIMER));
		acknowledge.setDisable(true); // Enabled after a delay.

		VBox content = new VBox(new BoundLabel(Lang.getBinding("java.savewitherrors")), acknowledge);
		content.setFillWidth(false);
		content.setSpacing(10);
		content.setAlignment(Pos.CENTER);

		TitledPane wrapper = new TitledPane();
		wrapper.textProperty().bind(Lang.getBinding("java.savewitherrors.title"));
		wrapper.setCollapsible(false);
		wrapper.setContent(content);
		wrapper.getStyleClass().add(Styles.ELEVATED_4);
		wrapper.setMaxWidth(650);

		overlayModal.show(wrapper);

		// Start transition which counts down how long until the popup can be closed.
		WaitToAcknowledgeTransition wait = new WaitToAcknowledgeTransition(acknowledge);
		wait.play();

		// Enable acknowledge button after 5 seconds.
		FxThreadUtil.delayedRun(wait.getMillis(), () -> {
			wait.stop();
			acknowledge.textProperty().bind(Lang.getBinding("misc.acknowledge"));
			acknowledge.setGraphic(new FontIconView(CarbonIcons.CHECKMARK, Color.LIME));
			acknowledge.setDisable(false);
		});

		// When pressed, mark flag so prompt is not shown again.
		acknowledge.setOnAction(e -> {
			config.getAcknowledgedSaveWithErrors().setValue(true);
			overlayModal.hide();
		});
	}

	/**
	 * Transition to handle countdown to allow acknowledging <i>"I can not save with errors"</i>.
	 */
	private static class WaitToAcknowledgeTransition extends Transition {
		private static final int SECONDS = 8;
		private final Labeled labeled;

		private WaitToAcknowledgeTransition(Labeled labeled) {
			this.labeled = labeled;
			setCycleDuration(Duration.seconds(SECONDS));
		}

		private long getMillis() {
			return SECONDS * 1000;
		}

		@Override
		protected void interpolate(double frac) {
			int secondsLeft = (int) Math.floor(SECONDS - (frac * SECONDS) + 1.01);
			labeled.setText(secondsLeft + "...");
		}
	}
}
