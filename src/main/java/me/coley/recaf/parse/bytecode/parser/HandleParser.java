package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.util.AutoCompleteUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * {@link HandleAST} parser.
 *
 * @author Matt
 */
public class HandleParser extends AbstractParser<HandleAST> {
	// This handle is what's used 90% of the time so lets just provide a helpful alias.
	public static final Handle DEFAULT_HANDLE = new Handle(Opcodes.H_INVOKESTATIC,
			"java/lang/invoke/LambdaMetafactory",
			"metafactory",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" +
			"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
			"Ljava/lang/invoke/CallSite;", false);
	public static final String DEFAULT_HANDLE_ALIAS = "H_META";

	@Override
	public HandleAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String[] trim = line.trim().split("\\s+");
			if (trim.length < 2)
				throw new ASTParseException(lineNo, "Not enough parameters");
			int start = line.indexOf(trim[0]);
			// op
			TagParser opParser = new TagParser();
			opParser.setOffset(line.indexOf(trim[0]));
			TagAST tag = opParser.visit(lineNo, trim[0]);
			// owner & name & desc
			String data = trim[1];
			int dot = data.indexOf('.');
			if (dot == -1)
				throw new ASTParseException(lineNo, "Format error: Missing '.' after owner type");
			// Determine split index, for field or method type
			int descSplit = data.indexOf('(');
			if(descSplit < dot) {
				descSplit = data.indexOf(' ');
				if(descSplit < dot)
					throw new ASTParseException(lineNo, "Format error: Missing valid handle descriptor");
			}
			String typeS = data.substring(0, dot);
			String nameS = data.substring(dot + 1, descSplit);
			String descS = data.substring(descSplit);
			// owner
			TypeParser typeParser = new TypeParser();
			typeParser.setOffset(line.indexOf(data));
			TypeAST owner = typeParser.visit(lineNo, typeS);
			// name
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(dot + 1);
			NameAST name = nameParser.visit(lineNo, nameS);
			// desc
			DescParser descParser = new DescParser();
			descParser.setOffset(descSplit);
			DescAST desc = descParser.visit(lineNo, descS);
			return new HandleAST(lineNo, start, tag, owner, name, desc);
		} catch(Exception ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for handle ");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// TAG owner.name+desc
		int space = text.indexOf(' ');
		if (space > 0) {
			String sub = text.substring(space + 1);
			int dot = sub.indexOf('.');
			if (dot == -1)
				return new TypeParser().suggest(lastParse, sub);
			// Determine if we need to suggest fields or methods based on tag
			boolean isMethod = false;
			try {
				TagParser opParser = new TagParser();
				TagAST tag = opParser.visit(0, text.substring(0, space));
				isMethod = tag.isMethod();
			} catch(Exception ex) { /* ignored */ }
			if(isMethod)
				return AutoCompleteUtil.method(sub);
			else
				return AutoCompleteUtil.field(sub);
		} else
			// No space, so must be typing the tag. Suggest tag names.
			return new TagParser().suggest(lastParse, text);
	}
}
