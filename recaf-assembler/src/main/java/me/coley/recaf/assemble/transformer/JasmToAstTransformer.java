package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.*;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.ast.arch.MethodParameters;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Expression;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.ast.meta.Signature;
import me.coley.recaf.util.EscapeUtil;
import me.darknet.assembler.compiler.FieldDescriptor;
import me.darknet.assembler.compiler.MethodDescriptor;
import me.darknet.assembler.parser.*;
import me.darknet.assembler.parser.groups.*;
import me.darknet.assembler.transform.MethodVisitor;
import me.darknet.assembler.transform.Transformer;
import me.darknet.assembler.transform.Visitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.*;

/**
 * JASM visitor to generate AST instances.
 *
 * @author Nowilltolife
 */
public class JasmToAstTransformer implements Visitor, MethodVisitor {
	private final Collection<Group> groups;
	private final Attributes currentAttributes = new Attributes();
	private Code code = new Code();
	private Unit unit;
	private ClassDefinition currentClass;
	private AbstractDefinition activeMember;
	private Group latestGroup;

	/**
	 * @param groups
	 * 		Parser group input generated from {@link ParserContext}.
	 */
	public JasmToAstTransformer(Collection<Group> groups) {
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
	public void visit(Group group) throws AssemblerException {
		latestGroup = group;
	}

	@Override
	public void visitLabel(LabelGroup label) throws AssemblerException {
		code.addLabel(wrap(label, new Label(label.getLabel())));
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
				content(catchGroup.getException())));
	}

	@Override
	public void visitVarInsn(int opcode, IdentifierGroup identifier) throws AssemblerException {
		add(new VarInstruction(opcode, content(identifier)));
	}

	@Override
	public void visitDirectVarInsn(int opcode, int var) throws AssemblerException {
		// not supported
	}

	@Override
	public void visitMethodInsn(int opcode, IdentifierGroup name, IdentifierGroup desc, boolean itf) throws AssemblerException {
		MethodDescriptor md = new MethodDescriptor(name.content(), desc.content());
		add(new MethodInstruction(opcode, md.owner, md.name, md.getDescriptor(), itf));
	}

	@Override
	public void visitFieldInsn(int opcode, IdentifierGroup name, IdentifierGroup desc) throws AssemblerException {
		FieldDescriptor fs = new FieldDescriptor(name.content(), desc.content());
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
		if (opcode == Opcodes.NEWARRAY) {
			add(new NewArrayInstruction(opcode, NewArrayInstruction.fromInt(value)));
		} else
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
	public void visitInvokeDynamicInstruction(String identifier, IdentifierGroup descriptor, HandleGroup handle, ArgsGroup args) throws AssemblerException {
		HandleInfo handleInfo = from(handle);
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
		currentClass = new ClassDefinition(fromAccessMods(accessMods), content(identifier));
	}

	@Override
	public void visitSuper(ExtendsGroup extendsGroup) throws AssemblerException {
		if(currentClass == null) throw new AssemblerException("No class defined, cannot define super class", extendsGroup.location());
		currentClass.setSuperClass(content(extendsGroup));
	}

	@Override
	public void visitImplements(ImplementsGroup implementsGroup) throws AssemblerException {
		if(currentClass == null) throw new AssemblerException("No class defined, cannot define super class", implementsGroup.location());
		currentClass.addInterface(content(implementsGroup));
	}

	@Override
	public void visitField(FieldDeclarationGroup dcl) throws AssemblerException {
		FieldDefinition field = wrap(dcl,
				new FieldDefinition(fromAccessMods(dcl.accessMods), dcl.name.content(), dcl.descriptor.content()));

		if (currentAttributes.getSignature() != null) {
			field.setSignature(currentAttributes.getSignature());
		}
		for (Annotation annotation : currentAttributes.getAnnotations()) {
			field.addAnnotation(annotation);
		}

		activeMember = field;
		if (dcl.constantValue != null)
			field.setConstVal(new ConstVal(convert(dcl.constantValue), from(dcl.constantValue)));

		currentAttributes.clear();
	}

	@Override
	public MethodVisitor visitMethod(MethodDeclarationGroup dcl) throws AssemblerException {
		MethodParameters parameters = new MethodParameters();
		for (MethodParameterGroup param : dcl.params.methodParameters) {
			parameters.add(new MethodParameter(param.getDescriptorValue(), param.getNameValue()));
		}
		this.code = new Code();
		MethodDefinition method = wrap(dcl, new MethodDefinition(
				fromAccessMods(dcl.accessMods),
				content(dcl.name),
				parameters,
				dcl.returnType,
				this.code));
		for (ThrownException thrown : currentAttributes.getThrownExceptions()) {
			method.addThrownException(thrown);
		}
		if (currentAttributes.getSignature() != null) {
			method.setSignature(currentAttributes.getSignature());
		}
		for (Annotation annotation : currentAttributes.getAnnotations()) {
			method.addAnnotation(annotation);
		}
		currentAttributes.clear();
		activeMember = method;
		return this;
	}

	@Override
	public void visitAnnotation(AnnotationGroup annotation) throws AssemblerException {
		Map<String, Annotation.AnnoArg> args = new HashMap<>();
		for (AnnotationParamGroup param : annotation.getParams()) {
			annotationParam(param, args);
		}
		Annotation anno = new Annotation(annotation.isInvisible(), annotation.getClassGroup().content(), args);
		currentAttributes.addAnnotation(anno);
	}

	@Override
	public void visitSignature(SignatureGroup signature) throws AssemblerException {
		currentAttributes.setSignature(new Signature(signature.getDescriptor().content()));
	}

	@Override
	public void visitThrows(ThrowsGroup throwsGroup) throws AssemblerException {
		currentAttributes.addThrownException(new ThrownException(content(throwsGroup.getClassName())));
	}

	@Override
	public void visitExpression(ExprGroup expr) throws AssemblerException {

	}

	@Override
	public void visitEnd() throws AssemblerException {
		if(currentClass != null) {
			if(activeMember instanceof FieldDefinition) {
				currentClass.addField((FieldDefinition) activeMember);
			} else if(activeMember instanceof MethodDefinition) {
				currentClass.addMethod((MethodDefinition) activeMember);
			}
		} else {
			this.unit = new Unit(activeMember);
		}
	}

	@Override
	public void visitEndClass() throws AssemblerException {
		if(currentClass != null) {
			this.unit = new Unit(currentClass);
		}
		if (activeMember != null && activeMember.isField())
			unit = new Unit(activeMember);
	}

	public void add(CodeEntry element) {
		code.add(wrap(latestGroup, (BaseElement & CodeEntry) element));
	}

	public static HandleInfo from(HandleGroup handle) {
		MethodDescriptor mdh = new MethodDescriptor(handle.getName().content(), handle.getDescriptor().content());
		return new HandleInfo(
				handle.getHandleType().content(),
				mdh.owner,
				mdh.name,
				mdh.descriptor);
	}

	private static ArgType from(Group group) {
		if (group instanceof NumberGroup) {
			NumberGroup number = (NumberGroup) group;
			if (number.isFloat()) {
				return number.isWide() ? ArgType.DOUBLE : ArgType.FLOAT;
			} else {
				return number.isWide() ? ArgType.LONG : ArgType.INTEGER;
			}
		} else if (group instanceof StringGroup) {
			return ArgType.STRING;
		} else if (group instanceof TypeGroup) {
			return ArgType.TYPE;
		} else if (group instanceof HandleGroup) {
			return ArgType.HANDLE;
		} else if (group instanceof IdentifierGroup) {
			String content = group.content();
			if (content.length() == 3 && content.charAt(0) == '\'' && content.charAt(2) == '\'')
				return ArgType.CHAR;
			switch (content) {
				case "true":
				case "false":
					return ArgType.BOOLEAN;
				case "NaN":
				case "Infinity":
				case "-Infinity":
					return ArgType.DOUBLE;
				case "NaNf":
				case "Infinityf":
				case "-Infinityf":
					return ArgType.FLOAT;
				default:
					throw new IllegalArgumentException("Cannot convert to constant '" + group.content() + "'");
			}
		}
		throw new IllegalArgumentException("Cannot convert to constant '" + group.content() + "'");
	}

	private static Modifiers fromAccessMods(AccessModsGroup accessMods) {
		Modifiers modifiers = new Modifiers();
		for (AccessModGroup accessMod : accessMods.accessMods) {
			modifiers.add(Modifier.byName(accessMod.content().replace(".", "")));
		}
		return modifiers;
	}

	private static void paramValue(String name, Group value, Map<String, Annotation.AnnoArg> map) throws AssemblerException {
		if (value.type == Group.GroupType.ARGS) {
			ArgsGroup args = (ArgsGroup) value;
			for (Group group : args.getBody().children) {
				paramValue(name, group, map);
			}
		} else if (value.type == Group.GroupType.ENUM) {
			EnumGroup enumGroup = (EnumGroup) value;
			map.put(name,
					new Annotation.AnnoEnum(
							enumGroup.getDescriptor().content(),
							enumGroup.getEnumValue().content()
					));
		} else if (value.type == Group.GroupType.ANNOTATION) {
			AnnotationGroup annotationGroup = (AnnotationGroup) value;
			Map<String, Annotation.AnnoArg> argMap = new HashMap<>();
			for (AnnotationParamGroup param : annotationGroup.getParams()) {
				annotationParam(param, argMap);
			}
			map.put(name,
					new Annotation.AnnoArg(
							ArgType.ANNO,
							new Annotation(
									!annotationGroup.isInvisible(),
									annotationGroup.getClassGroup().content(),
									argMap
							)));
		} else {
			map.put(name,
					new Annotation.AnnoArg(
							from(value),
							convert(value)
					));
		}

	}

	private static void annotationParam(AnnotationParamGroup annotationParam, Map<String, Annotation.AnnoArg> map) throws AssemblerException {
		if (annotationParam.value.type == Group.GroupType.ARGS) {
			ArgsGroup args = (ArgsGroup) annotationParam.value;
			Map<String, Annotation.AnnoArg> argMap = new HashMap<>();
			for (Group group : args.getBody().children) {
				paramValue(group.content(), group, argMap);
			}
			map.put(annotationParam.name.content(),
					new Annotation.AnnoArg(
							ArgType.ANNO_LIST,
							new ArrayList<>(argMap.values())
					));
		} else {
			paramValue(annotationParam.name.content(), annotationParam.value, map);
		}
	}

	private static Object convert(Group group) throws AssemblerException {
		if (group.getType() == Group.GroupType.NUMBER) {
			return ((NumberGroup) group).getNumber();
		} else if (group.type == Group.GroupType.TYPE) {
			TypeGroup typeGroup = (TypeGroup) group;
			try {
				String desc = typeGroup.descriptor.content();
				if (desc.isEmpty()) return Type.getType(desc);
				if (desc.charAt(0) == '(') {
					return Type.getMethodType(typeGroup.descriptor.content());
				} else {
					return Type.getObjectType(typeGroup.descriptor.content());
				}
			} catch (IllegalArgumentException e) {
				throw new AssemblerException("Invalid type: " + typeGroup.descriptor.content(), typeGroup.location());
			}
		} else if (group.type == Group.GroupType.HANDLE) {
			HandleGroup handle = (HandleGroup) group;
			HandleInfo info = from(handle);
			return info.toHandle();
		} else if (group.type == Group.GroupType.STRING) {
			return group.content();
		} else {
			String content = group.content();
			if (content.length() == 3 && content.charAt(0) == '\'' && content.charAt(2) == '\'')
				return content.charAt(1);
			if (content.equals("true")) return true;
			if (content.equals("false")) return false;
			if (content.equals("null")) return null;
			if (content.equals("NaN")) return Double.NaN;
			if (content.equals("NaNf")) return Float.NaN;
			if (content.equals("Infinity")) return Double.POSITIVE_INFINITY;
			if (content.equals("-Infinity")) return Double.NEGATIVE_INFINITY;
			if (content.equals("Infinityf")) return Float.POSITIVE_INFINITY;
			if (content.equals("-Infinityf")) return Float.NEGATIVE_INFINITY;
			return content;
		}
	}

	private static <E extends BaseElement> E wrap(Group group, E element) {
		Token start = group.value;
		Token end = group.end();
		if (end == null)
			end = start;
		Location startLocation = start.getLocation();
		int startPos = start.getStart();
		int endPos = end.getEnd();
		int column = startLocation.getColumn();
		return element.setLine(startLocation.getLine())
				.setColumnRange(column, column + (endPos - startPos))
				.setRange(startPos, endPos);
	}

	private static String content(Group group) {
		return EscapeUtil.unescape(group.content());
	}
}
