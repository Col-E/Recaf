package me.coley.recaf.ssvm;

import dev.xdark.ssvm.fs.BasicZipFile;
import dev.xdark.ssvm.fs.ZipFile;
import me.coley.recaf.code.LiteralInfo;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Dummy {@link ZipFile} to use in {@link SsvmIntegration}.
 * Essentially this "jar" file contains dynamically resolvable references to files in the workspace.
 *
 * @author Matt Coley
 * @author xDark
 */
public class WorkspaceZipFile extends BasicZipFile {
	public static final String RECAF_LIVE_ZIP =
			new File(System.getProperty("java.io.tmpdir"), "recaf-workspace.jar").getAbsolutePath();
	private final Workspace workspace;
	private List<ZipEntry> entries;

	/**
	 * @param rawHandle
	 * 		Zip file handle.
	 * @param workspace
	 * 		Workspace to pull data from.
	 */
	public WorkspaceZipFile(int rawHandle, Workspace workspace) {
		super(rawHandle);
		this.workspace = workspace;
	}

	@Override
	public int getTotal() {
		return 0;
	}

	@Override
	protected List<ZipEntry> getEntries() {
		List<ZipEntry> entries = this.entries;
		if (entries == null) {
			Resources resources = workspace.getResources();
			List<ZipEntry> zipEntries = new ArrayList<>();
			resources.getClasses()
					.map(x -> {
						ZipEntry entry = new ZipEntry(x.getName() + ".class");
						int size = x.getValue().length;
						entry.setMethod(ZipEntry.STORED);
						entry.setSize(size);
						entry.setCompressedSize(size);
						return entry;
					})
					.collect(Collectors.toCollection(() -> zipEntries));
			resources.getFiles()
					.map(x -> {
						ZipEntry entry = new ZipEntry(x.getName());
						int size = x.getValue().length;
						entry.setMethod(ZipEntry.STORED);
						entry.setSize(size);
						entry.setCompressedSize(size);
						return entry;
					})
					.collect(Collectors.toCollection(() -> zipEntries));
			return this.entries = zipEntries;
		}
		return entries;
	}

	@Override
	protected InputStream openStream(ZipEntry entry) throws IOException {
		String name = entry.getName();
		Resources resources = workspace.getResources();
		LiteralInfo info;
		if (name.endsWith(".class")) {
			info = resources.getClass(name.substring(0, name.length() - 6));
		} else {
			info = resources.getFile(name);
		}
		if (info == null) {
			throw new ZipException(name);
		}
		return new ByteArrayInputStream(info.getValue());
	}
}