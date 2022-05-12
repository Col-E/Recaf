package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.Descriptor;
import me.coley.recaf.assemble.ast.Element;

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
	public String print() {
		return parameters.stream()
				.map(MethodParameter::print)
				.collect(Collectors.joining(","));
	}

	@Override
	public String getDesc() {
		return "(" + parameters.stream()
				.map(MethodParameter::getDesc)
				.collect(Collectors.joining()) + ")";
	}

	@Override
	public Iterator<MethodParameter> iterator() {
		return parameters.iterator();
	}
}
