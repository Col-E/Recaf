package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;

import java.util.Collections;
import java.util.List;

/**
 * {@link MethodDefinitionAST} parser.
 *
 * @author Matt
 */
public class MethodDefinitionParser extends AbstractParser<MethodDefinitionAST> {
	@Override
	public MethodDefinitionAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			if(!trim.matches(".+(.*).+"))
				throw new ASTParseException(lineNo, "Bad format for DEFINE, bad method descriptor");
			// Fetch the name first, even though it appears after the access modifiers
			String name = trim.substring(0, trim.indexOf('('));
			int nameStart = name.lastIndexOf(' ') + 1;
			name = name.substring(nameStart);
			int descStart = line.indexOf(')') + 1;
			String desc = line.substring(descStart);
			int start = line.indexOf(trim);
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(nameStart);
			NameAST nameAST = nameParser.visit(lineNo, name);
			DescParser descParser = new DescParser();
			descParser.setOffset(descStart);
			DescAST descAST = descParser.visit(lineNo, desc);
			MethodDefinitionAST def = new MethodDefinitionAST(lineNo, start, nameAST, descAST);
			def.addChild(nameAST);
			// Parse access modifiers
			String modifiersSection = trim.substring(DefinitionParser.DEFINE_LEN, nameStart);
			while(!modifiersSection.trim().isEmpty()) {
				// Get current modifier
				start = line.indexOf(modifiersSection);
				int space = modifiersSection.indexOf(' ');
				int end = space;
				if (end == -1)
					end = modifiersSection.length();
				String modStr = modifiersSection.substring(0, end);
				// Parse modifier
				ModifierParser modifierParser = new ModifierParser();
				modifierParser.setOffset(start);
				DefinitionModifierAST modifierAST = modifierParser.visit(lineNo, modStr);
				def.addModifier(modifierAST);
				// cut section to fit next modifier
				if (space == -1)
					break;
				else
					modifiersSection = modifiersSection.substring(modStr.length()).trim();
			}
			// Parse argument types
			String argsSection = trim.substring(trim.indexOf('(') + 1, trim.indexOf(')'));
			while(!argsSection.trim().isEmpty()) {
				// Get current arg
				int comma = argsSection.indexOf(',');
				int end = comma;
				if(end == -1)
					end = argsSection.length();
				start = line.indexOf(argsSection);
				String argString = argsSection.substring(0, end);
				// Parse arg
				ArgParser argParser = new ArgParser();
				argParser.setOffset(start);
				DefinitionArgAST argAST = argParser.visit(lineNo, argString);
				def.addArgument(argAST);
				// cut section to fit next arg
				if(comma == -1)
					break;
				else
					argsSection = argsSection.substring(end + 1).trim();
			}
			def.addChild(descAST);
			return def;
		} catch(IndexOutOfBoundsException ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for DEFINE");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// If we have a space (after the DEFINE part) and have not started writing the descriptor
		// then we can suggest access modifiers
		if (text.contains(" ") && !text.contains("(")) {
			String[] parts = text.split("\\s+");
			return new ModifierParser().suggest(lastParse, parts[parts.length - 1]);
		}
		// TODO: Arg desc suggestions
		return Collections.emptyList();
	}
}
