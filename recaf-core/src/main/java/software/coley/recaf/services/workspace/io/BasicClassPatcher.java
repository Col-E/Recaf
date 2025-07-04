package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import software.coley.cafedude.InvalidClassException;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.classfile.behavior.AttributeHolder;
import software.coley.cafedude.classfile.constant.CpUtf8;
import software.coley.cafedude.io.ClassBuilder;
import software.coley.cafedude.io.ClassFileReader;
import software.coley.cafedude.io.ClassFileWriter;
import software.coley.cafedude.io.FallbackInstructionReader;
import software.coley.cafedude.transform.IllegalRewritingInstructionsReader;
import software.coley.cafedude.transform.IllegalStrippingTransformer;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.Types;

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
			ClassFileReader reader = new ClassFileReaderExt();
			ClassFile classFile = reader.read(code);
			if (name == null) name = classFile.getName();
			new IllegalStrippingTransformerExt(classFile).transform();
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

	/**
	 * Extended class file reader that plugs into {@link IllegalRewritingInstructionsReader}.
	 */
	private static class ClassFileReaderExt extends ClassFileReader {
		private FallbackInstructionReader fallback;

		@Nonnull
		@Override
		public FallbackInstructionReader getFallbackInstructionReader(@Nonnull ClassBuilder builder) {
			if (fallback == null)
				fallback = new IllegalRewritingInstructionsReader(builder.getPool(), builder.getVersionMajor());
			return fallback;
		}
	}

	/**
	 * Extended illegal stripping transformer that also drops invalid signatures.
	 */
	private static class IllegalStrippingTransformerExt extends IllegalStrippingTransformer {
		private IllegalStrippingTransformerExt(@Nonnull ClassFile clazz) {
			super(clazz);
		}

		@Override
		protected boolean matchSignature(@Nonnull CpUtf8 e, @Nonnull AttributeHolder context) {
			return switch (context.getHolderType()) {
				case CLASS -> Types.isValidSignature(e.getText(), Types.SignatureContext.CLASS);
				case FIELD, RECORD_COMPONENT -> Types.isValidSignature(e.getText(), Types.SignatureContext.FIELD);
				case METHOD -> Types.isValidSignature(e.getText(), Types.SignatureContext.METHOD);
				case ATTRIBUTE -> false;
			};
		}
	}
}
