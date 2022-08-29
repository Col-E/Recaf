package me.coley.recaf.decompile.procyon;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.workspace.resource.Resource;

/**
 * Type loader that pulls classes from a {@link Resource}.
 *
 * @author xDark
 */
public final class ResourceTypeLoader implements ITypeLoader {
	private final Resource resource;

	/**
	 * @param resource
	 * 		Resource to pull classes from.
	 */
	public ResourceTypeLoader(Resource resource) {
		this.resource = resource;
	}

	@Override
	public boolean tryLoadType(String internalName, Buffer buffer) {
		ClassInfo info = resource.getClasses().get(internalName);
		if (info == null)
			return false;
		buffer.position(0);
		byte[] data = info.getValue();
		buffer.putByteArray(data, 0, data.length);
		buffer.position(0);
		return true;
	}
}
