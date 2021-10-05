package me.coley.recaf.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Util class to work with mappings based on String operations, such as extracting portions of mapping keys or
 * folding new mappings onto an existing mapping.
 *
 * @author Marius Renner
 * @author Matt Coley
 */
public class AggregatedMappingUtils {
	private AggregatedMappingUtils() {
		// static class
	}

	/**
	 * Extracts the internal class name portion from a mapping key.
	 *
	 * @param key
	 * 		Mapping key to extract the internal class name from.
	 *
	 * @return Internal name of class of the given mapping key or {@code null} if this is not applicable.
	 */
	public static String getClassNameFromKey(String key) {
		int splitIndex = key.indexOf('\t');
		if (splitIndex == -1) {
			// This is a class, just return the original mapping key
			return key;
		}
		return key.substring(0, splitIndex);
	}

	/**
	 * Extracts the internal class name portion from a mapping key.
	 *
	 * @param key
	 * 		Mapping key to extract the internal class name from.
	 *
	 * @return Internal name of class of the given mapping key or {@code null} if this is not applicable.
	 */
	public static String getClassNameFromKeyEdgeCases(String key) {
		// Don't map constructors/static-initializers
		if (key.contains("<"))
			return null;
		int splitIndex = key.indexOf('\t');
		if (splitIndex == -1) {
			// This is a class, just return the original mapping key
			return key;
		}
		// Check if the key indicates an invoke-dynamic call
		// Don't return anything for invokdynamic
		boolean isInvokeDynamic = key.charAt(0) == '.';
		if (isInvokeDynamic)
			return null;
		return key.substring(0, splitIndex);
	}

	/**
	 * Applies a new mapping to an existing mapping.
	 * This will handle transitive renames ({@code a -> b -> c}
	 * by compressing them down to their ultimate result ({@code a -> c}).
	 * When this method is used for every update of the mappings, the resulting mapping can be applied to the original
	 * class files to achieve the same result again.
	 *
	 * <br>Note that the existing mapping is modified by this method!
	 *
	 * @param existing
	 * 		Existing mapping to be updated with the additional mappings.
	 * @param additional
	 * 		Additional mappings to update the original mapping with.
	 */
	public static void applyMappingToExisting(Map<String, String> existing, Map<String, String> additional) {
		Map<String, String> keyToKeyMapping = transformMappingValuesToKeyFormat(existing);
		Multimap<String, String> inverseMapping =
				Multimaps.invertFrom(Multimaps.forMap(keyToKeyMapping), ArrayListMultimap.create());

		Map<String, String> preimageAwareUpdates = new HashMap<>();
		for (Map.Entry<String, String> entry : additional.entrySet()) {
			String key = entry.getKey();
			final String value = entry.getValue();
			entry = null; // don't use the original entry anymore as we might modify the key

			boolean isMember = key.contains(".");
			if (isMember) {
				/*
				With members we need to take special care:
				The user might have renamed com/example/MyClass to com/example/MyAwesomeClass before and now renamed
				com/example/MyAwesomeClass.MY_CONSTANT to com/example/MyAwesomeClass.MY_AWESOME_CONSTANT.
				In this case we want to create the mapping "com/example/MyClass.MY_CONSTANT MY_AWESOME_CONSTANT".
                */
				String className = getClassNameFromKeyEdgeCases(key);
				Collection<String> classPreimages = inverseMapping.get(className);
				if (classPreimages.size() > 1) {
					throw new IllegalStateException("Reverse mapping of class" + className + " while reverse mapping "
							+ key + " gave more than 1 result: " + String.join(", " + classPreimages));
				}
				// If we have a preimage for the class, apply the mapping to that preimage class name
				if (classPreimages.size() == 1) {
					String classPreimage = classPreimages.iterator().next();
					String memberName = key.substring(key.indexOf('.') + 1);
					key = classPreimage + "." + memberName;
				}
			}

			// Check if this class/member has been mapped before and transform the mapping accordingly
			Collection<String> preimages = inverseMapping.get(key);
			if (preimages.size() > 1) {
				throw new IllegalStateException("Reverse mapping of " + key
						+ " gave more than 1 result: " + String.join(", " + preimages));
			}
			if (preimages.size() == 1) {
				preimageAwareUpdates.put(preimages.iterator().next(), value);
			} else {
				preimageAwareUpdates.put(key, value);
			}
		}

		existing.putAll(preimageAwareUpdates);
	}

	/**
	 * Transforms a given mapping to a mapping where the values are in the key format.
	 * This allows using the new value as keys for another transformation step.
	 *
	 * <p>For example the mapping {@code calc/MyCalculator.MAX_DEPTH -> MAX_DEPTH_LEVEL} will be transformed to
	 * {@code calc/MyCalculator.MAX_DEPTH -> calc/Calculator.MAX_DEPTH_LEVEL}
	 *
	 * @param mapping
	 * 		Mapping to transform.
	 *
	 * @return Transformed mapping where the value would be a valid key for another transformation step.
	 */
	public static Map<String, String> transformMappingValuesToKeyFormat(Map<String, String> mapping) {
		return mapping.entrySet().stream()
				.map(AggregatedMappingUtils::transformSingleMappingToKeyFormat)
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static Map.Entry<String, String> transformSingleMappingToKeyFormat(Map.Entry<String, String> mapping) {
		// Heavily inspired by SimpleRecordingRemapper.map()
		String key = mapping.getKey();
		String value = mapping.getValue();

		// Don't map constructors/static-initializers
		if (key.contains("<"))
			return null;

		boolean isMember = key.contains("\t");
		if (!isMember) {
			// This is a class, just return the original mapping as its value is the applied value
			return mapping;
		}

		// Don't map invokedynamic calls
		boolean isInvokeDynamic = key.charAt(0) == '.';
		if (isInvokeDynamic)
			return null;

		String[] split = key.split("\t");
		String className = split[0];
		if (split.length < 3) {
			// Field mapping without type
			String newName = className + "\t" + value;
			return new AbstractMap.SimpleEntry<>(key, newName);
		}
		// Field or method mapping
		String descriptor = split[split.length - 1];
		String newName = className + "\t" + value + "\t" + descriptor;
		return new AbstractMap.SimpleEntry<>(key, newName);
	}
}
