package me.coley.recaf.ui.dialog;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.value.TopValue;
import dev.xdark.ssvm.value.Value;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.scripting.impl.WorkspaceAPI;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.ssvm.processing.FlowRevisitingProcessors;
import me.coley.recaf.ssvm.processing.PeepholeProcessors;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic SSVM method caller dialog.
 *
 * @author Matt Coley
 */
public class SsvmOptimizeDialog extends SsvmCommonDialog {
	private static final Logger logger = Logging.get(SsvmOptimizeDialog.class);
	private final VirtualMachine vm;

	/**
	 * @param owner
	 * 		Method owner.
	 * @param info
	 * 		Method info.
	 * @param ssvm
	 * 		SSVM integration service.
	 */
	public SsvmOptimizeDialog(CommonClassInfo owner, MethodInfo info, SsvmIntegration ssvm) {
		super(Lang.getBinding("dialog.title.vm-peephole-invoke-args"), owner, info, ssvm);
		this.vm = ssvm.createVM(true, vm -> {
			PeepholeProcessors.installValuePushing(vm);
			PeepholeProcessors.installOperationFolding(vm);
			PeepholeProcessors.installReturnValueFolding(vm);
			FlowRevisitingProcessors.installBranchingProcessor(vm, ctx ->
					ctx.getOwner().getInternalName().equals(owner.getName()) &&
							ctx.getMethod().getName().equals(info.getName()));
		});
		Button runButton = new Button();
		runButton.textProperty().bind(Lang.getBinding("dialog.vm.execute"));
		runButton.setGraphic(Icons.getIconView(Icons.PLAY));
		runButton.setDisable(!validInputs());
		runButton.setOnMousePressed(e -> {
			// Only create values when confirm button pressed
			for (InputWrapper input : inputs) {
				Value value = input.supplier.get();
				values[input.slot] = value;
				if (value.isWide()) {
					values[input.slot + 1] = TopValue.INSTANCE;
				}
			}
			// Run and get result
			Future<SsvmIntegration.VmRunResult> resultFuture = ssvm.runMethod(vm, owner, info, getValues());
			ThreadUtil.run(() -> {
				try {
					resultFuture.get(1, TimeUnit.MINUTES);
					// Pull new bytecode from VM
					InstanceJavaClass target = (InstanceJavaClass) vm.findBootstrapClass(owner.getName());
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
					target.getNode().accept(writer);
					byte[] modified = writer.toByteArray();
					// Replace in workspace
					WorkspaceAPI.getPrimaryResource().getClasses().put(owner.getName(), ClassInfo.read(modified));
				} catch (InterruptedException ex) {
					logger.error("Invoke future thread interrupted", ex);
				} catch (ExecutionException ex) {
					logger.error("Invoke future thread encountered unhandled error", ex.getCause());
				} catch (TimeoutException ex) {
					logger.error("Invoke future thread timed out", ex);
				}
			});
		});
		runButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		GridPane.setFillWidth(runButton, true);
		grid.add(runButton, 0, grid.getRowCount(), 3, 1);
		// Run state tied to validity
		totality.addListener((observable, oldValue, newValue) -> runButton.setDisable(!newValue));
	}
}
