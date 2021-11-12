package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.ParserException;
import me.coley.recaf.assemble.ast.arch.*;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.ast.meta.Comment;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.ast.meta.Signature;
import me.coley.recaf.assemble.parser.BytecodeBaseVisitor;
import me.coley.recaf.assemble.parser.BytecodeParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ANTLR visitor to generate AST instances.
 *
 * @author Matt Coley
 */
public class BytecodeAstGenerator extends BytecodeBaseVisitor<Element> {
	@Override
	public Unit visitUnit(BytecodeParser.UnitContext ctx) {
		MemberDefinition definition = visitDefinition(ctx.definition());
		Code code = null;
		if (ctx.code() != null) {
			code = visitCode(ctx.code());
		}
		return wrap(ctx, new Unit(definition, code));
	}

	@Override
	public Signature visitSignature(BytecodeParser.SignatureContext ctx) {
		// child 0 is the keyword
		// child 1 is the signature
		return wrap(ctx, new Signature(ctx.getChild(1).getText()));
	}

	@Override
	public Label visitLabel(BytecodeParser.LabelContext ctx) {
		return wrap(ctx, new Label(ctx.name().getText()));
	}

	@Override
	public TryCatch visitTryCatch(BytecodeParser.TryCatchContext ctx) {
		String start = ctx.name(0).getText();
		String end = ctx.name(1).getText();
		String handler = ctx.name(2).getText();
		String type = ctx.catchType().getText();
		return wrap(ctx, new TryCatch(start, end, handler, type));
	}

	@Override
	public ThrownException visitThrowEx(BytecodeParser.ThrowExContext ctx) {
		String type = ctx.type().getText();
		return wrap(ctx, new ThrownException(type));
	}

	@Override
	public ConstVal visitConstVal(BytecodeParser.ConstValContext ctx) {
		if (ctx.stringLiteral() != null) {
			String string = getString(ctx.stringLiteral());
			return new ConstVal(string);
		} else if (ctx.intLiteral() != null) {
			String intStr = ctx.intLiteral().getText();
			if (intStr.toUpperCase().endsWith("L")) {
				long integer = getLong(ctx.intLiteral());
				return new ConstVal(integer);
			} else {
				int integer = getInt(ctx.intLiteral());
				return new ConstVal(integer);
			}
		} else if (ctx.floatLiteral() != null) {
			String floatStr = ctx.floatLiteral().getText();
			if (floatStr.toUpperCase().endsWith("F")) {
				float floatVal = getFloat(ctx.floatLiteral());
				return new ConstVal(floatVal);
			} else {
				double doubleVal = getDouble(ctx.floatLiteral());
				return new ConstVal(doubleVal);
			}
		} else if (ctx.hexLiteral() != null) {
			String intStr = ctx.hexLiteral().getText();
			if (intStr.toUpperCase().endsWith("L")) {
				long longInt = getLong(ctx.hexLiteral());
				return new ConstVal(longInt);
			} else {
				int integer = getInt(ctx.hexLiteral());
				return new ConstVal(integer);
			}
		} else {
			ParseTree child = ctx.getChild(1);
			throw new ParserException(ctx, "Unknown CONST-VALUE argument type: " + child.getClass() + " - " + child.getText());
		}
	}

	@Override
	public MemberDefinition visitDefinition(BytecodeParser.DefinitionContext ctx) {
		if (ctx.methodDef() != null) {
			return visitMethodDef(ctx.methodDef());
		} else if (ctx.fieldDef() != null) {
			return visitFieldDef(ctx.fieldDef());
		} else {
			throw new ParserException(ctx, "Failed to parse definition: " + ctx.getText());
		}
	}

	@Override
	public FieldDefinition visitFieldDef(BytecodeParser.FieldDefContext ctx) {
		String name = ctx.name().getText();
		Modifiers modifiers = visitModifiers(ctx.modifiers());
		String type = ctx.singleDesc().getText();
		return wrap(ctx, new FieldDefinition(modifiers, name, type));
	}

	@Override
	public MethodDefinition visitMethodDef(BytecodeParser.MethodDefContext ctx) {
		String name = ctx.name().getText();
		Modifiers modifiers = visitModifiers(ctx.modifiers());
		if (ctx.singleDesc() == null)
			throw new ParserException(ctx, "Could not locate return type");
		String retType = ctx.singleDesc().getText();
		MethodParameters params = visitMethodParams(ctx.methodParams());
		return wrap(ctx, new MethodDefinition(modifiers, name, params, retType));
	}

	@Override
	public MethodParameters visitMethodParams(BytecodeParser.MethodParamsContext ctx) {
		MethodParameters params = new MethodParameters();
		if (ctx == null)
			return params;
		if (ctx.methodParam() != null)
			params.add(visitMethodParam(ctx.methodParam()));
		if (ctx.methodParams() != null)
			params.addAll(visitMethodParams(ctx.methodParams()).getParameters());
		return wrap(ctx, params);
	}

	@Override
	public MethodParameter visitMethodParam(BytecodeParser.MethodParamContext ctx) {
		String type = ctx.paramType().getText();
		String name = ctx.name().getText();
		return wrap(ctx, new MethodParameter(type, name));
	}

	@Override
	public Modifiers visitModifiers(BytecodeParser.ModifiersContext ctx) {
		Modifiers modifiers = new Modifiers();
		if (ctx == null)
			return modifiers;
		for (BytecodeParser.ModifierContext modifier : ctx.modifier()) {
			String name = modifier.getText();
			modifiers.add(Modifier.byName(name));
		}
		return wrap(ctx, modifiers);
	}

	@Override
	public Code visitCode(BytecodeParser.CodeContext ctx) {
		Code code = new Code();
		for (BytecodeParser.CodeEntryContext codeEntryContext : ctx.codeEntry()) {
			CodeEntry entry = visitCodeEntry(codeEntryContext);
			code.add(entry);
		}
		return wrap(ctx, code);
	}

	@Override
	public CodeEntry visitCodeEntry(BytecodeParser.CodeEntryContext ctx) {
		if (ctx.instruction() != null) {
			return visitInstruction(ctx.instruction());
		} else if (ctx.label() != null) {
			return visitLabel(ctx.label());
		} else if (ctx.tryCatch() != null) {
			return visitTryCatch(ctx.tryCatch());
		} else if (ctx.throwEx() != null) {
			return visitThrowEx(ctx.throwEx());
		} else if (ctx.constVal() != null) {
			return visitConstVal(ctx.constVal());
		} else if (ctx.signature() != null) {
			return visitSignature(ctx.signature());
		} else if (ctx.comment() != null && ctx.comment().size() > 0) {
			String comment = ctx.comment().stream().map(c -> {
				String text = c.getText();
				if (text.startsWith("//")) {
					text = text.substring(2);
				} else {
					text = text.substring(2, text.length() - 2);
				}
				return text;
			}).collect(Collectors.joining("\n"));
			return wrap(ctx, new Comment(comment));
		} else {
			throw new ParserException(ctx, "Expected an instruction, label, try-catch, throws, const-val, or comment.");
		}
	}

	@Override
	public AbstractInstruction visitInstruction(BytecodeParser.InstructionContext ctx) {
		// visitChildren will invoke the correct "visitX" instruction below
		AbstractInstruction insn = (AbstractInstruction) visitChildren(ctx);
		return wrap(ctx, insn);
	}

	@Override
	public AbstractInstruction visitInsn(BytecodeParser.InsnContext ctx) {
		String opcode = ctx.getChild(0).getText();
		return new Instruction(opcode);
	}

	@Override
	public AbstractInstruction visitInsnInt(BytecodeParser.InsnIntContext ctx) {
		String opcode = ctx.getChild(0).getText();
		int value;
		if (ctx.intLiteral() != null) {
			value = getInt(ctx.intLiteral());
		} else if (ctx.hexLiteral() != null) {
			value = getInt(ctx.hexLiteral());
		} else {
			throw new ParserException(ctx, "No value token for INT: " + ctx.getText());
		}
		return new IntInstruction(opcode, value);
	}

	@Override
	public AbstractInstruction visitInsnNewArray(BytecodeParser.InsnNewArrayContext ctx) {
		String opcode = ctx.getChild(0).getText();
		char arrayType;
		if (ctx.intLiteral() != null) {
			arrayType = (char) getInt(ctx.intLiteral());
		} else if (ctx.charLiteral() != null) {
			arrayType = ctx.charLiteral().getText().charAt(1);
		} else {
			throw new ParserException(ctx, "Unknown array type: " + ctx.getChild(1));
		}
		return new NewArrayInstruction(opcode, arrayType);
	}

	@Override
	public AbstractInstruction visitInsnMethod(BytecodeParser.InsnMethodContext ctx) {
		BytecodeParser.MethodRefContext handle = ctx.methodRef();
		String opcode = ctx.getChild(0).getText();
		String owner = handle.type().getText();
		String name = handle.name().getText();
		String desc = handle.methodDesc().getText();
		return new MethodInstruction(opcode, owner, name, desc);
	}

	@Override
	public AbstractInstruction visitInsnField(BytecodeParser.InsnFieldContext ctx) {
		BytecodeParser.FieldRefContext handle = ctx.fieldRef();
		String opcode = ctx.getChild(0).getText();
		String owner = handle.type().getText();
		String name = handle.name().getText();
		String desc = handle.singleDesc().getText();
		return new FieldInstruction(opcode, owner, name, desc);
	}

	@Override
	public AbstractInstruction visitInsnLdc(BytecodeParser.InsnLdcContext ctx) {
		String opcode = ctx.getChild(0).getText();
		if (ctx.stringLiteral() != null) {
			String string = getString(ctx.stringLiteral());
			return new LdcInstruction(opcode, string);
		} else if (ctx.type() != null) {
			Type type = getType(ctx.type());
			return new LdcInstruction(opcode, type);
		} else if (ctx.intLiteral() != null) {
			String intStr = ctx.intLiteral().getText();
			if (intStr.toUpperCase().endsWith("L")) {
				long integer = getLong(ctx.intLiteral());
				return new LdcInstruction(opcode, integer);
			} else {
				int integer = getInt(ctx.intLiteral());
				return new LdcInstruction(opcode, integer);
			}
		} else if (ctx.floatLiteral() != null) {
			String floatStr = ctx.floatLiteral().getText();
			if (floatStr.toUpperCase().endsWith("F")) {
				float floatVal = getFloat(ctx.floatLiteral());
				return new LdcInstruction(opcode, floatVal);
			} else {
				double doubleVal = getDouble(ctx.floatLiteral());
				return new LdcInstruction(opcode, doubleVal);
			}
		} else if (ctx.hexLiteral() != null) {
			String intStr = ctx.hexLiteral().getText();
			if (intStr.toUpperCase().endsWith("L")) {
				long longInt = getLong(ctx.hexLiteral());
				return new LdcInstruction(opcode, longInt);
			} else {
				int integer = getInt(ctx.hexLiteral());
				return new LdcInstruction(opcode, integer);
			}
		} else {
			ParseTree child = ctx.getChild(1);
			throw new ParserException(ctx, "Unknown LDC argument type: " + child.getClass() + " - " + child.getText());
		}
	}

	@Override
	public AbstractInstruction visitInsnVar(BytecodeParser.InsnVarContext ctx) {
		String opcode = ctx.getChild(0).getText();
		String identifier = ctx.varId().getText();
		return new VarInstruction(opcode, identifier);
	}

	@Override
	public AbstractInstruction visitInsnType(BytecodeParser.InsnTypeContext ctx) {
		String opcode = ctx.getChild(0).getText();
		String identifier = ctx.internalType().getText();
		return new TypeInstruction(opcode, identifier);
	}

	@Override
	public AbstractInstruction visitInsnDynamic(BytecodeParser.InsnDynamicContext ctx) {
		String opcode = ctx.getChild(0).getText();
		String name = ctx.name().getText();
		String methodDesc = ctx.methodDesc().getText();
		BytecodeParser.DynamicHandleContext dynHandle = ctx.dynamicHandle();
		HandleInfo handle;
		if (dynHandle.fieldHandle() != null) {
			String hTag = dynHandle.fieldHandle().handleTag().getText();
			String hOwner = dynHandle.fieldHandle().type().getText();
			String hName = dynHandle.fieldHandle().name().getText();
			String hDesc = dynHandle.fieldHandle().singleDesc().getText();
			handle = new HandleInfo(hTag, hOwner, hName, hDesc);
		} else {
			String hTag = dynHandle.methodHandle().handleTag().getText();
			String hOwner = dynHandle.methodHandle().type().getText();
			String hName = dynHandle.methodHandle().name().getText();
			String hDesc = dynHandle.methodHandle().methodDesc().getText();
			handle = new HandleInfo(hTag, hOwner, hName, hDesc);
		}
		List<IndyInstruction.BsmArg> args = new ArrayList<>();
		if (ctx.dynamicArgs() != null) {
			BytecodeParser.ArgumentListContext entryList = ctx.dynamicArgs().argumentList();
			while (entryList != null) {
				BytecodeParser.ArgumentContext arg = entryList.argument();
				//  (dynamicHandle | intLiteral | charLiteral | hexLiteral | floatLiteral | stringLiteral | boolLiteral)
				if (arg.stringLiteral() != null) {
					String string = getString(arg.stringLiteral());
					args.add(new IndyInstruction.BsmArg(ArgType.STRING, string));
				} else if (arg.type() != null) {
					Type type = getType(arg.type());
					args.add(new IndyInstruction.BsmArg(ArgType.TYPE, type));
				} else if (arg.intLiteral() != null) {
					String intStr = arg.intLiteral().getText();
					if (intStr.toUpperCase().endsWith("L")) {
						long integer = getLong(arg.intLiteral());
						args.add(new IndyInstruction.BsmArg(ArgType.LONG, integer));
					} else {
						int integer = getInt(arg.intLiteral());
						args.add(new IndyInstruction.BsmArg(ArgType.INTEGER, integer));
					}
				} else if (arg.floatLiteral() != null) {
					String floatStr = arg.floatLiteral().getText();
					if (floatStr.toUpperCase().endsWith("F")) {
						float floatVal = getFloat(arg.floatLiteral());
						args.add(new IndyInstruction.BsmArg(ArgType.FLOAT, floatVal));
					} else {
						double doubleVal = getDouble(arg.floatLiteral());
						args.add(new IndyInstruction.BsmArg(ArgType.DOUBLE, doubleVal));
					}
				} else if (arg.hexLiteral() != null) {
					String intStr = arg.hexLiteral().getText().substring(2); // 0x
					if (intStr.toUpperCase().endsWith("L")) {
						long longInt = getLong(arg.hexLiteral());
						args.add(new IndyInstruction.BsmArg(ArgType.LONG, longInt));
					} else {
						int integer = getInt(arg.hexLiteral());
						args.add(new IndyInstruction.BsmArg(ArgType.INTEGER, integer));
					}
				} else if (arg.dynamicHandle() != null) {
					if (arg.dynamicHandle().fieldHandle() != null) {
						String hTag = arg.dynamicHandle().fieldHandle().handleTag().getText();
						String hOwner = arg.dynamicHandle().fieldHandle().type().getText();
						String hName = arg.dynamicHandle().fieldHandle().name().getText();
						String hDesc = arg.dynamicHandle().fieldHandle().singleDesc().getText();
						HandleInfo handle2 = new HandleInfo(hTag, hOwner, hName, hDesc);
						args.add(new IndyInstruction.BsmArg(ArgType.HANDLE, handle2));
					} else {
						String hTag = arg.dynamicHandle().methodHandle().handleTag().getText();
						String hOwner = arg.dynamicHandle().methodHandle().type().getText();
						String hName = arg.dynamicHandle().methodHandle().name().getText();
						String hDesc = arg.dynamicHandle().methodHandle().methodDesc().getText();
						HandleInfo handle2 = new HandleInfo(hTag, hOwner, hName, hDesc);
						args.add(new IndyInstruction.BsmArg(ArgType.HANDLE, handle2));
					}
				} else {
					ParseTree child = arg.getChild(1);
					throw new ParserException(arg, "Unknown LDC argument type: " + child.getClass() + " - " + child.getText());
				}
				entryList = entryList.argumentList();
			}
		}
		return new IndyInstruction(opcode, name, methodDesc, handle, args);
	}

	@Override
	public AbstractInstruction visitInsnJump(BytecodeParser.InsnJumpContext ctx) {
		String opcode = ctx.getChild(0).getText();
		String label = ctx.name().getText();
		return new JumpInstruction(opcode, label);
	}

	@Override
	public AbstractInstruction visitInsnIinc(BytecodeParser.InsnIincContext ctx) {
		String opcode = ctx.getChild(0).getText();
		String identifier = ctx.varId().getText();
		int increment;
		if (ctx.intLiteral() != null) {
			increment = getInt(ctx.intLiteral());
		} else if (ctx.hexLiteral() != null) {
			increment = getInt(ctx.hexLiteral());
		} else {
			throw new ParserException(ctx, "No value token for IINC: " + ctx.getText());
		}
		return new IincInstruction(opcode, identifier, increment);
	}

	@Override
	public AbstractInstruction visitInsnMultiA(BytecodeParser.InsnMultiAContext ctx) {
		String opcode = ctx.getChild(0).getText();
		String type = ctx.singleDesc().getText();
		int dimensions = getInt(ctx.intLiteral());
		return new MultiArrayInstruction(opcode, type, dimensions);
	}

	@Override
	public AbstractInstruction visitInsnLine(BytecodeParser.InsnLineContext ctx) {
		String opcode = ctx.getChild(0).getText();
		String label = ctx.name().getText();
		int line = getInt(ctx.intLiteral());
		return new LineInstruction(opcode, label, line);
	}

	@Override
	public AbstractInstruction visitInsnLookup(BytecodeParser.InsnLookupContext ctx) {
		String opcode = ctx.getChild(0).getText();
		List<LookupSwitchInstruction.Entry> entries = new ArrayList<>();
		BytecodeParser.SwitchMapListContext entryList = ctx.switchMap().switchMapList();
		while (entryList != null) {
			BytecodeParser.SwitchMapEntryContext entry = entryList.switchMapEntry();
			int key = getInt(entry.intLiteral());
			String id = entry.name().getText();
			entries.add(new LookupSwitchInstruction.Entry(key, id));
			entryList = entryList.switchMapList();
		}
		String defaultIdentifier = ctx.switchDefault().name().getText();
		return new LookupSwitchInstruction(opcode, entries, defaultIdentifier);
	}

	@Override
	public AbstractInstruction visitInsnTable(BytecodeParser.InsnTableContext ctx) {
		String opcode = ctx.getChild(0).getText();
		int min = getInt(ctx.switchRange().intLiteral(0));
		int max = getInt(ctx.switchRange().intLiteral(1));
		List<String> labels = new ArrayList<>();
		BytecodeParser.SwitchOffsetsListContext entryList = ctx.switchOffsets().switchOffsetsList();
		while (entryList != null) {
			String label = entryList.name().getText();
			labels.add(label);
			entryList = entryList.switchOffsetsList();
		}
		String defaultIdentifier = ctx.switchDefault().name().getText();
		return new TableSwitchInstruction(opcode, min, max, labels, defaultIdentifier);
	}


	private static String getString(BytecodeParser.StringLiteralContext stringLiteral) {
		String string = stringLiteral.getText();
		string = string.substring(1, string.length() - 1);
		return string;
	}

	private static Type getType(BytecodeParser.TypeContext typeLiteral) {
		return Type.getObjectType(typeLiteral.getText());
	}

	private static int getInt(BytecodeParser.IntLiteralContext intLiteral) {
		try {
			String intStr = intLiteral.getText();
			if (intStr.toUpperCase().endsWith("L")) {
				intStr = intStr.substring(0, intStr.length() - 1);
				return (int) Long.parseLong(intStr);
			} else {
				return Integer.parseInt(intStr);
			}
		} catch (NumberFormatException ex) {
			throw new ParserException(intLiteral, "Could not parse int from: " + intLiteral.getText());
		}
	}

	private static int getInt(BytecodeParser.HexLiteralContext intLiteral) {
		try {
			String intStr = intLiteral.getText().substring(2); // 0x
			if (intStr.toUpperCase().endsWith("L")) {
				intStr = intStr.substring(0, intStr.length() - 1);
				return (int) Long.parseLong(intStr, 16);
			} else {
				return Integer.parseInt(intStr, 16);
			}
		} catch (NumberFormatException ex) {
			throw new ParserException(intLiteral, "Could not parse int from: " + intLiteral.getText());
		}
	}

	private static long getLong(BytecodeParser.IntLiteralContext intLiteral) {
		try {
			String intStr = intLiteral.getText();
			if (intStr.toUpperCase().endsWith("L")) {
				intStr = intStr.substring(0, intStr.length() - 1);
				return Long.parseLong(intStr);
			} else {
				return Integer.parseInt(intStr);
			}
		} catch (NumberFormatException ex) {
			throw new ParserException(intLiteral, "Could not parse long from: " + intLiteral.getText());
		}
	}

	private static long getLong(BytecodeParser.HexLiteralContext intLiteral) {
		try {
			String intStr = intLiteral.getText().substring(2); // 0x
			if (intStr.toUpperCase().endsWith("L")) {
				intStr = intStr.substring(0, intStr.length() - 1);
				return Long.parseLong(intStr, 16);
			} else {
				return Long.parseLong(intStr, 16);
			}
		} catch (NumberFormatException ex) {
			throw new ParserException(intLiteral, "Could not parse long from: " + intLiteral.getText());
		}
	}

	private static float getFloat(BytecodeParser.FloatLiteralContext floatLiteral) {
		try {
			String floatStr = floatLiteral.getText();
			if (floatStr.toUpperCase().endsWith("F")) {
				floatStr = floatStr.substring(0, floatStr.length() - 1);
				return Float.parseFloat(floatStr);
			} else {
				return (float) Double.parseDouble(floatStr);
			}
		} catch (NumberFormatException ex) {
			throw new ParserException(floatLiteral, "Could not parse float from: " + floatLiteral.getText());
		}
	}

	private static double getDouble(BytecodeParser.FloatLiteralContext floatLiteral) {
		try {
			String floatStr = floatLiteral.getText();
			if (floatStr.toUpperCase().endsWith("F")) {
				floatStr = floatStr.substring(0, floatStr.length() - 1);
				return Float.parseFloat(floatStr);
			} else {
				return Double.parseDouble(floatStr);
			}
		} catch (NumberFormatException ex) {
			throw new ParserException(floatLiteral, "Could not parse double from: " + floatLiteral.getText());
		}
	}

	private static <E extends BaseElement> E wrap(ParserRuleContext ctx, E element) {
		Token start = ctx.getStart();
		Token stop = ctx.getStop();
		return element.setLine(start.getLine()).setRange(start.getStartIndex(), stop.getStopIndex());
	}
}
