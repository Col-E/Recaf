package software.coley.recaf.services.mapping.aggregate;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.WorkspaceBackedRemapper;
import software.coley.recaf.services.mapping.data.ClassMapping;
import software.coley.recaf.services.mapping.data.FieldMapping;
import software.coley.recaf.services.mapping.data.MemberMapping;
import software.coley.recaf.services.mapping.data.MethodMapping;
import software.coley.recaf.workspace.model.Workspace;

import java.util.*;

/**
 * Mappings implementation for internal tracking of aggregated mappings.
 * <br>
 * This will handle transitive renames such as ({@code a -> b -> c} by
 * compressing them down to their ultimate result ({@code a -> c}).
 * We will be referring to classes in these stages of naming as "a", "b", and "c" in the logic to keep this
 * example as the basis of all operations.
 * <br>
 * When this is done for every update of the mappings, the resulting mapping can be applied to the original
 * class files to achieve the same result again.
 *
 * @author Matt Coley
 * @author Marius Renner
 */
public class AggregatedMappings extends IntermediateMappings {
	private final Map<String, String> reverseOrderClassMapping = new HashMap<>();
	private final WorkspaceBackedRemapper reverseMapper;

	/**
	 * @param workspace
	 * 		Workspace associated with the aggregate mappings.
	 */
	public AggregatedMappings(@Nonnull Workspace workspace) {
		reverseMapper = new WorkspaceBackedRemapper(workspace, this) {
			@Override
			public String map(String internalName) {
				String reverseName = reverseOrderClassMapping.get(internalName);
				if (reverseName != null) {
					markModified();
					return reverseName;
				}
				return internalName;
			}
		};
	}

	/**
	 * Lookup the original name of a mapped class.
	 *
	 * @param name
	 * 		Current class name.
	 *
	 * @return Original class name.
	 * {@code null} if the class has not been mapped.
	 */
	public String getReverseClassMapping(String name) {
		return reverseOrderClassMapping.get(name);
	}

	/**
	 * @param owner
	 * 		Current class name.
	 * @param fieldName
	 * 		Current field name.
	 * @param fieldDesc
	 * 		Current field descriptor.
	 *
	 * @return Original name of the field if any mappings exist.
	 * {@code null} if the field has not been mapped.
	 */
	public String getReverseFieldMapping(String owner, String fieldName, String fieldDesc) {
		String originalOwnerName = getReverseClassMapping(owner);
		if (originalOwnerName == null)
			originalOwnerName = owner;
		List<FieldMapping> fieldMappings = fields.get(originalOwnerName);
		if (fieldMappings == null)
			return null;
		for (FieldMapping fieldMapping : fieldMappings) {
			if (!fieldName.equals(fieldMapping.getNewName()))
				continue;
			String originalDesc = reverseMapper.mapDesc(fieldDesc);
			if (originalDesc.equals(fieldMapping.getDesc()))
				return fieldMapping.getOldName();
		}
		return null;
	}

	/**
	 * @param owner
	 * 		Current class name.
	 * @param methodName
	 * 		Current method name.
	 * @param methodDesc
	 * 		Current method descriptor.
	 *
	 * @return Original name of the method if any mappings exist.
	 * {@code null} if the method has not been mapped.
	 */
	public String getReverseMethodMapping(String owner, String methodName, String methodDesc) {
		String originalOwnerName = getReverseClassMapping(owner);
		if (originalOwnerName == null)
			originalOwnerName = owner;
		List<MethodMapping> methodMappings = methods.get(originalOwnerName);
		if (methodMappings == null)
			return null;
		for (MethodMapping methodMapping : methodMappings) {
			if (!methodName.equals(methodMapping.getNewName()))
				continue;
			String originalDesc = reverseMapper.mapDesc(methodDesc);
			if (originalDesc.equals(methodMapping.getDesc()))
				return methodMapping.getOldName();
		}
		return null;
	}

	@Override
	public void addClass(String oldName, String newName) {
		super.addClass(oldName, newName);
		reverseOrderClassMapping.put(newName, oldName);
	}

	/**
	 * Clears the mapping entries.
	 */
	public void clear() {
		classes.clear();
		fields.clear();
		methods.clear();
		variables.clear();
	}

	/**
	 * Updates aggregated mappings with new values.
	 *
	 * @param newMappings
	 * 		The additional mappings that were added. They will be in the form of {@code b -> c}.
	 * 		We need to make sure we map the key type {@code b} to {@code a}.
	 *
	 * @return {@code true} when the mapping operation required bridging a current class name to its original name.
	 */
	public boolean update(Mappings newMappings) {
		// ORIGINAL:
		//  a -> b
		//  a.f1 -> b.f2
		// MAPPING:
		//  b -> c
		//  b.f2 -> c.f3
		// AGGREGATED:
		//  a -> c
		//  a.f1 -> f3
		boolean bridged;
		IntermediateMappings intermediate = newMappings.exportIntermediate();
		bridged = updateClasses(intermediate.getClasses());
		bridged |= updateMembers(intermediate.getFields().values());
		bridged |= updateMembers(intermediate.getMethods().values());
		return bridged;
	}

	private boolean updateClasses(Map<String, ClassMapping> classes) {
		boolean bridged = false;
		for (ClassMapping newMapping : classes.values()) {
			String cName = newMapping.getNewName();
			String bName = newMapping.getOldName();
			String aName = reverseOrderClassMapping.get(bName);
			if (aName != null) {
				// There is a prior entry of the class, 'aName' thus we use it as the key
				// and not 'bName' since that was the prior value the mapping for 'aName.
				bridged = true;
				addClass(aName, cName);
			} else {
				// No prior entry of the class.
				addClass(bName, cName);
			}
		}
		return bridged;
	}


	private <M extends MemberMapping> boolean updateMembers(Collection<List<M>> newMappings) {
		// With members, we need to take special care, for example:
		// 1. a --> b
		// 2. b.x --> b.y
		// Now we need to ensure the mapping "a.x --> y" exists.
		boolean bridged = false;
		for (List<M> members : newMappings) {
			for (MemberMapping newMemberMapping : members) {
				String bName = newMemberMapping.getOwnerName();
				String aName = reverseOrderClassMapping.get(bName);
				String oldMemberName = newMemberMapping.getOldName();
				String newMemberName = newMemberMapping.getNewName();
				String desc = newMemberMapping.getDesc();
				String owner = bName;
				if (aName != null) {
					// We need to map the member current mapped owner name to the
					// original owner's name.
					bridged = true;
					owner = aName;
					oldMemberName = findPriorMemberName(aName, newMemberMapping);
				}

				// Desc must always be checked for updates
				desc = applyReverseMappings(desc);

				// Add bridged entry
				if (newMemberMapping.isField()) {
					addField(owner, desc, oldMemberName, newMemberName);
				} else {
					addMethod(owner, desc, oldMemberName, newMemberName);
				}
			}
		}
		return bridged;
	}

	private String applyReverseMappings(String desc) {
		if (desc == null)
			return null;
		else if (desc.charAt(0) == '(')
			return reverseMapper.mapMethodDesc(desc);
		else
			return reverseMapper.mapDesc(desc);
	}

	private String findPriorMemberName(String oldClassName, MemberMapping memberMapping) {
		if (memberMapping.isField()) {
			return findPriorName(memberMapping, getClassFieldMappings(oldClassName));
		} else {
			return findPriorName(memberMapping, getClassMethodMappings(oldClassName));
		}
	}

	private String findPriorName(MemberMapping newMethodMapping, List<? extends MemberMapping> members) {
		// If the old name not previously mapped, then it's the same as what the new mapping has given.
		// So the passed new mapping is a safe default.
		MemberMapping target = newMethodMapping;
		String unmappedDesc = applyReverseMappings(newMethodMapping.getDesc());
		for (MemberMapping oldMethodMapping : members) {
			// The old name must be the new mapping's base name.
			// The descriptor types must also match.
			if (oldMethodMapping.getNewName().equals(newMethodMapping.getOldName()) &&
					Objects.equals(oldMethodMapping.getDesc(), unmappedDesc)) {
				target = oldMethodMapping;
				break;
			}
		}

		// Remove old mapping entry
		members.remove(target);
		return target.getOldName();
	}
}
