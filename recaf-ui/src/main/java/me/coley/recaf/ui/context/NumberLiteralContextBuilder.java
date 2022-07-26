package me.coley.recaf.ui.context;

import com.github.javaparser.Position;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import me.coley.recaf.ui.control.code.java.JavaArea;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.resource.Resource;
import org.fxmisc.richtext.model.TwoDimensional;

import static me.coley.recaf.ui.util.Menus.action;

public class NumberLiteralContextBuilder extends ContextBuilder {

	private final Number value;
	private final Expression expression;
	private final JavaArea area;
	private final int position;

	public NumberLiteralContextBuilder(Number value, Expression expression, JavaArea area, int position) {
		this.value = value;
		this.expression = expression;
		this.area = area;
		this.position = position;
	}

	@Override
	public ContextMenu build() {
		ContextMenu menu = new ContextMenu();
		MenuItem header = new MenuItem(isLiteral(expression) ? Lang.get("dialog.conv.title.literal") : Lang.get("dialog.conv.title.expression"));
		header.getStyleClass().add("context-menu-header");
		header.setDisable(true);
		menu.getItems().add(header);
		editAction(menu, value.toString() + (value instanceof Long ? "L" : ""));
		if (value instanceof Integer || value instanceof Long) {
			editAction(menu, String.format("0x%x" + (value instanceof Long ? "L" : ""), value));
			if (value instanceof Integer) {
				editAction(menu, "0b" + Integer.toBinaryString((Integer) value));
			} else {
				editAction(menu, "0b" + Long.toBinaryString((Long) value) + "L");
			}
		}
		return menu;
	}

	private boolean isLiteral(Expression expression) {
		while (expression instanceof EnclosedExpr) expression = ((EnclosedExpr) expression).getInner();
		return expression instanceof LiteralExpr;
	}

	private void replace(String valueAsString) {
		expression.getBegin().ifPresent(begin -> {
			Position end = expression.getEnd().orElse(begin);
			TwoDimensional.Position beingPos = area.position(begin.line, begin.column);
			TwoDimensional.Position endPos = area.position(end.line, end.column);
			area.replaceText(area.getAbsolutePosition(beingPos.getMajor() - 1, beingPos.getMinor()) - 1,
				area.getAbsolutePosition(endPos.getMajor() - 1, endPos.getMinor()), valueAsString);
		});
	}

	private void editAction(ContextMenu menu, String name) {
		menu.getItems().add(action(name, Icons.ACTION_EDIT, () -> replace(name)));
	}

	@Override
	protected Resource findContainerResource() {
		throw new UnsupportedOperationException("Not supported");
	}
}
