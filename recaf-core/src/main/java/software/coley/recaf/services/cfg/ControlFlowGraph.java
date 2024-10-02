package software.coley.recaf.services.cfg;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class ControlFlowGraph {

    final List<ControlFlowVertex> vertices = new ArrayList<>();
    final ClassNode klass;
    final MethodNode method;

    public ControlFlowGraph(ClassNode klass, MethodNode method) {
        this.klass = klass;
        this.method = method;
    }

    public List<ControlFlowVertex> getVertices() {
        return vertices;
    }

    public ClassNode getKlass() {
        return klass;
    }

    public MethodNode getMethod() {
        return method;
    }
}
