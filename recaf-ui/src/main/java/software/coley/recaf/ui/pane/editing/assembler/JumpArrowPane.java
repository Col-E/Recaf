package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;
import me.darknet.assembler.util.Location;
import org.reactfx.Change;
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

	@Inject
	public JumpArrowPane() {}

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
		updateModel();
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
		int paragraph = editor.getCodeArea().getCurrentParagraph();

		// Determine which variable is at the caret position
		LabelData currentLabelUsageSelection = null;
		for (LabelData item : model) {
			AstUsages usage = item.usage();
			ASTElement matchedAst = usage.readersAndWriters()
					.filter(e -> e.location().line() - 1 == paragraph)
					.findFirst().orElse(null);
			if (matchedAst != null) {
				currentLabelUsageSelection = item;
				break;
			}
		}

		// Notify the highlighter of the difference
		arrowFactory.setSelectedLabel(currentLabelUsageSelection);
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
				.map(e -> new LabelData(e.getKey(), e.getValue()))
				.toList();
	}

	private void clearData() {
		model = Collections.emptyList();
		currentMethod = null;
	}

	/**
	 * Highlighter which shows read and write access of a {@link LabelData}.
	 */
	private class JumpArrowFactory extends AbstractLineGraphicFactory {
		private LabelData label;

		private JumpArrowFactory() {
			super(AbstractLineGraphicFactory.P_LINE_NUMBERS - 1);
		}

		/**
		 * @param label
		 * 		New selected label.
		 */
		public void setSelectedLabel(@Nullable LabelData label) {
			LabelData existing = this.label;
			String existingName = existing == null ? null : existing.name();
			String newName = label == null ? null : label.name();
			if (Objects.equals(existingName, newName))
				return;

			this.label = label;
			editor.redrawParagraphGraphics();
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
			LabelData localLabel = label;
			if (localLabel == null) return;

			// To keep the ordering of the line graphic factory priority we need to add the stack pane now
			// since the rest of the work is done async below.
			StackPane stack = new StackPane();
			stack.setManaged(false);
			stack.setMouseTransparent(true);
			stack.setTranslateY(10); // To center the lines drawn below.
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
				int max = 16; // Size of line graphic

				stack.setPrefSize(max, max);
				stack.setAlignment(Pos.CENTER_LEFT);
				SceneUtils.getParentOfTypeLater(container, VirtualizedScrollPaneWrapper.class).whenComplete((parentScroll, error) -> {
					ObservableValue<? extends Number> translateX;
					if (parentScroll != null) {
						translateX = Bindings.add(container.widthProperty().subtract(max), parentScroll.horizontalScrollProperty().negate());
					} else {
						// Should never happen since the 'VirtualizedScrollPaneWrapper' is mandated internally by 'Editor'.
						translateX = container.widthProperty().subtract(max);
					}
					stack.translateXProperty().bind(translateX);
				});

				// There is always one 'reader' AKA the label itself.
				// We will use this to figure out which direction to draw lines in below.
				ASTElement labelTarget = localLabel.usage().readers().getFirst();
				int lineOffset = 1;
				int declarationLine = labelTarget.location().line() - 1;

				// For all AST elements that reference the label we'll draw a separate color coded line.
				for (ASTElement writer : localLabel.usage().writers()) {
					// Sanity check the AST element has location data.
					Location referenceLoc = writer.location();
					if (referenceLoc == null) continue;

					int referenceLine = referenceLoc.line() - 1;
					boolean isBackReference = referenceLine > declarationLine;
					double hue = new Random(hash(lineOffset * (isBackReference ? 13 : 17))).nextDouble(360);
					Color color = Color.hsb(hue, 1.0, 1.0);
					if (referenceLine == paragraph) {
						lineOffset += 2;

						double indent = editor.computeWhitespacePrefixWidth(paragraph);
						Canvas canvas = new Canvas(max + indent, max);
						GraphicsContext gc = canvas.getGraphicsContext2D();
						gc.setStroke(color);
						gc.setLineWidth(1);
						if (isBackReference) {
							// Shape: └
							gc.moveTo(lineOffset, 0);
							gc.lineTo(lineOffset, 8);
							gc.lineTo(max + indent, 8);
						} else {
							// Shape: ┌
							gc.moveTo(lineOffset, max);
							gc.lineTo(lineOffset, 8);
							gc.lineTo(max + indent, 8);
						}
						gc.stroke();
						stack.getChildren().add(canvas);
					} else if (paragraph == declarationLine) {
						lineOffset += 2;

						double indent = editor.computeWhitespacePrefixWidth(paragraph);
						Canvas canvas = new Canvas(max + indent, max);
						GraphicsContext gc = canvas.getGraphicsContext2D();
						gc.setStroke(color);
						gc.setLineWidth(1);
						if (isBackReference) {
							// Shape: ┌
							gc.moveTo(lineOffset, max);
							gc.lineTo(lineOffset, lineOffset);
							gc.lineTo(max + indent, lineOffset);
						} else {
							// Shape: └
							gc.moveTo(lineOffset, 0);
							gc.lineTo(lineOffset, max - lineOffset);
							gc.lineTo(max + indent, max - lineOffset);
						}
						gc.stroke();
						stack.getChildren().add(canvas);
					} else if ((isBackReference && (paragraph > declarationLine && paragraph < referenceLine)) ||
							(!isBackReference && (paragraph < declarationLine && paragraph > referenceLine))) {
						lineOffset += 2;
						Canvas canvas = new Canvas(max, max + 2);
						canvas.setTranslateY(-2.5);
						canvas.setHeight(max + 1);
						GraphicsContext gc = canvas.getGraphicsContext2D();
						gc.setStroke(color);
						gc.setLineWidth(1);
						gc.moveTo(lineOffset, 0);
						gc.lineTo(lineOffset, max + 2);
						gc.stroke();
						stack.getChildren().add(canvas);
					}
				}
			});
		}

		private static int hash(int x) {
			x = ((x >> 16) ^ x) * 0x45d9f3b;
			x = ((x >> 16) ^ x) * 0x45d9f3b;
			x = (x >> 16) ^ x;
			return x;
		}
	}
}