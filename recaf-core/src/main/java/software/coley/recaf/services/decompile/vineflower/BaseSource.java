package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Base Vineflower class/library source.
 *
 * @author therathatter
 */
public abstract class BaseSource implements IContextSource {
	protected final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to pull class files from.
	 */
	protected BaseSource(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	@Override
	public String getName() {
		return "Recaf";
	}

	@Override
	public InputStream getInputStream(String resource) {
		String name = resource.substring(0, resource.length() - IContextSource.CLASS_SUFFIX.length());
		ClassPathNode node = workspace.findClass(name);
		if (node == null) return InputStream.nullInputStream();
		return new ByteArrayInputStream(node.getValue().asJvmClass().getBytecode());
	}
}
