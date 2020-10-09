package me.coley.recaf.parse.bytecode.parser;

import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.AbstractParser;
import me.coley.recaf.parse.bytecode.ParseResult;
import me.coley.recaf.parse.bytecode.ast.DefinitionModifierAST;
import me.coley.recaf.parse.bytecode.ast.DescAST;
import me.coley.recaf.parse.bytecode.ast.FieldDefinitionAST;
import me.coley.recaf.parse.bytecode.ast.NameAST;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.util.AutoCompleteUtil;

import java.util.Collections;
import java.util.List;

/**
 * {@link FieldDefinitionAST} parser.
 *
 * @author Matt
 */
public class FieldDefinitionParser extends AbstractParser<FieldDefinitionAST> {
	@Override
	public FieldDefinitionAST visit(int lineNo, String line) throws ASTParseException {
		try {
			String trim = line.trim();
			// Fetch the name first, even though it appears after the access modifiers
			String[] split = trim.split("\\s+");
			if(split.length < 2)
				throw new ASTParseException(lineNo, "Bad format for FIELD, missing arguments");
			String name = split[split.length - 1];
			String desc =  split[split.length - 2];
			int nameStart = trim.lastIndexOf(name);
			int descStart = trim.lastIndexOf(" " + desc);
			int start = line.indexOf(trim);
			NameParser nameParser = new NameParser(this);
			nameParser.setOffset(nameStart);
			NameAST nameAST = nameParser.visit(lineNo, name);
			DescParser descParser = new DescParser();
			descParser.setOffset(descStart);
			DescAST descAST = descParser.visit(lineNo, desc);
			FieldDefinitionAST def = new FieldDefinitionAST(lineNo, start, nameAST, descAST);
			def.addChild(nameAST);
			// Parse access modifiers
			if (descStart > DefinitionParser.DEFINE_LEN) {
				String modifiersSection = trim.substring(DefinitionParser.DEFINE_LEN, descStart);
				while(!modifiersSection.trim().isEmpty()) {
					// Get current modifier
					start = line.indexOf(modifiersSection);
					int space = modifiersSection.indexOf(' ');
					int end = space;
					if(end == -1)
						end = modifiersSection.length();
					String modStr = modifiersSection.substring(0, end);
					// Parse modifier
					ModifierParser modifierParser = new ModifierParser();
					modifierParser.setOffset(start);
					DefinitionModifierAST modifierAST = modifierParser.visit(lineNo, modStr);
					def.addModifier(modifierAST);
					// cut section to fit next modifier
					if(space == -1)
						break;
					else
						modifiersSection = modifiersSection.substring(modStr.length()).trim();
				}
			}
			def.addChild(descAST);
			return def;
		} catch(IndexOutOfBoundsException ex) {
			throw new ASTParseException(ex, lineNo, "Bad format for FIELD");
		}
	}

	@Override
	public List<String> suggest(ParseResult<RootAST> lastParse, String text) {
		// If we have a space (after the FIELD part) and have not started writing the descriptor
		// then we can suggest access modifiers or the type
		if (text.contains(" ") && !text.contains(";")) {
			String[] parts = text.split("\\s+");
			String last = parts[parts.length - 1];
			if(last.charAt(0) == 'L') {
				// Complete type
				return AutoCompleteUtil.descriptorName(last);
			} else {
				// Complete modifier
				return new ModifierParser().suggest(lastParse, last);
			}
		}
		return Collections.emptyList();
	}
}
