package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Full library source for Vineflower.
 *
 * @author Matt Coley
 * @author therathatter
 */
public class LibrarySource extends BaseSource {
	/**
	 * @param workspace
	 * 		Workspace to pull class files from.
	 * @param targetInfo
	 * 		Target class to decompile.
	 */
	protected LibrarySource(@Nonnull Workspace workspace, @Nonnull JvmClassInfo targetInfo) {
		super(workspace, targetInfo);
	}

	@Override
	public Entries getEntries() {
		List<Entry> entries = workspace.getAllResources(false).stream()
				.map(WorkspaceResource::getJvmClassBundle)
				.flatMap(c -> c.keySet().stream())
				.map(className -> new Entry(className, Entry.BASE_VERSION))
				.collect(Collectors.toList());
		return new Entries(entries, Collections.emptyList(), Collections.emptyList());
	}
}
