package me.coley.recaf.ui.dialog;

import dev.xdark.ssvm.VirtualMachine;
import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.mirror.JavaClass;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.NullValue;
import dev.xdark.ssvm.value.TopValue;
import dev.xdark.ssvm.value.Value;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.ssvm.value.ConstStringValue;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Basic SSVM method caller dialog.
 *
 * @author Matt Coley
 */
public class SsvmInvokeCallDialog extends ClosableDialog {
	private final BooleanProperty totality = new SimpleBooleanProperty();
	private final List<InputWrapper> inputs = new ArrayList<>();
	private final SsvmIntegration ssvm;
	private final Value[] values;

	/**
	 * @param owner
	 * 		Method owner.
	 * @param info
	 * 		Method info.
	 * @param ssvm
	 * 		SSVM integration service.
	 */
	public SsvmInvokeCallDialog(CommonClassInfo owner, MethodInfo info, SsvmIntegration ssvm) {
		super(Lang.getBinding("dialog.title.vm-invoke-args"),
				Lang.getBinding("dialog.header.vm-invoke-args"),
				Icons.getImageView(Icons.PLAY));
		this.ssvm = ssvm;
		grid.addRow(0, new Label("Parameter"), new Label("Type"), new Label("Editor"));
		Type methodType = Type.getMethodType(info.getDescriptor());
		Type[] methodArgs = methodType.getArgumentTypes();
		boolean isStatic = AccessFlag.isStatic(info.getAccess());
		int argSlot = isStatic ? 0 : 1;
		for (int i = 0; i < methodArgs.length; i++) {
			Type arg = methodArgs[i];
			Label index = new Label(String.valueOf(i));
			Label type = new Label(arg.getDescriptor());
			InputWrapper input;
			switch (arg.getSort()) {
				case Type.BOOLEAN:
				case Type.CHAR:
				case Type.BYTE:
				case Type.SHORT:
				case Type.INT:
					input = intEditor();
					break;
				case Type.FLOAT:
					input = floatEditor();
					break;
				case Type.LONG:
					input = longEditor();
					break;
				case Type.DOUBLE:
					input = doubleEditor();
					break;
				case Type.ARRAY:
					input = arrayEditor(arg);
					break;
				case Type.OBJECT:
					input = objectEditor(arg);
					break;
				default:
					throw new IllegalStateException("Unsupported arg type: " + arg);
			}
			input.slot = argSlot;
			inputs.add(input);
			grid.addRow(i + 1, index, type, input.editor);
			argSlot += arg.getSize();
		}
		values = new Value[argSlot];
		if (!isStatic) {
			VirtualMachine vm = ssvm.getVm();
			VMHelper helper = vm.getHelper();
			JavaClass type = helper.tryFindClass(vm.getSymbols().java_lang_Object.getClassLoader(), owner.getName(), true);
			InstanceValue instance = vm.getMemoryManager().newInstance((InstanceJavaClass) type);
			helper.initializeDefaultValues(instance);
			values[0] = instance;
		}
		TextArea output = new TextArea();
		Button runButton = new Button("Execute");
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
			//
			SsvmIntegration.VmRunResult result = ssvm.runMethod(owner, info, getValues());
			if (result.hasError()) {
				Exception ex = result.getException();
				if (ex instanceof VMException) {
					ex = ssvm.getVm().getHelper().toJavaException(((VMException) ex).getOop());
				}
				output.setText(StringUtil.traceToString(ex));
				output.setStyle("-fx-text-fill: red;");
			} else {
				output.setStyle(null);
				String toString = result.getValue().toString();
				if (result.getValue() instanceof InstanceValue) {
					InstanceValue value = (InstanceValue) result.getValue();
					if (value.getJavaClass().getInternalName().equals("java/lang/String")) {
						toString = ssvm.getVm().getHelper().readUtf8(result.getValue());
					}
				}
				output.setText(toString);
			}
		});
		grid.add(runButton, 0, methodArgs.length + 1);
		grid.add(output, 0, methodArgs.length + 2, 3, 1);

		totality.addListener((observable, oldValue, newValue) -> runButton.setDisable(!newValue));
	}

	/**
	 * @return {@code true} when all inputs are valid.
	 */
	public boolean validInputs() {
		return inputs.stream().allMatch(i -> i.valid.getAsBoolean());
	}

	/**
	 * @return Last array of input parameter values.
	 */
	public Value[] getValues() {
		return values;
	}

	private InputWrapper intEditor() {
		BooleanProperty valid = new SimpleBooleanProperty(true);
		TextField field = new TextField("0");
		field.textProperty().addListener((observable, oldValue, newValue) -> {
			try {
				Integer.parseInt(newValue);
				valid.set(true);
			} catch (NumberFormatException nfe) {
				valid.set(false);
			}
			totality.set(validInputs());
		});
		return new InputWrapper(valid::get, field, () -> ConstNumericValue.ofInt(Integer.parseInt(field.getText())));
	}

	private InputWrapper floatEditor() {
		BooleanProperty valid = new SimpleBooleanProperty(true);
		TextField field = new TextField("0");
		field.textProperty().addListener((observable, oldValue, newValue) -> {
			try {
				Float.parseFloat(newValue);
				valid.set(true);
			} catch (NumberFormatException nfe) {
				valid.set(false);
			}
			totality.set(validInputs());
		});
		return new InputWrapper(valid::get, field, () -> ConstNumericValue.ofFloat(Float.parseFloat(field.getText())));
	}

	private InputWrapper longEditor() {
		BooleanProperty valid = new SimpleBooleanProperty(true);
		TextField field = new TextField("0");
		field.textProperty().addListener((observable, oldValue, newValue) -> {
			try {
				Long.parseLong(newValue);
				valid.set(true);
			} catch (NumberFormatException nfe) {
				valid.set(false);
			}
			totality.set(validInputs());
		});
		return new InputWrapper(valid::get, field, () -> ConstNumericValue.ofLong(Long.parseLong(field.getText())));
	}

	private InputWrapper doubleEditor() {
		BooleanProperty valid = new SimpleBooleanProperty(true);
		TextField field = new TextField("0.0");
		field.textProperty().addListener((observable, oldValue, newValue) -> {
			try {
				Double.parseDouble(newValue);
				valid.set(true);
			} catch (NumberFormatException nfe) {
				valid.set(false);
			}
			totality.set(validInputs());
		});
		return new InputWrapper(valid::get, field, () -> ConstNumericValue.ofDouble(Double.parseDouble(field.getText())));
	}

	private InputWrapper arrayEditor(Type type) {
		Label field = new Label("null");
		// helper.emptyArray(symbols.java_lang_String)
		return new InputWrapper(field, () -> NullValue.INSTANCE);
	}

	private InputWrapper objectEditor(Type type) {
		if (Types.STRING_TYPE.equals(type)) {
			TextField field = new TextField("");
			return new InputWrapper(field, () -> ConstStringValue.ofString(ssvm.getVm().getHelper(), field.getText()));
		}
		Label field = new Label("null");
		return new InputWrapper(field, () -> NullValue.INSTANCE);
	}

	private static class InputWrapper {
		private final Node editor;
		private final Supplier<Value> supplier;
		private BooleanSupplier valid;
		private int slot;


		public InputWrapper(BooleanSupplier valid, Node editor, Supplier<Value> supplier) {
			this.valid = valid;
			this.editor = editor;
			this.supplier = supplier;
		}

		public InputWrapper(Node editor, Supplier<Value> supplier) {
			this(() -> true, editor, supplier);
		}
	}
}
