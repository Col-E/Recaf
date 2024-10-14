package software.coley.recaf.services.cfg;

import jakarta.inject.Inject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.workspace.model.Workspace;

@WorkspaceScoped
public class ControlFlowGraphService {

    final Workspace workspace;

    @Inject
    public ControlFlowGraphService(Workspace workspace) {
        this.workspace = workspace;
    }

    public ControlFlowGraph createControlFlow(ClassInfo klass, MethodMember member) {
        if (!member.isMethod()) {
            return null;
        }
        JvmClassInfo jvmClassInfo = this.workspace.getPrimaryResource().getJvmClassBundle().get(klass.getName());
        if (jvmClassInfo == null) {
            return null;
        }
        ClassReader reader = jvmClassInfo.getClassReader();
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_DEBUG);
        MethodNode method = node.methods.stream()
                .filter(it -> it.name.equals(member.getName()) && it.desc.equals(member.getDescriptor()))
                .findFirst()
                .orElse(null);
        if (method == null) {
            return null;
        }
        return new ControlFlowGraphBuilder().build(node, method);
    }
}
