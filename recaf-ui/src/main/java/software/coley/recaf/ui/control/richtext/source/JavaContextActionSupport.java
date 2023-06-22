package software.coley.recaf.ui.control.richtext.source;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.ContextMenu;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.TwoDimensional;
import org.openrewrite.Tree;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Range;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.ContextMenuProviderService;
import software.coley.recaf.services.cell.ContextSource;
import software.coley.recaf.services.navigation.ClassNavigable;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.services.source.*;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.ui.pane.editing.tabs.FieldsAndMethodsPane;
import software.coley.recaf.util.EscapeUtil;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enables context actions on an {@link Editor} by parsing the source text as Java and modeling the AST.
 * The AST can then be used to get information required for operations offered by {@link ContextMenuProviderService}.
 *
 * @author Matt Coley
 * @see FieldsAndMethodsPane#setupSelectionNavigationListener(ClassNavigable) Originating call for {@link #select(ClassMember)}.
 */
@Dependent
public class JavaContextActionSupport implements EditorComponent, UpdatableNavigable {
	private static final DebuggingLogger logger = Logging.get(JavaContextActionSupport.class);
	private static final long REPARSE_ELAPSED_TIME = 2_000L;
	private final ExecutorService parseThreadPool = ThreadPoolFactory.newSingleThreadExecutor("java-parse");
	private final NavigableMap<Integer, Integer> offsetMap = new TreeMap<>();
	private final CellConfigurationService cellConfigurationService;
	private final AstService astService;
	private final AstContextHelper contextHelper;
	private int lastSourceHash;
	private ClassPathNode path;
	private Runnable queuedSelectionTask;
	private String className;
	private J.CompilationUnit unit;
	private JavaParser parser;
	private Editor editor;
	private ContextMenu menu;

	@Inject
	public JavaContextActionSupport(@Nonnull CellConfigurationService cellConfigurationService,
									@Nonnull AstService astService,
									@Nonnull Workspace workspace) {
		this.cellConfigurationService = cellConfigurationService;
		this.astService = astService;
		contextHelper = new AstContextHelper(workspace);
	}

	/**
	 * @return Current AST for the class.
	 */
	@Nullable
	public J.CompilationUnit getUnit() {
		return unit;
	}

	/**
	 * Initializes the internal Java source parser.
	 *
	 * @param targetClass
	 * 		Class to initialize parser against.
	 */
	public void initialize(@Nonnull ClassInfo targetClass) {
		if (targetClass.isJvmClass()) {
			initialize(targetClass.asJvmClass());
		} else if (targetClass.isAndroidClass()) {
			initialize(targetClass.asAndroidClass());
		}
	}

	/**
	 * Initializes the internal Java source parser by converting the Android class to a Java class.
	 *
	 * @param targetClass
	 * 		Class to initialize parser against.
	 */
	private void initialize(@Nonnull AndroidClassInfo targetClass) {
		if (targetClass.canMapToJvmClass()) {
			initialize(targetClass.asJvmClass());
		}
	}

	/**
	 * Initializes the internal Java source parser.
	 *
	 * @param targetClass
	 * 		Class to initialize parser against.
	 */
	private void initialize(@Nonnull JvmClassInfo targetClass) {
		// Set name
		className = EscapeUtil.escapeStandard(targetClass.getName());

		// Allocate new parser
		if (parser != null)
			parser.reset();
		parser = astService.newParser(targetClass);
	}

	/**
	 * Schedules an AST parse job.
	 */
	public void scheduleAstParse() {
		if (editor == null)
			throw new IllegalStateException("Can only initialize after installed to an editor");

		// Do initial source parse
		if (!editor.getText().isBlank())
			handleLongDurationChange();
	}

	/**
	 * Selects a member in the AST.
	 *
	 * @param member
	 * 		Member to select.
	 */
	public void select(@Nonnull ClassMember member) {
		if (unit == null) {
			queuedSelectionTask = () -> select(member);
		} else {
			queuedSelectionTask = null;
			SortedMap<Range, Tree> map = AstRangeMapper.computeRangeToTreeMapping(unit, editor.getText());
			for (Map.Entry<Range, Tree> entry : map.entrySet()) {
				Tree tree = entry.getValue();
				Range range = entry.getKey();

				// Check against method and variable (field) declarations.
				if (member.isMethod() && tree instanceof J.MethodDeclaration method) {
					JavaType.Method methodType = method.getMethodType();

					// Extract method info.
					String name = method.getSimpleName();
					String desc = methodType == null ? null : AstUtils.toDesc(methodType);
					if (method.isConstructor()) {
						name = "<init>";
						if (desc != null) desc = StringUtil.cutOffAtFirst(desc, ")") + ")V";
					}

					// Compare to passed member.
					if (member.getName().equals(name) && (desc == null || member.getDescriptor().equals(desc))) {
						// Select it in the editor.
						selectRange(range);
						return;
					}
				} else if (member.isField() && tree instanceof J.VariableDeclarations variableDeclarations) {
					for (J.VariableDeclarations.NamedVariable variable : variableDeclarations.getVariables()) {
						JavaType.Variable variableType = variable.getVariableType();

						// Skip variable declarations that are not fields.
						if (variableType != null && !(variableType.getOwner() instanceof JavaType.FullyQualified))
							continue;

						// Extract variable info.
						String name = variable.getSimpleName();
						String desc = variableType == null ? null : AstUtils.toDesc(variableType);

						// Compare to passed member.
						if (member.getName().equals(name) && (desc == null || member.getDescriptor().equals(desc))) {
							// Select it in the editor.
							selectRange(range);
							return;
						}
					}
				} else if (member.getName().equals("<clinit>") && tree instanceof J.Block block && block.isStatic()) {
					// Select it in the editor.
					selectRange(range);
					return;
				}
			}
		}
	}

	/**
	 * Selects the range in the {@link #editor} on the FX thread.
	 *
	 * @param range
	 * 		Range to select.
	 */
	private void selectRange(@Nonnull Range range) {
		CodeArea area = editor.getCodeArea();
		FxThreadUtil.run(() -> {
			area.selectRange(range.getEnd().getOffset(), range.getStart().getOffset());
			area.showParagraphAtCenter(area.getCurrentParagraph());
		});
	}

	/**
	 * @param pos
	 * 		Offset in the source.
	 *
	 * @return Resolution of content at the given offset.
	 */
	@Nullable
	public AstResolveResult resolvePosition(int pos) {
		return resolvePosition(pos, true);
	}

	@Nullable
	private AstResolveResult resolvePosition(int pos, boolean doOffset) {
		if (unit == null) return null;
		if (doOffset) pos = offset(pos);
		return contextHelper.resolve(unit, pos, editor.getText());
	}

	/**
	 * Handle updating the offset-map so that we do not need to do a full reparse of the source.
	 * <br>
	 * When the user makes small changes, its unlikely they will be immediately doing context actions in that area.
	 * We can take advantage of this by not recomputing the AST model for every change, but instead tracking where
	 * text inserts/deletions occur. We can them map a position in the current text to the original parsed AST.
	 *
	 * @param change
	 * 		Text changed.
	 */
	private void handleShortDurationChange(@Nonnull PlainTextChange change) {
		int position = change.getPosition();
		int offset = change.getNetLength();
		offsetMap.merge(position, offset, Integer::sum);
	}

	/**
	 * Handle a full reparse of the source, updating the {@link #unit}.
	 */
	private void handleLongDurationChange() {
		// Skip if parser is not ready yet.
		if (parser == null)
			return;

		// Do parsing on BG thread, it can be slower on complex inputs.
		parseThreadPool.submit(() -> {
			String text = editor.getText();

			// Skip if the source hasn't changed since the last time.
			// This may occur when the user inserts some text, then removes it, resulting in the original text again.
			int textHash = text.hashCode();
			if (lastSourceHash == textHash) {
				logger.debugging(l -> l.info("Skipping AST parse, source hash has not changed"));
				return;
			}
			lastSourceHash = textHash;

			// Clear parser cache
			parser.reset();

			// Parse the current source
			long start = System.currentTimeMillis();
			logger.debugging(l -> l.info("Starting AST parse..."));
			List<J.CompilationUnit> units = parser.parse(text);
			if (units.isEmpty()) {
				unit = null;
				logger.warn("Could not create Java AST model from source of: {} after {}ms",
						className, (System.currentTimeMillis() - start));
			} else {
				unit = units.get(0);
				logger.debugging(l -> l.info("AST parsed successfully, took {}ms",
						(System.currentTimeMillis() - start)));

				// Run queued tasks
				if (queuedSelectionTask != null) queuedSelectionTask.run();
			}

			// Wipe offset map now that we have a new AST
			offsetMap.clear();
		});
	}

	/**
	 * Offsets the given input index.
	 *
	 * @param index
	 * 		Input index.
	 *
	 * @return Offset index based on values in {@link #offsetMap} up until the given index.
	 *
	 * @see #handleShortDurationChange(PlainTextChange)
	 */
	private int offset(int index) {
		if (offsetMap.isEmpty())
			return index;
		NavigableMap<Integer, Integer> subOffsetMap = offsetMap.subMap(0, true, index, false);
		int offset = -subOffsetMap.values().stream().mapToInt(i -> i).sum();
		logger.debugging(l -> l.info("Offset request hit index: {} --> {}", index, index + offset));
		return index + offset;
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
		editor.getTextChangeEventStream()
				.addObserver(this::handleShortDurationChange);
		editor.getTextChangeEventStream().successionEnds(Duration.ofMillis(REPARSE_ELAPSED_TIME))
				.addObserver(e -> handleLongDurationChange());
		CodeArea area = editor.getCodeArea();
		area.setOnContextMenuRequested(e -> {
			// Close old menu
			if (menu != null) {
				menu.hide();
				menu = null;
			}

			// Check AST model has been generated
			if (unit == null) {
				logger.warn("Could not request context menu, AST model not available");
				return;
			}

			// Convert the event position to line/column
			CharacterHit hit = area.hit(e.getX(), e.getY());
			TwoDimensional.Position hitPos = area.offsetToPosition(hit.getInsertionIndex(),
					TwoDimensional.Bias.Backward);
			int line = hitPos.getMajor() + 1; // Position is 0 indexed
			int column = hitPos.getMinor();

			// Sync caret
			area.moveTo(hit.getInsertionIndex());

			// Create menu
			int offsetHitIndex = offset(hit.getInsertionIndex());
			AstResolveResult result = resolvePosition(offsetHitIndex, false);
			if (result != null) {
				PathNode<?> path = result.path();
				logger.debugging(l -> l.info("Path at offset '{}' = {}", offsetHitIndex, path));

				// Map the result's declaration state to a context-source.
				ContextSource source = result.isDeclaration() ? ContextSource.DECLARATION : ContextSource.REFERENCE;
				menu = cellConfigurationService.contextMenuOf(source, path);
			}

			// Show menu
			if (menu != null) {
				menu.setAutoHide(true);
				menu.setHideOnEscape(true);
				menu.show(area.getScene().getWindow(), e.getScreenX(), e.getScreenY());
				menu.requestFocus();
			} else {
				logger.warn("No recognized class or member at selected position [line {}, column {}]", line, column);
			}
		});
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		editor.getCodeArea().setOnContextMenuRequested(null);
		this.editor = null;
	}

	@Nonnull
	@Override
	public ClassPathNode getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void requestFocus() {
		// no-op
	}

	@Override
	public void disable() {
		// no-op
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (path instanceof ClassPathNode classPath) {
			this.path = classPath;

			// Re-initialize the parser if the path updates.
			// This addresses situations where changes to the class introduce new type dependencies.
			// If we used the existing parser, the newly added types would be unresolvable.
			ClassInfo classInfo = classPath.getValue();
			Executors.newSingleThreadExecutor().submit(() -> initialize(classInfo));
		}
	}
}
