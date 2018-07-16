package me.coley.recaf.ui;

import java.awt.Toolkit;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.JavaFX;

public abstract class FxCode extends Stage {
	protected final CodeArea code = new CodeArea();
	protected final HiddenSidesPane pane = new HiddenSidesPane();
	protected final CustomTextField search = new CustomTextField();

	protected FxCode(String initialText, int width, int height) {
		setTitle(createTitle());
		getIcons().add(createIcon());
		//
		setupCode(initialText);
		setupSearch();
		setupPane();
		//
		setScene(JavaFX.scene(pane, width, height));
	}

	protected void setupCode(String initialText) {
		code.setEditable(false);
		code.richChanges().filter(ch -> !ch.getInserted().equals(ch.getRemoved())).subscribe(change -> {
			code.setStyleSpans(0, computeStyle(code.getText()));
		});
		// The text is not passed to constructor so that the CSS can be applied
		// via the change listener.
		code.appendText(initialText);
		// Dont allow undo to remove the initial text.
		code.getUndoManager().forgetHistory();
		// set position
		code.selectRange(0, 0);
		code.moveTo(0);
		code.scrollToPixel(0, 0);
	}

	protected void setupSearch() {
		// Main panel, has hidden top-node for search bar.
		search.setLeft(new ImageView(Icons.FIND));
		search.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (KeyCode.ESCAPE == e.getCode()) {
				pane.setPinnedSide(null);
			} else if (KeyCode.ENTER == e.getCode()) {
				int caret = code.getCaretPosition();
				String codeText = code.getText();
				int index = codeText.indexOf(search.getText(), caret + 1);
				if (index == -1) {
					// not found after caret, so search unbound (wrap around)
					index = codeText.indexOf(search.getText(), 0);
				}
				// set caret to index
				if (index >= 0) {
					code.selectRange(index, index + search.getText().length());
					code.requestFollowCaret();
				} else {
					Toolkit.getDefaultToolkit().beep();
				}
			}
		});
		// Show search bar
		addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (e.isControlDown() && KeyCode.F == e.getCode()) {
				if (pane.getPinnedSide() == null) {
					pane.setPinnedSide(Side.TOP);
					// "search.requestFocus()" doesnt work because of the rules
					// limiting focus.
					// So we have to wait a bit for it to be focusable.... which
					// is really ugly.
					uglyFocus(search);
				} else {
					pane.setPinnedSide(null);
				}
			}
		});
	}

	protected void setupPane() {
		pane.animationDurationProperty().setValue(Duration.millis(50));
		pane.setContent(new VirtualizedScrollPane<>(code));
		pane.setTop(search);
	}

	private void uglyFocus(CustomTextField search) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							try {
								search.requestFocus();
							} catch (Exception e) {}
						}
					});
				} catch (Exception e) {}
			}
		}.start();
	}

	/**
	 * @param text
	 *            Text to apply styles to.
	 * @return Stylized regions of the text <i>(via css tags)</i>.
	 */
	private StyleSpans<Collection<String>> computeStyle(String text) {
		Matcher matcher = createPattern().matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			//@formatter:off
			String styleClass = getStyleClass(matcher);
			//@formatter:on
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	protected abstract String createTitle();

	protected abstract Image createIcon();

	protected abstract Pattern createPattern();

	protected abstract String getStyleClass(Matcher matcher);
}
