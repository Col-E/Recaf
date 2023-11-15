package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compiler.ClassRepresentation;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.util.FxThreadUtil;

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
public class AssemblerToolTabs extends BorderPane implements AssemblerAstConsumer, AssemblerBuildConsumer, Navigable, UpdatableNavigable {
	private final Instance<JvmStackAnalysisPane> jvmStackAnalysisPaneProvider;
	private final Instance<JvmVariablesPane> jvmVariablesPaneProvider;
	private final List<Navigable> children = new CopyOnWriteArrayList<>();
	private final TabPane tabPane = new TabPane();
	private PathNode<?> path;

	@Inject
	public AssemblerToolTabs(@Nonnull Instance<JvmStackAnalysisPane> jvmStackAnalysisPaneProvider,
							 @Nonnull Instance<JvmVariablesPane> jvmVariablesPaneProvider) {
		this.jvmStackAnalysisPaneProvider = jvmStackAnalysisPaneProvider;
		this.jvmVariablesPaneProvider = jvmVariablesPaneProvider;
		setCenter(tabPane);
	}

	private void createChildren(@Nonnull ClassInfo classInPath) {
		children.clear();

		if (classInPath.isJvmClass()) {
			// Create contents for JVM classes
			JvmStackAnalysisPane stackAnalysisPane = jvmStackAnalysisPaneProvider.get();
			JvmVariablesPane variablesPane = jvmVariablesPaneProvider.get();

			// TODO: Translation titles / graphicss
			ObservableList<Tab> tabs = tabPane.getTabs();
			tabs.clear();
			tabs.add(new Tab("Analysis", stackAnalysisPane));
			tabs.add(new Tab("Variables", variablesPane));
			children.addAll(Arrays.asList(stackAnalysisPane, variablesPane));
		} else if (classInPath.isAndroidClass()) {
			// Create contents for Android classes
		}
	}

	@Override
	public void consumeAst(@Nonnull List<ASTElement> astElements, @Nonnull AstPhase phase) {
		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof AssemblerAstConsumer consumer)
				consumer.consumeAst(astElements, phase);

		FxThreadUtil.run(() -> onUpdatePath(path));
	}

	@Override
	public void consumeClass(@Nonnull ClassRepresentation classRepresentation,
							 @Nonnull ClassInfo classInfo) {
		for (Navigable navigableChild : getNavigableChildren())
			if (navigableChild instanceof AssemblerBuildConsumer consumer)
				consumer.consumeClass(classRepresentation, classInfo);

		FxThreadUtil.run(() -> onUpdatePath(path));
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
	public void disable() {
		for (Navigable navigableChild : getNavigableChildren())
			navigableChild.disable();
	}
}
