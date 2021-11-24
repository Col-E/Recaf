package me.coley.recaf.ui.pane.assembler;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.*;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.ui.control.ErrorDisplay;
import me.coley.recaf.ui.control.code.ProblemIndicatorInitializer;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.pane.DockingRootPane;
import me.coley.recaf.ui.pane.DockingWrapperPane;
import me.coley.recaf.ui.util.Icons;
import org.fxmisc.flowless.VirtualizedScrollPane;

/**
 * Wrapper pane of all the assembler components.
 *
 * @author Matt Coley
 * @see AssemblerArea Assembler text editor
 */
public class AssemblerPane extends BorderPane implements MemberEditor, Cleanable {
	private final AssemblerArea assemblerArea;
	private final Tab tab;
	private boolean ignoreNextDisassemble;
	private MemberInfo targetMember;
	private ClassInfo classInfo;

	/**
	 * Setup the assembler pane and it's sub-components.
	 */
	public AssemblerPane() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.setIndicatorInitializer(new ProblemIndicatorInitializer(tracking));
		assemblerArea = new AssemblerArea(tracking);
		Node node = new VirtualizedScrollPane<>(assemblerArea);
		Node errorDisplay = new ErrorDisplay(assemblerArea, tracking);
		StackPane stack = new StackPane();
		StackPane.setAlignment(errorDisplay, Configs.editor().errorIndicatorPos);
		StackPane.setMargin(errorDisplay, new Insets(16, 25, 25, 53));
		stack.getChildren().add(node);
		stack.getChildren().add(errorDisplay);

		DockingWrapperPane wrapper = DockingWrapperPane.builder()
				.title("Assembler")
				.content(stack)
				.build();
		tab = wrapper.getTab();
		setCenter(wrapper);

		Configs.keybinds().installEditorKeys(this);
		// TODO: Bottom tabs
		//  - local variable table
		//  - stack analysis
	}

	@Override
	public SaveResult save() {
		SaveResult result = assemblerArea.save();
		if (result == SaveResult.SUCCESS) {
			// TODO: Update target member if needed (user changes member name)
			ignoreNextDisassemble = true;
		}
		return result;
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		if (newValue instanceof ClassInfo) {
			classInfo = (ClassInfo) newValue;
			assemblerArea.onUpdate(classInfo);
			// Skip if we triggered this update
			if (ignoreNextDisassemble)
				return;
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
		assemblerArea.setTargetMember(targetMember);
		// Update tab display
		tab.setText(targetMember.getName());
		if (targetMember.isMethod())
			tab.setGraphic(Icons.getMethodIcon((MethodInfo) targetMember));
		else if (targetMember.isField())
			tab.setGraphic(Icons.getFieldIcon((FieldInfo) targetMember));
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

	private static DockingRootPane docking() {
		return RecafUI.getWindows().getMainWindow().getDockingRootPane();
	}
}
