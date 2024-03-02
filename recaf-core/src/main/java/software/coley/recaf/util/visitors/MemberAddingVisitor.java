package software.coley.recaf.util.visitors;


import org.objectweb.asm.ClassVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;


public class MemberAddingVisitor extends ClassVisitor {

    private final ClassMember member;

    public MemberAddingVisitor(ClassVisitor cv, ClassMember member) {
        super(RecafConstants.getAsmVersion(), cv);
        this.member = member;
    }

    @Override
    public void visit(int i, int i1, String s, String s1, String s2, String[] strings) {
        super.visit(i, i1, s, s1, s2, strings);
        if(member instanceof FieldMember fm) {
            super.visitField(member.getAccess(), member.getName(), member.getDescriptor(), member.getSignature(),
                    fm.getDefaultValue()).visitEnd();
        } else if (member instanceof MethodMember mm) {
            super.visitMethod(member.getAccess(), member.getName(), member.getDescriptor(), member.getSignature(),
                    mm.getThrownTypes().toArray(new String[0])).visitEnd();
        }
    }
}
