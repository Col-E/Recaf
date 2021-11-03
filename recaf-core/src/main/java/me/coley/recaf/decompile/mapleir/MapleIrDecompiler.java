package me.coley.recaf.decompile.mapleir;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.DecompileOption;
import me.coley.recaf.decompile.Decompiler;
import me.coley.recaf.decompile.mapleir.ripped.MapleIrClassPrinter;
import me.coley.recaf.workspace.Workspace;
import org.mapleir.asm.ClassNode;

import java.util.HashMap;
import java.util.Map;

public class MapleIrDecompiler extends Decompiler {
    private MapleClassSource source;
    private final MapleIrClassPrinter printer = new MapleIrClassPrinter();

    public MapleIrDecompiler() {
        super("MapleIR", "1.0.2");
    }

    @Override
    protected String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
        final String name = classInfo.getName();

        if (source == null || source.getWorkspace() != workspace) {
            source = new MapleClassSource(workspace);
        }

        final ClassNode node = source.findClassNode(name);
        return printer.decompile(new StringBuilder(), node).toString();
    }

    @Override
    protected Map<String, DecompileOption<?>> createDefaultOptions() {
        return new HashMap<>();
    }
}
