package me.xdark.recaf.jvm.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MemberTable<V> {
	private final Map<MemberDescriptor, V> table;

	MemberTable(List<V> list, Function<V, String> name, Function<V, String> desc) {
		int size = list.size();
		Map<MemberDescriptor, V> table = this.table = new HashMap<>(size);
		while (size-- > 0) {
			V member = list.get(size);
			MemberDescriptor descriptor = new MemberDescriptor(name.apply(member), desc.apply(member));
			table.put(descriptor, member);
		}
	}

	public V findMember(MemberDescriptor desc) {
		return this.table.get(desc);
	}
}
