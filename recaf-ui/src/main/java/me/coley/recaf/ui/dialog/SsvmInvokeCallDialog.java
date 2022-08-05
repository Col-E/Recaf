package me.coley.recaf.ui.dialog;

import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.value.TopValue;
import dev.xdark.ssvm.value.Value;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.ssvm.VmRunResult;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Basic SSVM method caller dialog.
 *
 * @author Matt Coley
 */
public class SsvmInvokeCallDialog extends SsvmCommonDialog {
	private static final Logger logger = Logging.get(SsvmInvokeCallDialog.class);

	/**
	 * @param owner
	 * 		Method owner.
	 * @param info
	 * 		Method info.
	 * @param ssvm
	 * 		SSVM integration service.
	 */
	public SsvmInvokeCallDialog(CommonClassInfo owner, MethodInfo info, SsvmIntegration ssvm) {
		super(Lang.getBinding("dialog.title.vm-invoke-args"), owner, info, ssvm);
		TextArea output = new TextArea();
		Button runButton = new Button();
		runButton.textProperty().bind(Lang.getBinding("dialog.vm.execute"));
		runButton.setGraphic(Icons.getIconView(Icons.PLAY));
		runButton.setOnMousePressed(e -> {
			// Only create values when confirm button pressed
			for (InputWrapper input : inputs) {
				Value value = input.toValue();
				values[input.slot] = value;
				if (value.isWide()) {
					values[input.slot + 1] = TopValue.INSTANCE;
				}
			}
			// Run and get result
			CompletableFuture<VmRunResult> resultFuture = SsvmIntegration.runMethod(vm, owner, info, getValues());
			resultFuture.orTimeout(1L, TimeUnit.MINUTES)
					.whenComplete((result, throwable) -> {
						if (throwable != null) {
							if (throwable instanceof TimeoutException) {
								logger.error("Invoke future thread timed out");
							} else {
								logger.error("Invoke future thread encountered unhandled error:\n{}", encodeThrowable(throwable));
							}
							return;
						}
						throwable = result.getException();
						if (throwable != null) {
							String errorText = encodeThrowable(throwable);
							FxThreadUtil.run(() -> {
								output.setText(errorText);
								output.setStyle("-fx-text-fill: red;");
							});
						} else {
							String resultText = vm.getVmUtil().toString(result.getValue());
							FxThreadUtil.run(() -> {
								output.setStyle(null);
								output.setText(resultText);
							});
						}
					});
		});
		runButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		GridPane.setFillWidth(runButton, true);
		grid.add(runButton, 0, grid.getRowCount(), 3, 1);
		grid.add(output, 0, grid.getRowCount(), 3, 1);
		// Run state tied to validity
		runButton.disableProperty().bind(totality.not());
	}

	@Override
	protected void initVm() {
		// Use primary VM
		vm = ssvm.getVm();
	}
}
