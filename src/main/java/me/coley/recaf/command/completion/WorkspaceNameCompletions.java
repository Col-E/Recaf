package me.coley.recaf.command.completion;

import me.coley.recaf.Recaf;
import me.coley.recaf.workspace.Workspace;

import java.util.*;

/**
 * Picocli completion for names loaded in the current workspace.
 *
 * @author Matt
 */
public class WorkspaceNameCompletions implements Iterable<String> {
	@Override
	public Iterator<String> iterator() {
		Workspace workspace = Recaf.getCurrentWorkspace();
		if (workspace == null)
			return Collections.emptyIterator();
		return workspace.getClassNames().stream().sorted().iterator();
	}
}
