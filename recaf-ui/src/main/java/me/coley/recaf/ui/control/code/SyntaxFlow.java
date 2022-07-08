package me.coley.recaf.ui.control.code;

import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Text flow for syntax highlighted code.
 *
 * @author Matt Coley
 */
public class SyntaxFlow extends TextFlow implements Styleable {
	private final LanguageStyler styler;
	private List<LanguageStyler.Section> sections;

	/**
	 * @param language
	 * 		Language to use for syntax highlighting.
	 */
	public SyntaxFlow(Language language) {
		styler = new LanguageStyler(language, this);
		getStyleClass().add("code-area");
	}

	/**
	 * @param code
	 * 		Code to generate styled syntax flow of.
	 *
	 * @return Future for tracking completion of style computations.
	 * Delegated to {@link #onClearStyle()} or {@link #onApplyStyle(int, List)}.
	 */
	public CompletableFuture<Void> setCode(String code) {
		return styler.styleCompleteDocument(code);
	}

	@Override
	public Collection<String> getStyleAtPosition(int pos) {
		int offset = 0;
		for (LanguageStyler.Section section : sections) {
			int start = offset;
			int end = start + section.length;
			if (pos >= start && pos <= end) {
				return section.classes;
			}
			offset += section.length;
		}
		return Collections.emptySet();
	}

	@Override
	public CompletableFuture<Void> onClearStyle() {
		return CompletableFuture.runAsync(() -> getChildren().clear(), FxThreadUtil.executor());
	}

	@Override
	public CompletableFuture<Void> onApplyStyle(int start, List<LanguageStyler.Section> sections) {
		this.sections = sections;
		List<Node> nodes = new ArrayList<>();
		for (LanguageStyler.Section section : sections) {
			Text text = new Text(section.text);
			text.getStyleClass().addAll(section.classes);
			nodes.add(text);
		}
		if (Thread.currentThread().isInterrupted())
			return ThreadUtil.failedFuture(new InterruptedException());
		return CompletableFuture.runAsync(() -> getChildren().addAll(nodes), FxThreadUtil.executor());
	}
}
