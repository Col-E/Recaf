package me.coley.recaf.assemble.ast.arch.record;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;

import java.util.ArrayList;
import java.util.List;

public class Record extends BaseElement {

	private final List<RecordComponent> components = new ArrayList<>();

	public void addComponent(RecordComponent component) {
		components.add(component);
		child(component);
	}

	public List<RecordComponent> getComponents() {
		return components;
	}

	@Override
	public String print(PrintContext context) {
		StringBuilder sb = new StringBuilder();
		sb.append(context.fmtKeyword("record")).append("\n");
		for (RecordComponent component : components) {
			sb.append(component.print(context));
		}
		sb.append(context.fmtKeyword("end"));
		return sb.toString();
	}
}
