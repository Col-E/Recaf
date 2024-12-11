package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.Tab;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compiler.ClassResult;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.ui.pane.editing.SideTabs;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * UI component shown at the bottom of the {@link AssemblerPane}. Wraps various tool components.
 *
 * @author Matt Coley
 */
@Dependent
public class AssemblerToolTabs implements AssemblerAstConsumer, AssemblerBuildConsumer,
		Navigable, UpdatableNavigable, EditorComponent {
	private final Instance<JvmStackAnalysisPane> jvmStackAnalysisPaneProvider;
	private final Instance<JvmVariablesPane> jvmVariablesPaneProvider;
	private final Instance<JvmExpressionCompilerPane> jvmExpressionCompilerPaneProvider;
	private final Instance<SnippetsPane> snippetPaneProvider;
	private final Instance<ControlFlowLines> controlFlowLineProvider;
	private final List<Navigable> children = new CopyOnWriteArrayList<>();
	private final SideTabs tabs = new SideTabs(Orientation.HORIZONTAL);
	private PathNode<?> path;

	@Inject
	public AssemblerToolTabs(@Nonnull Instance<JvmStackAnalysisPane> jvmStackAnalysisPaneProvider,
							 @Nonnull Instance<JvmVariablesPane> jvmVariablesPaneProvider,
							 @Nonnull Instance<JvmExpressionCompilerPane> jvmExpressionCompilerPaneProvider,
							 @Nonnull Instance<SnippetsPane> snippetPaneProvider,
							 @Nonnull Instance<ControlFlowLines> controlFlowLineProvider) {
		this.jvmStackAnalysisPaneProvider = jvmStackAnalysisPaneProvider;
		this.jvmVariablesPaneProvider = jvmVariablesPaneProvider;
		this.jvmExpressionCompilerPaneProvider = jvmExpressionCompilerPaneProvider;
		this.snippetPaneProvider = snippetPaneProvider;
		this.controlFlowLineProvider = controlFlowLineProvider;

		// Without an initial size, the first frame of a method has nothing in it. So the auto-size to fit content
		// has nothing to fit to, which leads to only table headers being visible. Looks really dumb so giving it
		// a little bit of default space regardless mostly solves that.
		tabs.setInitialSize(200);
	}

	/**
	 * @return Side tabs instance.
	 */
	@Nonnull
	public SideTabs getTabs() {
		return tabs;
	}

	private void createChildren(@Nonnull ClassInfo classInPath) {
		children.clear();

		if (classInPath.isJvmClass()) {
			// Create contents for JVM classes
			JvmStackAnalysisPane stackAnalysisPane = jvmStackAnalysisPaneProvider.get();
			JvmVariablesPane variablesPane = jvmVariablesPaneProvider.get();
			JvmExpressionCompilerPane expressionPane = jvmExpressionCompilerPaneProvider.get();
			SnippetsPane snippetsPane = snippetPaneProvider.get();
			ControlFlowLines controlFlowLines = controlFlowLineProvider.get();
			children.addAll(Arrays.asList(stackAnalysisPane, variablesPane, expressionPane, snippetsPane, controlFlowLines));
			FxThreadUtil.run(() -> {
				ObservableList<Tab> tabs = this.tabs.getTabs();
				tabs.clear();
				tabs.add(new BoundTab(Lang.getBinding("assembler.analysis.title"), CarbonIcons.VIEW_NEXT, stackAnalysisPane));
				tabs.add(new BoundTab(Lang.getBinding("assembler.variables.title"), CarbonIcons.LIST_BOXES, variablesPane));
				tabs.add(new BoundTab(Lang.getBinding("assembler.playground.title"), CarbonIcons.CODE, expressionPane));
				tabs.add(new BoundTab(Lang.getBinding("assembler.snippets.title"), CarbonIcons.BOOK, snippetsPane));
				// Note: There is intentionally no tab for the jump arrow pane at the moment
				tabs.forEach(t -> t.setClosable(false));
			});
		} else if (classInPath.isAndroidClass()) {
			// Create contents for Android classes
		}
	}

	@Override
	public void consumeAst(@Nonnull List<ASTElement> astElements, @Nonnull AstPhase phase) {
		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof AssemblerAstConsumer consumer)
				consumer.consumeAst(astElements, phase);

		onUpdatePath(path);
	}

	@Override
	public void consumeClass(@Nonnull ClassResult result,
							 @Nonnull ClassInfo classInfo) {
		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof AssemblerBuildConsumer consumer)
				consumer.consumeClass(result, classInfo);

		onUpdatePath(path);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		boolean isInitial = this.path == null;

		this.path = path;

		if (isInitial) {
			ClassInfo classInPath = path.getValueOfType(ClassInfo.class);
			if (classInPath != null)
				createChildren(classInPath);
		}

		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof UpdatableNavigable updatableChild)
				updatableChild.onUpdatePath(path);
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public void requestFocus() {
		// no-op
	}

	@Override
	public void disable() {
		for (Navigable navigableChild : getNavigableChildren())
			navigableChild.disable();
	}

	@Override
	public void install(@Nonnull Editor editor) {
		children.forEach(n -> {
			if (n instanceof EditorComponent component)
				component.install(editor);
		});
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		children.forEach(n -> {
			if (n instanceof EditorComponent component)
				component.uninstall(editor);
		});
	}
}
