package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.parser.*;
import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.*;
import java.util.function.Supplier;

/**
 * AST Parser utility.
 *
 * @author Matt
 */
public class Parse {
	private static final Map<Integer, Supplier<AbstractParser>> insnTypeToParser = new HashMap<>();

	static {
		Parse.insnTypeToParser.put(AbstractInsnNode.LINE, LineInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.INSN, InsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.INT_INSN, IntInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.VAR_INSN, VarInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.TYPE_INSN, TypeInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.IINC_INSN, IincInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.MULTIANEWARRAY_INSN, MultiArrayParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.FIELD_INSN, FieldInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.METHOD_INSN, MethodInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.LDC_INSN, LdcInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.JUMP_INSN, JumpInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.TABLESWITCH_INSN, TableSwitchInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.LOOKUPSWITCH_INSN, LookupSwitchInsnParser::new);
		Parse.insnTypeToParser.put(AbstractInsnNode.INVOKE_DYNAMIC_INSN, InvokeDynamicParser::new);
	}

	/**
	 * @param text
	 * 		Text to visit.
	 *
	 * @return Parse result wrapper of generated AST.
	 */
	public static ParseResult<RootAST> parse(String text) {
		List<ASTParseException> problems = new ArrayList<>();
		RootAST root = new RootAST();
		int lineNo = 0;
		String[] lines = text.split("[\n\r]");
		// Collect aliases
		List<AliasAST> aliases = new ArrayList<>();
		for(String line : lines) {
			lineNo++;
			// Skip empty lines
			if(line.trim().isEmpty())
				continue;
			// Check for alias
			String token = line.trim().split("\\s")[0].toUpperCase();
			try {
				if (token.equals("ALIAS")) {
					// Why? Because we want to support aliases-in-aliases when they
					// are defined in order.
					String lineCopy = line;
					for (AliasAST alias : aliases)
						lineCopy = lineCopy.replace("${" + alias.getName().getName() + "}",
								alias.getValue().getValue());
					// Parse alias
					aliases.add((AliasAST) getParser(lineNo, token).visit(lineNo, lineCopy));
				}
			} catch(ClassCastException | ASTParseException ex) {
				/* ignored, we will collect the error on the second pass */
			}
		}
		// Parse again
		lineNo = 0;
		for(String line : lines) {
			lineNo++;
			// Skip empty lines
			String trim = line.trim();
			if(trim.isEmpty())
				continue;
			// Determine parse action from starting token
			String token = trim.split("\\s")[0].toUpperCase();
			try {
				AbstractParser parser = getParser(lineNo, token);
				if(parser == null)
					throw new ASTParseException(lineNo, "Unknown identifier: " + token);
				// Apply aliases
				String lineCopy = line;
				for (AliasAST alias : aliases)
					lineCopy = lineCopy.replace("${" + alias.getName().getName() + "}",
							alias.getValue().getValue());
				// Parse and add to root
				AST ast = parser.visit(lineNo, lineCopy);
				root.addChild(ast);
			} catch(ASTParseException ex) {
				problems.add(ex);
			}
		}
		return new ParseResult<>(root, problems);
	}

	/**
	 * @param lineNo
	 * 		Line the token appears on.
	 * @param token
	 * 		First token on line.
	 *
	 * @return Parser associated with the token.
	 *
	 * @throws ASTParseException
	 * 		When the token is not valid.
	 */
	public static AbstractParser<?> getParser(int lineNo, String token) throws ASTParseException {
		if (token.startsWith("//"))
			return new CommentParser();
		if(token.endsWith(":"))
			return new LabelParser();
		if (token.equals("DEFINE"))
			return new DefinitionParser();
		if (token.equals("VALUE"))
			return new DefaultValueParser();
		if(token.equals("THROWS"))
			return new ThrowsParser();
		if(token.equals("TRY"))
			return new TryCatchParser();
		if(token.equals("ALIAS"))
			return new AliasDeclarationParser();
		if(token.equals("SIGNATURE"))
			return new SignatureParser();
		if(token.equals("EXPR"))
			return new ExpressionParser();
		// Get opcode from token (opcode?) then get the opcode's group
		// Lookup group's parser
		try {
			int opcode = OpcodeUtil.nameToOpcode(token);
			int type = OpcodeUtil.opcodeToType(opcode);
			return getInsnParser(type);
		} catch(NullPointerException ex) {
			// Thrown when the opcode name isn't a real opcode
			throw new ASTParseException(lineNo, "Not a real opcode: " + token);
		}
	}

	/**
	 * @param type
	 * 		Instruction type, see {@link AbstractInsnNode#getType()}.
	 *
	 * @return Parser for type.
	 */
	public static AbstractParser<?> getInsnParser(int type) {
		Supplier<AbstractParser> supplier = insnTypeToParser.get(type);
		if(supplier == null)
			return null;
		return supplier.get();
	}
}
