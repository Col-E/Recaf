package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.slf4j.Logger;
import software.coley.cafedude.InvalidClassException;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.classfile.ConstantPoolConstants;
import software.coley.cafedude.classfile.attribute.BootstrapMethodsAttribute;
import software.coley.cafedude.classfile.behavior.AttributeHolder;
import software.coley.cafedude.classfile.constant.ConstDynamic;
import software.coley.cafedude.classfile.constant.CpEntry;
import software.coley.cafedude.classfile.constant.CpUtf8;
import software.coley.cafedude.io.ClassBuilder;
import software.coley.cafedude.io.ClassFileReader;
import software.coley.cafedude.io.ClassFileWriter;
import software.coley.cafedude.io.FallbackInstructionReader;
import software.coley.cafedude.transform.IllegalRewritingInstructionsReader;
import software.coley.cafedude.transform.IllegalStrippingTransformer;
import software.coley.cafedude.transform.Transformer;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.Types;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic patcher implementation with CafeDude.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicClassPatcher implements ClassPatcher {
	private static final Logger logger = Logging.get(BasicClassPatcher.class);

	@Nullable
	@Override
	public byte[] prefilter(@Nullable String name, @Nonnull byte[] code) throws IOException {
		if (!hasDynamicConstants(code))
			return null;
		return patch(name, code, true);
	}

	@Nonnull
	@Override
	public byte[] patch(@Nullable String name, @Nonnull byte[] code) throws IOException {
		byte[] patched = patch(name, code, false);
		if (patched == null)
			throw new IllegalStateException("Unconditional patching produced no output");
		return patched;
	}

	@Nullable
	private static byte[] patch(@Nullable String name, @Nonnull byte[] code, boolean onlyIfNecessary) throws IOException {
		try {
			// Patch via CafeDude
			ClassFileReader reader = new ClassFileReaderExt();
			ClassFile classFile = reader.read(code);
			if (name == null) name = classFile.getName();
			BootstrapSpamTransformer bootstrapTransformer = new BootstrapSpamTransformer(classFile);
			bootstrapTransformer.transform();
			if (onlyIfNecessary && !bootstrapTransformer.wasTransformed())
				return null;
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
	 * @param code
	 * 		Input bytecode.
	 *
	 * @return {@code true} if the bytecode contains dynamic constants,
	 * which can exploit ASM's constant pool copying to exhaust memory and CPU.
	 */
	private static boolean hasDynamicConstants(@Nonnull byte[] code) {
		try {
			ClassReader reader = new ClassReader(code);
			for (int i = 1; i < reader.getItemCount(); i++) {
				int offset = reader.getItem(i);
				if (offset > 0 && reader.readByte(offset - 1) == ConstantPoolConstants.DYNAMIC)
					return true;
			}
		} catch (Throwable ignored) {
			// Let the normal ASM validation and CafeDude fallback handle malformed constant pools.
		}
		return false;
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
	 * When {@link ClassWriter} initializes from an existing {@link ClassReader} it populates a {@code SymbolTable}.
	 * This process will copy the {@link BootstrapMethodsAttribute} which can be filled with entries that have
	 * thousands of arguments. Arguments can refer to other bootstrap methods with their own arguments,
	 * and this can be stacked to any arbitrary level of depth.
	 * <ul>
	 *     <li>If you have 1,000 arguments referring to another BSM, with 1,000 arguments of its own,
	 *     you get 1,000,000 arguments total.</li>
	 *     <li>If you have 1 argument referring to another BSM which has 2 arguments,
	 *     each which refer to another BSM which has 2 arguments,
	 *     repeat this 20 times and you get 1,048,576 values.</li>
	 * </ul>
	 * The way ASM copies this data is <i>incredibly slow</i>.
	 */
	private static class BootstrapSpamTransformer extends Transformer {
		private static final int ARG_THRESHOLD = 100;
		private final Map<BootstrapMethodsAttribute.BootstrapMethod, Integer> argCount = new IdentityHashMap<>();
		private final Set<BootstrapMethodsAttribute.BootstrapMethod> visiting =
				Collections.newSetFromMap(new IdentityHashMap<>());
		private boolean transformed;

		public BootstrapSpamTransformer(@Nonnull ClassFile clazz) {
			super(clazz);
		}

		@Override
		public void transform() {
			BootstrapMethodsAttribute attribute = clazz.getAttribute(BootstrapMethodsAttribute.class);
			if (attribute == null)
				return;
			for (BootstrapMethodsAttribute.BootstrapMethod bootstrapMethod : attribute.getBootstrapMethods()) {
				if (computeTotalArgs(attribute, bootstrapMethod) >= ARG_THRESHOLD &&
						!bootstrapMethod.getArgs().isEmpty()) {
					bootstrapMethod.setArgs(Collections.emptyList());
					transformed = true;
				}
			}
		}

		public boolean wasTransformed() {
			return transformed;
		}

		private int computeTotalArgs(@Nonnull BootstrapMethodsAttribute bsmAttribute,
		                             @Nonnull BootstrapMethodsAttribute.BootstrapMethod bsm) {
			// Get cached count if visited before.
			Integer cachedValue = argCount.get(bsm);
			if (cachedValue != null)
				return cachedValue;

			// A bootstrap graph is allowed to be cyclic. ASM recursively materializes dynamic constants without a
			// cycle guard, so a cycle must be treated as exceeding the limit.
			if (!visiting.add(bsm))
				return ARG_THRESHOLD;

			// Get direct arg count.
			List<CpEntry> args = bsm.getArgs();
			int total = args.size();

			// Only sum indirect-arguments if we're under the threshold.
			if (total < ARG_THRESHOLD) {
				for (CpEntry arg : args) {
					total += countIndirectArgs(bsmAttribute, arg);
					if (total > ARG_THRESHOLD)
						break;
				}
			}

			visiting.remove(bsm);
			argCount.put(bsm, total);
			return total;
		}

		private int countIndirectArgs(@Nonnull BootstrapMethodsAttribute bsmAttribute, @Nonnull CpEntry arg) {
			if (arg instanceof ConstDynamic dynamic) {
				int index = dynamic.getBsmIndex();
				var bootstrapMethods = bsmAttribute.getBootstrapMethods();
				if (index < 0 || index >= bootstrapMethods.size())
					return ARG_THRESHOLD; // Short-circuit loops.

				// Yield the arg count of the referenced bootstrap method.
				BootstrapMethodsAttribute.BootstrapMethod bsm = bootstrapMethods.get(dynamic.getBsmIndex());
				return computeTotalArgs(bsmAttribute, bsm);
			}
			return 0;
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
