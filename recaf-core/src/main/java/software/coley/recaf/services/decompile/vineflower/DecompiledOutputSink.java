package software.coley.recaf.services.decompile.vineflower;

import jakarta.annotation.Nonnull;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import software.coley.recaf.info.JvmClassInfo;

import java.io.IOException;

/**
 * Output sink for Vineflower decompiler.
 *
 * @author therathatter
 */
public class DecompiledOutputSink implements IContextSource.IOutputSink {
	protected final JvmClassInfo target;
	protected final ThreadLocal<String> out = new ThreadLocal<>();

	/**
	 * @param target
	 * 		Target class to get output of.
	 */
	protected DecompiledOutputSink(@Nonnull JvmClassInfo target) {
		this.target = target;
	}

	/**
	 * @return Local wrapper of decompilation output.
	 */
	@Nonnull
	protected ThreadLocal<String> getDecompiledOutput() {
		return out;
	}

	@Override
	public void begin() {
		// no-op
	}

	@Override
	public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
		if (target.getName().equals(qualifiedName))
			out.set(content);
	}

	@Override
	public void acceptDirectory(String directory) {
		// no-op
	}

	@Override
	public void acceptOther(String path) {
		// no-op
	}

	@Override
	public void close() throws IOException {
		// no-op
	}
}
