package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.ast.Unit;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.Token;

import java.util.Collection;
import java.util.List;

public interface ParserCompletionListener {

    void onCompleteTokenize(Collection<Token> tokens);

    /**
     * Called when the parser has completed parsing.
     * @param groups List of parsed groups.
     */
    void onCompleteParse(Collection<Group> groups);

    /**
     * Called when the transformer has transformed the parsed groups to a Unit
     * @param unit The transformed unit.
     */
    void onCompleteTransform(Unit unit);

}
