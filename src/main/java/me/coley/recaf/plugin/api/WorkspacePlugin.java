package me.coley.recaf.plugin.api;

import me.coley.recaf.workspace.Workspace;

/**
 * Allow plugins to perform workspace specific actions
 * @see Workspace
 *
 * @author xxDark
 */
public interface WorkspacePlugin extends BasePlugin {
    void onClosed(Workspace workspace);

    void onOpened(Workspace workspace);
}
