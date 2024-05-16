package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.Location;
import org.reactfx.Change;
import software.coley.collections.box.Box;
import software.coley.collections.box.IntBox;
import software.coley.recaf.ui.control.VirtualizedScrollPaneWrapper;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.SceneUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Dependent
public class JumpArrowPane extends AstBuildConsumerComponent {
	private static final Set<String> INSN_SET = Set.of("goto", "ifnull", "ifnonnull", "ifeq", "ifne", "ifle", "ifge", "iflt", "ifgt",
			"if_acmpeq", "if_acmpne", "if_icmpeq", "if_icmpge", "if_icmpgt", "if_icmple", "if_icmplt", "if_icmpne");

	private final Consumer<Change<Integer>> onCaretMove = this::onCaretMove;
	private final JumpArrowFactory arrowFactory = new JumpArrowFactory();
	private List<LabelData> model = Collections.emptyList();

	// TODO: Make config for switching between all labels and just selected labels being targeted

	@Inject
	public JumpArrowPane() {
		arrowFactory.active.addListener((ob, old, cur) -> {
			if (editor != null)
				editor.redrawParagraphGraphics();
		});
	}

	@Override
	public void install(@Nonnull Editor editor) {
		super.install(editor);

		editor.getRootLineGraphicFactory().addLineGraphicFactory(arrowFactory);
		editor.getCaretPosEventStream().addObserver(onCaretMove);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		super.uninstall(editor);

		editor.getRootLineGraphicFactory().removeLineGraphicFactory(arrowFactory);
		editor.getCaretPosEventStream().removeObserver(onCaretMove);
	}

	@Override
	protected void onClassSelected() {
		clearData();
	}

	@Override
	protected void onMethodSelected() {
		updateModel();
	}

	@Override
	protected void onFieldSelected() {
		clearData();
	}

	@Override
	protected void onPipelineOutputUpdate() {
		// Keep a reference to the old model.
		List<LabelData> oldModel = model;

		// Update the model.
		updateModel();

		// If the model has changed, refresh the visible paragraph graphics.
		// This can mean a new label as added, new reference to one, etc.
		// This could result in new line shapes, so redrawing them all is wise.
		List<LabelData> newModel = model;
		if (!Objects.equals(oldModel, newModel))
			FxThreadUtil.run(() -> editor.redrawParagraphGraphics());
	}

	/**
	 * Handles updating the {@link JumpArrowFactory}.
	 * <p/>
	 * This logic is shoe-horned into here <i>(for now)</i> because
	 * the variable tracking logic is internal to this class only.
	 *
	 * @param caretChange
	 * 		Caret pos change.
	 */
	private void onCaretMove(Change<Integer> caretChange) {
		int pos = editor.getCodeArea().getCaretPosition();
		int line = editor.getCodeArea().getCurrentParagraph() + 1;

		Box<ASTElement> selected = new Box<>();
		for (ASTElement element : astElements) {
			if (element.range().within(pos)) {
				element.walk(ast -> {
					if (ast instanceof ASTInstruction instruction) {
						Location location = ast.location();
						if (location != null && location.line() == line)
							selected.set(ast);
						else {
							String identifier = instruction.identifier().content();
							if (("tableswitch".equals(identifier) || "lookupswitch".equals(identifier)) && ast.range().within(pos)) {
								selected.set(ast);
							}
						}
					}
					return !selected.isSet();
				});
			}
		}

		ASTElement current = selected.get();
		boolean active = false;
		if (current instanceof ASTLabel) {
			active = true;
		} else if (current instanceof ASTInstruction instruction) {
			String insnName = instruction.identifier().content();
			List<ASTElement> arguments = instruction.arguments();
			if (!arguments.isEmpty() && INSN_SET.contains(insnName) ||
					"tableswitch".equals(insnName) || "lookupswitch".equals(insnName)) {
				active = true;
			}
		}
		arrowFactory.active.setValue(active);
	}

	private void updateModel() {
		// Collect all label usage information from the AST.
		AstUsages emptyUsage = AstUsages.EMPTY_USAGE;
		Map<String, AstUsages> labelUsages = new HashMap<>();
		BiConsumer<String, ASTElement> readUpdater = (name, element) -> {
			AstUsages existing = labelUsages.getOrDefault(name, emptyUsage);
			labelUsages.put(name, existing.withNewRead(element));
		};
		BiConsumer<String, ASTElement> writeUpdater = (name, element) -> {
			AstUsages existing = labelUsages.getOrDefault(name, emptyUsage);
			labelUsages.put(name, existing.withNewWrite(element));
		};
		if (astElements != null) {
			Consumer<ASTMethod> methodConsumer = astMethod -> {
				if (currentMethod != null && !Objects.equals(currentMethod.getName(), astMethod.getName().literal()))
					return;
				for (ASTInstruction instruction : astMethod.code().instructions()) {
					if (instruction instanceof ASTLabel label) {
						readUpdater.accept(label.identifier().content(), label);
					} else {
						String insnName = instruction.identifier().content();
						List<ASTElement> arguments = instruction.arguments();
						if (!arguments.isEmpty()) {
							if (INSN_SET.contains(insnName)) {
								writeUpdater.accept(arguments.getLast().content(), instruction);
							} else if ("tableswitch".equals(insnName)) {
								if (!instruction.arguments().isEmpty() && instruction.arguments().getFirst() instanceof ASTObject switchObj) {
									ASTArray cases = switchObj.value("cases");
									ASTElement defaultCase = switchObj.value("default");
									cases.values().forEach(caseAst -> writeUpdater.accept(caseAst.content(), caseAst));
									writeUpdater.accept(defaultCase.content(), defaultCase);
								}
							} else if ("lookupswitch".equals(insnName)) {
								if (!instruction.arguments().isEmpty() && instruction.arguments().getFirst() instanceof ASTObject switchObj) {
									ASTElement defaultCase = switchObj.value("default");
									writeUpdater.accept(defaultCase.content(), defaultCase);
									switchObj.values().pairs().forEach(pair -> {
										writeUpdater.accept(pair.second().content(), pair.first());
									});
								}
							}
						}
					}
				}
			};
			for (ASTElement astElement : astElements) {
				if (astElement instanceof ASTMethod astMethod) {
					methodConsumer.accept(astMethod);
				} else if (astElement instanceof ASTClass astClass) {
					for (ASTElement child : astClass.children()) {
						if (child instanceof ASTMethod astMethod) {
							methodConsumer.accept(astMethod);
						}
					}
				}
			}
		}
		model = labelUsages.entrySet().stream()
				.filter(e -> !e.getValue().readers().isEmpty()) // Must have a label declaration
				.map(e -> new LabelData(e.getKey(), e.getValue(), new IntBox(-1), new Box<>()))
				.sorted(Comparator.comparing(LabelData::name))
				.toList();
		model.forEach(data -> {
			List<LabelData> overlapping = data.computeOverlapping(model);
			IntBox slot = data.lineSlot();
			int slotIndex = 0;
			while (true) {
				incr:
				{
					for (LabelData d : overlapping) {
						if (slotIndex == d.lineSlot().get()) {
							slotIndex++;
							break incr;
						}
					}
					break;
				}
			}
			slot.set(slotIndex);
		});
	}

	private void clearData() {
		model = Collections.emptyList();
		currentMethod = null;
	}

	/**
	 * Highlighter which shows read and write access of a {@link LabelData}.
	 */
	private class JumpArrowFactory extends AbstractLineGraphicFactory {
		private static final int MASK_NORTH = 0;
		private static final int MASK_SOUTH = 1;
		private static final int MASK_EAST = 2;
		private final BooleanProperty active = new SimpleBooleanProperty(false);

		private JumpArrowFactory() {
			super(AbstractLineGraphicFactory.P_BRACKET_MATCH - 1);
		}

		@Override
		public void install(@Nonnull Editor editor) {
			// no-op, outer class has all the data we need
		}

		@Override
		public void uninstall(@Nonnull Editor editor) {
			// no-op
		}

		@Override
		public void apply(@Nonnull LineContainer container, int paragraph) {
			int containerHeight = 16; // Each line graphic region is only 16px tall
			int containerWidth = 16;

			List<LabelData> localModel = model;

			if (!active.get() || localModel.isEmpty()) {
				container.addHorizontal(new Spacer(containerWidth));
				return;
			}

			// To keep the ordering of the line graphic factory priority we need to add the stack pane now
			// since the rest of the work is done async below. We want this to have zero width so that it doesn't
			// shit the editor around when the content becomes active/inactive.
			StackPane stack = new StackPane();
			stack.setManaged(false);
			stack.setPrefWidth(0);
			stack.setMouseTransparent(true);
			container.addHorizontal(stack);

			// This looks stupid because it is, however it is necessary.
			//
			// When we're computing how long the lines need to be to connect to the text that references labels
			// we need the cell layout to be up-to-date. But because the line graphic can be initialized at the same
			// time as the paragraph in some cases it won't be always up-to-date, leading to the wrong length of lines.
			//
			// We could make this faster AND less complex if we assumed the font family and font size NEVER change
			// and is always 'JetBrains Mono 12px' but if we did that and changed things we'd 100% forget about the hack
			// and wonder why the thing broke. The magic 'width' per space char in such case is '7.2'.
			FxThreadUtil.delayedRun(0, () -> {
				stack.setPrefSize(containerHeight, containerHeight);
				stack.setAlignment(Pos.CENTER_LEFT);
				SceneUtils.getParentOfTypeLater(container, VirtualizedScrollPaneWrapper.class).whenComplete((parentScroll, error) -> {
					ObservableValue<? extends Number> translateX;
					if (parentScroll != null) {
						translateX = Bindings.add(container.widthProperty().subtract(containerHeight), parentScroll.horizontalScrollProperty().negate());
					} else {
						// Should never happen since the 'VirtualizedScrollPaneWrapper' is mandated internally by 'Editor'.
						translateX = container.widthProperty().subtract(containerHeight);
					}
					stack.translateXProperty().bind(translateX);
				});


				double indent = editor.computeWhitespacePrefixWidth(paragraph) - 3 /* padding so lines aren't right up against text */;
				double width = containerWidth + indent;
				double height = containerHeight + 2;
				int[] offsets = new int[containerWidth];
				int j = 0;
				for (int i = 0; i < offsets.length; i++) {
					offsets[i] = 1 + (i * 3);
				}
				Canvas canvas = new Canvas(width, height);
				canvas.setManaged(false);
				canvas.setMouseTransparent(true);
				canvas.setTranslateY(-1);

				Blend blend = new Blend(BlendMode.HARD_LIGHT);
				Bloom bloom = new Bloom(0.2);
				Glow glow = new Glow(1.0);
				bloom.setInput(blend);
				glow.setInput(bloom);
				canvas.setEffect(glow);

				GraphicsContext gc = canvas.getGraphicsContext2D();
				gc.setLineWidth(1);
				stack.getChildren().add(canvas);

				for (LabelData labelData : localModel) {
					// Skip if there are no references to the current label.
					List<ASTElement> labelReferrers = labelData.usage().writers();
					if (labelReferrers.isEmpty()) continue;

					// Skip if line is not inside a jump range.
					if (!labelData.isInRange(paragraph + 1)) continue;

					// There is always one 'reader' AKA the label itself.
					// We will use this to figure out which direction to draw lines in below.
					ASTElement labelTarget = labelData.labelDeclaration();
					int declarationLine = labelTarget.location().line() - 1;
					int nameHashBase = labelData.name().repeat(15).hashCode();

					int parallelLines = Math.max(1, labelData.computeOverlapping(model).size());
					int lineSlot = labelData.lineSlot().get();
					int offsetIndex = lineSlot % offsets.length;
					int horizontalOffset = offsets[offsetIndex];
					double hue = 360.0 / parallelLines * lineSlot;
					Color color = createColor(hue);

					// Mask for tracking which portions of the jump lines have been drawn.
					BitSet shapeMask = new BitSet(3);

					// Iterate over AST elements that refer to the label.
					// We will use their position and the label declaration position to determine what shape to draw.
					for (ASTElement referrer : labelReferrers) {
						// Sanity check the AST element has location data.
						Location referenceLoc = referrer.location();
						if (referenceLoc == null) continue;

						int referenceLine = referenceLoc.line() - 1;
						boolean isBackReference = referenceLine > declarationLine;

						gc.setStroke(color);
						gc.beginPath();
						boolean multiLine = labelData.countRefsOnLine(referenceLine) > 0;
						double targetY = multiLine ? horizontalOffset : height / 2;
						if (referenceLine == paragraph) {
							// The Y coordinates in these lines is the midpoint because as references
							// there should only be one line coming out of them. We don't need to fit
							// multiple lines.
							if (isBackReference) {
								// Shape: └
								if (!shapeMask.get(MASK_NORTH)) {
									// Top section
									gc.moveTo(horizontalOffset, 0);
									gc.lineTo(horizontalOffset, targetY);
									shapeMask.set(MASK_NORTH);
								}
								if (!shapeMask.get(MASK_EAST)) {
									// Right section
									gc.moveTo(horizontalOffset, targetY);
									gc.lineTo(width, targetY);
									shapeMask.set(MASK_EAST);
								}
							} else {
								// Shape: ┌
								if (!shapeMask.get(MASK_SOUTH)) {
									// Bottom section
									gc.moveTo(horizontalOffset, height);
									gc.lineTo(horizontalOffset, targetY);
									shapeMask.set(MASK_SOUTH);
								}
								if (!shapeMask.get(MASK_EAST)) {
									// Right section
									gc.moveTo(horizontalOffset, targetY);
									gc.lineTo(width, targetY);
									shapeMask.set(MASK_EAST);
								}
							}
							gc.stroke();
						} else if (paragraph == declarationLine) {
							if (isBackReference) {
								// Shape: ┌
								if (!shapeMask.get(MASK_SOUTH)) {
									// Bottom section
									gc.moveTo(horizontalOffset, height);
									gc.lineTo(horizontalOffset, targetY);
									shapeMask.set(MASK_SOUTH);
								}
								if (!shapeMask.get(MASK_EAST)) {
									// Right section
									gc.moveTo(horizontalOffset, targetY);
									gc.lineTo(width, targetY);
									shapeMask.set(MASK_EAST);
								}
							} else {
								// Shape: └
								if (!shapeMask.get(MASK_NORTH)) {
									// Top section
									gc.moveTo(horizontalOffset, 0);
									gc.lineTo(horizontalOffset, targetY);
									shapeMask.set(MASK_NORTH);
								}
								if (!shapeMask.get(MASK_EAST)) {
									// Right section
									gc.moveTo(horizontalOffset, targetY);
									gc.lineTo(width, targetY);
									shapeMask.set(MASK_EAST);
								}
							}
							gc.stroke();
						} else if ((isBackReference && (paragraph > declarationLine && paragraph < referenceLine)) ||
								(!isBackReference && (paragraph < declarationLine && paragraph > referenceLine))) {
							if (!shapeMask.get(MASK_NORTH)) {
								// Top section
								gc.moveTo(horizontalOffset, 0);
								gc.lineTo(horizontalOffset, height / 2);
								shapeMask.set(MASK_NORTH);
							}
							if (!shapeMask.get(MASK_SOUTH)) {
								// Bottom section
								gc.moveTo(horizontalOffset, height / 2);
								gc.lineTo(horizontalOffset, height);
								shapeMask.set(MASK_SOUTH);
							}
							gc.stroke();
						}
						gc.closePath();
					}
				}
			});
		}

		@Nonnull
		private static Color createColor(double hue) {
			Color color = Color.hsb(hue, 1.0, 1.0);

			// Ensure the color is actually bright enough.
			// In cases like pure blue, we have to lower the saturation incrementally to allow the brightness
			// boosting math to have any effect. The brightness constants should approximate perceived brightness.
			int i = 0;
			while (i < 30) {
				double red = color.getRed();
				double green = color.getGreen();
				double blue = color.getBlue();
				double brightness = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
				if (brightness > 0.4)
					break;
				color = color.deriveColor(0, 0.97, 1.2, 1);
				i++;
			}

			return color;
		}

		private static int hash(int x) {
			x = ((x >> 16) ^ x) * 0x45d9f3b;
			x = ((x >> 16) ^ x) * 0x45d9f3b;
			x = (x >> 16) ^ x;
			return x;
		}
	}
}