package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Mappings} implementation that assumes all classes, fields, and methods are uniquely identified.
 * Mapping outputs are keyed to these unique identifiers, rather than a full {@code name + desc} pair.
 * <p/>
 * This mappings implementation can be useful to pass to {@link MappingApplier} for some scenarios like
 * Minecraft mappings, where each class, field, and method are uniquely identified. Minecraft's Forge, Fabric-Yarn and
 * Mod-Coder-Pack all have <i>"intermediate"</i> names that follow this pattern.
 * <p/>
 * Example use case:
 * <pre>{@code
 * MappingApplier applier = ...;
 * EnigmaMappings enigma = ...;
 *
 * // Read minecraft yarn mappings from a directory
 * IntermediateMappings mappings = enigma.parse(Paths.get("fabric/yarn/1.20.1"));
 *
 * // Convert to unique-keyed mappings
 * UniqueKeyMappings unique = new UniqueKeyMappings(mappings);
 *
 * // Use the unique-keyed mappings with a mapping-applier on a fabric mod
 * applier.applyToPrimaryResource(unique);
 * }</pre>
 *
 * @author Matt Coley
 */
public class UniqueKeyMappings implements Mappings {
	private final Map<String, String> mappings = new HashMap<>();
	private final IntermediateMappings backing;

	/**
	 * @param backing
	 * 		Backing intermediate mappings to adapt into unique keyed mappings.
	 */
	public UniqueKeyMappings(@Nonnull IntermediateMappings backing) {
		// We need to explicitly keep the input mappings for 'exportIntermediate()' support.
		this.backing = backing;

		populateLookup();
	}

	/**
	 * Copies all values from the backing mappings into this instance.
	 */
	private void populateLookup() {
		backing.classes.values()
				.forEach(mapping -> mappings.put(mapping.getOldName(), mapping.getNewName()));
		backing.fields.values().stream()
				.flatMap(Collection::stream)
				.forEach(mapping -> mappings.put(mapping.getOldName(), mapping.getNewName()));
		backing.methods.values().stream()
				.flatMap(Collection::stream)
				.forEach(mapping -> mappings.put(mapping.getOldName(), mapping.getNewName()));
		backing.variables.values().stream()
				.flatMap(Collection::stream)
				.forEach(mapping -> {
					if (mapping.getOldName() != null)
						mappings.put(mapping.getOldName(), mapping.getNewName());
					else
						mappings.put(mapping.getMethodName() + "." + mapping.getIndex(), mapping.getNewName());
				});
	}

	@Nullable
	@Override
	public String getMappedClassName(@Nonnull String internalName) {
		return mappings.get(internalName);
	}

	@Nullable
	@Override
	public String getMappedFieldName(@Nonnull String ownerName, @Nonnull String fieldName, @Nonnull String fieldDesc) {
		return mappings.get(fieldName);
	}

	@Nullable
	@Override
	public String getMappedMethodName(@Nonnull String ownerName, @Nonnull String methodName, @Nonnull String methodDesc) {
		return mappings.get(methodName);
	}

	@Nullable
	@Override
	public String getMappedVariableName(@Nonnull String className, @Nonnull String methodName, @Nonnull String methodDesc,
	                                    @Nullable String name, @Nullable String desc, int index) {
		String mapped = null;
		if (name != null)
			mapped = mappings.get(name);
		if (mapped == null)
			mapped = mappings.get(methodName + "." + index);
		return mapped;
	}

	@Nonnull
	@Override
	public IntermediateMappings exportIntermediate() {
		// Yield the backing intermediate mappings.
		return backing;
	}
}
