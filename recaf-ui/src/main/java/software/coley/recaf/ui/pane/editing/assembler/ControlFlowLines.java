package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Spacer;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.Node;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.image.WritableImage;
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
import software.coley.bentofx.control.canvas.PixelCanvas;
import software.coley.bentofx.control.canvas.PixelPainter;
import software.coley.bentofx.control.canvas.PixelPainterIntArgb;
import software.coley.collections.Unchecked;
import software.coley.collections.box.Box;
import software.coley.collections.box.IntBox;
import software.coley.observables.AbstractObservable;
import software.coley.observables.ChangeListener;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.AbstractTextBoundLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.linegraphics.LineContainer;
import software.coley.recaf.util.Colors;
import software.coley.recaf.util.DesktopUtil;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.NumberUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
	private static final Set<String> FLOW_INSN_SET = Set.of("goto", "ifnull", "ifnonnull", "ifeq", "ifne", "ifle", "ifge", "iflt", "ifgt",
			"if_acmpeq", "if_acmpne", "if_icmpeq", "if_icmpge", "if_icmpgt", "if_icmple", "if_icmplt", "if_icmpne", "jsr");
	private static final Set<String> SWITCH_INSNS = Set.of("tableswitch", "lookupswitch");
	private final Consumer<Change<Integer>> onCaretMove = this::onCaretMove;
	private final ObservableObject<ASTInstruction> currentInstructionSelection = new ObservableObject<>(null);
	private final ObservableBoolean drawLines = new ObservableBoolean(false);
	private final ControlFlowLineFactory arrowFactory = new ControlFlowLineFactory();
	private final ControlFlowLinesConfig config;
	private final ListenerHost redrawListener = new ListenerHost();
	private List<LabelData> model = Collections.emptyList();

	@Inject
	public ControlFlowLines(@Nonnull ControlFlowLinesConfig config) {
		this.config = config;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void install(@Nonnull Editor editor) {
		super.install(editor);

		editor.getRootLineGraphicFactory().addLineGraphicFactory(arrowFactory);
		editor.getCaretPosEventStream().addObserver(onCaretMove);

		drawLines.addChangeListener(redrawListener);
		currentInstructionSelection.addChangeListener(redrawListener);
		config.getConnectionMode().addChangeListener(redrawListener);
		config.getRenderMode().addChangeListener(redrawListener);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void uninstall(@Nonnull Editor editor) {
		super.uninstall(editor);

		arrowFactory.cleanup();
		editor.getRootLineGraphicFactory().removeLineGraphicFactory(arrowFactory);
		editor.getCaretPosEventStream().removeObserver(onCaretMove);

		drawLines.removeChangeListener(redrawListener);
		currentInstructionSelection.removeChangeListener(redrawListener);
		config.getConnectionMode().removeChangeListener(redrawListener);
		config.getRenderMode().removeChangeListener(redrawListener);
	}

	@Override
	public void disable() {
		super.disable();

		if (editor != null)
			uninstall(editor);
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
		Editor editor = this.editor;
		if (editor != null && !Objects.equals(oldModel, newModel))
			FxThreadUtil.run(editor::redrawParagraphGraphics);
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
				if (!arguments.isEmpty() && FLOW_INSN_SET.contains(insnName) || SWITCH_INSNS.contains(insnName)) {
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
		Map<String, AstUsages> labelUsages = collectLabelReferences();
		model = labelUsages.entrySet().stream()
				.filter(e -> !e.getValue().readers().isEmpty()) // Must have a label declaration
				.map(e -> new LabelData(e.getKey(), e.getValue(), new Int2IntArrayMap(), new IntBox(-1), new Box<>()))
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

	@Nonnull
	private Map<String, AstUsages> collectLabelReferences() {
		AstUsages emptyUsage = AstUsages.EMPTY_USAGE;
		Map<String, List<ASTElement>> labelReads = new HashMap<>();
		Map<String, List<ASTElement>> labelWrites = new HashMap<>();
		BiConsumer<String, ASTElement> readUpdater = (name, element) -> labelReads.computeIfAbsent(name, n -> new ArrayList<>()).add(element);
		BiConsumer<String, ASTElement> writeUpdater = (name, element) -> labelWrites.computeIfAbsent(name, n -> new ArrayList<>()).add(element);
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
							if (FLOW_INSN_SET.contains(insnName)) {
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
		Set<String> keys = new HashSet<>();
		keys.addAll(labelReads.keySet());
		keys.addAll(labelWrites.keySet());
		Map<String, AstUsages> labelUsages = new HashMap<>();
		for (String key : keys) {
			List<ASTElement> reads = labelReads.get(key);
			List<ASTElement> writes = labelWrites.get(key);
			labelUsages.put(key, new AstUsages(
					Objects.requireNonNullElse(reads, Collections.emptyList()),
					Objects.requireNonNullElse(writes, Collections.emptyList()),
					false));
		}
		return labelUsages;
	}

	private void clearData() {
		model = Collections.emptyList();
		currentMethod = null;
	}

	/**
	 * Highlighter which shows read and write access of a {@link LabelData}.
	 */
	private class ControlFlowLineFactory extends AbstractTextBoundLineGraphicFactory {
		private static final int MASK_NORTH = 1;
		private static final int MASK_SOUTH = 2;
		private static final int MASK_EAST = 4;
		private final ArrayList<ASTElement> switchDestinations = new ArrayList<>(64);
		private final Int2ObjectMap<ImageRecycler> recyclers = new Int2ObjectArrayMap<>();
		private final int[] offsets = new int[containerWidth];
		private final long rainbowHueRotationDurationMillis = 3000;
		private final PixelPainter<?> pixelPainter = new PixelPainterIntArgb();

		private ControlFlowLineFactory() {
			super(AbstractLineGraphicFactory.P_BRACKET_MATCH - 1);

			// Populate offsets
			int j = 0;
			for (int i = 0; i < offsets.length; i++)
				offsets[i] = 1 + (i * 3);
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

			double indent = editor.computeWhitespacePrefixWidth(paragraph) - 3 /* padding so lines aren't right up against text */;
			double width = containerWidth + Math.min(100, indent); // Limit dimensions of canvas
			double height = containerHeight + 2;

			PixelCanvas canvas = null;
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
						ArrayList<ASTElement> destinations = switchDestinations;
						destinations.clear();
						value.walk(e -> {
							destinations.add(e);
							destinations.ensureCapacity(e.children().size() + 1);
							return true;
						});
						if (labelData.usage().readersAndWriters().noneMatch(destinations::contains))
							continue;
					} else if (labelData.usage().readersAndWriters().noneMatch(m -> m.equals(value))) {
						// Anything else like a label declaration or a jump instruction mentioning a label
						// can be handled with a basic equality check against all the usage readers/writers.
						continue;
					}
				}

				// If we've gotten to this point we will need a canvas to draw the lines on.
				if (canvas == null) {
					canvas = new CachingPixelCanvas(pixelPainter, (int) width, (int) height);
					canvas.setManaged(false);
					canvas.setMouseTransparent(true);
					canvas.resize(width, height);

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

					container.getChildren().add(canvas);
				}

				// There is always one 'reader' AKA the label itself.
				// We will use this to figure out which direction to draw lines in below.
				ASTElement labelTarget = labelData.labelDeclaration();
				int declarationLine = labelTarget.location().line() - 1;

				int parallelLines = Math.max(1, labelData.computeOverlapping(model).size());
				int lineSlot = labelData.lineSlot().get();
				int offsetIndex = lineSlot % offsets.length;
				int horizontalOffset = offsetIndex >= 0 && offsetIndex < offsets.length ? offsets[offsetIndex] : 1;
				double hue = 360.0 / parallelLines * lineSlot;
				int color = createColor(hue);
				final int lineWidth = 1;

				// Mask for tracking which portions of the jump lines have been drawn.
				int shapeMask = 0;

				// Iterate over AST elements that refer to the label.
				// We will use their position and the label declaration position to determine what shape to draw.
				for (ASTElement referrer : labelReferrers) {
					// Sanity check the AST element has location data.
					Location referenceLoc = referrer.location();
					if (referenceLoc == null) continue;

					int referenceLine = referenceLoc.line() - 1;
					boolean isBackReference = referenceLine > declarationLine;

					boolean multiLine = labelData.countRefsOnLine(referenceLine) > 0;
					double targetY = multiLine ? horizontalOffset : height / 2;
					if (referenceLine == paragraph) {
						// The Y coordinates in these lines is the midpoint because as references
						// there should only be one line coming out of them. We don't need to fit
						// multiple lines.
						// Right section
						if (isBackReference) {
							// Shape: └
							if ((shapeMask & MASK_NORTH) == 0) {
								// Top section
								canvas.drawVerticalLine(horizontalOffset, 0, targetY, lineWidth, color);
								shapeMask |= MASK_NORTH;
							}
						} else {
							// Shape: ┌
							if ((shapeMask & MASK_SOUTH) == 0) {
								// Bottom section
								canvas.drawVerticalLine(horizontalOffset, targetY, height - targetY, lineWidth, color);
								shapeMask |= MASK_SOUTH;
							}
						}
						if ((shapeMask & MASK_EAST) == 0) {
							// Right section
							canvas.drawHorizontalLine(horizontalOffset, targetY, width - horizontalOffset, lineWidth, color);
							shapeMask |= MASK_EAST;
						}
						canvas.commit();
					} else if (paragraph == declarationLine) {
						// Right section
						if (isBackReference) {
							// Shape: ┌
							if ((shapeMask & MASK_SOUTH) == 0) {
								// Bottom section
								canvas.drawVerticalLine(horizontalOffset, targetY, height - targetY, lineWidth, color);
								shapeMask |= MASK_SOUTH;
							}
						} else {
							// Shape: └
							if ((shapeMask & MASK_NORTH) == 0) {
								// Top section
								canvas.drawVerticalLine(horizontalOffset, 0, targetY, lineWidth, color);
								shapeMask |= MASK_NORTH;
							}
						}
						if ((shapeMask & MASK_EAST) == 0) {
							// Right section
							canvas.drawHorizontalLine(horizontalOffset, targetY, width - horizontalOffset, lineWidth, color);
							shapeMask |= MASK_EAST;
						}
						canvas.commit();
					} else if ((isBackReference && (paragraph > declarationLine && paragraph < referenceLine)) ||
							(!isBackReference && (paragraph < declarationLine && paragraph > referenceLine))) {
						if ((shapeMask & MASK_NORTH) == 0) {
							// Top section
							canvas.drawVerticalLine(horizontalOffset, 0, height / 2, lineWidth, color);
							shapeMask |= MASK_NORTH;
						}
						if ((shapeMask & MASK_SOUTH) == 0) {
							// Bottom section
							canvas.drawVerticalLine(horizontalOffset, height / 2, height / 2, lineWidth, color);
							shapeMask |= MASK_SOUTH;
						}
						canvas.commit();
					}
				}
			}
		}

		public void cleanup() {
			recyclers.clear();
			switchDestinations.clear();
		}

		private static int createColor(double hue) {
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

			return Colors.argb(color);
		}

		@Nonnull
		private Transition setupRainbowAnimation(@Nonnull Effect effect, @Nonnull Node node) {
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
					node.setEffect(adjust);
				}
			};
		}

		/**
		 * @param width
		 * 		Image width.
		 * @param height
		 * 		Image height.
		 *
		 * @return An image recycler for the given dimensions.
		 */
		@Nonnull
		private ImageRecycler getRecycler(int width, int height) {
			int key = width << 16 | height;
			return recyclers.computeIfAbsent(key, i -> new ImageRecycler(width, height));
		}

		/**
		 * Ring buffer of {@link WritableImage} keyed to a specific width/height.
		 */
		private static class ImageRecycler {
			private static final int MAX_IMAGE_BUFFER;
			private final List<WritableImage> images = new ArrayList<>(MAX_IMAGE_BUFFER);
			private final int width, height;
			private int index;

			static {
				// Each row is ~18px so if we divide the screen height by that amount we should
				// get the number of images needed to not visually show that they're recycled.
				//
				// Of course, we want to have some leeway, so we'll round ~18px down a bit.
				// And if the UI scale property is assigned, we'll also accommodate for that.
				Number scale = Unchecked.getOr(() -> NumberUtil.parse(System.getProperty("sun.java2d.uiScale", "1.0")), 1);
				double scaledHeight = DesktopUtil.getLargestScreenSize().height * scale.doubleValue();
				final double rowHeight = 15D;
				final double minRows = 72; // I have ~60 rows on a 1080p monitor so this is probably common enough fallback.
				MAX_IMAGE_BUFFER = (int) Math.max(minRows, scaledHeight / rowHeight);
			}

			/**
			 * @param width
			 * 		Width of images to create.
			 * @param height
			 * 		Height of images to create.
			 */
			public ImageRecycler(int width, int height) {
				this.width = width;
				this.height = height;
			}

			/**
			 * @return Next available image.
			 */
			@Nonnull
			public WritableImage next() {
				if (index >= MAX_IMAGE_BUFFER)
					index = 0;
				WritableImage image;
				if (index >= images.size()) {
					image = new WritableImage(width, height);
					images.add(image);
				} else {
					image = images.get(index);
				}
				index++;
				return image;
			}
		}

		/**
		 * Canvas implementation that recycles backing images via {@link ImageRecycler}.
		 */
		private class CachingPixelCanvas extends PixelCanvas {
			public CachingPixelCanvas(@Nonnull PixelPainter<?> pixelPainter, int width, int height) {
				super(pixelPainter, width, height);
			}

			@Nonnull
			@Override
			protected WritableImage newImage(int width, int height) {
				return getRecycler(width, height).next();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private class ListenerHost implements ChangeListener {
		@Override
		public void changed(AbstractObservable ob, Object old, Object current) {
			if (editor != null) editor.redrawParagraphGraphics();
		}
	}
}