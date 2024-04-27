package me.coley.recaf.ui.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.UiUtil;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays markdown text.
 * <br>
 * Adapted from <a href="https://github.com/JPro-one/markdown-javafx-renderer/blob/master/src/main/java/com/sandec/mdfx/impl/MDFXNodeHelper.java#L523">MDFX</a>,
 * which couldn't be used directly in order to support targeting Java 8. Some minor changes to the visitor internals were also made.
 */
public class MarkdownView extends BorderPane {
	private final static String ITALICE_CLASS_NAME = "italic";
	private final static String BOLD_CLASS_NAME = "bold";
	private static final PseudoClass PSEUDO_HOVER = PseudoClass.getPseudoClass("hover");
	// State
	private VBox root = newVBox();
	private TextFlow flow = null;

	public MarkdownView(String markdown) {
		Parser parser = Parser.builder().build();
		Document document = (Document) parser.parse(markdown);
		document.accept(new AbstractVisitor() {
			private boolean nodePerWord;
			private boolean isListOrdered;
			private int listCounter;

			@Override
			public void visit(Document document) {
				visitChildren(document);
			}

			@Override
			public void visit(BlockQuote customBlock) {
				VBox oldRoot = root;
				root = newVBox();
				root.getStyleClass().add("block-quote");
				oldRoot.getChildren().add(root);

				visitChildren(customBlock);

				root = oldRoot;
				newParagraph();
			}

			@Override
			public void visit(Code code) {
				Label label = new Label(code.getLiteral());
				label.getStyleClass().add("code");

				Region background = new Region();
				background.setManaged(false);
				background.getStyleClass().add("code-background");
				label.boundsInParentProperty().addListener((p, oldV, newV) -> {
					background.setTranslateX(newV.getMinX() + 2);
					background.setTranslateY(newV.getMinY() - 2);
					background.resize(newV.getWidth() - 4, newV.getHeight() + 4);
				});

				flow.getChildren().add(background);
				flow.getChildren().add(label);
			}

			@Override
			public void visit(FencedCodeBlock code) {
				Label label = new Label(code.getLiteral());
				label.getStyleClass().add("codeblock");

				VBox vbox = newVBox();
				vbox.getChildren().add(label);
				vbox.getStyleClass().add("codeblock-background");

				root.getChildren().add(vbox);
			}

			@Override
			public void visit(SoftLineBreak softLineBreak) {
				flow.getChildren().add(new Text("\n"));
				visitChildren(softLineBreak);
			}

			@Override
			public void visit(HardLineBreak hardLineBreak) {
				newParagraph();
				flow.getChildren().add(new Text("\n"));
				visitChildren(hardLineBreak);
			}

			@Override
			public void visit(Heading heading) {
				newParagraph();
				flow.getStyleClass().add("h" + heading.getLevel());
				visitChildren(heading);
			}

			@Override
			public void visit(Paragraph paragraph) {
				newParagraph();
				flow.getStyleClass().add("p");
				visitChildren(paragraph);
			}

			@Override
			public void visit(BulletList bulletList) {
				isListOrdered = false;
				visitList(bulletList);
			}

			@Override
			public void visit(OrderedList orderedList) {
				isListOrdered = true;
				visitList(orderedList);
			}

			private void visitList(ListBlock list) {
				// Copy state
				boolean prevIsListOrdered = isListOrdered;
				int prevListDepth = listCounter;
				isListOrdered = false;
				VBox oldRoot = root;

				// Visit children
				root = newVBox();
				oldRoot.getChildren().add(root);
				newParagraph();
				visitChildren(list);

				// Restore
				isListOrdered = prevIsListOrdered;
				listCounter = prevListDepth;
				root = oldRoot;
			}

			@Override
			public void visit(ListItem listItem) {
				VBox oldRoot = root;

				VBox newRoot = newVBox();
				newRoot.getStyleClass().add("li");
				newRoot.setFillWidth(true);

				listCounter += 1;

				String text = isListOrdered ? (" " + (listCounter + 1) + ". ") : " • ";

				Label dot = new Label(text);
				dot.getStyleClass().add("li-dot");

				HBox hbox = new HBox();
				hbox.getChildren().add(dot);
				hbox.setAlignment(Pos.TOP_LEFT);
				HBox.setHgrow(newRoot, Priority.ALWAYS);
				newRoot.setPrefWidth(1.0); // This way, it doesn't take space from the "dot" label
				hbox.getChildren().add(newRoot);

				oldRoot.getChildren().add(hbox);

				root = newRoot;

				visitChildren(listItem);
				root = oldRoot;
			}

			@Override
			public void visit(Link link) {
				// Save state and update
				boolean oldNodePerWord = nodePerWord;
				nodePerWord = true;

				TextFlow currentFlow = flow;
				int childIndex = currentFlow.getChildren().size();
				visitChildren(link);
				int newChildIndex = currentFlow.getChildren().size();

				List<Node> linkNodes = new ArrayList<>(currentFlow.getChildren().subList(childIndex, newChildIndex));
				BooleanProperty lastHoverState = new SimpleBooleanProperty(false);
				Runnable updateState = () -> {
					boolean isHover = false;
					for (Node node : linkNodes) {
						if (node.isHover()) {
							isHover = true;
						}
					}
					if (isHover != lastHoverState.get()) {
						lastHoverState.set(isHover);
						for (Node node : linkNodes) {
							node.pseudoClassStateChanged(PSEUDO_HOVER, isHover);
						}
					}
				};
				String url = link.getDestination();
				for (Node node : linkNodes) {
					node.getStyleClass().add("a");
					node.setOnMousePressed(e -> {
						try {
							UiUtil.showDocument(new URL(url).toURI());
						} catch (Exception ex) {
							Log.error("Could not open URL: " + url);
						}
					});
					node.setCursor(Cursor.HAND);
					node.hoverProperty().addListener((ob, old, cur) -> updateState.run());
					updateState.run();
				}

				// Restore
				nodePerWord = oldNodePerWord;
			}


			@Override
			public void visit(Emphasis emphasis) {
				TextFlow currentFlow = flow;
				int childIndex = currentFlow.getChildren().size();
				visitChildren(emphasis);
				int newChildIndex = currentFlow.getChildren().size();

				List<Node> emphasisNodes = new ArrayList<>(currentFlow.getChildren().subList(childIndex, newChildIndex));
				for (Node node : emphasisNodes) {
					node.getStyleClass().add(ITALICE_CLASS_NAME);
				}
			}

			@Override
			public void visit(StrongEmphasis strongEmphasis) {
				TextFlow currentFlow = flow;
				int childIndex = currentFlow.getChildren().size();
				visitChildren(strongEmphasis);
				int newChildIndex = currentFlow.getChildren().size();

				List<Node> emphasisNodes = new ArrayList<>(currentFlow.getChildren().subList(childIndex, newChildIndex));
				for (Node node : emphasisNodes) {
					node.getStyleClass().add(BOLD_CLASS_NAME);
				}
			}

			@Override
			public void visit(org.commonmark.node.Text text) {
				String wholeText = text.getLiteral();

				String[] textsSplit;
				if (nodePerWord) {
					// Split with " " but keep the " " in the array
					textsSplit = wholeText.split("(?<= )");
					// Combine split texts, which only contain a space:
					for (int i = 0; i <= textsSplit.length - 1; i += 1) {
						if (textsSplit[i].equals(" ")) {
							if (i == 0) {
								if (i + 1 <= textsSplit.length - 1) {
									textsSplit[i + 1] = " " + textsSplit[i + 1];
									textsSplit[i] = "";
								}
							} else {
								textsSplit[i - 1] = textsSplit[i - 1] + textsSplit[i];
								textsSplit[i] = "";
							}
						}
					}
				} else {
					textsSplit = new String[1];
					textsSplit[0] = wholeText;
				}
				final String[] textSplitfinal = textsSplit;

				for (int i = 0; i <= textsSplit.length - 1; i += 1) {
					if (!textSplitfinal[i].isEmpty()) {
						addText(textSplitfinal[i]);
					}
				}
			}
		});
		getStyleClass().add("content");
		setCenter(root);
	}

	@Nullable
	public Text addText(String text) {
		if (!text.isEmpty()) {
			Text t = new Text(text);
			t.getStyleClass().add("text");
			flow.getChildren().add(t);
			return t;
		}
		return null;
	}

	public TextFlow newParagraph() {
		TextFlow newFlow = new TextFlow();
		root.getChildren().add(newFlow);
		flow = newFlow;
		return newFlow;
	}

	private VBox newVBox() {
		return new VBox();
	}
}
