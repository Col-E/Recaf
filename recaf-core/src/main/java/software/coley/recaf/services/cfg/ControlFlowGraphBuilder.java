package software.coley.recaf.services.cfg;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

public class ControlFlowGraphBuilder {

    final Map<AbstractInsnNode, ControlFlowVertex> vertexByInsn = new HashMap<>();

    public ControlFlowGraph build(ClassNode klass, MethodNode method) {
        AbstractInsnNode[] insns = method.instructions.toArray();
        if (insns == null || insns.length == 0) {
            return null;
        }

        ControlFlowGraph graph = new ControlFlowGraph(klass, method);
        ControlFlowVertex vertex = new ControlFlowVertex();
        for (AbstractInsnNode insn : insns) {
            if (insn.getType() == AbstractInsnNode.LABEL) {
                if (insn != insns[0] && !vertex.getInsns().isEmpty()) {
                    graph.getVertices().add(vertex);
                }

                ControlFlowVertex last = vertex;
                vertex = new ControlFlowVertex();
                vertex.getInsns().add(insn);
                this.vertexByInsn.put(insn, vertex);

                vertex.getInRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.LABEL, last, insn));
                last.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.LABEL, vertex, insn));

                continue;
            }

            vertex.getInsns().add(insn);
            this.vertexByInsn.put(insn, vertex);
            if (this.isTerminalInsn(insn)) {
                graph.getVertices().add(vertex);
                vertex = new ControlFlowVertex();
            }
        }

        for (AbstractInsnNode insn : insns) {
            if (!this.isTerminalInsn(insn)) {
                continue;
            }
            ControlFlowVertex v = this.vertexByInsn.get(insn);
            if (v == null) {
                continue;
            }
            this.computeRefs(v, insn);
        }

        return graph;
    }

    boolean isTerminalInsn(AbstractInsnNode insn) {
        return insn.getType() == AbstractInsnNode.JUMP_INSN
                || (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN);
    }

    // FEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE,
    // IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL
    // TABLESWITCH
    void computeRefs(ControlFlowVertex vertex, AbstractInsnNode node) {
        if (node.getType() == AbstractInsnNode.JUMP_INSN) {
            JumpInsnNode jump = (JumpInsnNode) node;
            ControlFlowVertex jumpVertex = this.vertexByInsn.get(jump.label);
            ControlFlowVertex nextVertex = node.getNext() == null ? null : this.vertexByInsn.get(node.getNext());
            switch (node.getOpcode()) {
                case Opcodes.IFEQ:
                case Opcodes.IFNE:
                case Opcodes.IFLT:
                case Opcodes.IFGE:
                case Opcodes.IFGT:
                case Opcodes.IFLE:

                case Opcodes.IF_ICMPEQ:
                case Opcodes.IF_ICMPNE:
                case Opcodes.IF_ICMPLT:
                case Opcodes.IF_ICMPGE:
                case Opcodes.IF_ICMPGT:
                case Opcodes.IF_ICMPLE:
                case Opcodes.IF_ACMPEQ:
                case Opcodes.IF_ACMPNE:

                case Opcodes.IFNULL:
                case Opcodes.IFNONNULL:
                    if (jumpVertex != null) {
                        vertex.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.JUMP, jumpVertex, node));
                        jumpVertex.getInRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.JUMP, vertex, node));
                    }
                    if (nextVertex != null) {
                        vertex.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.GOTO, nextVertex, node));
                        nextVertex.getInRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.GOTO, vertex, node));
                    }
                    break;

                case Opcodes.GOTO:
                    if (jumpVertex != null) {
                        vertex.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.GOTO, jumpVertex, node));
                        jumpVertex.getInRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.GOTO, vertex, node));
                    }
                    break;

            }
        } else if (node.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
            TableSwitchInsnNode tableSwitchInsn = (TableSwitchInsnNode) node;
            ControlFlowVertex defaultVertex = this.vertexByInsn.get(tableSwitchInsn.dflt);
            if (defaultVertex != null) {
                vertex.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.SWITCH_DEFAULT, defaultVertex, node));
                defaultVertex.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.SWITCH_DEFAULT, vertex, node));
            }

            for (LabelNode label : tableSwitchInsn.labels) {
                ControlFlowVertex labelVertex = this.vertexByInsn.get(label);
                if (labelVertex == null) {
                    continue;
                }
                vertex.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.SWITCH, labelVertex, node));
                labelVertex.getOutRefs().add(new ControlFlowVertexReference(ControlFlowVertexReferenceKind.SWITCH, vertex, node));
            }
        }
    }

}
