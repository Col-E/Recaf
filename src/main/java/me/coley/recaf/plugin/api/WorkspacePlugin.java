package me.coley.recaf.plugin.api;

import me.coley.recaf.workspace.Workspace;

/**
 * Allow plugins to perform workspace specific actions
 *
 * @author xxDark
 * @see Workspace
 */
public interface WorkspacePlugin extends BasePlugin {
    /**
     * Called whether workspace is closed.
     *
     * @param workspace the workspace.
     */
    void onClosed(Workspace workspace);

    /**
     * Called whether new workspace is opened.
     * @param workspace the workspace.
     */
    void onOpened(Workspace workspace);
}
