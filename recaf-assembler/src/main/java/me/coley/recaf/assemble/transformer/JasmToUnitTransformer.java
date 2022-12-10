package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.*;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.util.logging.Logging;
import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.ParserContext;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.parser.groups.attributes.*;
import me.darknet.assembler.parser.groups.annotation.*;
import me.darknet.assembler.parser.groups.declaration.*;
import me.darknet.assembler.parser.groups.frame.*;
import me.darknet.assembler.parser.groups.instructions.*;
import me.darknet.assembler.parser.groups.method.*;
import me.darknet.assembler.parser.groups.module.*;
import me.darknet.assembler.parser.groups.record.RecordComponentGroup;
import me.darknet.assembler.parser.groups.record.RecordGroup;
import me.darknet.assembler.transform.*;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static me.coley.recaf.assemble.transformer.JasmTransformUtil.*;

/**
 * JASM visitor to generate AST instances.
 *
 * @author Justus Garbe
 * @author Matt Coley
 */
public class JasmToUnitTransformer extends AbstractTopLevelGroupVisitor implements Opcodes {
	private static final Logger logger = Logging.get(JasmToUnitTransformer.class);
	private final Collection<Group> groups;
	private Unit unit;
	private AbstractDefinition definition;


	/**
	 * @param groups
	 * 		Parser group input generated from {@link ParserContext}.
	 */
	public JasmToUnitTransformer(Collection<Group> groups) {
		this.groups = groups;
	}

	/**
	 * @return Recaf AST unit.
	 *
	 * @throws AssemblerException
	 * 		When the unit failed to be assembled.
	 */
	public Unit generateUnit() throws AssemblerException {
		Transformer transformer = new Transformer(this);
		transformer.transform(groups);
		return unit;
	}

	@Override
	public void visitEnd() {
		unit = new Unit(definition);
	}

	@Override
	public ClassGroupVisitor visitImplClass(ClassDeclarationGroup decl) {
		ClassDefinition classDefinition = wrap(decl, new ClassDefinition(
				convertModifiers(decl.getAccessMods()),
				content(decl.getName())
		));
		if(decl.getExtendsGroup() != null) {
			classDefinition.setSuperClass(content(decl.getExtendsGroup().getClassName()));
		}
		if(decl.getImplementsGroups() != null) {
			for(ImplementsGroup impl : decl.getImplementsGroups()) {
				classDefinition.addInterface(content(impl.getClassName()));
			}
		}
		setDefinition(classDefinition);
		return new ClassGroupVisitor() {

			@Override
			public void visitAnnotation(AnnotationGroup annotation) throws AssemblerException {
				classDefinition.addAnnotation(convertAnnotation(annotation));
			}

			@Override
			public void visitSignature(SignatureGroup signature) {
				classDefinition.setSignature(convertSignature(signature));
			}

			@Override
			public void visitVersion(VersionGroup version) throws AssemblerException {
				classDefinition.setVersion(version.getVersion());
			}

			@Override
			public void visitSourceFile(SourceFileGroup sourceFile) throws AssemblerException {
				classDefinition.setSourceFile(sourceFile.getSourceFile());
			}

			@Override
			public void visitInnerClass(InnerClassGroup innerClass) throws AssemblerException {
				classDefinition.addInnerClass(convertInnerClass(innerClass));
			}

			@Override
			public void visitNestHost(NestHostGroup nestHost) throws AssemblerException {
				classDefinition.setNestHost(content(nestHost.getHostName()));
			}

			@Override
			public void visitNestMember(NestMemberGroup nestMember) throws AssemblerException {
				classDefinition.addNestMember(content(nestMember.getMemberName()));
			}

			@Override
			public void visitModule(ModuleGroup module) throws AssemblerException {
				classDefinition.setModule(convertModule(module));
			}

			@Override
			public void visitRecord(RecordGroup record) throws AssemblerException {
				classDefinition.setRecord(convertRecord(record));
			}

			@Override
			public void visitPermittedSubclass(PermittedSubclassGroup permittedSubclass) throws AssemblerException {
				classDefinition.addPermittedSubclass(content(permittedSubclass.getSubclass()));
			}

			@Override
			public void visitDeprecated(DeprecatedGroup deprecated) throws AssemblerException {
				classDefinition.setDeprecated(true);
			}
		};
	}

	@Override
	public FieldGroupVisitor visitImplField(FieldDeclarationGroup decl) throws AssemblerException {
		FieldDefinition fieldDefinition = wrap(decl, new FieldDefinition(
				convertModifiers(decl.getAccessMods()),
				content(decl.getName()),
				content(decl.getDescriptor())
		));
		if (decl.getConstantValue() != null) {
			fieldDefinition.setConstVal(new ConstVal(
					convert(decl.getConstantValue()),
					argType(decl.getConstantValue())
			));
		}
		return new FieldGroupVisitor() {
			@Override
			public void visitEnd() {
				if (hasDefinition()) {
					if (!operateClass(cls -> cls.addField(fieldDefinition))) {
						logger.error("Attempted to visit a field without being the sole definition, " +
										"or child of a class definition. Skipping this field: {}",
								fieldDefinition.getName());
					}
				} else {
					setDefinition(fieldDefinition);
				}
			}

			@Override
			public void visitAnnotation(AnnotationGroup annotation) throws AssemblerException {
				fieldDefinition.addAnnotation(convertAnnotation(annotation));
			}

			@Override
			public void visitSignature(SignatureGroup signature) {
				fieldDefinition.setSignature(convertSignature(signature));
			}

			@Override
			public void visitDeprecated(DeprecatedGroup deprecated) throws AssemblerException {
				fieldDefinition.setDeprecated(true);
			}
		};
	}

	@Override
	public MethodGroupVisitor visitImplMethod(MethodDeclarationGroup decl) {
		MethodParameters parameters = new MethodParameters();
		for (MethodParameterGroup param : decl.getParams().getMethodParameters())
			parameters.add(new MethodParameter(param.getDescriptorValue(), param.getNameValue()));
		Code code = new Code();
		MethodDefinition methodDefinition = wrap(decl, new MethodDefinition(
				convertModifiers(decl.getAccessMods()),
				decl.getName().content(),
				parameters,
				decl.getReturnType(),
				code
		));
		return new MethodGroupVisitor() {
			private Group lastGroup;

			@Override
			public void visitEnd() {
				if (hasDefinition()) {
					if (!operateClass(cls -> cls.addMethod(methodDefinition))) {
						logger.error("Attempted to visit a method without being the sole definition, " +
										"or child of a class definition. Skipping this method: {}",
								methodDefinition.getName());
					}
				} else {
					setDefinition(methodDefinition);
				}
			}

			@Override
			public void visitAnnotation(AnnotationGroup annotation) throws AssemblerException {
				methodDefinition.addAnnotation(convertAnnotation(annotation));
			}

			@Override
			public void visitSignature(SignatureGroup signature) {
				methodDefinition.setSignature(convertSignature(signature));
			}

			@Override
			public void visitThrows(ThrowsGroup thrw) {
				methodDefinition.addThrownException(convertThrows(thrw));
			}

			@Override
			public void visitLabel(LabelGroup label) {
				code.addLabel(wrap(label, new Label(label.getLabel())));
			}

			@Override
			public void visitLookupSwitchInsn(LookupSwitchGroup lookupSwitch) {
				List<LookupSwitchInstruction.Entry> entries = new ArrayList<>();
				for (CaseLabelGroup caseLabel : lookupSwitch.getCaseLabels()) {
					entries.add(new LookupSwitchInstruction.Entry(
							caseLabel.getKey().asInt(),
							caseLabel.getLabelValue().getLabel())
					);
				}
				add(new LookupSwitchInstruction(Opcodes.LOOKUPSWITCH, entries,
						lookupSwitch.getDefaultLabel().getLabel()));
			}

			@Override
			public void visitTableSwitchInsn(TableSwitchGroup tableSwitch) {
				List<String> lables = new ArrayList<>();
				for (LabelGroup labelGroup : tableSwitch.getLabels()) {
					lables.add(labelGroup.getLabel());
				}
				add(new TableSwitchInstruction(Opcodes.TABLESWITCH,
						tableSwitch.getMin().asInt(),
						tableSwitch.getMax().asInt(),
						lables,
						tableSwitch.getDefaultLabel().getLabel()));
			}

			@Override
			public void visitCatch(CatchGroup catchGroup) {
				code.add(new TryCatch(
						catchGroup.getBegin().getLabel(),
						catchGroup.getEnd().getLabel(),
						catchGroup.getHandler().getLabel(),
						content(catchGroup.getException())));
			}

			@Override
			public void visitVarInsn(int opcode, IdentifierGroup identifier) {
				add(new VarInstruction(opcode, content(identifier)));
			}

			@Override
			public void visitDirectVarInsn(int opcode, int var) {
				// no-op, Recaf only supports named variables
			}

			@Override
			public void visitMethodInsn(int opcode, IdentifierGroup name, IdentifierGroup desc, boolean itf) {
				MethodDescriptor md = new MethodDescriptor(name.content(), desc.content());
				add(new MethodInstruction(opcode, md.getOwner(), md.getName(), md.getDescriptor(), itf));
			}

			@Override
			public void visitFieldInsn(int opcode, IdentifierGroup name, IdentifierGroup desc) {
				FieldDescriptor fs = new FieldDescriptor(name.content(), desc.content());
				add(new FieldInstruction(opcode, fs.getOwner(), fs.getName(), desc.content()));
			}

			@Override
			public void visitJumpInsn(int opcode, LabelGroup label) {
				add(new JumpInstruction(opcode, label.getLabel()));
			}

			@Override
			public void visitLdcInsn(Group constant) throws AssemblerException {
				add(new LdcInstruction(Opcodes.LDC, convert(constant)));
			}

			@Override
			public void visitTypeInsn(int opcode, IdentifierGroup type) {
				add(new TypeInstruction(opcode, content(type)));
			}

			@Override
			public void visitIincInsn(IdentifierGroup var, int value) {
				add(new IincInstruction(Opcodes.IINC, var.content(), value));
			}

			@Override
			public void visitIntInsn(int opcode, int value) {
				if (opcode == Opcodes.NEWARRAY)
					add(new NewArrayInstruction(opcode, NewArrayInstruction.fromInt(value)));
				else
					add(new IntInstruction(opcode, value));
			}

			@Override
			public void visitLineNumber(NumberGroup line, IdentifierGroup label) {
				add(new LineInstruction(-1, label.content(), line.asInt()));
			}

			@Override
			public void visitLocalVariable(IdentifierGroup name, IdentifierGroup desc, IdentifierGroup start, IdentifierGroup end, int index) throws AssemblerException {
				// no-op, Recaf only supports named variables
			}

			@Override
			public void visitFrame(FrameGroup frame) throws AssemblerException {
				// no-op, Recaf computes frames
			}

			@Override
			public void visitMultiANewArrayInsn(String desc, int dims) {
				add(new MultiArrayInstruction(Opcodes.MULTIANEWARRAY, desc, dims));
			}

			@Override
			public void visitInvokeDynamicInstruction(String identifier, IdentifierGroup descriptor, HandleGroup handle, ArgsGroup args) throws AssemblerException {
				HandleInfo handleInfo = convertHandle(handle);
				List<IndyInstruction.BsmArg> bsmArgs = new ArrayList<>();
				for (Group arg : args.getBody().getChildren()) {
					bsmArgs.add(new IndyInstruction.BsmArg(argType(arg), convert(arg)));
				}
				add(new IndyInstruction(
						INVOKEDYNAMIC,
						identifier,
						descriptor.content(),
						handleInfo,
						bsmArgs));
			}

			@Override
			public void visitInsn(int opcode) {
				add(new Instruction(opcode));
			}

			@Override
			public void visitExpr(ExprGroup expr) {
				add(new Expression(expr.getTextGroup().content()));
			}

			@Override
			public void visitDeprecated(DeprecatedGroup deprecated) throws AssemblerException {
				methodDefinition.setDeprecated(true);
			}

			@Override
			public void visit(Group group) {
				lastGroup = group;
			}

			private void add(CodeEntry element) {
				if (lastGroup != null)
					code.add(wrap(lastGroup, (BaseElement & CodeEntry) element));
			}
		};
	}

	private void setDefinition(AbstractDefinition definition) {
		this.definition = definition;
	}

	private boolean hasDefinition() {
		return definition != null;
	}

	private AbstractDefinition getDefinition() {
		return definition;
	}

	private boolean operateClass(Consumer<ClassDefinition> consumer) {
		if (hasDefinition() && definition instanceof ClassDefinition) {
			consumer.accept((ClassDefinition) definition);
			return true;
		}
		return false;
	}

	private boolean operateField(Consumer<FieldDefinition> consumer) {
		if (hasDefinition() && definition instanceof FieldDefinition) {
			consumer.accept((FieldDefinition) definition);
			return true;
		}
		return false;
	}

	private boolean operateMethod(Consumer<MethodDefinition> consumer) {
		if (hasDefinition() && definition instanceof MethodDefinition) {
			consumer.accept((MethodDefinition) definition);
			return true;
		}
		return false;
	}
}
