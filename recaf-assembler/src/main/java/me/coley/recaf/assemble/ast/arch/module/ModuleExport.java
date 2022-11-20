package me.coley.recaf.assemble.ast.arch.module;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.arch.Modifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * Module export statement.
 *
 * @author Justus Garbe
 */
public class ModuleExport extends BaseElement {

    private final String name;
    private final Modifiers modifiers;
    private final List<String> packages = new ArrayList<>();

    public ModuleExport(String name, Modifiers modifiers) {
        this.name = name;
        this.modifiers = modifiers;
    }

    public void addPackage(String pkg) {
        packages.add(pkg);
    }

    public String getName() {
        return name;
    }

    public Modifiers getModifiers() {
        return modifiers;
    }

    public List<String> getPackages() {
        return packages;
    }

    @Override
    public String print(PrintContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.fmtKeyword("exports"));
        sb.append(' ');
        if(modifiers.value() > 0) {
            sb.append(modifiers.print(context));
            sb.append(' ');
        }
        sb.append(context.fmtIdentifier(name));
        if(packages.size() > 0) {
            sb.append(' ');
            sb.append(context.fmtKeyword("to"));
            sb.append(' ');
            sb.append(String.join(" ", packages));
            sb.append(' ');
            sb.append(context.fmtKeyword("end"));
        }
        return sb.toString();
    }
}
