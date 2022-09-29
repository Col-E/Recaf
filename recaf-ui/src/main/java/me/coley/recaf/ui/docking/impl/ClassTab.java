package me.coley.recaf.ui.docking.impl;

import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.FontSizeChangeable;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.util.TextDisplayUtil;

import java.util.function.Consumer;

/**
 * Wrapper around content representing a {@link me.coley.recaf.code.CommonClassInfo}.
 *
 * @author Matt Coley
 * @see ClassRepresentation
 */
public class ClassTab extends DockTab implements FontSizeChangeable {
	private final ClassRepresentation classRepresentation;

	/**
	 * @param title
	 * 		Title of the tab.
	 * @param classRepresentation
	 * 		Representation of the class.
	 */
	public ClassTab(String title, ClassRepresentation classRepresentation) {
		super(TextDisplayUtil.shortenEscapeLimit(title), classRepresentation.getNodeRepresentation());
		this.classRepresentation = classRepresentation;
	}

	/**
	 * @return Representation of the class.
	 */
	public ClassRepresentation getClassRepresentation() {
		return classRepresentation;
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		if (!(classRepresentation instanceof FontSizeChangeable)) return;
		FontSizeChangeable fsc = (FontSizeChangeable) classRepresentation;
		fsc.bindFontSize(property);
	}

	@Override
	public void  applyEventsForFontSizeChange(Consumer<Node> consumer) {
		if (!(classRepresentation instanceof FontSizeChangeable)) return;
		FontSizeChangeable fsc = (FontSizeChangeable) classRepresentation;
		fsc.applyEventsForFontSizeChange(consumer);
	}
}
