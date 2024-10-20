package software.coley.recaf.services.cfg;

import com.google.common.collect.Iterables;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

public class ControlFlowVertex {
    final List<ControlFlowVertexReference> inRefs = new ArrayList<>();
    final List<ControlFlowVertexReference> outRefs = new ArrayList<>();

    final List<AbstractInsnNode> insns = new ArrayList<>();

    public List<ControlFlowVertexReference> getInRefs() {
        return inRefs;
    }

    public List<ControlFlowVertexReference> getOutRefs() {
        return outRefs;
    }

    public List<AbstractInsnNode> getInsns() {
        return insns;
    }

    public AbstractInsnNode getTerminalNode() {
        return Iterables.getLast(this.insns);
    }
}
