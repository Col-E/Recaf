package me.coley.recaf.graph;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Analyzer;
import me.coley.recaf.assemble.analysis.Block;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.visitor.SingleMemberVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.TreeMap;

public class MethodGraph {
	private final MethodInfo targetMethod;
	private final ClassInfo classInfo;

	public MethodGraph(MethodInfo methodInfo, ClassInfo classInfo) {
		this.targetMethod = methodInfo;
		this.classInfo = classInfo;
	}

	public TreeMap<Integer, Block> generate() throws AstException {

		ClassNode node = new ClassNode();
		ClassReader cr = classInfo.getClassReader();
		cr.accept(new SingleMemberVisitor(node, targetMethod), ClassReader.SKIP_FRAMES);

		MethodNode methodNode = node.methods.get(0);

		BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(methodNode);
		transformer.visit();
		Unit unit = transformer.getUnit();

		MethodDefinition method = unit.getDefinitionAsMethod();

		Analyzer analyzer = new Analyzer(classInfo.getName(), method);

		Analysis analysis = analyzer.analyzeBlocks();

		return analysis.getBlocks();

	}

}
