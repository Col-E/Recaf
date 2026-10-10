package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.geometry.Side;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.backend.dalvik.DalvikClassRepresentation;
import me.darknet.assembler.backend.dalvik.DalvikTargetContext;
import me.darknet.assembler.backend.jvm.JvmTargetContext;
import me.darknet.assembler.backend.jvm.compile.JavaClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
import me.darknet.assembler.error.Diagnostic;
import me.darknet.assembler.error.Outcome;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.target.TargetContext;
import me.darknet.assembler.util.Location;
import org.fxmisc.richtext.CodeArea;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.collections.box.Box;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.assembler.AssemblerPipeline;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.assembler.AssemblyStage;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.highlight.SelectedWordHighlighting;
import software.coley.recaf.ui.control.richtext.problem.Problem;
import software.coley.recaf.ui.control.richtext.problem.ProblemLevel;
import software.coley.recaf.ui.control.richtext.problem.ProblemPhase;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.assembler.AssemblerTabCompleter;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndexService;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;
import software.coley.recaf.ui.pane.editing.AbstractContentPane;
import software.coley.recaf.ui.pane.editing.SideTabsInjector;
import software.coley.recaf.ui.pane.editing.tabs.FieldsAndMethodsPane;
import software.coley.recaf.ui.pane.editing.text.TextConfig;
import software.coley.recaf.util.Animations;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.WorkspaceSaveService;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Display dissassembled {@link ClassInfo} and {@link ClassMember} content.
 *
 * @author Matt Coley
 */
@Dependent
public class AssemblerPane extends AbstractContentPane<PathNode<?>> implements UpdatableNavigable, ClassNavigable {
	private static final Logger logger = Logging.get(AssemblerPane.class);

	private final AssemblerPipelineManager pipelineManager;
	private final AssemblerToolTabs assemblerToolTabs;
	private final WorkspaceSaveService saveService;
	private final ProblemTracking problemTracking = new ProblemTracking();
	private final Editor editor = new Editor();
	private final AtomicBoolean updateLock = new AtomicBoolean();
	private AssemblerPipeline<? extends ClassInfo, ? extends ClassResult<?>, ?> pipeline;
	private AssemblerTabCompleter tabCompleter;
	private ClassResult<?> lastResult;
	private Object lastAssembledClassRepresentation;
	private ClassInfo lastAssembledClass;
	private List<ASTElement> lastConcreteAst;

	@Inject
	public AssemblerPane(@Nonnull AssemblerPipelineManager pipelineManager,
	                     @Nonnull AssemblerToolTabs assemblerToolTabs,
	                     @Nonnull AssemblerContextActionSupport contextActionSupport,
	                     @Nonnull SearchBar searchBar,
	                     @Nonnull KeybindingConfig keys,
	                     @Nonnull SideTabsInjector sideTabsInjector,
	                     @Nonnull WorkspaceManager workspaceManager,
	                     @Nonnull WorkspaceSaveService saveService,
	                     @Nonnull InheritanceGraphService graphService,
	                     @Nonnull CellConfigurationService cellConfigurationService,
	                     @Nonnull TypeIndexService typeIndexService,
	                     @Nonnull TabCompletionConfig tabCompletionConfig,
	                     @Nonnull TextConfig textConfig) {
		super(Side.BOTTOM);

		this.pipelineManager = pipelineManager;
		this.assemblerToolTabs = assemblerToolTabs;
		this.saveService = saveService;

		assemblerToolTabs.setOwner(this);

		int timeToWait = pipelineManager.getServiceConfig().getDisassemblyAstParseDelay().getValue();

		InheritanceGraph inheritanceGraph = Objects.requireNonNull(graphService.getCurrentWorkspaceInheritanceGraph(), "Graph not created");
		if (tabCompletionConfig.isEnabledInAssembler()) {
			Workspace workspace = Objects.requireNonNull(workspaceManager.getCurrent());
			tabCompleter = new AssemblerTabCompleter(workspace, inheritanceGraph, typeIndexService, cellConfigurationService, tabCompletionConfig);
			editor.setTabCompleter(tabCompleter);
		}
		editor.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());
		if (textConfig.doHighlightWord()) editor.setSelectedWordHighlighting(new SelectedWordHighlighting());
		if (textConfig.doTrackBrackets()) editor.setSelectedBracketTracking(new SelectedBracketTracking());
		editor.setSyntaxHighlighter(new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage()));
		editor.setProblemTracking(problemTracking);
		editor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
		editor.getTextChangeEventStream()
				.successionEnds(Duration.ofMillis(timeToWait))
				.addObserver(e -> assemble());

		contextActionSupport.install(editor);
		searchBar.install(editor);

		// Context action should be passed along path updates
		children.add(contextActionSupport);
		children.add(assemblerToolTabs);

		setOnKeyPressed(event -> {
			if (keys.getSave().match(event))
				assembleAndUpdateWorkspace();
		});
	}

	/**
	 * Called by {@link #lateInitForClass(ClassPathNode)} or {@link #lateInitForMethod(ClassMemberPathNode)}.
	 * <p>
	 * Does late initialization that couldn't be done in the constructor.
	 */
	private void lateInit() {
		// Some tool tabs are not initialized immediately in the constructor, so we do a late installation of them.
		assemblerToolTabs.install(editor);
	}

	/**
	 * Called by {@link #onUpdatePath(PathNode)} once before the {@link #path} is set for the first time.
	 * <p>
	 * Sets up {@link FieldsAndMethodsPane} as a side-tab and sets up notifications for {@link AssemblerToolTabs}
	 * and its children when the selected {@link ClassMember} in the {@link #lastConcreteAst} changes.
	 *
	 * @param classPath
	 * 		The given path.
	 */
	private void lateInitForClass(@Nonnull ClassPathNode classPath) {
		// Since the content displayed is for a whole class, and the tool tabs are scoped to a method, we need to
		// update them when a method is selected. We do so by tracking the caret position for being within the
		// range of one of the methods in the last AST model.
		Box<PathNode<?>> lastPathBox = new Box<>();
		Box<ClassResult<?>> lastResultBox = new Box<>();
		editor.getCaretPosEventStream().addObserver(e -> {
			if (lastConcreteAst == null)
				return;
			ClassInfo declaringClass = classPath.getValue();
			int caret = editor.getCodeArea().getCaretPosition();
			for (ASTElement root : lastConcreteAst) {
				if (root instanceof ASTClass astClass) {
					for (ASTElement child : astClass.children()) {
						ClassMember classMember;
						if (child instanceof ASTMethod astMethod && astMethod.range().within(caret)) {
							String name = astMethod.getName().literal();
							String desc = astMethod.getDescriptor().literal();
							classMember = declaringClass.getDeclaredMethod(name, desc);
						} else if (child instanceof ASTField astField && astField.range().within(caret)) {
							String name = astField.getName().literal();
							String desc = astField.getDescriptor().literal();
							classMember = declaringClass.getDeclaredField(name, desc);
						} else {
							continue;
						}

						PathNode<?> prior = lastPathBox.get();
						if (classMember != null) {
							ClassMemberPathNode memberPath = classPath.child(classMember);
							if (!Objects.equals(prior, memberPath)) {
								lastPathBox.set(memberPath);
								eachChild(UpdatableNavigable.class, c -> c.onUpdatePath(memberPath));
							}
						} else {
							if (!Objects.equals(prior, classPath)) {
								lastPathBox.set(classPath);
								eachChild(UpdatableNavigable.class, c -> c.onUpdatePath(classPath));
							}
						}

						ClassResult<?> oldResult = lastResultBox.get();
						if (oldResult != lastResult) {
							lastResultBox.set(lastResult);
							eachChild(AssemblerBuildConsumer.class, c -> c.consumeClass(lastResult, lastAssembledClass));
						}
						return;
					}
				}
			}
		});

		// Common init
		assemblerToolTabs.onUpdatePath(classPath);
		lateInit();
	}

	/**
	 * Called by {@link #onUpdatePath(PathNode)} once before the {@link #path} is set for the first time.
	 *
	 * @param memberPathNode
	 * 		The given path.
	 */
	private void lateInitForMethod(@Nonnull ClassMemberPathNode memberPathNode) {
		// Common init
		assemblerToolTabs.onUpdatePath(memberPathNode);
		lateInit();
	}

	@Override
	protected void generateDisplay() {
		if (!hasDisplay()) {
			setDisplay(editor);

			// Trigger a disassembly so the initial text is set in the editor.
			disassemble().whenComplete((unused, error) -> {
				editor.getCodeArea().getUndoManager().forgetHistory();
				if (error == null && unused != null)
					unused.onSuccess(ignored -> assemble());
			});
		}
	}

	@Override
	public void disable() {
		super.disable();
		editor.close();
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public ClassPathNode getClassPath() {
		return Objects.requireNonNull(path.getPathOfType(ClassInfo.class), "Missing class parent path");
	}

	@Override
	public void requestFocus(@Nonnull InnerClassInfo innerClass) {
		// TODO: When the AST is of a class, rather than a member, we should be able to focus on the inner class declaration.
		//  - There isn't a full inner class structure containing all the methods and such, but just the attribute detailing 'inner class' which is the closest we can get to a declaration.
	}

	@Override
	public void requestFocus(@Nonnull ClassMember member) {
		if (lastConcreteAst != null) {
			for (ASTElement root : lastConcreteAst) {
				if (root instanceof ASTClass astClass) {
					for (ASTElement child : astClass.children()) {
						String name;
						String desc;
						if (member.isMethod() && child instanceof ASTMethod astMethod) {
							name = astMethod.getName().literal();
							desc = astMethod.getDescriptor().literal();
						} else if (member.isField() && child instanceof ASTField astField) {
							name = astField.getName().literal();
							desc = astField.getDescriptor().literal();
						} else {
							name = desc = null;
						}
						if (member.getName().equals(name) && member.getDescriptor().equals(desc)) {
							CodeArea area = editor.getCodeArea();
							area.moveTo(child.range().start());
							area.showParagraphAtCenter(area.getCurrentParagraph());
							requestFocus();
							return;
						}
					}
				}
			}
		}
	}

	@Override
	public void requestFocus() {
		// Delegate focus to the editor.
		editor.getCodeArea().requestFocus();
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		// If we've not seen a path before, do late initialization for UI elements
		// that require path information.
		if (this.path == null)
			if (path instanceof ClassPathNode classPathNode)
				lateInitForClass(classPathNode);
			else if (path instanceof ClassMemberPathNode memberPathNode)
				lateInitForMethod(memberPathNode);

		// Update the path and call any path listeners.
		this.path = path;
		Unchecked.checkedForEach(pathUpdateListeners, listener -> listener.accept(path),
				(listener, t) -> logger.error("Exception thrown when handling assembler-pane path update callback", t));

		// Update UI state.
		if (!updateLock.get()) {
			pipeline = pipelineManager.getPipeline(path);

			// Setup from existing class data from the path.
			lastAssembledClass = path.getValueOfType(ClassInfo.class);
			if (tabCompleter != null && lastAssembledClass != null) {
				tabCompleter.setEditedClass(lastAssembledClass);
				TargetContext targetContext = lastAssembledClass.isAndroidClass()
						? DalvikTargetContext.INSTANCE : JvmTargetContext.INSTANCE;
				tabCompleter.setTargetContext(targetContext);
			}
			lastAssembledClassRepresentation = pipeline.getRepresentation(Unchecked.cast(lastAssembledClass));
			lastResult = representationResult(lastAssembledClassRepresentation);
			eachChild(UpdatableNavigable.class, c -> c.onUpdatePath(path));
			eachChild(AssemblerBuildConsumer.class, c -> c.consumeClass(lastResult, lastAssembledClass));

			// Refresh the primary assembler display.
			FxThreadUtil.run(this::refreshDisplay);
		}
	}

	@Override
	public boolean isTrackable() {
		// Disabling tracking allows other panels with the same path-node to be opened.
		return false;
	}

	/**
	 * Disassemble the content of the {@link #path} and set the editor text to the resulting output.
	 *
	 * @return Future of disassembling completion.
	 */
	@Nonnull
	private CompletableFuture<Outcome<String>> disassemble() {
		problemTracking.removeByPhase(ProblemPhase.LINT);
		return CompletableFuture.supplyAsync(() -> pipeline.disassemble(path), ThreadUtil.executor())
				.orTimeout(10, TimeUnit.SECONDS)
				.whenCompleteAsync((result, error) -> {
					// If an uncaught exception was thrown, log it.
					if (result == null) {
						logger.error("Disassemble encountered an unexpected error", error);
						return;
					}

					// Otherwise process the result, updating the editor text and any problems.
					result.onPartial(partial -> processErrors(partial.errors(), ProblemPhase.LINT))
							.onFailure(failure -> processErrors(failure.errors(), ProblemPhase.LINT))
							.onSuccess(editor::setText);
					if (result.hasWarnings())
						processErrors(result.warnings(), ProblemLevel.WARN, ProblemPhase.LINT);
				}, FxThreadUtil.executor());
	}

	/**
	 * Parse the current editor's text into AST.
	 *
	 * @return Future of parse completion.
	 */
	@Nonnull
	private CompletableFuture<Outcome<List<ASTElement>>> parseAST() {
		// Nothing to parse
		if (editor.getText().isBlank()) return CompletableFuture.completedFuture(null);

		// Clear lint errors since we are running the linter again.
		if (problemTracking.removeByPhase(ProblemPhase.LINT))
			FxThreadUtil.run(editor::redrawParagraphGraphics);

		return CompletableFuture.supplyAsync(() -> {
			// Tokenization remains separate so token warnings/errors can update lint tracking immediately.
			Outcome<List<Token>> tokenResult = pipeline.tokenize(editor.getText(), "<assembler>");
			tokenResult.onPartial(partial -> processErrors(partial.errors(), ProblemPhase.LINT))
					.onFailure(failure -> processErrors(failure.errors(), ProblemPhase.LINT));
			if (tokenResult.hasWarnings())
				processErrors(tokenResult.warnings(), ProblemLevel.WARN, ProblemPhase.LINT);
			if (!tokenResult.hasValue()) {
				if (tabCompleter != null) tabCompleter.clearAst();
				return Outcome.<List<ASTElement>>of(null, tokenResult.diagnostics());
			}

			return pipeline.fullParse(tokenResult.requireValue(), this::processParseStage);
		}).whenComplete((res, err) -> {
			if (err != null) {
				logger.error("Failed to parse assembler content", err);
				FxThreadUtil.run(() -> Animations.animateFailure(editor, 1000));
			}
		});
	}

	/**
	 * Process the result of a parse stage.
	 *
	 * @param stage
	 * 		The stage that was completed.
	 * @param result
	 * 		Result of the parse stage.
	 */
	private void processParseStage(@Nonnull AssemblyStage stage, @Nonnull Outcome<List<ASTElement>> result) {
		// On explicit success, update the AST and inform any consumers.
		if (result.isSuccess()) {
			if (stage == AssemblyStage.CONCRETE) {
				List<ASTElement> concreteAst = result.requireValue();
				lastConcreteAst = concreteAst;
				if (tabCompleter != null) tabCompleter.setAst(concreteAst);
				eachChild(AssemblerAstConsumer.class, c -> c.consumeAst(concreteAst, AstPhase.CONCRETE));
			}
			if (result.hasWarnings())
				processErrors(result.warnings(), ProblemLevel.WARN, ProblemPhase.LINT);
			return;
		}

		// For partial/failure, we still want to update the AST and inform any consumers when possible.
		result.onPartial(partial -> {
			processErrors(partial.errors(), ProblemPhase.LINT);

			List<ASTElement> partialAst = partial.value();
			if (stage == AssemblyStage.ROUGH) {
				eachChild(AssemblerAstConsumer.class, c -> c.consumeAst(partialAst, AstPhase.ROUGH_PARTIAL));
			} else {
				if (tabCompleter != null)
					tabCompleter.setAst(partialAst);
				eachChild(AssemblerAstConsumer.class, c -> c.consumeAst(partialAst, AstPhase.CONCRETE_PARTIAL));
			}
		}).onFailure(failure -> {
			processErrors(failure.errors(), ProblemPhase.LINT);
			if (tabCompleter != null)
				tabCompleter.clearAst();
		});
	}

	/**
	 * Build the contents of the editor's text and update the workspace when successful.
	 *
	 * @return Future of parse completion.
	 */
	@Nonnull
	private CompletableFuture<Void> assemble() {
		// Ensure the AST is up-to-date before moving onto build stage.
		return parseAST().thenAccept(astResult -> {
			// If the parse finished, clear old build errors.
			if (problemTracking.removeByPhase(ProblemPhase.BUILD))
				FxThreadUtil.run(editor::redrawParagraphGraphics);

			// Skip if any problems remain in the AST
			if (!problemTracking.getProblemsByPhase(ProblemPhase.LINT).isEmpty() || lastConcreteAst == null)
				return;

			// Clear build errors since we are running the build process again.
			problemTracking.removeByPhase(ProblemPhase.BUILD);

			try {
				// The 'path' field may not hold the up-to-date class in the workspace.
				//
				// We want to look up the current version of the class to pass to the assembler pipeline
				// so that we don't overwrite the current class with whatever state happens to be in this path instance.
				//
				// Note: The path isn't updated in this pane because that may mess with the UX - which we want to avoid.
				final PathNode<?> localPath;
				if (path instanceof ClassMemberPathNode memberPath) {
					ClassPathNode localClassPath = Objects.requireNonNull(memberPath.getParent(), "Member path missing parent");
					ClassInfo classInfo = localClassPath.getValue();
					ClassMember member = memberPath.getValue();
					Workspace workspace = Objects.requireNonNull(memberPath.getValueOfType(Workspace.class), "Member path missing workspace");
					ClassPathNode newClassPath = Objects.requireNonNull(workspace.findClass(classInfo.getName()), "Member parent no longer defined in workspace");
					localPath = newClassPath.child(member);
				} else if (path instanceof ClassPathNode classPath) {
					ClassInfo classInfo = classPath.getValue();
					Workspace workspace = Objects.requireNonNull(classPath.getValueOfType(Workspace.class), "Class path missing workspace");
					localPath = Objects.requireNonNull(workspace.findClass(classInfo.getName()), "Class no longer defined in workspace");
				} else {
					throw new IllegalStateException("Unsupported assembler path type: " + path);
				}

				Outcome<? extends ClassResult<?>> assemblyOutcome = pipeline.assemble(lastConcreteAst, localPath);
				assemblyOutcome.onPartial(partial -> processErrors(partial.errors(), ProblemPhase.BUILD))
						.onFailure(failure -> processErrors(failure.errors(), ProblemPhase.BUILD));
				if (assemblyOutcome.isSuccess()) {
					ClassResult<?> result = assemblyOutcome.requireValue();
					Object representation = result.representation();

					lastResult = result;
					lastAssembledClassRepresentation = representation;

					ClassInfo assembledClass = classInfoFromRepresentation(pipeline, representation);
					if (assembledClass != null) {
						// Get last class's field/method count for ensuring no duplication happened.
						// If the user changes the definition of a field/method it inserts a new one
						// instead of changing the existing declaration.
						int fieldCount = lastAssembledClass.getFields().size();
						int methodCount = lastAssembledClass.getMethods().size();

						// Update the local path value, this will also inform sub-components of the new content.
						// The update-lock must be set so that we don't trigger a disassembly (which would trigger an endless loop)
						updateLock.set(true);
						if (localPath instanceof ClassPathNode classPath) {
							// Only update if the class name was untouched.
							if (assembledClass.getName().equals(classPath.getValue().getName())) {
								ClassPathNode newPath = classPath.getParent().child(assembledClass);
								onUpdatePath(newPath);
								lastAssembledClass = assembledClass;
							} else {
								ASTElement sourceAst = lastConcreteAst.get(0);
								processError("Changing the class name is not allowed in the assembler.\n" +
										"Use Recaf's refactoring capabilities to rename classes.", sourceAst.location(), ProblemPhase.BUILD);
							}
						} else if (localPath instanceof ClassMemberPathNode memberPath) {
							ClassMember oldMember = memberPath.getValue();
							ClassMember newMember;
							String memberType;
							if (oldMember.isMethod()) {
								memberType = "method";
								newMember = assembledClass.getDeclaredMethod(oldMember.getName(), oldMember.getDescriptor());
								if (methodCount != assembledClass.getMethods().size()) {
									ASTElement sourceAst = lastConcreteAst.get(0);
									processError("Assembling in this context detected a change in the number of methods.\n" +
											"Check and see if your class has illegal duplicate method definitions.", sourceAst.location(), ProblemPhase.BUILD);
								}
							} else {
								memberType = "field";
								newMember = assembledClass.getDeclaredField(oldMember.getName(), oldMember.getDescriptor());
								if (fieldCount != assembledClass.getFields().size()) {
									ASTElement sourceAst = lastConcreteAst.get(0);
									processError("Assembling in this context detected a change in the number of fields.\n" +
											"Check and see if your class has illegal duplicate field definitions.", sourceAst.location(), ProblemPhase.BUILD);
								}
							}

							// Only update if the member name/type was untouched.
							if (newMember != null) {
								lastAssembledClass = assembledClass;
								DirectoryPathNode packagePath = memberPath.getParent().getParent();
								ClassMemberPathNode newPath = packagePath.child(lastAssembledClass).child(newMember);
								onUpdatePath(newPath);
							} else {
								ASTElement sourceAst = lastConcreteAst.get(0);
								processError("Changing the " + memberType + " name/type is not allowed in the assembler.\n" +
										"Use Recaf's refactoring capabilities to rename fields & methods.", sourceAst.location(), ProblemPhase.BUILD);
							}
						}
						updateLock.set(false);
					}
					eachChild(AssemblerBuildConsumer.class, c -> c.consumeClass(result, lastAssembledClass));
				}
				if (assemblyOutcome.hasWarnings())
					processErrors(assemblyOutcome.warnings(), ProblemLevel.WARN, ProblemPhase.BUILD);
			} catch (Throwable ex) {
				logger.error("Uncaught exception when assembling contents of {}", path, ex);
				FxThreadUtil.run(() -> Animations.animateFailure(editor, 1000));
			}
		});
	}

	/**
	 * Assemble the current editor's text and update the workspace when successful.
	 *
	 * @return Future of parse completion.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	private CompletableFuture<Void> assembleAndUpdateWorkspace() {
		return assemble().whenComplete((unused, error) -> {
			if (lastAssembledClass != null && problemTracking.getProblemsByLevel(ProblemLevel.ERROR).isEmpty()) {
				updateLock.set(true);
				try {
					Bundle<ClassInfo> bundle = path.getValueOfType(Bundle.class);
					if (bundle == null)
						throw new IllegalStateException("Bundle not included in assembler pane's path");
					Workspace workspace = path.getValueOfType(Workspace.class);
					if (workspace == null)
						throw new IllegalStateException("Workspace not included in assembler pane's path");
					saveService.commit(workspace, bundle, lastAssembledClass);
					FxThreadUtil.run(() -> Animations.animateSuccess(editor, 1000));
				} catch (Throwable t) {
					logger.error("Uncaught exception when updating class of {}", lastAssembledClass.getName(), t);
					FxThreadUtil.run(() -> Animations.animateWarn(editor, 1000));
				} finally {
					updateLock.set(false);
				}
			} else {
				FxThreadUtil.run(() -> Animations.animateFailure(editor, 1000));
			}
		});
	}

	/**
	 * Add the given errors to {@link #problemTracking} and refresh the UI.
	 *
	 * @param diagnostics
	 * 		Problems to add.
	 * @param phase
	 * 		Phase the problems belong to.
	 */
	private void processErrors(@Nonnull Collection<Diagnostic> diagnostics, @Nonnull ProblemPhase phase) {
		processErrors(diagnostics, ProblemLevel.ERROR, phase);
	}

	/**
	 * Add the given diagnostics to {@link #problemTracking} and refresh the UI.
	 *
	 * @param diagnostics
	 * 		Problems to add.
	 * @param level
	 * 		Severity level of problems.
	 * @param phase
	 * 		Phase the problems belong to.
	 */
	private void processErrors(@Nonnull Collection<? extends Diagnostic> diagnostics, @Nonnull ProblemLevel level, @Nonnull ProblemPhase phase) {
		for (Diagnostic diagnostic : diagnostics) {
			Location location = diagnostic.location();
			int line = location == null ? 1 : location.line();
			int start = location == null ? 1 : location.column();
			int length = location == null ? 1 : location.length();
			problemTracking.addItem(new Problem(line, start, length, level, phase, diagnostic.message()));
		}
		FxThreadUtil.run(() -> {
			if (!diagnostics.isEmpty())
				editor.redrawParagraphGraphics();
		});
	}

	/**
	 * Add the given error to {@link #problemTracking} and refresh the UI.
	 *
	 * @param message
	 * 		Error message.
	 * @param location
	 * 		Error location.
	 * @param phase
	 * 		Phase the problem belongs to.
	 */
	private void processError(@Nonnull String message, @Nullable Location location, @Nonnull ProblemPhase phase) {
		int line = location == null ? 1 : location.line();
		int start = location == null ? 1 : location.column();
		int length = location == null ? 1 : location.length();
		problemTracking.addItem(new Problem(line, start, length, ProblemLevel.ERROR, phase, message));
		FxThreadUtil.run(editor::redrawParagraphGraphics);
	}

	/**
	 * @return Editor control holding the assembly contents.
	 */
	@Nonnull
	public Editor getEditor() {
		return editor;
	}

	/**
	 * @param representation
	 * 		Class representation to wrap in a {@link ClassResult}.
	 * @param <T>
	 * 		Type of representation.
	 *
	 * @return Class result that returns the given representation.
	 */
	@Nonnull
	private static <T> ClassResult<T> representationResult(T representation) {
		return () -> representation;
	}

	/**
	 * @param pipeline
	 * 		Pipeline to use for looking up class info.
	 * @param representation
	 * 		Class representation to look up.
	 *
	 * @return Class info for the given representation, or {@code null} if the representation is not a class.
	 */
	@Nullable
	private static ClassInfo classInfoFromRepresentation(@Nonnull AssemblerPipeline<?, ?, ?> pipeline,
	                                                     @Nonnull Object representation) {
		if (representation instanceof JavaClassRepresentation || representation instanceof DalvikClassRepresentation)
			return pipeline.getClassInfo(Unchecked.cast(representation));
		return null;
	}
}
