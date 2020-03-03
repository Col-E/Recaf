package me.coley.recaf.search;

import java.util.function.IntSupplier;

/**
 * Query to find member references matching the given information.
 *
 * @author Matt
 */
public class MemberReferenceQuery extends Query {
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * Constructs a member references query.
	 *
	 * @param owner
	 * 		Name of class containing the member. May be {@code null} to match members of any class.
	 * @param name
	 * 		Member name. May be {@code null} to match members of any name.
	 * @param desc
	 * 		Member descriptor. May be {@code null} to match members of any type.
	 * @param stringMode
	 * 		How to match strings.
	 */
	public MemberReferenceQuery(String owner, String name, String desc, StringMatchMode
			stringMode) {
		super(QueryType.MEMBER_REFERENCE, stringMode);
		if(owner == null && name == null && desc == null) {
			throw new IllegalArgumentException("At least one query parameter must be non-null!");
		}
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	/**
	 * Adds a result if the given member matches the specified member.
	 *
	 * @param access
	 * 		Member modifers.
	 * @param owner
	 * 		Name of class containing the member.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	public void match(IntSupplier access, String owner, String name, String desc) {
		boolean hasOwner = this.owner == null || stringMode.match(this.owner, owner);
		boolean hasName = this.name == null || stringMode.match(this.name, name);
		boolean hasDesc = this.desc == null || stringMode.match(this.desc, desc);
		if(hasOwner && hasName && hasDesc) {
			getMatched().add(new MemberResult(access.getAsInt(), owner, name, desc));
		}
	}
}
