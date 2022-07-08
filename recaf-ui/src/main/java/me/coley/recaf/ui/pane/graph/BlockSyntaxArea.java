package me.coley.recaf.ui.pane.graph;

import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;

public class BlockSyntaxArea extends SyntaxArea {

	public BlockSyntaxArea() {
		super(Languages.JAVA_BYTECODE, null);
	}

	@Override
	protected void setupParagraphFactory() {
		// Noop
	}
}
