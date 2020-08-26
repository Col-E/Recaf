package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.AST;
import me.coley.recaf.parse.bytecode.ast.LabelAST;
import me.coley.recaf.parse.bytecode.ast.MethodDefinitionAST;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AST compilation context.
 *
 * @author xxDark
 */
public final class MethodCompilation {

    private final ParseResult<RootAST> ast;
    private final MethodDefinitionAST methodDefinition;
    private final MethodNode node;
    private final Map<String, LabelNode> nameToLabel = new HashMap<>();
    private final Map<LabelNode, LabelAST> labelToAst = new HashMap<>();
    private final Map<AbstractInsnNode, AST> insnToAst = new HashMap<>();
    private final Map<Integer, AbstractInsnNode> lineToInsn = new HashMap<>();
    private Variables variables;

    /**
     * @param ast
     *      Root AST.
     * @param methodDefinition
     *      Definition of the method to compile.
     * @param node
     *      ASM representation of the method.
     */
    public MethodCompilation(ParseResult<RootAST> ast, MethodDefinitionAST methodDefinition, MethodNode node) {
        this.ast = ast;
        this.methodDefinition = methodDefinition;
        this.node = node;
    }

    /**
     * @return  root AST parse result.
     */
    public ParseResult<RootAST> getAst() {
        return ast;
    }

    /**
     * @return method definition.
     */
    public MethodDefinitionAST getMethodDefinition() {
        return methodDefinition;
    }

    /**
     * @return ASM representation of the method.
     */
    public MethodNode getNode() {
        return node;
    }

    /**
     * @param name
     *      Name of the label.
     *
     * @return label by it's name. May be {@code null}.
     */
    public LabelNode getLabel(String name) {
        return nameToLabel.get(name);
    }

    /**
     * @param node
     *      Label node.
     *
     * @return label's AST. May be {@code null}.
     */
    public LabelAST getLabelAst(LabelNode node) {
        return labelToAst.get(node);
    }

    /**
     * @return a map of name > label layout.
     */
    public Map<String, LabelNode> getNameToLabel() {
        return Collections.unmodifiableMap(nameToLabel);
    }

    /**
     * @return a map of label > AST layout.
     */
    public Map<LabelNode, LabelAST> getLabelToAst() {
        return Collections.unmodifiableMap(labelToAst);
    }

    /**
     * Adds and register instruction to specific AST.
     *
     * @param insn
     *      Instruction to assign.
     * @param ast
     *      Instruction's AST.
     *
     * @see MethodCompilation#assignInstruction(AbstractInsnNode, AST)
     */
    public void addInstruction(AbstractInsnNode insn, AST ast) {
        node.instructions.add(insn);
        assignInstruction(insn, ast);
    }

    /**
     * Assigns instruction to specific AST.
     * Also sets the line to specific instruction.
     *
     * @param insn
     *      Instruction to assign.
     * @param ast
     *      Instruction's AST.
     */
    public void assignInstruction(AbstractInsnNode insn, AST ast) {
        insnToAst.put(insn, ast);
        lineToInsn.put(ast.getLine(), insn);
    }

    /**
     * Assigns label to specific AST.
     *
     * @param label
     *      Instruction to assign.
     * @param ast
     *      Label's AST.
     */
    public void assignLabel(LabelNode label, LabelAST ast) {
        nameToLabel.put(ast.getName().getName(), label);
        labelToAst.put(label, ast);
    }

    /**
     * @param node
     *      Instruction to get line from.
     *
     * @return line of the instruction. May return {@code -1} if AST is missing.
     */
    public int getLine(AbstractInsnNode node) {
        AST ast = insnToAst.get(node);
        return ast == null ? -1 : ast.getLine();
    }

    /**
     * @param line
     *      Line to get instruction from.
     * @return instruction by it's line. May be {@code null}.
     */
    public AbstractInsnNode getInsn(int line) {
        return lineToInsn.get(line);
    }

    /**
     * @return variables of the method
     */
    public Variables getVariables() {
        return variables;
    }

    // Internal methods
    void setVariables(Variables variables) {
        this.variables = variables;
    }
}
