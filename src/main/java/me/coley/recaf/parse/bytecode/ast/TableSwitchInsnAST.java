package me.coley.recaf.parse.bytecode.ast;

import me.coley.recaf.parse.bytecode.MethodCompilation;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Table switch instruction AST.
 *
 * @author Matt
 */
public class TableSwitchInsnAST extends InsnAST implements FlowController {
	private final NumberAST rangeMin;
	private final NumberAST rangeMax;
	private final List<NameAST> labels;
	private final NameAST dfltLabel;

	/**
	 * @param line
	 * 		Line number this node is written on.
	 * @param start
	 * 		Offset from line start this node starts at.
	 * @param opcode
	 * 		Opcode AST.
	 * @param rangeMin
	 * 		Min switch range value AST.
	 * @param rangeMax
	 * 		Max switch range value AST.
	 * @param labels
	 * 		Labels (AST) that link to value in range.
	 * @param dfltLabel
	 * 		Default fallback label AST.
	 */
	public TableSwitchInsnAST(int line, int start, OpcodeAST opcode, NumberAST rangeMin,
							  NumberAST rangeMax, List<NameAST> labels, NameAST dfltLabel) {
		super(line, start, opcode);
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		this.labels = labels;
		this.dfltLabel = dfltLabel;
		addChild(rangeMin);
		addChild(rangeMax);
		labels.forEach(this::addChild);
		addChild(dfltLabel);
	}

	/**
	 * @return Min switch range value AST.
	 */
	public NumberAST getRangeMin() {
		return rangeMin;
	}

	/**
	 * @return Max switch range value AST.
	 */
	public NumberAST getRangeMax() {
		return rangeMax;
	}

	/**
	 * @return Labels (AST) that link to value in range.
	 */
	public List<NameAST> getLabels() {
		return labels;
	}

	/**
	 * @return Default fallback label AST.
	 */
	public NameAST getDfltLabel() {
		return dfltLabel;
	}

	@Override
	public String print() {
		String lbls = labels.stream()
				.map(NameAST::print)
				.collect(Collectors.joining(", "));
		return getOpcode().print() +
				" range[" + rangeMin.print() +":" + rangeMax.print() + "]" +
				" offsets[" + lbls + "]" +
				" default[" + dfltLabel.print() + "]";
	}

	@Override
	public void compile(MethodCompilation compilation) throws AssemblerException {
		LabelNode[] lbls = getLabels().stream()
				.map(ast -> compilation.getLabel(ast.getName()))
				.toArray(LabelNode[]::new);
		LabelNode dflt = compilation.getLabel(getDfltLabel().getName());
		compilation.addInstruction(new TableSwitchInsnNode(getRangeMin().getIntValue(), getRangeMax().getIntValue(),
				dflt, lbls), this);
	}

	@Override
	public List<String> targets() {
		List<String> targets = new ArrayList<>();
		targets.add(getDfltLabel().getName());
		targets.addAll(getLabels().stream().map(NameAST::getName).collect(Collectors.toList()));
		return targets;
	}
}
