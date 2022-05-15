package me.coley.recaf.util.visitor;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.code.MemberInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public class ClassReplacingVisitor extends ClassVisitor {

    private final MemberInfo memberInfo;
    private final ClassNode replacementClass;
    private boolean replaced;

    /**
     * @param cv
     * 		Parent visitor where the removal will be applied in.
     * @param memberInfo
     * 		Details of the field to replace.
     * @param replacementClass
     * 		Class to replace with.
     */
    public ClassReplacingVisitor(org.objectweb.asm.ClassVisitor cv, MemberInfo memberInfo, ClassNode replacementClass) {
        super(RecafConstants.ASM_VERSION, cv);
        this.memberInfo = memberInfo;
        this.replacementClass = replacementClass;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if(memberInfo.getName().equals(name)) {
            replaced = true;
            replacementClass.accept(this);
        }
    }
}
