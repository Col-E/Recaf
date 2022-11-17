package me.coley.recaf.ui.pane.assembler;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import me.coley.recaf.assemble.ContextualPipeline;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.behavior.WindowCloseListener;
import me.coley.recaf.ui.control.*;
import me.coley.recaf.ui.control.code.ProblemIndicatorInitializer;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.pane.DockingWrapperPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.util.WorkspaceClassSupplier;
import me.coley.recaf.util.WorkspaceInheritanceChecker;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper pane of all the assembler components.
 *
 * @author Matt Coley
 * @see AssemblerArea Assembler text editor
 */
public class AssemblerPane extends BorderPane implements MemberEditor, Cleanable, WindowCloseListener {
	private static final boolean DEBUG_AST = false;
	private final ContextualPipeline pipeline = new ContextualPipeline();
	private final List<MemberEditor> components = new ArrayList<>();
	private final CollapsibleTabPane bottomTabs = new CollapsibleTabPane();
	private final SplitPane split = new SplitPane();
	private final AssemblerArea assemblerArea;
	private final VirtualizedScrollPane<AssemblerArea> virtualScroll;
	private final DockTab tab;
	private boolean ignoreNextDisassemble;
	private MemberInfo targetMember;
	private ClassInfo classInfo;

	/**
	 * Setup the assembler pane and it's sub-components.
	 */
	public AssemblerPane() {
		pipeline.setInheritanceChecker(WorkspaceInheritanceChecker.getInstance());
		pipeline.setClassSupplier(WorkspaceClassSupplier.getInstance());
		ProblemTracking tracking = new ProblemTracking();
		tracking.setIndicatorInitializer(new ProblemIndicatorInitializer(tracking));
		assemblerArea = createAssembler(tracking, pipeline);
		components.add(assemblerArea);
		virtualScroll = new VirtualizedScrollPane<>(assemblerArea);
		Node errorDisplay = new ErrorDisplay(assemblerArea, tracking);
		BorderPane layoutWrapper = new ClassBorderPane(this);
		layoutWrapper.setCenter(virtualScroll);
		ClassStackPane stack = new ClassStackPane(this);
		StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
		StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
		stack.getChildren().add(layoutWrapper);
		stack.getChildren().add(errorDisplay);

		// Put tabs on bottom via split-view
		split.setOrientation(Orientation.VERTICAL);
		split.getItems().addAll(stack);
		split.setDividerPositions(0.75);
		split.getStyleClass().add("view-split-pane");

		// Build the UI
		DockingWrapperPane dockingWrapper = DockingWrapperPane.builder()
				.title(Lang.getBinding("assembler.title"))
				.content(split)
				.build();
		tab = dockingWrapper.getTab();
		tab.setOnClosed(e -> cleanup());
		setCenter(dockingWrapper);

		// Keybinds and other doodads
		Configs.keybinds().installEditorKeys(layoutWrapper);
		SearchBar.install(layoutWrapper, assemblerArea);
	}

	private Tab createPlayground() {
		Tab tab = new Tab();
		tab.textProperty().bind(Lang.getBinding("assembler.playground.title"));
		tab.setGraphic(Icons.getIconView(Icons.COMPILE));
		ExpressionPlaygroundPane expressionPlayground = new ExpressionPlaygroundPane(pipeline);
		components.add(expressionPlayground);
		tab.setContent(expressionPlayground);
		return tab;
	}

	private Tab createStackAnalysis() {
		Tab tab = new Tab();
		tab.textProperty().bind(Lang.getBinding("assembler.analysis.title"));
		tab.setGraphic(Icons.getIconView(Icons.SMART));
		tab.setContent(new StackAnalysisPane(assemblerArea, pipeline));
		return tab;
	}

	private Tab createDebug() {
		Tab tab = new Tab();
		tab.textProperty().bind(Lang.getBinding("Debug"));
		tab.setGraphic(Icons.getIconView(Icons.EYE));
		tab.setContent(new DebugPane(assemblerArea, pipeline));
		return tab;
	}

	private Tab createVariableTable() {
		Tab tab = new Tab();
		tab.textProperty().bind(Lang.getBinding("assembler.vartable.title"));
		tab.setGraphic(Icons.getIconView(Icons.T_STRUCTURE));
		VariableTable variableTable = new VariableTable(assemblerArea, pipeline);
		components.add(variableTable);
		tab.setContent(variableTable);
		return tab;
	}

	protected AssemblerArea createAssembler(ProblemTracking tracking, ContextualPipeline pipeline) {
		return new AssemblerArea(tracking, pipeline);
	}

	protected VirtualizedScrollPane<AssemblerArea> getScroll() {
		return virtualScroll;
	}

	protected AssemblerArea getAssemblerArea() {
		return assemblerArea;
	}

	@Override
	public SaveResult save() {
		// Because the 'save' method updates the workspace we must pre-emptively set this flag,
		// even if we cannot be sure if it will succeed.
		ignoreNextDisassemble = true;
		return assemblerArea.save();
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		if (newValue instanceof ClassInfo) {
			classInfo = (ClassInfo) newValue;
			components.forEach(c -> c.onUpdate(classInfo));
			// Update target member
			Unit unit = pipeline.getUnit();
			if (unit != null) {
				String name = unit.getDefinition().getName();
				String desc = unit.getDefinition().getDesc();
				if (unit.isField()) {
					for (FieldInfo field : newValue.getFields()) {
						if (field.getName().equals(name) && field.getDescriptor().equals(desc)) {
							setTargetMember(field);
							break;
						}
					}
				} else if (unit.isMethod()) {
					for (MethodInfo method : newValue.getMethods()) {
						if (method.getName().equals(name) && method.getDescriptor().equals(desc)) {
							setTargetMember(method);
							break;
						}
					}
				} else if (unit.isClass()) {
					// Prepare for class editing
					targetMember = null;
					assemblerArea.setTargetMember(null);
				}
			}
			// Skip if we triggered this update
			if (ignoreNextDisassemble) {
				ignoreNextDisassemble = false;
				return;
			}
			// Update disassembly text
			assemblerArea.disassemble();
		}
	}

	@Override
	public void cleanup() {
		assemblerArea.cleanup();
	}

	@Override
	public MemberInfo getTargetMember() {
		return targetMember;
	}

	@Override
	public void setTargetMember(MemberInfo targetMember) {
		this.targetMember = targetMember;
		components.forEach(c -> c.setTargetMember(targetMember));
		if (targetMember == null) {
			// Update tab display
			tab.textProperty().unbind();
			tab.setText(TextDisplayUtil.shortenEscapeLimit(classInfo.getName()));
			// Setup bottom tabs with bytecode helper tools for methods.
			if (bottomTabs.getTabs().isEmpty()) {
				bottomTabs.setSide(Side.BOTTOM);
				bottomTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
				bottomTabs.getTabs().addAll(
						createVariableTable(),
						createStackAnalysis(),
						createPlayground()
				);
				if (DEBUG_AST || Configs.assembler().astDebug)
					bottomTabs.getTabs().add(createDebug());
				bottomTabs.setup();
				split.getItems().add(bottomTabs);
			}
			tab.setGraphic(Icons.getClassIcon(classInfo));
		} else if (targetMember.isMethod()) {
			// Update tab display
			tab.textProperty().unbind();
			tab.setText(TextDisplayUtil.shortenEscapeLimit(targetMember.getName()));
			// Setup bottom tabs with bytecode helper tools for methods.
			if (bottomTabs.getTabs().isEmpty()) {
				bottomTabs.setSide(Side.BOTTOM);
				bottomTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
				bottomTabs.getTabs().addAll(
						createVariableTable(),
						createStackAnalysis(),
						createPlayground()
				);
				if (DEBUG_AST || Configs.assembler().astDebug)
					bottomTabs.getTabs().add(createDebug());
				bottomTabs.setup();
				split.getItems().add(bottomTabs);
			}
			tab.setGraphic(Icons.getMethodIcon((MethodInfo) targetMember));
		} else if (targetMember.isField()) {
			if (bottomTabs.getTabs().isEmpty()) {
				bottomTabs.setSide(Side.BOTTOM);
				bottomTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
				if (DEBUG_AST)
					bottomTabs.getTabs().add(createDebug());
				bottomTabs.setup();
				split.getItems().add(bottomTabs);
			}
			tab.setGraphic(Icons.getFieldIcon((FieldInfo) targetMember));
		}
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return classInfo;
	}

	@Override
	public boolean supportsEditing() {
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public boolean supportsMemberSelection() {
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// no-op, represents an actual member so nothing to select
	}

	@Override
	public void onClose(WindowEvent e) {
		cleanup();
	}
}
