package me.coley.recaf.ui.dialog;

import dev.xdark.ssvm.execution.VMException;
import dev.xdark.ssvm.memory.management.MemoryManager;
import dev.xdark.ssvm.mirror.InstanceJavaClass;
import dev.xdark.ssvm.mirror.JavaClass;
import dev.xdark.ssvm.symbol.VMPrimitives;
import dev.xdark.ssvm.symbol.VMSymbols;
import dev.xdark.ssvm.util.VMHelper;
import dev.xdark.ssvm.value.ArrayValue;
import dev.xdark.ssvm.value.InstanceValue;
import dev.xdark.ssvm.value.Value;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.ssvm.IntegratedVirtualMachine;
import me.coley.recaf.ssvm.VmUtil;
import me.coley.recaf.ssvm.value.ConstNumericValue;
import me.coley.recaf.ssvm.value.ConstStringValue;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.UncheckedConsumer;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Basic SSVM method caller dialog.
 *
 * @author Matt Coley
 */
public abstract class SsvmCommonDialog extends ClosableDialog {
	protected final BooleanProperty totality = new SimpleBooleanProperty();
	protected final List<InputWrapper> inputs = new ArrayList<>();
	protected final SsvmIntegration ssvm;
	protected final CommonClassInfo owner;
	protected final MethodInfo info;
	protected IntegratedVirtualMachine vm;
	protected Value[] values;
	protected VMHelper helper;
	protected VMSymbols symbols;
	protected VMPrimitives primitives;
	protected MemoryManager memory;
	protected VmUtil vmUtil;

	/**
	 * @param title
	 * 		Title binding.
	 * @param owner
	 * 		Method owner.
	 * @param info
	 * 		Method info.
	 * @param ssvm
	 * 		SSVM integration service.
	 */
	public SsvmCommonDialog(StringBinding title, CommonClassInfo owner, MethodInfo info, SsvmIntegration ssvm) {
		super(title,
				new StringBinding() {
					@Override
					protected String computeValue() {
						return StringUtil.shortenPath(owner.getName()) + "." + info.getName() + info.getDescriptor();
					}
				},
				Icons.getMethodIcon(info));
		this.ssvm = ssvm;
		this.owner = owner;
		this.info = info;
		initModality(Modality.NONE);
		initVm();
		setup();
	}

	protected abstract void initVm();

	private void setup() {
		helper = vm.getHelper();
		symbols = vm.getSymbols();
		primitives = vm.getPrimitives();
		memory = vm.getMemoryManager();
		vmUtil = VmUtil.create(vm);
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
		totality.bind(Bindings.createBooleanBinding(
				this::validInputs,
				inputs.stream().map(x -> x.valid).toArray(BooleanProperty[]::new)
		));
		values = new Value[argSlot];
		if (!isStatic) {
			try {
				JavaClass type = helper.tryFindClass(symbols.java_lang_Object().getClassLoader(), owner.getName(), true);
				InstanceValue instance = memory.newInstance((InstanceJavaClass) type);
				values[0] = instance;
			} catch (VMException ex) {
				String reason = vmUtil.throwableToString(ex.getOop());
				throw new RuntimeException(reason, vm.getHelper().toJavaException(ex.getOop()));
			}
		}
	}

	/**
	 * @return {@code true} when all inputs are valid.
	 */
	public boolean validInputs() {
		return inputs.stream().allMatch(i -> i.valid.get());
	}

	/**
	 * @return Last array of input parameter values.
	 */
	public Value[] getValues() {
		return values;
	}

	protected BooleanProperty newProperty(TextField field, UncheckedConsumer<String> consumer) {
		BooleanProperty property = new SimpleBooleanProperty(true);
		property.bind(Bindings.createBooleanBinding(
				() -> {
					try {
						consumer.accept(field.getText());
						return true;
					} catch (Throwable t) {
						return false;
					}
				},
				field.textProperty()
		));
		return property;
	}

	/**
	 * @return Editor for integers, shorts, chars, bytes, and booleans.
	 */
	protected InputWrapper intEditor() {
		TextField field = new TextField("0");
		return new InputWrapper(newProperty(field, Integer::parseInt), field, text -> ConstNumericValue.ofInt(Integer.parseInt(text)));
	}

	/**
	 * @return Editor for floats.
	 */
	protected InputWrapper floatEditor() {
		TextField field = new TextField("0");
		return new InputWrapper(newProperty(field, Float::parseFloat), field, text -> ConstNumericValue.ofFloat(Float.parseFloat(text)));
	}

	/**
	 * @return Editor for longs.
	 */
	protected InputWrapper longEditor() {
		TextField field = new TextField("0");
		return new InputWrapper(newProperty(field, Long::parseLong), field, text -> ConstNumericValue.ofLong(Long.parseLong(text)));
	}

	/**
	 * @return Editor for doubles.
	 */
	protected InputWrapper doubleEditor() {
		TextField field = new TextField("0.0");
		return new InputWrapper(newProperty(field, Double::parseDouble), field, text -> ConstNumericValue.ofDouble(Double.parseDouble(text)));
	}

	/**
	 * @param type
	 * 		Array type.
	 *
	 * @return Editor for the array type.
	 */
	protected InputWrapper arrayEditor(Type type) {
		Type element = type.getElementType();
		switch (element.getSort()) {
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT: {
				TextField field = new TextField("1, 2, 3");
				return new InputWrapper(newProperty(field, input -> {
					for (String part : input.split("[, ]+"))
						Integer.parseInt(part);
				}), field, text -> {
					if (text.isBlank())
						switch (element.getSort()) {
							case Type.BOOLEAN:
								return helper.emptyArray(primitives.booleanPrimitive());
							case Type.CHAR:
								return helper.emptyArray(primitives.charPrimitive());
							case Type.BYTE:
								return helper.emptyArray(primitives.bytePrimitive());
							case Type.SHORT:
								return helper.emptyArray(primitives.shortPrimitive());
							case Type.INT:
								return helper.emptyArray(primitives.intPrimitive());
						}
					String[] args = text.split("\\s*,\\s*");
					int count = args.length;
					ArrayValue array;
					switch (element.getSort()) {
						case Type.BOOLEAN:
							array = helper.newArray(primitives.booleanPrimitive(), count);
							for (int i = 0; i < count; i++)
								array.setBoolean(i, Integer.parseInt(args[i]) != 0);
							break;
						case Type.CHAR:
							array = helper.newArray(primitives.charPrimitive(), count);
							for (int i = 0; i < count; i++)
								array.setChar(i, (char) Integer.parseInt(args[i]));
							break;
						case Type.BYTE:
							array = helper.newArray(primitives.bytePrimitive(), count);
							for (int i = 0; i < count; i++)
								array.setByte(i, (byte) Integer.parseInt(args[i]));
							break;
						case Type.SHORT:
							array = helper.newArray(primitives.shortPrimitive(), count);
							for (int i = 0; i < count; i++)
								array.setShort(i, (short) Integer.parseInt(args[i]));
							break;
						case Type.INT:
							array = helper.newArray(primitives.intPrimitive(), count);
							for (int i = 0; i < count; i++)
								array.setInt(i, Integer.parseInt(args[i]));
							break;
						default:
							throw new IllegalStateException("Unsupported 'int' type: " + element.getInternalName());
					}
					return array;
				});
			}
			case Type.FLOAT: {
				TextField field = new TextField("1, 2, 3");
				return new InputWrapper(newProperty(field, input -> {
					for (String part : input.split("[, ]+"))
						Float.parseFloat(part);
				}), field, text -> {
					if (text.isBlank())
						return helper.emptyArray(primitives.floatPrimitive());
					String[] args = text.split("\\s*,\\s*");
					int count = args.length;
					ArrayValue array = helper.newArray(primitives.floatPrimitive(), count);
					for (int i = 0; i < count; i++)
						array.setFloat(i, Float.parseFloat(args[i]));
					return array;
				});
			}
			case Type.LONG: {
				TextField field = new TextField("1, 2, 3");
				return new InputWrapper(newProperty(field, input -> {
					for (String part : input.split("[, ]+"))
						Long.parseLong(part);
				}), field, text -> {
					if (text.isBlank())
						return helper.emptyArray(primitives.longPrimitive());
					String[] args = text.split("\\s*,\\s*");
					int count = args.length;
					ArrayValue array = helper.newArray(primitives.longPrimitive(), count);
					for (int i = 0; i < count; i++)
						array.setLong(i, Long.parseLong(args[i]));
					return array;
				});
			}
			case Type.DOUBLE: {
				TextField field = new TextField("1.0, 2.0, 3.0");
				return new InputWrapper(newProperty(field, input -> {
					for (String part : input.split("[, ]+"))
						Double.parseDouble(part);
				}), field, text -> {
					if (text.isBlank())
						return helper.emptyArray(primitives.doublePrimitive());
					String[] args = text.split("\\s*,\\s*");
					int count = args.length;
					ArrayValue array = helper.newArray(primitives.doublePrimitive(), count);
					for (int i = 0; i < count; i++)
						array.setDouble(i, Double.parseDouble(args[i]));
					return array;
				});
			}
			case Type.OBJECT: {
				if (Types.STRING_TYPE.equals(element)) {
					TextField field = new TextField("\"one\", \"two\", \"three\"");
					return new InputWrapper(field, text -> {
						if (text.isBlank())
							return helper.emptyArray(symbols.java_lang_String());
						String[] args = text.split("^\"|\"\\s*,\\s*\"|\"$");
						int count = args.length;
						ArrayValue array = helper.newArray(symbols.java_lang_String(), count);
						for (int i = 0; i < count; i++)
							array.setValue(i, helper.newUtf8(args[i]));
						return array;
					});
				}
			}
			default:
				Label field = new Label("null");
				return new InputWrapper(field, unused -> memory.nullValue());
		}
	}

	/**
	 * @param type
	 * 		Object type.
	 *
	 * @return Editor for the type.
	 */
	protected InputWrapper objectEditor(Type type) {
		if (Types.STRING_TYPE.equals(type)) {
			TextField field = new TextField("string_text");
			return new InputWrapper(field, text -> ConstStringValue.ofString(helper, text));
		}
		CheckBox checkCreateDefault = new CheckBox();
		checkCreateDefault.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				checkCreateDefault.textProperty().bind(Lang.getBinding("dialog.vm.create-dummy"));
			} else {
				checkCreateDefault.textProperty().bind(Lang.getBinding("dialog.vm.create-null"));
			}
		});
		checkCreateDefault.setSelected(true);
		return new InputWrapper(checkCreateDefault, unused -> {
			if (checkCreateDefault.isSelected()) {
				InstanceJavaClass cls = (InstanceJavaClass) vm.findBootstrapClass(type.getInternalName(), true);
				if (cls != null)
					return memory.newInstance(cls);
			}
			return memory.nullValue();
		});
	}

	/**
	 * Updates {@link #totality} validity.
	 *
	 * @param valid
	 * 		Validity property of the editor.
	 * @param input
	 * 		Text input on text edit.
	 * @param action
	 * 		Action to run, throwing an exception if invalid.
	 */
	protected void handle(BooleanProperty valid, String input, UncheckedConsumer<String> action) {
		try {
			action.accept(input);
			valid.set(true);
		} catch (Throwable t) {
			valid.set(false);
		}
	}

	protected String encodeThrowable(Throwable t) {
		if (t instanceof VMException) {
			return vmUtil.throwableToString(((VMException) t).getOop());
		}
		return StringUtil.traceToString(t);
	}

	protected static class InputWrapper {
		protected final Node editor;
		protected final Function<String, Value> supplier;
		protected final BooleanProperty valid;
		protected int slot;

		public InputWrapper(BooleanProperty valid, Node editor, Function<String, Value> supplier) {
			this.valid = valid;
			this.editor = editor;
			this.supplier = supplier;
		}

		public InputWrapper(Node editor, Function<String, Value> supplier) {
			this(new SimpleBooleanProperty(true), editor, supplier);
		}

		public Value toValue() {
			String text = null;
			if (editor instanceof TextField) {
				text = ((TextField) editor).getText();
			} else if (editor instanceof Label) {
				text = ((Label) editor).getText();
			}
			// 'editor' can be something else, but typically means text input goes ignored, so 'null' is fine.
			return supplier.apply(text);
		}
	}
}
