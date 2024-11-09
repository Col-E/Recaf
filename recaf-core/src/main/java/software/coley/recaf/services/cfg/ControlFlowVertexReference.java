package software.coley.recaf.services.cfg;

import org.objectweb.asm.tree.AbstractInsnNode;

public class ControlFlowVertexReference {
    final ControlFlowVertexReferenceKind kind;
    final ControlFlowVertex vertex;
    final AbstractInsnNode insn;

    public ControlFlowVertexReference(ControlFlowVertexReferenceKind kind, ControlFlowVertex vertex, AbstractInsnNode insn) {
        this.kind = kind;
        this.vertex = vertex;
        this.insn = insn;
    }

    public ControlFlowVertexReferenceKind getKind() {
        return kind;
    }

    public ControlFlowVertex getVertex() {
        return vertex;
    }

    public AbstractInsnNode getInsn() {
        return insn;
    }
}
