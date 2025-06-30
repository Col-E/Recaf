package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import static org.objectweb.asm.Opcodes.NOP;

/**
 * A transformer that clean the nop instructions from methods.
 *
 * @author Canrad
 */
public class NopRemovingTransformer implements JvmClassTransformer {

    public NopRemovingTransformer() {

    }

    @Override
    public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
                          @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
                          @Nonnull JvmClassInfo initialClassState) throws TransformationException {

        boolean dirty = false;
        ClassNode node = context.getNode(bundle, initialClassState);
        for (MethodNode method : node.methods) {
            InsnList instructions = method.instructions;
            if (instructions == null)
                continue;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == NOP) {
                    dirty = true;
                    method.instructions.remove(insn);
                }
            }
        }
        if (dirty){
            context.setNode(bundle, initialClassState, node);
        }
    }

    @Nonnull
    @Override
    public String name() {
        return "nop removal";
    }
}
