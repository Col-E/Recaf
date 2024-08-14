package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single class source for Vineflower.
 *
 * @author Matt Coley
 * @author therathatter
 */
public class ClassSource extends BaseSource {
	private final DecompiledOutputSink sink;

	/**
	 * @param workspace
	 * 		Workspace to pull class files from.
	 * @param targetInfo
	 * 		Target class to decompile.
	 */
	protected ClassSource(@Nonnull Workspace workspace, @Nonnull JvmClassInfo targetInfo) {
		super(workspace, targetInfo);
		sink = new DecompiledOutputSink(targetInfo);
	}

	/**
	 * @return Output which holds the decompilation result after the decompilation task completes.
	 */
	@Nonnull
	protected DecompiledOutputSink getSink() {
		return sink;
	}

	@Override
	public Entries getEntries() {
		// TODO: Bug in QF/VF makes it so that 'addLibrary' doesn't yield inner info for a class provided with 'addSource'
		//  So for now until this is fixed upstream we will also supply inners here.
		//  This will make QF/VF decompile each inner class separately as well, but its the best fix for now without
		//  too much of a perf hit.
		List<Entry> entries = new ArrayList<>();
		entries.add(new Entry(targetInfo.getName(), Entry.BASE_VERSION));
		for (InnerClassInfo innerClass : targetInfo.getInnerClasses()) {
			// Only add entry if it exists in the workspace.
			if (workspace.findClass(innerClass.getInnerClassName()) != null)
				entries.add(new Entry(innerClass.getName(), Entry.BASE_VERSION));
		}
		return new Entries(entries, Collections.emptyList(), Collections.emptyList());
	}

	@Override
	public IOutputSink createOutputSink(IResultSaver saver) {
		return sink;
	}
}
