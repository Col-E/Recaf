package me.coley.recaf.ssvm.loader;

import dev.xdark.recaf.jdk.resources.JdkResourcesServer;
import dev.xdark.ssvm.classloading.BootClassLoader;
import dev.xdark.ssvm.classloading.ClassParseResult;
import dev.xdark.ssvm.util.ClassUtil;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

/**
 * Boot class loader that pulls data from remote JVM.
 *
 * @author xDark
 */
public final class RemoteBootClassLoader implements BootClassLoader {
	private static final Logger logger = Logging.get(RemoteBootClassLoader.class);
	private final JdkResourcesServer peer;

	/**
	 * @param peer
	 * 		Remote JVM peer.
	 */
	public RemoteBootClassLoader(JdkResourcesServer peer) {
		this.peer = peer;
	}

	@Override
	public ClassParseResult findBootClass(String name) {
		try {
			byte[] resource = peer.requestResource(name + ".class");
			if (resource == null) {
				return null;
			}
			ClassReader reader = new ClassReader(resource);
			return new ClassParseResult(reader, ClassUtil.readNode(reader));
		} catch (Exception ex) {
			logger.error("Could not get remote resource {}: \n{}", name, ex.getMessage());
			return null;
		}
	}
}
