package me.coley.recaf.assemble.ast.arch.module;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.arch.Modifiers;

/**
 * Module require statement.
 *
 * @author Justus Garbe
 */
public class ModuleRequire extends BaseElement {

    private final String name;
    private final Modifiers modifiers;
    private String version;

    public ModuleRequire(String name, Modifiers modifiers) {
        this.name = name;
        this.modifiers = modifiers;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public Modifiers getModifiers() {
        return modifiers;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String print(PrintContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.fmtKeyword("requires"));
        sb.append(' ');
        if(modifiers.value() > 0) {
            sb.append(modifiers.print(context));
            sb.append(' ');
        }
        sb.append(context.fmtIdentifier(name));
        if(version != null) {
            sb.append(" ");
            sb.append(context.fmtKeyword("version"));
            sb.append(" ");
            sb.append(context.fmtIdentifier(version));
        }
        return sb.toString();
    }
}
