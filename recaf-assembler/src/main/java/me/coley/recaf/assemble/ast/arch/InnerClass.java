package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.EscapeUtil;

/**
 * Inner class declaration.
 *
 * @author Justus Garbe
 */
public class InnerClass extends BaseElement {

    private final Modifiers modifiers;
    private final String name;
    private final String outerName;
    private final String innerName;

    public InnerClass(Modifiers mods, String name, String outerName, String innerName) {
        this.modifiers = mods;
        this.name = name;
        this.outerName = outerName;
        this.innerName = innerName;
    }

    public Modifiers getModifiers() {
        return modifiers;
    }

    public String getName() {
        return name;
    }

    public String getOuterName() {
        return outerName;
    }

    public String getInnerName() {
        return innerName;
    }

    @Override
    public String print(PrintContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.fmtKeyword("innerclass")).append(' ');
        if (modifiers.value() > 0) {
            sb.append(modifiers.print(context).toLowerCase()).append(' ');
        }
        sb.append(context.fmtIdentifier(name)).append(' ');
        sb.append(context.fmtIdentifier(outerName)).append(' ');
        sb.append(context.fmtIdentifier(innerName));
        return sb.toString();
    }
}
