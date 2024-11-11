package software.coley.recaf.services.deobfuscation.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.Set;

/**
 * A transformer that inlines values from {@link StaticValueCollectionTransformer}.
 *
 * @author Matt Coley
 */
@Dependent
public class StaticValueInliningTransformer implements JvmClassTransformer {
	@Override
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo classInfo) throws TransformationException {
		var staticValueCollector = context.getJvmTransformer(StaticValueCollectionTransformer.class);

		boolean dirty = false;
		ClassNode node = context.getNode(bundle, classInfo);
		for (MethodNode method : node.methods) {
			// Skip static initializer and abstract methods
			if (method.name.contains("<clinit>") || method.instructions == null)
				continue;

			for (AbstractInsnNode instruction : method.instructions) {
				if (instruction instanceof FieldInsnNode fieldInsn) {
					// Get the known value of the static field
					ReValue value = staticValueCollector.getStaticValue(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
					if (value == null)
						continue;

					// Replace static get calls with their known values
					if (value.hasKnownValue()) {
						switch (value) {
							case IntValue intValue -> {
								method.instructions.set(instruction, AsmInsnUtil.intToInsn(intValue.value().getAsInt()));
								dirty = true;
							}
							case LongValue longValue -> {
								method.instructions.set(instruction, AsmInsnUtil.longToInsn(longValue.value().getAsLong()));
								dirty = true;
							}
							case DoubleValue doubleValue -> {
								method.instructions.set(instruction, AsmInsnUtil.doubleToInsn(doubleValue.value().getAsDouble()));
								dirty = true;
							}
							case FloatValue floatValue -> {
								method.instructions.set(instruction, AsmInsnUtil.floatToInsn((float) floatValue.value().getAsDouble()));
								dirty = true;
							}
							case StringValue stringValue -> {
								method.instructions.set(instruction, new LdcInsnNode(stringValue.getText().get()));
								dirty = true;
							}
							default -> {
								// no-op
							}
						}
					} else if (value == ObjectValue.VAL_OBJECT_NULL) {
						method.instructions.set(instruction, new InsnNode(Opcodes.ACONST_NULL));
						dirty = true;
					}
				}
			}
		}

		// Record transformed class if we made any changes
		if (dirty)
			context.setNode(bundle, classInfo, node);
	}

	@Nonnull
	@Override
	public String name() {
		return "Static value inlining";
	}

	@Nonnull
	@Override
	public Set<Class<? extends JvmClassTransformer>> dependencies() {
		return Collections.singleton(StaticValueCollectionTransformer.class);
	}
}
