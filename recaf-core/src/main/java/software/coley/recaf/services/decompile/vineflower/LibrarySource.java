package software.coley.recaf.services.decompile.vineflower;

import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Full library source for Vineflower
 *
 * @author Matt Coley
 * @author therathatter
 */
public class LibrarySource extends BaseSource {
    protected LibrarySource(Workspace workspace) {
        super(workspace);
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
