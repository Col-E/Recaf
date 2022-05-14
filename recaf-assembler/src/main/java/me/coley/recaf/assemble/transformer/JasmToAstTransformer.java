package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.*;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.util.EscapeUtil;
import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.MethodParameter;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.transform.MethodVisitor;
import me.darknet.assembler.transform.Transformer;
import me.darknet.assembler.transform.Visitor;
import me.darknet.assembler.util.Handles;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * JASM visitor to generate AST instances.
 *
 */
public class JasmToAstTransformer implements Visitor, MethodVisitor {

    Collection<Group> groups;
    Unit unit;
    AbstractMemberDefinition activeMember;
    Code code = new Code();
    List<ThrownException> caughtExceptions = new ArrayList<>();

    public JasmToAstTransformer(Collection<Group> groups) {
        this.groups = groups;
    }

    public Unit generateUnit() throws AssemblerException {
        Transformer transformer = new Transformer(this);
        transformer.transform(groups);
        return unit;
    }

    public String content(Group group) {
        return EscapeUtil.unescapeUnicode(group.content());
    }

    public void add(CodeEntry element) {
        code.add(wrap(latestGroup, (BaseElement & CodeEntry) element));
    }

    Group latestGroup;

    @Override
    public void visit(Group group) throws AssemblerException {
        latestGroup = group;
    }

    @Override
    public void visitLabel(LabelGroup label) throws AssemblerException {
        code.addLabel(new Label(label.getLabel()));
    }

    @Override
    public void visitLookupSwitchInsn(LookupSwitchGroup lookupSwitch) throws AssemblerException {
        List<LookupSwitchInstruction.Entry> entries = new ArrayList<>();
        for (CaseLabelGroup caseLabel : lookupSwitch.caseLabels) {
            entries.add(new LookupSwitchInstruction.Entry(caseLabel.key.asInt(), caseLabel.value.getLabel()));
        }
        add(new LookupSwitchInstruction(Opcodes.LOOKUPSWITCH, entries, lookupSwitch.defaultLabel.getLabel()));
    }

    @Override
    public void visitTableSwitchInsn(TableSwitchGroup tableSwitch) throws AssemblerException {
        List<String> lables = new ArrayList<>();
        for (LabelGroup labelGroup : tableSwitch.getLabelGroups()) {
            lables.add(labelGroup.getLabel());
        }
        add(new TableSwitchInstruction(Opcodes.TABLESWITCH,
                tableSwitch.getMin().asInt(),
                tableSwitch.getMax().asInt(),
                lables,
                tableSwitch.getDefaultLabel().getLabel()));
    }

    @Override
    public void visitCatch(CatchGroup catchGroup) throws AssemblerException {
        code.add(new TryCatch(
                catchGroup.getBegin().getLabel(),
                catchGroup.getEnd().getLabel(),
                catchGroup.getHandler().getLabel(),
                catchGroup.getException().content()));
    }

    @Override
    public void visitVarInsn(int opcode, IdentifierGroup identifier) throws AssemblerException {
        add(new VarInstruction(opcode, content(identifier)));
    }

    @Override
    public void visitDirectVarInsn(int opcode, int var) throws AssemblerException {

    }

    @Override
    public void visitMethodInsn(int opcode, IdentifierGroup desc, boolean itf) throws AssemblerException {
        MethodDescriptor md = new MethodDescriptor(desc.content(), false);
        add(new MethodInstruction(opcode, md.owner, md.name, md.getDescriptor()));
    }

    @Override
    public void visitFieldInsn(int opcode, IdentifierGroup name, IdentifierGroup desc) throws AssemblerException {
        FieldDescriptor fs = new FieldDescriptor(name.content());
        add(new FieldInstruction(opcode, fs.owner, fs.name, desc.content()));
    }

    @Override
    public void visitJumpInsn(int opcode, LabelGroup label) throws AssemblerException {
        add(new JumpInstruction(opcode, label.getLabel()));
    }

    @Override
    public void visitLdcInsn(Group constant) throws AssemblerException {
        add(new LdcInstruction(Opcodes.LDC, convert(constant), from(constant)));
    }

    @Override
    public void visitTypeInsn(int opcode, IdentifierGroup type) throws AssemblerException {
        add(new TypeInstruction(opcode, content(type)));
    }

    @Override
    public void visitIincInsn(IdentifierGroup var, int value) throws AssemblerException {
        add(new IincInstruction(Opcodes.IINC, var.content(), value));
    }

    @Override
    public void visitIntInsn(int opcode, int value) throws AssemblerException {
        if(opcode == Opcodes.NEWARRAY) {
            add(new NewArrayInstruction(opcode, NewArrayInstruction.fromInt(value)));
        }else
            add(new IntInstruction(opcode, value));
    }

    @Override
    public void visitLineNumber(NumberGroup line, IdentifierGroup label) throws AssemblerException {
        add(new LineInstruction(-1, label.content(), line.asInt()));
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) throws AssemblerException {
        add(new MultiArrayInstruction(Opcodes.MULTIANEWARRAY, desc, dims));
    }

    @Override
    public void visitInvokeDyanmicInsn(String identifier, IdentifierGroup descriptor, HandleGroup handle, ArgsGroup args) throws AssemblerException {
        MethodDescriptor mdh = new MethodDescriptor(handle.getDescriptor().content(), false);
        HandleInfo handleInfo = new HandleInfo(
                handle.getHandleType().content(),
                mdh.owner,
                mdh.name,
                mdh.getDescriptor());
        List<IndyInstruction.BsmArg> bsmArgs = new ArrayList<>();
        for (Group arg : args.getBody().getChildren()) {
            bsmArgs.add(new IndyInstruction.BsmArg(from(arg), convert(arg)));
        }
        add(new IndyInstruction(
                Opcodes.INVOKEDYNAMIC,
                identifier,
                descriptor.content(),
                handleInfo,
                bsmArgs));
    }

    public ArgType from(Group group) {
        if(group instanceof NumberGroup) {
            NumberGroup number = (NumberGroup) group;
            if(number.isFloat()) {
                return number.isWide() ? ArgType.DOUBLE : ArgType.FLOAT;
            }else {
                return number.isWide() ? ArgType.LONG : ArgType.INTEGER;
            }
        }else if(group instanceof StringGroup) {
            return ArgType.STRING;
        }else if(group instanceof TypeGroup) {
            return ArgType.TYPE;
        }else if(group instanceof HandleGroup) {
            return ArgType.HANDLE;
        }
        throw new IllegalArgumentException("Cannot convert to constant '" + group.content() + "'");
    }

    @Override
    public void visitInsn(int opcode) throws AssemblerException {
        add(new Instruction(opcode));
    }

    @Override
    public void visitExpr(ExprGroup expr) throws AssemblerException {
        add(new Expression(expr.textGroup.content()));
    }

    @Override
    public void visitClass(AccessModsGroup accessMods, IdentifierGroup identifier) throws AssemblerException {

    }

    @Override
    public void visitSuper(ExtendsGroup extendsGroup) throws AssemblerException {

    }

    @Override
    public void visitImplements(ImplementsGroup implementsGroup) throws AssemblerException {

    }

    @Override
    public void visitField(AccessModsGroup accessMods, IdentifierGroup name, IdentifierGroup descriptor, Group constantValue) throws AssemblerException {

        FieldDefinition field = new FieldDefinition(fromAccessMods(accessMods), name.content(), descriptor.content());

        activeMember = field;
        if(constantValue != null)
            field.setConstVal(new ConstVal(convert(constantValue), from(constantValue)));

    }

    @Override
    public MethodVisitor visitMethod(AccessModsGroup accessMods, IdentifierGroup descriptor, BodyGroup body) throws AssemblerException {
        MethodDescriptor md = new MethodDescriptor(descriptor.content(), true);
        MethodParameters parameters = new MethodParameters();
        for(MethodParameter mp : md.parameters) {
            parameters.add(new me.coley.recaf.assemble.ast.arch.MethodParameter(mp.getDescriptor(), mp.getName()));
        }
        this.code = new Code();
        MethodDefinition method = new MethodDefinition(
                fromAccessMods(accessMods),
                md.name,
                parameters,
                md.returnType,
                this.code);
        for(ThrownException thrown : this.caughtExceptions) {
            method.addThrownException(thrown);
        }
        activeMember = method;
        caughtExceptions.clear();
        return this;
    }

    public Modifiers fromAccessMods(AccessModsGroup accessMods) {
        Modifiers modifiers = new Modifiers();
        for(AccessModGroup accessMod : accessMods.accessMods) {
            modifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
        }
        return modifiers;
    }

    @Override
    public void visitAnnotation(AnnotationGroup annotation) throws AssemblerException {

    }

    @Override
    public void visitSignature(SignatureGroup signature) throws AssemblerException {

    }

    @Override
    public void visitThrows(ThrowsGroup throwsGroup) throws AssemblerException {
        caughtExceptions.add(new ThrownException(throwsGroup.getClassName().content()));
    }

    @Override
    public void visitExpression(ExprGroup expr) throws AssemblerException {

    }

    @Override
    public void visitEnd() throws AssemblerException {
        unit = new Unit(activeMember);
    }

    @Override
    public void visitEndClass() throws AssemblerException {
        if(activeMember != null && activeMember.isField())
            unit = new Unit(activeMember);
    }

    public Object convert(Group group) throws AssemblerException {
        if (group.getType() == Group.GroupType.NUMBER) {
            return ((NumberGroup) group).getNumber();
        } else if(group.type == Group.GroupType.TYPE) {
            TypeGroup typeGroup = (TypeGroup) group;
            try {
                String desc = typeGroup.descriptor.content();
                if(desc.isEmpty()) return Type.getType(desc);
                if(desc.charAt(0) == '(') {
                    return Type.getMethodType(typeGroup.descriptor.content());
                } else {
                    return Type.getObjectType(typeGroup.descriptor.content());
                }
            } catch (IllegalArgumentException e) {
                throw new AssemblerException("Invalid type: " + typeGroup.descriptor.content(), typeGroup.location());
            }
        } else if(group.type == Group.GroupType.HANDLE) {
            HandleGroup handle = (HandleGroup) group;
            String typeString = handle.getHandleType().content();
            if(!Handles.isValid(typeString)) {
                throw new AssemblerException("Unknown handle type " + typeString, handle.location());
            }
            int type = Handles.getType(typeString);
            MethodDescriptor md = new MethodDescriptor(handle.getDescriptor().content(), false);
            return new Handle(
                    type,
                    md.owner == null ? "" : md.owner,
                    md.name,
                    md.getDescriptor(),
                    type == Opcodes.H_INVOKEINTERFACE);
        }else {
            return group.content();
        }
    }

    private static <E extends BaseElement> E wrap(Group group, E element) {
        Token start = group.value;
        Token end = group.end();
        if(end == null)
            end = start;
        return element.setLine(start.getLocation().getLine()).setRange(
                start.getLocation().getStartPosition(),
                end.getLocation().getEndPosition());
    }
}
