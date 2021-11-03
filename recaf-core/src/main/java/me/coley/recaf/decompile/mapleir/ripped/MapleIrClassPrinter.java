package me.coley.recaf.decompile.mapleir.ripped;

import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MapleIrClassPrinter {
    public StringBuilder decompile(final StringBuilder sb, final ClassNode mcn) {
        final org.objectweb.asm.tree.ClassNode cn = mcn.node;
        
        ArrayList<String> unableToDecompile = new ArrayList<>();
        sb.append(getAccessString(cn.access));
        sb.append(" ");
        sb.append(cn.name);
        if (cn.superName != null && !cn.superName.equals("java/lang/Object")) {
            sb.append(" extends ");
            sb.append(cn.superName);
        }

        int amountOfInterfaces = cn.interfaces.size();
        if (amountOfInterfaces > 0) {
            sb.append(" implements ");
            sb.append(cn.interfaces.get(0));
            if (amountOfInterfaces > 1) {
                // sb.append(",");
            }
            for (int i = 1; i < amountOfInterfaces; i++) {
                sb.append(", ");
                sb.append(cn.interfaces.get(i));
            }
        }
        sb.append(" {");
        sb.append(MapleIrUtils.nl);

        for (Iterator<FieldNode> it = cn.fields.iterator(); it.hasNext(); ) {
            sb.append("     ");
            getFieldNodeDecompiler(sb, it).decompile();
            sb.append(MapleIrUtils.nl);
            if (!it.hasNext())
                sb.append(MapleIrUtils.nl);
        }

        for (Iterator<MethodNode> it = mcn.getMethods().iterator(); it.hasNext(); ) {
            getMethodNodeDecompiler(sb, mcn, it).decompile();
            if (it.hasNext())
                sb.append(MapleIrUtils.nl);
        }

        /*if (settings.getEntry("decompile-inner-classes").getBool())
            for (InnerClassNode innerClassNode : cn.innerClasses) {
                String innerClassName = innerClassNode.name;
                if ((innerClassName != null) && !decompiledClasses.contains(innerClassName)) {
                    decompiledClasses.add(innerClassName);
                    ClassNode cn1 = container.loadClassFile(container.findClassfile(innerClassName));
                    applyFilters(cn1);
                    if (cn1 != null) {
                        sb.appendPrefix("     ");
                        sb.append(MapleIrUtils.nl + MapleIrUtils.nl);
                        sb = decompile(sb, decompiledClasses, container, cn1);
                        sb.trimPrefix(5);
                        sb.append(MapleIrUtils.nl);
                    } else {
                        unableToDecompile.add(innerClassName);
                    }
                }
            }*/

        if (!unableToDecompile.isEmpty()) {
            sb.append("// The following inner classes couldn't be decompiled: ");
            for (String s : unableToDecompile) {
                sb.append(s);
                sb.append(" ");
            }
            sb.append(MapleIrUtils.nl);
        }

        sb.append("}");
        // System.out.println("Wrote end for " + cn.name +
        // " with prefix length: " + sb.prefix.length());
        return sb;
    }

    protected MapleIrFieldPrinter getFieldNodeDecompiler(StringBuilder sb, Iterator<FieldNode> it) {
        return new MapleIrFieldPrinter(sb, it.next());
    }

    protected MapleIrMethodPrinter getMethodNodeDecompiler(StringBuilder sb, ClassNode cn, Iterator<MethodNode> it) {
        return new MapleIrMethodPrinter(cn.node, it.next(), sb);
    }
    public static String getAccessString(int access) {
        List<String> tokens = new ArrayList<>();
        if ((access & Opcodes.ACC_PUBLIC) != 0)
            tokens.add("public");
        if ((access & Opcodes.ACC_PRIVATE) != 0)
            tokens.add("private");
        if ((access & Opcodes.ACC_PROTECTED) != 0)
            tokens.add("protected");
        if ((access & Opcodes.ACC_FINAL) != 0)
            tokens.add("final");
        if ((access & Opcodes.ACC_SYNTHETIC) != 0)
            tokens.add("synthetic");
        // if ((access & Opcodes.ACC_SUPER) != 0)
        // tokens.add("super"); implied by invokespecial insn
        if ((access & Opcodes.ACC_ABSTRACT) != 0)
            tokens.add("abstract");
        if ((access & Opcodes.ACC_INTERFACE) != 0)
            tokens.add("interface");
        if ((access & Opcodes.ACC_ENUM) != 0)
            tokens.add("enum");
        if ((access & Opcodes.ACC_ANNOTATION) != 0)
            tokens.add("annotation");
        if (!tokens.contains("interface") && !tokens.contains("enum") && !tokens.contains("annotation"))
            tokens.add("class");
        if (tokens.size() == 0)
            return "[Error parsing]";

        // hackery delimeters
        StringBuilder sb = new StringBuilder(tokens.get(0));
        for (int i = 1; i < tokens.size(); i++) {
            sb.append(" ");
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }
}
