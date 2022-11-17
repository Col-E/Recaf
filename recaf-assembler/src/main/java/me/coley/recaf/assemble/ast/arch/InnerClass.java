package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;

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
        return context.fmtKeyword("innerclass") + ' ' +
                modifiers.print(context).toLowerCase() + ' ' +
                name + ' ' +
                outerName + ' ' +
                innerName;
    }
}
