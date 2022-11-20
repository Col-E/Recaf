package me.coley.recaf.assemble.ast.arch.module;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Module provide statement.
 *
 * @author Justus Garbe
 */
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
        StringBuilder sb = new StringBuilder();
        sb.append(context.fmtKeyword("provides"))
          .append(" ")
          .append(context.fmtIdentifier(name));
        if(packages.size() > 0) {
            sb.append(" ")
              .append(context.fmtKeyword("with"))
              .append(" ")
              .append(String.join(" ", packages))
              .append(" ")
              .append(context.fmtKeyword("end"));
        }
        return sb.toString();
    }
}
