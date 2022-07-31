package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.workspace.Workspace;

/**
 * Type loader that pulls classes from a {@link Workspace}.
 *
 * @author xDark
 */
public final class WorkspaceTypeLoader implements ITypeLoader {
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Active workspace.
	 */
	public WorkspaceTypeLoader(Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public boolean tryLoadType(String internalName, Buffer buffer) {
		ClassInfo info = workspace.getResources().getClass(internalName);
		if (info == null)
			return false;
		buffer.position(0);
		byte[] data = info.getValue();
		buffer.putByteArray(data, 0, data.length);
		buffer.position(0);
		return true;
	}
}
