package me.coley.recaf.ui.context;

import com.github.javaparser.Position;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.resource.Resource;
import org.fxmisc.richtext.model.TwoDimensional;

import static me.coley.recaf.ui.util.Menus.actionLiteral;

public class NumberLiteralContextBuilder extends ContextBuilder {

	private final Number value;
	private final Expression expression;
	private final JavaArea area;

	public NumberLiteralContextBuilder(Number value, Expression expression, JavaArea area) {
		this.value = value;
		this.expression = expression;
		this.area = area;
	}

	@Override
	public ContextMenu build() {
		ContextMenu menu = new ContextMenu();
		// header
		MenuItem header = new MenuItem(
			Lang.get(expression instanceof LiteralExpr ? "dialog.conv.title.literal" : "dialog.conv.title.expression")
		);
		header.getStyleClass().add("context-menu-header");
		header.setDisable(true);
		menu.getItems().add(header);
		// it is basically what's in the source code, null is returned if it's not a literal
		String literal = expression instanceof LiteralExpr ? expression.toString().toLowerCase() : null;
		// literal
		editAction(menu,
			value.toString() + (value instanceof Long ? "L" : ""),
			literal == null || literal.startsWith("0x") || literal.startsWith("0b"));
		// only int and long literals can be converted to hex and binary
		if (value instanceof Integer || value instanceof Long) {
			// hex
			editAction(menu,
				String.format(value instanceof Long ? "0x%xL" : "0x%x", value),
				literal == null || !literal.startsWith("0x") || literal.startsWith("0b"));
			// binary
			final boolean notBin = literal == null || literal.startsWith("0x") || !literal.startsWith("0b");
			if (value instanceof Integer) {
				editAction(menu, "0b" + Integer.toBinaryString((Integer) value), notBin);
			} else {
				editAction(menu, "0b" + Long.toBinaryString((Long) value) + "L", notBin);
			}
		}
		// scientific notation for floating point literals is not supported
		return menu;
	}

	private void replace(String valueAsString) {
		expression.getBegin().ifPresent(begin -> {
			Position end = expression.getEnd().orElse(begin);
			// translating line/column to position
			TwoDimensional.Position beingPos = area.position(begin.line, begin.column);
			TwoDimensional.Position endPos = area.position(end.line, end.column);
			area.replaceText(
				area.getAbsolutePosition(beingPos.getMajor() - 1, beingPos.getMinor()) - 1,
				area.getAbsolutePosition(endPos.getMajor() - 1, endPos.getMinor()),
				valueAsString);
		});
	}

	private void editAction(ContextMenu menu, String textKey, boolean active) {
		ActionMenuItem item = actionLiteral(textKey, Icons.ACTION_EDIT, () -> replace(textKey));
		item.setDisable(!active);
		menu.getItems().add(item);
	}

	@Override
	protected Resource findContainerResource() {
		throw new UnsupportedOperationException("Not supported");
	}
}
