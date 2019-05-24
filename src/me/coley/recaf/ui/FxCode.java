package me.coley.recaf.ui;

import java.awt.Toolkit;
import java.util.Collection;
import java.util.Collections;

import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.component.CodeAreaExt;
import org.controlsfx.control.HiddenSidesPane;
import org.controlsfx.control.textfield.CustomTextField;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.config.impl.ConfKeybinds;
import me.coley.recaf.util.*;

/**
 * Code window with search bar. Syntax-highlighting powered by regex specified
 * by child classes.
 * 
 * @author Matt
 */
public abstract class FxCode extends Stage {
	private final static int WIDTH_BUFF = 30;
	private final static int HEIGHT_BUFF = 40;
	protected final CodeArea code = new CodeAreaExt();
	protected final BorderPane wrapper = new BorderPane();
	protected final HiddenSidesPane pane = new HiddenSidesPane();
	protected final CustomTextField search = new CustomTextField();
	protected final VirtualizedScrollPane<CodeArea> scroll =  new VirtualizedScrollPane<>(code);
	protected FxCode() {
		getIcons().add(createIcon());
		//
		setupCodePane();
		setupSearch();
		setupPane();
		// Default size, but it will be auto-scaled on the JavaFX thread
		wrapper.setCenter(pane);
		setScene(JavaFX.scene(wrapper, ScreenUtil.prefWidth(), ScreenUtil.prefHeight()));
		wrapper.getStyleClass().add("fxcode");
		//
		setupTitle();
		setupAutoSize();
	}

	protected void setupCodePane() {
		code.setEditable(false);
		code.richChanges()
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
				.filter(ch -> ch.getPosition() != 0 || ch.getNetLength() >= code.getLength() - 1)
				.subscribe(change -> {
			code.setStyleSpans(0, computeStyle(code.getText()));
			onCodeChange(code.getText());
		});
	}

	protected void setInitialText(String initialText) {
		// The text is not passed to constructor so that the CSS can be applied
		// via the change listener.
		if (initialText != null && !initialText.isEmpty()) {
			code.appendText(initialText);
			// Dont allow undo to remove the initial text.
			code.getUndoManager().forgetHistory();
			// set position
			code.selectRange(0, 0);
			code.moveTo(0);
			code.scrollToPixel(0, 0);
		}
	}

	/**
	 * Create the search bar and add its functionality <i>(Find exact text
	 * matches)</i>.
	 */
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
			ConfKeybinds keys = ConfKeybinds.instance();
			if (keys.active && !e.isControlDown()) {
				return;
			}
			String kcode = e.getCode().getName();
			if (kcode.equalsIgnoreCase(keys.find)) {
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
		// Add to main pane
		pane.setTop(search);
	}

	/**
	 * Setup main pane, wrapper for other components.
	 */
	protected void setupPane() {
		pane.animationDurationProperty().setValue(Duration.millis(50));
		pane.setContent(scroll);
	}

	/**
	 * Hack for requesting focus for the given text field.
	 * 
	 * @param search
	 *            Field to focus.
	 */
	private void uglyFocus(CustomTextField search) {
		new Thread(() -> {
			try {
				Threads.runLaterFx(500, () -> {
					try {
						search.requestFocus();
					} catch (Exception e) {}
				});
			} catch (Exception e) {}
		}).start();
	}

	/**
	 * createTitle() isn't available immediately, so running it on the FX thread
	 * allows it to be accessed.
	 */
	private void setupTitle() {
		// Allows the value returned by the createTitle to be set in the
		// constructor of child classes.
		Threads.runFx(() -> setTitle(createTitle()));
	}

	/**
	 * Automatically resize the current stage to fix the estimated size of the
	 * code-area. Its not perfect, but its way better than just max-sizing the
	 * window.
	 */
	private void setupAutoSize() {
		// Scrolling is a hack for force the VirtualizedScrollPane to discover
		// the size of its cells (the lines of the CodeArea)
		// This results in a MUCH better estimation of the proper window size.
		int scrollHackCount = 10;
		int scrollHackSize = 1000;
		Threads.runFx(() -> {
			for (int i = 0; i < scrollHackCount; i++)
				scroll.scrollYBy(scrollHackSize);
		});
		// Now auto-size the window
		Threads.runLaterFx(50, () -> {
			// Auto-size window to the size of the decompiled code.
			double autoWidth = scroll.totalWidthEstimateProperty().getValue() + WIDTH_BUFF;
			double autoHeight = scroll.totalHeightEstimateProperty().getValue() + HEIGHT_BUFF;
			autoWidth = Math.min(autoWidth, ScreenUtil.prefWidth() - 100);
			autoHeight = Math.min(autoHeight, ScreenUtil.prefHeight() - 100);
			// Undo scroll hack (back to top)
			for (int i = 0; i < scrollHackCount; i++)
				scroll.scrollYBy(-scrollHackSize);
			// Set size
			setWidth(autoWidth);
			setHeight(autoHeight);
		});
	}

	/**
	 * @param text
	 *            Text to apply styles to.
	 * @return Stylized regions of the text <i>(via css tags)</i>.
	 */
	private StyleSpans<Collection<String>> computeStyle(String text) {
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		Matcher matcher = createPattern().matcher(text);
		while (matcher.find()) {
			String styleClass = getStyleClass(matcher);
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	/**
	 * @return Window title.
	 */
	protected abstract String createTitle();

	/**
	 * @return Window icon.
	 */
	protected abstract Image createIcon();

	/**
	 * @return Regex pattern for applying styles to matched groups.
	 */
	protected abstract Pattern createPattern();

	/**
	 * Intended usage follows the pattern:
	 * 
	 * <pre>
	matcher.group("GROUP1")   != null ? "group1"
	: matcher.group("GROUP2") != null ? "group2"
	: matcher.group("GROUP3") != null ? "group3" :null;
	 * </pre>
	 * 
	 * @param matcher
	 *            Regex matcher.
	 * @return CSS class name associated with the discovered group found by the
	 *         matcher.
	 */
	protected abstract String getStyleClass(Matcher matcher);

	/**
	 * Called when the code in the text pane is updated.
	 * 
	 * @param code
	 *            New code text.
	 */
	protected abstract void onCodeChange(String code);
}
