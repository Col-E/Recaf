package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Descriptor;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.PrintContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Part of a {@link MethodDefinition} describing the method descriptor and parameter names.
 *
 * @author Matt Coley
 */
public class MethodParameters extends BaseElement implements Element, Descriptor, Iterable<MethodParameter> {
	private final List<MethodParameter> parameters = new ArrayList<>();

	/**
	 * @param params
	 * 		Parameters to add.
	 */
	public void addAll(Collection<MethodParameter> params) {
		parameters.addAll(params);
	}

	/**
	 * @param param
	 * 		Parameter to add.
	 */
	public void add(MethodParameter param) {
		parameters.add(param);
	}

	/**
	 * @return Existing parameters.
	 */
	public List<MethodParameter> getParameters() {
		return parameters;
	}

	@Override
	public String print(PrintContext context) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parameters.size(); i++) {
			MethodParameter param = parameters.get(i);
			sb.append(param.print(context));
			if (i < parameters.size() - 1)
				sb.append(", ");
		}
		return sb.toString();
	}

	@Override
	public String getDesc() {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for (MethodParameter param : parameters) {
			sb.append(param.getDesc());
		}
		sb.append(')');
		return sb.toString();
	}

	@Override
	public Iterator<MethodParameter> iterator() {
		return parameters.iterator();
	}
}
