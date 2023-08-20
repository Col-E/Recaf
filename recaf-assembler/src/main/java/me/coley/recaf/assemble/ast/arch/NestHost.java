package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;

/**
 * Nest host declaration.
 *
 * @author Justus Garbe
 */
public class NestHost extends BaseElement {
    private final String host;

    public NestHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    @Override
    public String print(PrintContext context) {
        return context.fmtKeyword("nesthost") + ' ' + context.fmtIdentifier(host);
    }
}
