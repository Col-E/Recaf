package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.Node;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.cafedude.classfile.instruction.BasicInstruction;
import software.coley.cafedude.classfile.instruction.CpRefInstruction;
import software.coley.cafedude.classfile.instruction.IincInstruction;
import software.coley.cafedude.classfile.instruction.Instruction;
import software.coley.cafedude.classfile.instruction.IntOperandInstruction;
import software.coley.cafedude.classfile.instruction.LookupSwitchInstruction;
import software.coley.cafedude.classfile.instruction.MultiANewArrayInstruction;
import software.coley.cafedude.classfile.instruction.Opcodes;
import software.coley.cafedude.classfile.instruction.TableSwitchInstruction;
import software.coley.cafedude.classfile.instruction.WideInstruction;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.SVG;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Registers bytecode instruction values for low-level class-file trees.
 *
 * @author Matt Coley
 * @see JvmLowLevelTreeBuilder
 */
public final class JvmInstructionItems {
	private JvmInstructionItems() {}

	/**
	 * Registers instruction model handlers.
	 *
	 * @param registry
	 * 		Tree item registry.
	 * @param context
	 * 		Per-build rendering context.
	 */
	static void register(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		registerInstruction(registry, Instruction.class, JvmInstructionItems::instructionText, JvmInstructionItems::instructionGraphic,
			(item, value) -> {
			});
		registerInstruction(registry, BasicInstruction.class, JvmInstructionItems::instructionText,
			JvmInstructionItems::instructionGraphic, (item, value) -> {
			});
		registerInstruction(registry, CpRefInstruction.class,
			value -> instructionName(value) + " " + context.cpToString(value.getEntry()),
			value -> context.cpToGraphic(value.getEntry()),
			(item, value) -> registry.add(item, "Entry", value.getEntry()));
		registerInstruction(registry, IincInstruction.class,
			value -> instructionName(value) + " " + value.getVar() + " += " + value.getIncrement(),
			value -> new FontIconView(CarbonIcons.ADD),
			(item, value) -> {
				registry.add(item, "Variable", value.getVar());
				registry.add(item, "Increment", value.getIncrement());
			});
		registerInstruction(registry, IntOperandInstruction.class,
			value -> instructionName(value) + " " + value.getOperand(),
			JvmInstructionItems::intOperandGraphic,
			(item, value) -> registry.add(item, "Operand", value.getOperand()));
		registerInstruction(registry, MultiANewArrayInstruction.class,
			value -> instructionName(value) + " " + context.cpToString(value.getDescriptor()) + " x" + value.getDimensions(),
			value -> Icons.getIconView(Icons.ARRAY),
			(item, value) -> {
				registry.add(item, "Descriptor", value.getDescriptor());
				registry.add(item, "Dimensions", value.getDimensions());
			});
		registerInstruction(registry, LookupSwitchInstruction.class,
			value -> instructionName(value),
			value -> new FontIconView(CarbonIcons.BRANCH),
			(item, value) -> addLookupChildren(registry, item, value));
		registerInstruction(registry, TableSwitchInstruction.class,
			value -> instructionName(value),
			value -> new FontIconView(CarbonIcons.BRANCH),
			(item, value) -> addTableChildren(registry, item, value));
		registerInstruction(registry, WideInstruction.class,
			value -> instructionName(value),
			value -> new FontIconView(CarbonIcons.DRAG_HORIZONTAL),
			(item, value) -> registry.add(item, "Backing", value.getBacking()));
	}

	private static <E extends Instruction> void registerInstruction(@Nonnull ClassItemRegistry registry,
	                                                                @Nonnull Class<E> type,
	                                                                @Nonnull Function<E, String> text,
	                                                                @Nonnull Function<E, Node> graphic,
	                                                                @Nonnull BiConsumer<ClassItem, E> children) {
		ClassItemRegistry.Definition<E> definition = registry.register(type)
				.text(text)
				.graphic(graphic)
				.children((item, value) -> addBaseChildren(registry, item, value));
		definition.children(children);
	}

	private static void addBaseChildren(@Nonnull ClassItemRegistry registry, @Nonnull ClassItem item,
	                                   @Nonnull Instruction instruction) {
		registry.add(item, "Opcode", instruction.getOpcode() + " (" + instructionName(instruction) + ")");
		registry.add(item, "Size", instruction.computeSize());
	}


	@Nonnull
	private static String instructionText(@Nonnull Instruction instruction) {
		return instructionName(instruction);
	}

	@Nonnull
	private static String instructionName(@Nonnull Instruction instruction) {
		return AsmInsnUtil.getInsnName(instruction.getOpcode());
	}

	@Nonnull
	private static Node instructionGraphic(@Nonnull Instruction instruction) {
		if (instruction instanceof BasicInstruction basic)
			return basicGraphic(basic.getOpcode());
		if (instruction instanceof CpRefInstruction)
			return Icons.getIconView(Icons.CLASS);
		if (instruction instanceof IincInstruction)
			return new FontIconView(CarbonIcons.ADD);
		if (instruction instanceof IntOperandInstruction operand)
			return intOperandGraphic(operand);
		if (instruction instanceof MultiANewArrayInstruction)
			return Icons.getIconView(Icons.ARRAY);
		if (instruction instanceof LookupSwitchInstruction || instruction instanceof TableSwitchInstruction)
			return new FontIconView(CarbonIcons.BRANCH);
		return new FontIconView(CarbonIcons.DRAG_HORIZONTAL);
	}

	@Nonnull
	private static Node intOperandGraphic(@Nonnull IntOperandInstruction instruction) {
		return basicGraphic(instruction.getOpcode());
	}

	@Nonnull
	private static Node basicGraphic(int opcode) {
		// Constant numbers.
		if (opcode <= Opcodes.SIPUSH)
			return new FontIconView(CarbonIcons.STRING_INTEGER);

		// Variable and array loads.
		if (opcode <= Opcodes.ALOAD_3)
			return SVG.ofIconFile(SVG.REF_READ);
		if (opcode <= Opcodes.SALOAD)
			return Icons.getIconView(Icons.ARRAY);

		// Variable and array stores.
		if (opcode <= Opcodes.ASTORE_3)
			return SVG.ofIconFile(SVG.REF_WRITE);
		if (opcode <= Opcodes.SASTORE)
			return Icons.getIconView(Icons.ARRAY);

		// Stack operations.
		if (opcode <= Opcodes.SWAP)
			return new FontIconView(CarbonIcons.STACKED_SCROLLING_1);

		// Math operations.
		if (opcode <= Opcodes.LXOR)
			return new FontIconView(CarbonIcons.CALCULATOR);

		// Primitive conversions.
		if (opcode <= Opcodes.I2S)
			return new FontIconView(CarbonIcons.DATA_SHARE);

		// Stack value comparisons.
		if (opcode <= Opcodes.DCMPG)
			return new FontIconView(CarbonIcons.CALCULATOR);

		// Returns and exceptional control flow.
		if (opcode <= Opcodes.RETURN)
			return new FontIconView(CarbonIcons.EXIT, javafx.scene.paint.Color.STEELBLUE);
		if (opcode == Opcodes.ATHROW)
			return Icons.getIconView(Icons.CLASS_EXCEPTION);
		if (opcode == Opcodes.ARRAYLENGTH)
			return Icons.getIconView(Icons.ARRAY);

		// Remaining monitor/control-flow operations use the existing magnify icon.
		return new FontIconView(CarbonIcons.MAGNIFY);
	}

	private static void addLookupChildren(@Nonnull ClassItemRegistry registry, @Nonnull ClassItem item,
	                                     @Nonnull LookupSwitchInstruction instruction) {
		registry.add(item, "Default", instruction.getDefault());
		registry.add(item, "Padding", instruction.getPadding());
		List<Integer> keys = instruction.getKeys();
		List<Integer> offsets = instruction.getOffsets();
		List<SwitchPair> pairs = new ArrayList<>(Math.max(keys.size(), offsets.size()));
		for (int i = 0; i < Math.max(keys.size(), offsets.size()); i++)
			pairs.add(new SwitchPair(i < keys.size() ? keys.get(i) : null, i < offsets.size() ? offsets.get(i) : null));
		ClassItem pairContainer = registry.add(item, "Pairs", pairs, definition -> definition
				.text(values -> "[" + values.size() + "]")
				.graphic(values -> Icons.getIconView(Icons.ARRAY)));
		for (int i = 0; i < pairs.size(); i++) {
			SwitchPair pair = pairs.get(i);
			ClassItem pairItem = registry.add(pairContainer, "[" + i + "]", pair, definition -> definition
					.text(JvmInstructionItems::pairText)
					.graphic(value -> new FontIconView(CarbonIcons.BRANCH))
					.children((parent, value) -> {
						registry.addNullable(parent, "Key", value.key());
						registry.addNullable(parent, "Offset", value.offset());
					}));
		}
	}

	private static void addTableChildren(@Nonnull ClassItemRegistry registry, @Nonnull ClassItem item,
	                                    @Nonnull TableSwitchInstruction instruction) {
		registry.add(item, "Default", instruction.getDefault());
		registry.add(item, "Low", instruction.getLow());
		registry.add(item, "High", instruction.getHigh());
		registry.add(item, "Padding", instruction.getPadding());
		List<Integer> offsets = instruction.getOffsets();
		ClassItem offsetContainer = registry.add(item, "Offsets", offsets, definition -> definition
				.text(values -> "[" + values.size() + "]")
				.graphic(values -> Icons.getIconView(Icons.ARRAY)));
		for (int i = 0; i < offsets.size(); i++)
			registry.add(offsetContainer, "[" + i + "]", offsets.get(i));
	}

	@Nonnull
	private static String pairText(@Nonnull SwitchPair pair) {
		return valueOrNull(pair.key()) + " -> " + valueOrNull(pair.offset());
	}

	@Nonnull
	private static String valueOrNull(@Nullable Integer value) {
		return value == null ? "<null>" : String.valueOf(value);
	}

	private record SwitchPair(@Nullable Integer key, @Nullable Integer offset) {
	}
}
