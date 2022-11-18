package me.coley.recaf.assemble.ast.arch.module;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;

import java.util.ArrayList;
import java.util.List;

public class ModuleProvide extends BaseElement {

    private final String name;
    private final List<String> packages = new ArrayList<>();

    public ModuleProvide(String name) {
        this.name = name;
    }

    public void addPackage(String pkg) {
        packages.add(pkg);
    }

    public String getName() {
        return name;
    }

    public List<String> getPackages() {
        return packages;
    }

    @Override
    public String print(PrintContext context) {
        return context.fmtKeyword("provides")
                + " " + context.fmtIdentifier(name)
                + " " + context.fmtKeyword("with")
                + " " + String.join(" ", packages)
                + " " + context.fmtKeyword("end");
    }
}
