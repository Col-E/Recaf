package me.coley.recaf.assemble.ast.arch.module;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.arch.Modifiers;

import java.util.ArrayList;
import java.util.List;

/**
 * AST element for the java module attribute.
 *
 * @author Justus Garbe
 */
public class Module extends BaseElement {

    private final String name;
    private final Modifiers modifiers;
    private String version;
    private String mainClass;
    private final List<String> packages = new ArrayList<>();
    private final List<ModuleRequire> requires = new ArrayList<>();
    private final List<ModuleExport> exports = new ArrayList<>();
    private final List<ModuleOpen> opens = new ArrayList<>();
    private final List<String> uses = new ArrayList<>();
    private final List<ModuleProvide> provides = new ArrayList<>();

    public Module(String name, Modifiers modifiers) {
        this.name = name;
        this.modifiers = modifiers;
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

    public void setVersion(String version) {
        this.version = version;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void addPackage(String pkg) {
        packages.add(pkg);
    }

    public void addRequire(ModuleRequire require) {
        requires.add(require);
    }

    public void addExport(ModuleExport export) {
        exports.add(export);
    }

    public void addOpen(ModuleOpen open) {
        opens.add(open);
    }

    public void addUse(String use) {
        uses.add(use);
    }

    public void addProvide(ModuleProvide provide) {
        provides.add(provide);
    }

    public String getMainClass() {
        return mainClass;
    }

    public List<String> getPackages() {
        return packages;
    }

    public List<ModuleRequire> getRequires() {
        return requires;
    }

    public List<ModuleExport> getExports() {
        return exports;
    }

    public List<ModuleOpen> getOpens() {
        return opens;
    }

    public List<String> getUses() {
        return uses;
    }

    public List<ModuleProvide> getProvides() {
        return provides;
    }

    @Override
    public String print(PrintContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.fmtKeyword("module"));
        sb.append(" ");
        sb.append(modifiers.print(context));
        sb.append(" ");
        sb.append(context.fmtIdentifier(name));
        if(version != null) {
            sb.append(" ");
            sb.append(context.fmtKeyword("version"));
            sb.append(" ");
            sb.append(context.fmtIdentifier(version));
        }
        sb.append("\n");
        if(mainClass != null) {
            sb.append('\t').append(context.fmtKeyword("mainclass")).append(" ");
            sb.append(context.fmtIdentifier(mainClass));
            sb.append("\n");
        }
        if(!packages.isEmpty()) {
            for (String aPackage : packages) {
                sb.append('\t').append(context.fmtKeyword("package")).append(" ");
                sb.append(context.fmtIdentifier(aPackage));
                sb.append("\n");
            }
            sb.append('\n');
        }
        if(!requires.isEmpty()) {
            for (ModuleRequire require : requires) {
                sb.append('\t').append(require.print(context));
                sb.append("\n");
            }
            sb.append('\n');
        }
        if(!exports.isEmpty()) {
            for (ModuleExport export : exports) {
                sb.append('\t').append(export.print(context));
                sb.append("\n");
            }
            sb.append('\n');
        }
        if(!opens.isEmpty()) {
            for (ModuleOpen open : opens) {
                sb.append('\t').append(open.print(context));
                sb.append("\n");
            }
            sb.append('\n');
        }
        if(!uses.isEmpty()) {
            for (String use : uses) {
                sb.append('\t').append(context.fmtKeyword("uses")).append(" ");
                sb.append(context.fmtIdentifier(use));
                sb.append("\n");
            }
            sb.append('\n');
        }
        if(!provides.isEmpty()) {
            for (ModuleProvide provide : provides) {
                sb.append('\t').append(provide.print(context));
                sb.append("\n");
            }
        }
        sb.append(context.fmtKeyword("end"));
        return sb.toString();
    }
}
