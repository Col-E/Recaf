package me.coley.recaf.ssvm;

import dev.xdark.ssvm.fs.ZipFile;
import me.coley.recaf.workspace.Workspace;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * Dummy {@link ZipFile} to use in {@link SsvmIntegration}.
 * Essentially this "jar" file contains dynamically resolvable references to files in the workspace.
 *
 * @author Matt Coley
 */
public class WorkspaceZipFile implements ZipFile {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull data from.
	 */
	public WorkspaceZipFile(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public ZipEntry getEntry(String name) {
		return new ZipEntry(name);
	}

	@Override
	public byte[] readEntry(ZipEntry entry) throws IOException {
		if (entry == null)
			throw new IOException();
		String name = entry.getName();
		if (name.endsWith(".class"))
			name = name.substring(0, name.length() - ".class".length());
		// TODO: Allow access to workspace files too?
		return workspace.getResources().getClass(name).getValue();
	}

	@Override
	public Stream<ZipEntry> stream() {
		return Stream.empty();
	}

	@Override
	public int getTotal() {
		return 1;
	}

	@Override
	public boolean startsWithLOC() {
		return true;
	}

	@Override
	public ZipEntry getEntry(int index) {
		return null;
	}

	@Override
	public long makeHandle(ZipEntry entry) {
		return 0;
	}

	@Override
	public ZipEntry getEntry(long handle) {
		return null;
	}

	@Override
	public boolean freeHandle(long handle) {
		return true;
	}

	@Override
	public void close() throws IOException {
		// no-op
	}
}