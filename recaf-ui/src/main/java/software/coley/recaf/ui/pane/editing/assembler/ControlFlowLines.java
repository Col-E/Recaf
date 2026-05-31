package software.coley.recaf.ui.pane.editing.assembler;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.query.AssemblyQueries;
import me.darknet.assembler.query.AssemblyUtils;
import me.darknet.assembler.query.LabelInfo;
import me.darknet.assembler.query.LabelQueryResult;
import me.darknet.assembler.query.LabelUsage;
import me.darknet.assembler.util.Location;
import me.darknet.assembler.util.Range;
import org.reactfx.Change;
import org.slf4j.Logger;
import software.coley.bentofx.control.canvas.PixelCanvas;
import software.coley.bentofx.control.canvas.PixelPainter;
import software.coley.bentofx.control.canvas.PixelPainterIntArgb;
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
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.collect.primitive.Int2IntMap;
import software.coley.recaf.util.threading.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Controller for displaying control flow jump lines.
 *
 * @author Matt Coley
 */
@Dependent
public class ControlFlowLines extends AstBuildConsumerComponent {
	private static final Logger logger = Logging.get(ControlFlowLines.class);
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
		// Update the model.
		List<LabelData> oldModel = model;
		updateModel();
		List<LabelData> newModel = model;

		// If the model has changed, refresh the visible paragraph graphics.
		// This can mean a new label as added, new reference to one, etc.
		// This could result in new line shapes, so redrawing them all is wise.
		Editor editor = this.editor;
		if (editor != null && !Objects.equals(oldModel, newModel))
			FxThreadUtil.run(editor::redrawParagraphGraphics);
	}

	/**
	 * Handles updating the {@link ControlFlowLineFactory}.
	 * <p>
	 * This logic is shoe-horned into here <i>(for now)</i> because
	 * the variable tracking logic is internal to this class only.
	 *
	 * @param caretChange
	 * 		Caret pos change.
	 */
	private void onCaretMove(@Nonnull Change<Integer> caretChange) {
		// Find the selected element off of the FX thread, then update our selection and line draw states on the FX thread.
		CompletableFuture.supplyAsync(this::findSelected, ThreadUtil.executor()).thenAcceptAsync(selected -> {
			try {
				// Check if the selection was a label or supported instruction.
				ASTInstruction current = selected.get();
				boolean hasSelection = false;
				if (current instanceof ASTLabel) {
					hasSelection = true;
				} else if (current != null) {
					String insnName = current.identifier().content();
					List<ASTElement> arguments = current.arguments();
					if ((!arguments.isEmpty() && AssemblyUtils.isFlowControlInstruction(BytecodeFormat.JVM, insnName))
							|| AssemblyUtils.isSwitchInstruction(BytecodeFormat.JVM, insnName)) {
						hasSelection = true;
					}
				}
				currentInstructionSelection.setValue(current);
				drawLines.setValue(hasSelection);
			} catch (Throwable t) {
				logger.error("Error updating control flow line targets", t);
			}
		}, FxThreadUtil.executor());
	}

	@Nonnull
	private Box<ASTInstruction> findSelected() {
		Box<ASTInstruction> selected = new Box<>();
		try {
			int pos = editor.getCodeArea().getCaretPosition();
			int line = editor.getCodeArea().getCurrentParagraph() + 1;
			selected.set(AssemblyUtils.findInstruction(astElements, pos, line));
		} catch (Throwable t) {
			logger.warn("Error finding selected AST element", t);
		}
		return selected;
	}

	private void updateModel() {
		// Skip if we've not selected a method
		ASTMethod astMethod = findCurrentAstMethod();
		if (astMethod == null) {
			model = Collections.emptyList();
			return;
		}

		// Collect all label usage information from the AST.
		LabelQueryResult labelQuery = AssemblyQueries.labels(astMethod, BytecodeFormat.JVM);
		Map<String, LabelData> labels = new LinkedHashMap<>();
		for (LabelInfo declaration : labelQuery.declarations()) {
			labels.putIfAbsent(declaration.name(), new LabelData(
					declaration.name(),
					declaration.declaration(),
					labelQuery.usagesOf(declaration.name()).stream()
							.<ASTElement>map(LabelUsage::reference)
							.toList(),
					new Int2IntMap(),
					new IntBox(-1),
					new Box<>()
			));
		}

		model = labels.values().stream()
				.sorted(Comparator.comparing(LabelData::name))
				.toList();
		model.forEach(data -> {
			List<LabelData> overlapping = data.computeOverlapping(model);
			IntBox slot = data.lineSlot();
			int slotIndex = 0;
			while (true) {
				incr:
				{
					for (LabelData other : overlapping) {
						if (slotIndex == other.lineSlot().get()) {
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

	@Nullable
	private ASTMethod findCurrentAstMethod() {
		if (currentMethod == null)
			return null;

		String methodName = currentMethod.getName();
		String methodDescriptor = currentMethod.getDescriptor();
		for (ASTElement astElement : astElements) {
			if (astElement instanceof ASTMethod astMethod) {
				if (matchesMethod(astMethod, methodName, methodDescriptor))
					return astMethod;
			} else if (astElement instanceof ASTClass astClass) {
				for (ASTElement child : astClass.children()) {
					if (child instanceof ASTMethod astMethod && matchesMethod(astMethod, methodName, methodDescriptor))
						return astMethod;
				}
			}
		}
		return null;
	}

	private static boolean matchesMethod(@Nonnull ASTMethod astMethod, @Nonnull String methodName, @Nonnull String methodDescriptor) {
		return Objects.equals(methodName, astMethod.getName().literal()) &&
				Objects.equals(methodDescriptor, astMethod.getDescriptor().literal());
	}

	/**
	 * Highlighter which shows connecting lines between contents of {@link LabelData}.
	 */
	private class ControlFlowLineFactory extends AbstractTextBoundLineGraphicFactory {
		private static final int MASK_NORTH = 1;
		private static final int MASK_SOUTH = 2;
		private static final int MASK_EAST = 4;
		private final ArrayList<ASTElement> switchDestinations = new ArrayList<>(64);
		private final int[] offsets = new int[containerWidth];
		private final long rainbowHueRotationDurationMillis = 3000;
		private final PixelPainter<?> pixelPainter = new PixelPainterIntArgb();

		private ControlFlowLineFactory() {
			super(AbstractLineGraphicFactory.P_BRACKET_MATCH - 1);

			// Populate offsets
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
				List<ASTElement> labelReferrers = labelData.references();
				if (labelReferrers.isEmpty()) continue;

				// Skip if line is not inside a jump range.
				if (!labelData.isInRange(paragraph + 1)) continue;

				// Handle skipping over cases if we only want to draw lines for what is currently selected.
				if (config.getConnectionMode().getValue() == ControlFlowLinesConfig.ConnectionMode.CURRENT_CONNECTION) {
					ASTInstruction value = currentInstructionSelection.getValue();
					if (value == null) {
						// No current selection?  We can skip everything. Just return.
						return;
					} else if (AssemblyUtils.isSwitchInstruction(BytecodeFormat.JVM, value.identifier().literal())) {
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
						if (labelData.allElements().noneMatch(destinations::contains))
							continue;
					} else if (labelData.allElements().noneMatch(value::equals)) {
						// Anything else like a label declaration or a jump instruction mentioning a label
						// can be handled with a basic equality check against all the usage readers/writers.
						continue;
					}
				}

				// If we've gotten to this point we will need a canvas to draw the lines on.
				if (canvas == null) {
					canvas = new PixelCanvas(pixelPainter, (int) width, (int) height);
					canvas.setManaged(false);
					canvas.setMouseTransparent(true);
					canvas.resize(width, height);
					canvas.clear();

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
				ASTElement labelTarget = labelData.declaration();
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

					float hue = Math.abs((4 * diff / rainbowHueRotationDurationMillis) - 2) - 1;
					ColorAdjust adjust = new ColorAdjust(hue, 0.0, 0.0, 0.0);
					adjust.setInput(effect);
					node.setEffect(adjust);
				}
			};
		}
	}

	@SuppressWarnings("rawtypes")
	private class ListenerHost implements ChangeListener {
		@Override
		public void changed(AbstractObservable ob, Object old, Object current) {
			if (editor != null) editor.redrawParagraphGraphics();
		}
	}

	/**
	 * Enriched data about a label declaration and its references.
	 * <br>
	 * Pulled from {@link LabelQueryResult} <i>(See: {@link #updateModel()})</i> but with additional helpers.
	 *
	 * @param name
	 * 		The label name.
	 * @param declaration
	 * 		The AST element where the label is declared.
	 * @param references
	 * 		The AST elements that reference the label.
	 * @param linesOnLinesMap
	 * 		Cached count of how many times this label is referenced on a given line. Key is line number, value is count.
	 * @param lineSlot
	 * 		The assigned line slot for this label. Used for drawing parallel lines.
	 * @param overlapping
	 * 		Cached list of other labels that have overlapping line ranges with this label.
	 */
	private record LabelData(@Nonnull String name,
	                         @Nonnull ASTLabel declaration,
	                         @Nonnull List<ASTElement> references,
	                         @Nonnull Int2IntMap linesOnLinesMap,
	                         @Nonnull IntBox lineSlot,
	                         @Nonnull Box<List<LabelData>> overlapping) {
		@Nonnull
		public Stream<ASTElement> allElements() {
			return Stream.concat(Stream.of(declaration), references.stream());
		}

		@Nonnull
		public Range range() {
			var summary = allElements()
					.mapToInt(element -> Objects.requireNonNull(element.location()).line())
					.summaryStatistics();
			return new Range(summary.getMin(), summary.getMax());
		}

		public int countRefsOnLine(int line) {
			return linesOnLinesMap.computeIfAbsent(line,
					ignored -> Math.toIntExact(allElements()
							.filter(element -> element.location().line() == line)
							.count()));
		}

		public List<LabelData> computeOverlapping(@Nonnull Collection<LabelData> labelDatum) {
			return overlapping.computeIfAbsent(() -> {
				Range range = range();
				List<LabelData> overlap = new ArrayList<>();
				for (LabelData data : labelDatum) {
					if (name.equals(data.name)) continue;
					if (data.references().isEmpty()) continue;

					Range otherRange = data.range();
					if (Math.max(range.start(), otherRange.start()) <= Math.min(range.end(), otherRange.end()))
						overlap.add(data);
				}
				return overlap;
			});
		}

		public boolean isInRange(int line) {
			Location declarationLoc = declaration.location();
			if (declarationLoc == null) return false;
			int declarationLine = declarationLoc.line();
			if (declarationLine == line) return true;

			for (ASTElement referrer : references) {
				Location referrerLoc = referrer.location();
				if (referrerLoc == null) continue;
				int referrerLine = referrerLoc.line();

				if (referrerLine == line) return true;
				if ((declarationLine > referrerLine) ?
						(line > referrerLine && line < declarationLine) :
						(line > declarationLine && line < referrerLine)) return true;
			}
			return false;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			LabelData labelData = (LabelData) o;
			return name.equals(labelData.name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}
}
