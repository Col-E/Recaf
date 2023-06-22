package software.coley.recaf.services.decompile.procyon;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

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
		ClassPathNode node = workspace.findClass(internalName);
		if (node == null)
			return false;
		byte[] data = node.getValue().asJvmClass().getBytecode();
		buffer.position(0);
		buffer.putByteArray(data, 0, data.length);
		buffer.position(0);
		return true;
	}
}