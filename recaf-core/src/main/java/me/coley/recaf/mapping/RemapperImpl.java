package me.coley.recaf.mapping;

import org.objectweb.asm.commons.Remapper;

/**
 * {@link Remapper} implementation that delegates to a provided {@link Mappings} and supports local variable renaming.
 *
 * @author Matt Coley
 */
public class RemapperImpl extends Remapper {
	private final Mappings mappings;
	private boolean modified;

	/**
	 * @param mappings
	 * 		Mappings wrapper to pull values from.
	 */
	public RemapperImpl(Mappings mappings) {
		this.mappings = mappings;
	}

	@Override
	public String map(String internalName) {
		String mapped = mappings.getMappedClassName(internalName);
		if (mapped != null) {
			markModified();
			return mapped;
		}
		return super.map(internalName);
	}

	@Override
	public String mapType(String internalName) {
		// Check for array type
		if (internalName.charAt(0) == '[')
			return mapDesc(internalName);
		// Standard internal name
		return map(internalName);
	}

	@Override
	public String mapFieldName(String owner, String name, String descriptor) {
		String mapped = mappings.getMappedFieldName(owner, name, descriptor);
		if (mapped != null) {
			markModified();
			return mapped;
		}
		return super.mapFieldName(owner, name, descriptor);
	}

	@Override
	public String mapMethodName(String owner, String name, String descriptor) {
		String mapped = mappings.getMappedMethodName(owner, name, descriptor);
		if (mapped != null) {
			markModified();
			return mapped;
		}
		return super.mapMethodName(owner, name, descriptor);
	}

	@Override
	public String mapMethodDesc(String methodDescriptor) {
		int lastTypeEndOffset = methodDescriptor.indexOf(';');
		if (lastTypeEndOffset == -1) {
			// No object typees to map
			return methodDescriptor;
		}
		int lastTypeStartOffset = 0;
		StringBuilder builder = new StringBuilder(methodDescriptor.length());
		int tail;
		do {
			int bookkeep = lastTypeStartOffset;
			lastTypeStartOffset = methodDescriptor.indexOf('L', lastTypeStartOffset);
			// Append leftover parts on the left side
			builder.append(methodDescriptor, bookkeep, lastTypeStartOffset);
			String type = methodDescriptor.substring(lastTypeStartOffset + 1, lastTypeEndOffset);
			String mapped = mapType(type);
			builder.append('L').append(mapped).append(';');
			// Skip L_TYPE_;
			lastTypeStartOffset += type.length() + 2;
			tail = lastTypeStartOffset;
		} while ((lastTypeEndOffset = methodDescriptor.indexOf(';', lastTypeEndOffset + 1)) != -1);
		// Append remaining characters (tail onwards)
		builder.append(methodDescriptor, tail, methodDescriptor.length());
		return builder.toString();
	}

	@Override
	public String mapDesc(String descriptor) {
		if(descriptor.isEmpty()) return descriptor;
		if (descriptor.charAt(0) == '(') {
			return mapMethodDesc(descriptor);
		}
		if (descriptor.charAt(descriptor.length() - 1) != ';') {
			return descriptor;
		}
		int dimensions = 0;
		while (descriptor.charAt(dimensions) == '[') {
			dimensions++;
		}
		StringBuilder builder = new StringBuilder(descriptor.length());
		int bookkeep = dimensions;
		while (dimensions-- != 0) {
			builder.append('[');
		}
		builder.append('L');
		builder.append(map(descriptor.substring(bookkeep + 1, descriptor.length() - 1)));
		builder.append(';');
		return builder.toString();
	}

	@Override
	public String mapRecordComponentName(String owner, String name, String descriptor) {
		return mapMethodName(owner, name, descriptor);
	}

	@Override
	public String mapPackageName(String name) {
		// Used only by module attributes
		return super.mapPackageName(name);
	}

	@Override
	public String mapModuleName(String name) {
		// Used only by module attributes
		return super.mapModuleName(name);
	}

	protected void markModified() {
		modified = true;
	}

	/**
	 * @param className
	 * 		Internal name of the class defining the method the variable resides in.
	 * @param methodName
	 * 		Name of the method.
	 * @param methodDesc
	 * 		Descriptor of the method.
	 * @param name
	 * 		Name of the variable.
	 * @param desc
	 * 		Descriptor of the variable.
	 * @param index
	 * 		Index of the variable.
	 *
	 * @return Mapped name of the variable, or the existing name if no mapping exists.
	 */
	public String mapVariableName(String className, String methodName, String methodDesc,
								  String name, String desc, int index) {
		String mapped = mappings.getMappedVariableName(className, methodName, methodDesc, name, desc, index);
		if (mapped != null) {
			markModified();
			return mapped;
		}
		// Use existing variable name.
		return name;
	}

	/**
	 * @return {@code true} when any mapping has been found and used.
	 */
	public boolean hasMappingBeenApplied() {
		return modified;
	}
}
