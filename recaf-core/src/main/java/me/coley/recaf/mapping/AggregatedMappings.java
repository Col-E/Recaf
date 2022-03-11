package me.coley.recaf.mapping;

import me.coley.recaf.mapping.data.ClassMapping;
import me.coley.recaf.mapping.data.MemberMapping;
import me.coley.recaf.mapping.impl.IntermediateMappings;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private final RemapperImpl remapper = new RemapperImpl(this) {
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


	@Override
	public void addClass(String oldName, String newName) {
		super.addClass(oldName, newName);
		reverseOrderClassMapping.put(newName, oldName);
	}

	@Override
	public String implementationName() {
		return "AGGREGATED";
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
	 * @return {@code true} if changes were made.
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
		boolean updated;
		IntermediateMappings intermediate = newMappings.exportIntermediate();
		updated = updateClasses(intermediate.getClasses());
		updated |= updateMembers(intermediate.getFields().values());
		updated |= updateMembers(intermediate.getMethods().values());
		return updated;
	}

	private boolean updateClasses(Map<String, ClassMapping> classes) {
		boolean updated = false;
		for (ClassMapping newMapping : classes.values()) {
			String cName = newMapping.getNewName();
			String bName = newMapping.getOldName();
			String aName = reverseOrderClassMapping.get(bName);
			if (aName != null) {
				updated = true;
				addClass(aName, cName);
			} else {
				addClass(bName, cName);
			}
		}
		return updated;
	}


	private <M extends MemberMapping> boolean updateMembers(Collection<List<M>> x) {
		// With members, we need to take special care, for example:
		// 1. a --> b
		// 2. b.x --> b.y
		// Now we need to ensure the mapping "a.x --> y" exists.
		boolean updated = false;
		for (List<M> members : x) {
			for (MemberMapping newMemberMapping : members) {
				String bName = newMemberMapping.getOwnerName();
				String aName = reverseOrderClassMapping.get(bName);
				String oldMemberName = newMemberMapping.getOldName();
				String newMemberName = newMemberMapping.getNewName();
				String desc = newMemberMapping.getDesc();
				String owner = bName;
				if (aName != null) {
					updated = true;
					owner = aName;
					oldMemberName = findPriorMemberName(aName, newMemberMapping);
				}
				// Desc must always be checked for updates
				desc = updateDesc(desc);
				// Add updated entry
				if (newMemberMapping.isField()) {
					addField(owner, desc, oldMemberName, newMemberName);
				} else {
					addMethod(owner, desc, oldMemberName, newMemberName);
				}
			}
		}
		return updated;
	}

	private String updateDesc(String desc) {
		if (desc == null)
			return null;
		else if (desc.charAt(0) == '(')
			return remapper.mapMethodDesc(desc);
		else
			return remapper.mapDesc(desc);
	}

	private String findPriorMemberName(String oldClassName, MemberMapping newFieldMapping) {
		if (newFieldMapping.isField()) {
			return findPriorName(newFieldMapping, getClassFieldMappings(oldClassName));
		} else {
			return findPriorName(newFieldMapping, getClassFieldMappings(oldClassName));
		}
	}

	private String findPriorName(MemberMapping newMethodMapping, List<? extends MemberMapping> members) {
		// If the old name not previously mapped, then it's the same as what the new mapping has given.
		// So the passed new mapping is a safe default.
		MemberMapping target = newMethodMapping;
		for (MemberMapping oldMethodMapping : members) {
			// The old name must be the new mapping's base name.
			// The descriptor types must also match.
			if (oldMethodMapping.getNewName().equals(newMethodMapping.getOldName()) &&
					oldMethodMapping.getDesc().equals(newMethodMapping.getDesc())) {
				target = oldMethodMapping;
				break;
			}
		}
		// Remove old mapping entry
		members.remove(target);
		return target.getOldName();
	}
}
