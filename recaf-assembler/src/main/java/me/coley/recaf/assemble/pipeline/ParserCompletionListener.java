package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.ast.Unit;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collection;
import java.util.List;

/**
 * Listener for Jasm completion and the complete parsing into an eventual {@link Unit} for the Recaf AST.
 *
 * @author Justus Garbe
 */
public interface ParserCompletionListener {
	/**
	 * Called when the parser has completed tokenizing the input text.
	 *
	 * @param tokens
	 * 		List of parsed tokens.
	 */
	void onCompleteTokenize(List<Token> tokens);

	/**
	 * Called when the parser has completed parsing.
	 *
	 * @param groups
	 * 		List of parsed groups.
	 */
	void onCompleteParse(List<Group> groups);

	/**
	 * Called when the transformer has transformed the parsed groups to a {@link Unit}.
	 *
	 * @param unit
	 * 		The transformed unit.
	 */
	void onCompleteTransform(Unit unit);
}
