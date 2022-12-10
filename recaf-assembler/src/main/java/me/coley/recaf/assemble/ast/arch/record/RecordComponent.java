package me.coley.recaf.assemble.ast.arch.record;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.arch.AbstractAttributable;
import me.coley.recaf.assemble.ast.arch.Attributable;
import me.coley.recaf.assemble.ast.arch.Definition;

public class RecordComponent extends AbstractAttributable {

    private final String name;
    private final String descriptor;

    public RecordComponent(String name, String descriptor) {
        this.name = name;
        this.descriptor = descriptor;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public String print(PrintContext context) {
        String def = buildDefString(context);
        if(!def.isEmpty())
            def = '\t' + def;
        return def + '\t' + context.fmtKeyword("component") + " " + context.fmtIdentifier(name) + " " + context.fmtIdentifier(descriptor) + "\n";
    }
}
