package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;

/**
 * Nest member declaration.
 *
 * @author Justus Garbe
 */
public class NestMember extends BaseElement {

    private final String member;

    public NestMember(String member) {
        this.member = member;
    }

    public String getMember() {
        return member;
    }

    @Override
    public String print(PrintContext context) {
        return context.fmtKeyword("nestmember") + ' ' + context.fmtIdentifier(member);
    }

}
