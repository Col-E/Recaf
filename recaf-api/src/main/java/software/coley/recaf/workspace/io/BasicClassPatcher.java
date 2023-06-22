package software.coley.recaf.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.classfile.ClassFile;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.cafedude.io.ClassFileWriter;
import me.coley.cafedude.transform.IllegalStrippingTransformer;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.io.IOException;

/**
 * Basic patcher implementation with CafeDude.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicClassPatcher implements ClassPatcher {
	private static final Logger logger = Logging.get(BasicClassPatcher.class);

	@Nonnull
	@Override
	public byte[] patch(@Nullable String name, @Nonnull byte[] code) throws IOException {
		try {
			// Patch via CafeDude
			ClassFileReader reader = new ClassFileReader();
			ClassFile classFile = reader.read(code);
			if (name == null) name = classFile.getName();
			new IllegalStrippingTransformer(classFile).transform();
			return new ClassFileWriter().write(classFile);
		} catch (InvalidClassException ex) {
			if (name == null) name = "<no-name-given>";
			logger.error("CafeDude failed to parse '{}'", name, ex);
			throw new IOException(ex);
		} catch (Throwable t) {
			if (name == null) name = "<no-name-given>";
			logger.error("CafeDude failed to patch '{}'", name, t);
			throw new IOException(t);
		}
	}
}
