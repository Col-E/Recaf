package me.coley.recaf.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import me.coley.recaf.util.CollectionUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Util class to work with ASM mappings based on String operations, such as extracting portions of ASM mapping keys or
 * folding new mappings onto an existing mapping.
 *
 * @author Marius Renner
 */
public class AsmMappingUtils {
    private AsmMappingUtils() {
        // static class
    }

    /**
     * Extracts the fully qualified class name portion from a mapping key in ASM format.
     * (See {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}) for format information.
     *
     * @param asmKey ASM mapping key to extract the fully qualified class name from.
     * @return Fully qualified class name of the given ASM mapping key or {@code null} if this is not applicable.
     */
    public static String getClassNameFromAsmKey(String asmKey) {
        // Don't map constructors/static-initializers
        if (asmKey.contains("<"))
            return null;

        boolean isMember = asmKey.contains(".");
        if (!isMember) {
            // This is a class, just return the original mapping key
            return asmKey;
        }

        // Check if the key indicates an invoke-dynamic call
        // Don't return anything for invokdynamic
        boolean isInvokeDynamic = asmKey.charAt(0) == '.';
        if (isInvokeDynamic)
            return null;

        int dotIndex = asmKey.indexOf('.');
        return asmKey.substring(0, dotIndex);
    }

    /**
     * Applies a new mapping in ASM format (See {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)})
     * to an existing mapping in ASM format. This will handle transitive renames ({@code a -> b -> c} by compressing
     * them down to their ultimate result ({@code a -> c}).
     * When this method is used for every update of the mappings, the resulting mapping can be applied to the original
     * class files to achieve the same result again.
     *
     * <p>Note that the exiting mapping is modified by this method!
     *
     * @param existing   Existing ASM mapping to be updated with the additional mappings.
     * @param additional Additional ASM mappings to update the original mapping with.
     */
    public static void applyMappingToExisting(Map<String, String> existing, Map<String, String> additional) {
        Map<String, String> keyToKeyMapping = transformAsmMappingValuesToKeyFormat(existing);
        Multimap<String, String> inverseMapping =
                Multimaps.invertFrom(Multimaps.forMap(keyToKeyMapping), ArrayListMultimap.create());

        Map<String, String> preimageAwareUpdates = new HashMap<>();
        for (Map.Entry<String, String> entry : additional.entrySet()) {
            String key = entry.getKey();
            final String value = entry.getValue();
            entry = null; // don't use the original entry anymore as we might modify the key

            boolean isMember = key.contains(".");
            if (isMember) {
                /* With members we need to take special care:
                   The user might have renamed com/example/MyClass to com/example/MyAwesomeClass before and now renamed
                   com/example/MyAwesomeClass.MY_CONSTANT to com/example/MyAwesomeClass.MY_AWESOME_CONSTANT.
                   In this case we want to create the mapping "com/example/MyClass.MY_CONSTANT MY_AWESOME_CONSTANT". */
                String className = getClassNameFromAsmKey(key);
                Collection<String> classPreimages = inverseMapping.get(className);
                if (classPreimages.size() > 1) {
                    throw new IllegalStateException("Reverse mapping of class" + className + " while reverse mapping "
                            + key + " gave more than 1 result: " + String.join(", " + classPreimages));
                }
                // if we have a preimage for the class, apply the mapping to that preimage class name
                // otherwise use given name
                String targetClassName = classPreimages.isEmpty() ? className : classPreimages.iterator().next();
                String memberInfo = key.substring(key.indexOf('.') + 1);
                if (memberInfo.contains(" ")) {
                    int x = memberInfo.indexOf(" ");
                    String fieldName = memberInfo.substring(0, x);
                    String fieldDesc = memberInfo.substring(x + 1);
                    key = targetClassName + "." + fieldName + " " + mapDesc(existing, fieldDesc);
                } else if (memberInfo.contains("(")) {
                    int x = memberInfo.indexOf("(");
                    String methodName = memberInfo.substring(0, x);
                    String methodDesc = memberInfo.substring(x);
                    key = targetClassName + "." + methodName + mapDesc(existing, methodDesc);
                } else {
                    key = targetClassName + "." + memberInfo;
                }
            }

            // check if this class/member has been mapped before and transform the mapping accordingly
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
     * @param existing
     * 		Current aggregate mappings.
     * @param desc
     * 		Descriptor to map.
     *
     * @return Mapped descriptor.
     */
    private static String mapDesc(Map<String, String> existing, String desc) {
        SimpleRecordingRemapper remapper = new SimpleRecordingRemapper(
                CollectionUtil.invert(existing), false, false, false, null);
        return desc.charAt(0) == '(' ?
                remapper.mapMethodDesc(desc) :
                remapper.mapDesc(desc);
    }

    /**
     * Transforms a given mapping in ASM format (See
     * {@link org.objectweb.asm.commons.SimpleRemapper#SimpleRemapper(Map)}) to a mapping where the values are in the
     * key format of ASM mappings. This allows using the new value as keys for another transformation step.
     *
     * <p>For example the mapping {@code calc/MyCalculator.MAX_DEPTH -> MAX_DEPTH_LEVEL} will be transformed to
     * {@code calc/MyCalculator.MAX_DEPTH -> calc/Calculator.MAX_DEPTH_LEVEL}
     *
     * @param mapping ASM mapping to transform.
     * @return Transformed mapping where the value would be a valid key for another ASM transformation step.
     */
    public static Map<String, String> transformAsmMappingValuesToKeyFormat(Map<String, String> mapping) {
        return mapping.entrySet().stream()
                .map(AsmMappingUtils::transformSingleAsmMappingToKeyFormat)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map.Entry<String, String> transformSingleAsmMappingToKeyFormat(Map.Entry<String, String> mapping) {
        // Heavily inspired by SimpleRecordingRemapper.map()
        String key = mapping.getKey();
        String value = mapping.getValue();

        // Don't map constructors/static-initializers
        if (key.contains("<"))
            return null;

        boolean isMember = key.contains(".");
        if (!isMember) {
            // This is a class, just return the original mapping as its value is the applied value
            return mapping;
        }

        // Don't map invokedynamic calls
        boolean isInvokeDynamic = key.charAt(0) == '.';
        if (isInvokeDynamic)
            return null;

        int braceIndex = key.indexOf('(');
        boolean isMethod = braceIndex != -1;
        int dotIndex = key.indexOf('.');
        String className = key.substring(0, dotIndex);
        if (!isMethod) {
            String newName = className + "." + value;
            return new AbstractMap.SimpleEntry<>(key, newName);
        }

        String descriptor = key.substring(braceIndex);
        String newName = className + "." + value + descriptor;
        return new AbstractMap.SimpleEntry<>(key, newName);
    }
}
