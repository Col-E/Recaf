package software.coley.recaf.services.deobfuscation.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.analysis.ReAnalyzer;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A transformer that collects values of {@code static final} field assignments.
 *
 * @author Matt Coley
 */
@Dependent
public class StaticValueCollectionTransformer implements JvmClassTransformer {
	private final Map<String, StaticValues> classValues = new ConcurrentHashMap<>();
	private final Map<String, EffectivelyFinalFields> classFinals = new ConcurrentHashMap<>();
	private final InheritanceGraphService graphService;
	private final WorkspaceManager workspaceManager;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public StaticValueCollectionTransformer(@Nonnull WorkspaceManager workspaceManager, @Nonnull InheritanceGraphService graphService) {
		this.workspaceManager = workspaceManager;
		this.graphService = graphService;
	}

	@Nullable
	public ReValue getStaticValue(@Nonnull String className, @Nonnull String fieldName, @Nonnull String fieldDesc) {
		StaticValues values = classValues.get(className);
		if (values == null)
			return null;
		return values.get(fieldName, fieldDesc);
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = workspace == workspaceManager.getCurrent() ?
				graphService.getCurrentWorkspaceInheritanceGraph() :
				graphService.newInheritanceGraph(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo classInfo) throws TransformationException {
		StaticValues valuesContainer = new StaticValues();
		EffectivelyFinalFields finalContainer = new EffectivelyFinalFields();

		// TODO: Make some config options for this
		//  - Option to make unsafe assumptions
		//    - treat all effectively final candidates as actually final
		//  - Option to scan other classes for references to our fields to have more thorough 'effective-final' checking
		//    - will be slower, but it will be opt-in and off by default

		// Populate initial values based on field's default value attribute
		for (FieldMember field : classInfo.getFields()) {
			if (!field.hasStaticModifier())
				continue;

			// Add to effectively-final container if it is 'static final'
			// If the field is private add it to the "maybe" effectively-final list, and we'll confirm it later
			if (field.hasFinalModifier())
				finalContainer.add(field.getName(), field.getDescriptor());
			else if (field.hasPrivateModifier())
				// We can only assume private fields are effectively-final if nothing outside the <clinit> writes to them.
				// Any other level of access can be written to by child classes or classes in the same package.
				finalContainer.addMaybe(field.getName(), field.getDescriptor());

			// Skip if there is no default value
			Object defaultValue = field.getDefaultValue();
			if (defaultValue == null)
				continue;

			// Skip if the value cannot be mapped to our representation
			ReValue mappedValue = extractFromAsmConstant(defaultValue);
			if (mappedValue == null)
				continue;

			// Store the value
			valuesContainer.put(field.getName(), field.getDescriptor(), mappedValue);
		}

		// Visit <clinit> of classes and collect static field values of primitives
		String className = classInfo.getName();
		if (classInfo.getDeclaredMethod("<clinit>", "()V") != null) {
			ClassNode node = context.getNode(bundle, classInfo);

			// Find the static initializer and determine which fields are "effectively-final"
			MethodNode clinit = null;
			for (MethodNode method : node.methods) {
				if ((method.access & Opcodes.ACC_STATIC) != 0 && method.name.equals("<clinit>") && method.desc.equals("()V")) {
					clinit = method;
				} else if (method.instructions != null) {
					// Any put-static to a field in our class means it is not effectively-final because the method is not the static initializer
					for (AbstractInsnNode instruction : method.instructions) {
						if (instruction.getOpcode() == Opcodes.PUTSTATIC && instruction instanceof FieldInsnNode fieldInsn) {
							// Skip if not targeting our class
							if (!fieldInsn.owner.equals(className))
								continue;
							String fieldName = fieldInsn.name;
							String fieldDesc = fieldInsn.desc;
							finalContainer.removeMaybe(fieldName, fieldDesc);
						}
					}
				}
			}
			finalContainer.commitMaybeIntoEffectivelyFinals();

			// Only analyze if we see static setters
			if (clinit != null && hasStaticSetters(clinit)) {
				ReInterpreter interpreter = new ReInterpreter(inheritanceGraph);
				ReAnalyzer analyzer = new ReAnalyzer(interpreter);
				try {
					Frame<ReValue>[] frames = analyzer.analyze(className, clinit);
					AbstractInsnNode[] instructions = clinit.instructions.toArray();
					for (int i = 0; i < instructions.length; i++) {
						AbstractInsnNode instruction = instructions[i];
						if (instruction.getOpcode() == Opcodes.PUTSTATIC && instruction instanceof FieldInsnNode fieldInsn) {
							// Skip if not targeting our class
							if (!fieldInsn.owner.equals(className))
								continue;

							// Skip if the field is not final, or effectively final
							String fieldName = fieldInsn.name;
							String fieldDesc = fieldInsn.desc;
							if (!finalContainer.contains(fieldName, fieldDesc))
								continue;

							// Merge the static value state
							Frame<ReValue> frame = frames[i];
							ReValue existingValue = valuesContainer.get(fieldName, fieldDesc);
							ReValue stackValue = frame.getStack(frame.getStackSize() - 1);
							ReValue merged = existingValue == null ? stackValue : interpreter.merge(existingValue, stackValue);
							valuesContainer.put(fieldName, fieldDesc, merged);
						}
					}
				} catch (Throwable t) {
					throw new TransformationException("Analysis failure", t);
				}
			}
		}

		// Record the values for the target class if we recorded at least one value
		if (!valuesContainer.staticFieldValues.isEmpty())
			classValues.put(className, valuesContainer);
	}

	@Nonnull
	@Override
	public String name() {
		return "Static value collection";
	}

	/**
	 * @param method
	 * 		Method to check for {@link Opcodes#PUTSTATIC} use.
	 *
	 * @return {@code true} when the method has a {@link Opcodes#PUTSTATIC} instruction.
	 */
	private static boolean hasStaticSetters(@Nonnull MethodNode method) {
		if (method.instructions == null)
			return false;
		for (AbstractInsnNode abstractInsnNode : method.instructions)
			if (abstractInsnNode.getOpcode() == Opcodes.PUTSTATIC) return true;
		return false;
	}

	/**
	 * @param value
	 * 		ASM constant value.
	 *
	 * @return A {@link ReValue} wrapper of the given input,
	 * or {@code null} if the value could not be represented.
	 *
	 * @see LdcInsnNode#cst Possible values
	 */
	@Nullable
	private static ReValue extractFromAsmConstant(Object value) {
		if (value instanceof String s)
			return ObjectValue.string(s);
		if (value instanceof Integer i)
			return IntValue.of(i);
		if (value instanceof Float f)
			return FloatValue.of(f);
		if (value instanceof Long l)
			return LongValue.of(l);
		if (value instanceof Double d)
			return DoubleValue.of(d);
		if (value instanceof Type type) {
			if (type.getSort() == Type.METHOD)
				return ObjectValue.VAL_METHOD_TYPE;
			else
				return ObjectValue.VAL_CLASS;
		}
		if (value instanceof Handle handle)
			return ObjectValue.VAL_METHOD_HANDLE;
		return null;
	}

	/**
	 * Wrapper/utility for field finality storage/lookups.
	 */
	private static class EffectivelyFinalFields {
		private Set<String> finalFieldKeys;
		private Set<String> maybeFinalFieldKeys;

		/**
		 * Add a {@code static final} field.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void add(@Nonnull String name, @Nonnull String desc) {
			if (finalFieldKeys == null)
				finalFieldKeys = new HashSet<>();
			finalFieldKeys.add(key(name, desc));
		}

		/**
		 * Add a {@code static} field that <i>may be</i> effectively final.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void addMaybe(@Nonnull String name, @Nonnull String desc) {
			if (maybeFinalFieldKeys == null)
				maybeFinalFieldKeys = new HashSet<>();
			maybeFinalFieldKeys.add(key(name, desc));
		}

		/**
		 * Remove a field from being considered possibly effectively final.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void removeMaybe(@Nonnull String name, @Nonnull String desc) {
			if (maybeFinalFieldKeys != null)
				maybeFinalFieldKeys.remove(key(name, desc));
		}

		/**
		 * Commit all possible effectively final fields into the final fields set.
		 */
		public void commitMaybeIntoEffectivelyFinals() {
			if (maybeFinalFieldKeys != null)
				if (finalFieldKeys == null)
					finalFieldKeys = new HashSet<>(maybeFinalFieldKeys);
				else
					finalFieldKeys.addAll(maybeFinalFieldKeys);
		}

		/**
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 *
		 * @return {@code true} when the field is {@code final} or effectively {@code final}.
		 */
		public boolean contains(@Nonnull String name, @Nonnull String desc) {
			if (finalFieldKeys == null)
				return false;
			return finalFieldKeys.contains(key(name, desc));
		}

		@Nonnull
		private static String key(@Nonnull String name, @Nonnull String desc) {
			return name + " " + desc;
		}
	}

	/**
	 * Wrapper/utility for field value storage/lookups.
	 */
	private static class StaticValues {
		private final Map<String, ReValue> staticFieldValues = new ConcurrentHashMap<>();

		private void put(@Nonnull String name, @Nonnull String desc, @Nonnull ReValue value) {
			staticFieldValues.put(getKey(name, desc), value);
		}

		@Nullable
		private ReValue get(@Nonnull String name, @Nonnull String desc) {
			return staticFieldValues.get(getKey(name, desc));
		}

		@Nonnull
		private static String getKey(@Nonnull String name, @Nonnull String desc) {
			return name + ' ' + desc;
		}
	}
}
