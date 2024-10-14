package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTArray;
import me.darknet.assembler.ast.primitive.ASTCode;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.primitive.ASTObject;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.Location;
import org.reactfx.Change;
import org.slf4j.Logger;
import software.coley.collections.box.Box;
import software.coley.collections.box.IntBox;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractTextBoundLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.util.FxThreadUtil;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Controller for displaying control flow jump lines.
 *
 * @author Matt Coley
 */
@Dependent
public class ControlFlowLines extends AstBuildConsumerComponent {
	private static final Logger logger = Logging.get(ControlFlowLines.class);
	private static final Set<String> INSN_SET = Set.of("goto", "ifnull", "ifnonnull", "ifeq", "ifne", "ifle", "ifge", "iflt", "ifgt",
			"if_acmpeq", "if_acmpne", "if_icmpeq", "if_icmpge", "if_icmpgt", "if_icmple", "if_icmplt", "if_icmpne");
	private static final Set<String> SWITCH_INSNS = Set.of("tableswitch", "lookupswitch");
	private final Consumer<Change<Integer>> onCaretMove = this::onCaretMove;
	private final ObservableObject<ASTInstruction> currentInstructionSelection = new ObservableObject<>(null);
	private final ObservableBoolean drawLines = new ObservableBoolean(false);
	private final ControlFlowLineFactory arrowFactory = new ControlFlowLineFactory();
	private final ControlFlowLinesConfig config;
	private List<LabelData> model = Collections.emptyList();


	@Inject
	public ControlFlowLines(@Nonnull ControlFlowLinesConfig config) {
		this.config = config;

		Runnable redraw = () -> {if (editor != null) editor.redrawParagraphGraphics();};
		drawLines.addChangeListener((ob, old, cur) -> redraw.run());
		currentInstructionSelection.addChangeListener((ob, old, cur) -> redraw.run());
		config.getConnectionMode().addChangeListener((ob, old, cur) -> redraw.run());
		config.getRenderMode().addChangeListener((ob, old, cur) -> redraw.run());
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
	 * Handles updating the {@link ControlFlowLineFactory}.
	 * <p/>
	 * This logic is shoe-horned into here <i>(for now)</i> because
	 * the variable tracking logic is internal to this class only.
	 *
	 * @param caretChange
	 * 		Caret pos change.
	 */
	private void onCaretMove(@Nonnull Change<Integer> caretChange) {
		try {
			// Find selected instruction (can be null)
			Box<ASTInstruction> selected = extracted();

			// Check if the selection was a label or supported instruction.
			ASTInstruction current = selected.get();
			boolean hasSelection = false;
			if (current instanceof ASTLabel) {
				hasSelection = true;
			} else if (current != null) {
				String insnName = current.identifier().content();
				List<ASTElement> arguments = current.arguments();
				if (!arguments.isEmpty() && INSN_SET.contains(insnName) || SWITCH_INSNS.contains(insnName)) {
					hasSelection = true;
				}
			}
			currentInstructionSelection.setValue(current);
			drawLines.setValue(hasSelection);
		} catch (Throwable t) {
			logger.error("Error updating control flow line targets", t);
		}
	}

	@Nonnull
	private Box<ASTInstruction> extracted() {
		Box<ASTInstruction> selected = new Box<>();
		int pos = editor.getCodeArea().getCaretPosition();
		int line = editor.getCodeArea().getCurrentParagraph() + 1;
		for (ASTElement element : astElements) {
			if (element == null)
				continue;
			if (element.range().within(pos)) {
				element.walk(ast -> {
					if (ast instanceof ASTInstruction instruction) {
						Location location = ast.location();
						if (location != null && location.line() == line)
							selected.set(instruction);
						else {
							String identifier = instruction.identifier().content();
							if (("tableswitch".equals(identifier)
									|| "lookupswitch".equals(identifier))
									&& ast.range().within(pos)) {
								selected.set(instruction);
							}
						}
					}
					return !selected.isSet();
				});
			}
		}
		return selected;
	}

	private void updateModel() {
		// Skip if we've not selected a method
		if (currentMethod == null) {
			model = Collections.emptyList();
			return;
		}

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
				ASTCode code = astMethod.code();
				if (code == null)
					return;
				for (ASTInstruction instruction : code.instructions()) {
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
	private class ControlFlowLineFactory extends AbstractTextBoundLineGraphicFactory {
		private static final int MASK_NORTH = 0;
		private static final int MASK_SOUTH = 1;
		private static final int MASK_EAST = 2;
		private final int[] offsets = new int[containerWidth];
		private final long rainbowHueRotationDurationMillis = 3000;

		private ControlFlowLineFactory() {
			super(AbstractLineGraphicFactory.P_BRACKET_MATCH - 1);

			// Populate offsets
			int j = 0;
			for (int i = 0; i < offsets.length; i++) {
				offsets[i] = 1 + (i * 3);
			}
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
			List<LabelData> localModel = model;

			if (!drawLines.getValue() || localModel.isEmpty()) {
				container.addHorizontal(new Spacer(0));
				return;
			}

			super.apply(container, paragraph);
		}

		@Override
		public void apply(@Nonnull StackPane container, int paragraph) {
			List<LabelData> localModel = model;

			double indent = ControlFlowLines.this.editor.computeWhitespacePrefixWidth(paragraph) - 3 /* padding so lines aren't right up against text */;
			double width = containerWidth + indent;
			double height = containerHeight + 2;
			Canvas canvas = new Canvas(width, height);
			canvas.setManaged(false);
			canvas.setMouseTransparent(true);
			canvas.setTranslateY(-1);

			// Setup canvas styling for the render mode.
			var renderMode = config.getRenderMode().getValue();
			Blend blend = new Blend(BlendMode.HARD_LIGHT);
			Effect effect = switch (renderMode) {
				case FLAT -> blend;
				case RAINBOW_GLOWING, FLAT_GLOWING -> {
					Bloom bloom = new Bloom(0.2);
					Glow glow = new Glow(0.7);
					bloom.setInput(blend);
					glow.setInput(bloom);
					yield glow;
				}
			};
			canvas.setEffect(effect);
			if (renderMode == ControlFlowLinesConfig.LineRenderMode.RAINBOW_GLOWING) {
				setupRainbowAnimation(effect, canvas).play();
			}

			GraphicsContext gc = canvas.getGraphicsContext2D();
			gc.setLineWidth(1);
			container.getChildren().add(canvas);

			for (LabelData labelData : localModel) {
				// Skip if there are no references to the current label.
				List<ASTElement> labelReferrers = labelData.usage().writers();
				if (labelReferrers.isEmpty()) continue;

				// Skip if line is not inside a jump range.
				if (!labelData.isInRange(paragraph + 1)) continue;

				// Handle skipping over cases if we only want to draw lines for what is currently selected.
				if (config.getConnectionMode().getValue() == ControlFlowLinesConfig.ConnectionMode.CURRENT_CONNECTION) {
					ASTInstruction value = currentInstructionSelection.getValue();
					if (value == null) {
						// No current selection?  We can skip everything. Just return.
						return;
					} else if (SWITCH_INSNS.contains(value.identifier().literal())) {
						// If the selected item is a switch we want to draw all the lines to all destinations.
						//
						// The label data writers will be targeting the label identifier children in the AST
						// so if we walk the switch instruction's children we can see if the current label data
						// references one of those elements.
						List<ASTElement> elements = new ArrayList<>();
						value.walk(e -> {
							elements.add(e);
							return true;
						});
						if (labelData.usage().readersAndWriters().noneMatch(elements::contains))
							continue;
					} else if (labelData.usage().readersAndWriters().noneMatch(m -> m.equals(value))) {
						// Anything else like a label declaration or a jump instruction mentioning a label
						// can be handled with a basic equality check against all the usage readers/writers.
						continue;
					}
				}

				// There is always one 'reader' AKA the label itself.
				// We will use this to figure out which direction to draw lines in below.
				ASTElement labelTarget = labelData.labelDeclaration();
				int declarationLine = labelTarget.location().line() - 1;

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
						// Right section
						if (isBackReference) {
							// Shape: └
							if (!shapeMask.get(MASK_NORTH)) {
								// Top section
								gc.moveTo(horizontalOffset, 0);
								gc.lineTo(horizontalOffset, targetY);
								shapeMask.set(MASK_NORTH);
							}
						} else {
							// Shape: ┌
							if (!shapeMask.get(MASK_SOUTH)) {
								// Bottom section
								gc.moveTo(horizontalOffset, height);
								gc.lineTo(horizontalOffset, targetY);
								shapeMask.set(MASK_SOUTH);
							}
						}
						if (!shapeMask.get(MASK_EAST)) {
							// Right section
							gc.moveTo(horizontalOffset, targetY);
							gc.lineTo(width, targetY);
							shapeMask.set(MASK_EAST);
						}
						gc.stroke();
					} else if (paragraph == declarationLine) {
						// Right section
						if (isBackReference) {
							// Shape: ┌
							if (!shapeMask.get(MASK_SOUTH)) {
								// Bottom section
								gc.moveTo(horizontalOffset, height);
								gc.lineTo(horizontalOffset, targetY);
								shapeMask.set(MASK_SOUTH);
							}
						} else {
							// Shape: └
							if (!shapeMask.get(MASK_NORTH)) {
								// Top section
								gc.moveTo(horizontalOffset, 0);
								gc.lineTo(horizontalOffset, targetY);
								shapeMask.set(MASK_NORTH);
							}
						}
						if (!shapeMask.get(MASK_EAST)) {
							// Right section
							gc.moveTo(horizontalOffset, targetY);
							gc.lineTo(width, targetY);
							shapeMask.set(MASK_EAST);
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

		@Nonnull
		private Transition setupRainbowAnimation(@Nonnull Effect effect, @Nonnull Canvas canvas) {
			return new Transition() {
				{
					setInterpolator(Interpolator.LINEAR);
					setCycleDuration(Duration.millis(rainbowHueRotationDurationMillis));
					setCycleCount(Integer.MAX_VALUE);
				}

				@Override
				protected void interpolate(double frac) {
					long now = System.currentTimeMillis();
					float diff = now % rainbowHueRotationDurationMillis;

					float halfMillis = (float) rainbowHueRotationDurationMillis / 2;
					float hue = Math.abs((4 * diff / rainbowHueRotationDurationMillis) - 2) - 1;
					ColorAdjust adjust = new ColorAdjust(hue, 0.0, 0.0, 0.0);
					adjust.setInput(effect);
					canvas.setEffect(adjust);
				}
			};
		}
	}
}