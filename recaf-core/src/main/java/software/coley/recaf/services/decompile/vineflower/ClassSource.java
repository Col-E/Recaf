package software.coley.recaf.services.decompile.vineflower;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single class source for Vineflower
 *
 * @author Matt Coley
 * @author therathatter
 */
public class ClassSource extends BaseSource {
    private final JvmClassInfo info;
    private final DecompiledOutputSink sink;

    protected ClassSource(Workspace workspace, JvmClassInfo info) {
        super(workspace);
        this.info = info;
        sink = new DecompiledOutputSink(info);
    }

    @Override
    public Entries getEntries() {
        // TODO: Bug in QF/VF makes it so that 'addLibrary' doesn't yield inner info for a class provided with 'addSource'
        //  So for now until this is fixed upstream we will also supply inners here.
        //  This will make QF/VF decompile each inner class separately as well, but its the best fix for now without
        //  too much of a perf hit.
        List<Entry> entries = new ArrayList<>();

        entries.add(new Entry(info.getName(), Entry.BASE_VERSION));
        for (InnerClassInfo innerClass : info.getInnerClasses())
            entries.add(new Entry(innerClass.getName(), Entry.BASE_VERSION));
        return new Entries(entries, Collections.emptyList(), Collections.emptyList());
    }

    DecompiledOutputSink getSink() {
        return this.sink;
    }

    @Override
    public IOutputSink createOutputSink(IResultSaver saver) {
        return sink;
    }
}
