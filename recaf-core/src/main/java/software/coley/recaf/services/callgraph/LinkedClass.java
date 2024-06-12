package software.coley.recaf.services.callgraph;

import dev.xdark.jlinker.ClassInfo;
import dev.xdark.jlinker.MemberInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.MemoizedFunctions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * JLinker wrapper of {@link JvmClassInfo}.
 *
 * @author Matt Coley
 */
public class LinkedClass implements ClassInfo<JvmClassInfo> {
	private static final DebuggingLogger logger = Logging.get(LinkedClass.class);
	private final JvmClassInfo info;
	private final Function<String, LinkedClass> superClassLookup;
	private final Function<List<String>, List<ClassInfo<JvmClassInfo>>> interfacesLookup;
	private final BiFunction<String, String, MemberInfo<FieldMember>> fieldLookup;
	private final BiFunction<String, String, MemberInfo<MethodMember>> methodLookup;

	public LinkedClass(@Nonnull ClassLookup lookup, @Nonnull JvmClassInfo info) {
		this.info = info;

		superClassLookup = MemoizedFunctions.memoize((String superName) -> {
			JvmClassInfo superClass = lookup.apply(superName);
			return new LinkedClass(lookup, superClass);
		});

		interfacesLookup = MemoizedFunctions.memoize((List<String> interfaces) -> {
			if (interfaces.isEmpty())
				return Collections.emptyList();
			List<ClassInfo<JvmClassInfo>> values = new ArrayList<>();
			for (String itf : interfaces) {
				JvmClassInfo itfInfo = lookup.apply(itf);
				if (itfInfo == null)
					logger.debugging(l -> l.warn("Lookup failed for interface: {}", itf));
				else
					values.add(new LinkedClass(lookup, itfInfo));
			}
			return values;
		});

		fieldLookup = MemoizedFunctions.memoize((name, descriptor) -> {
			FieldMember declaredField = info.getDeclaredField(name, descriptor);
			if (declaredField == null) {
				logger.debugging(l -> l.warn("Missing declared field: {} {}", descriptor, name));
				return null;
			}

			return new MemberInfo<>() {
				@Override
				public FieldMember innerValue() {
					return declaredField;
				}

				@Override
				public int accessFlags() {
					return declaredField.getAccess();
				}

				@Override
				public boolean isPolymorphic() {
					return false;
				}
			};
		});

		methodLookup = MemoizedFunctions.memoize((name, descriptor) -> {
			MethodMember declaredMethod = info.getDeclaredMethod(name, descriptor);
			if (declaredMethod == null) {
				logger.debugging(l -> l.warn("Missing declared method: {}{}", name, descriptor));
				return null;
			}
			return new MemberInfo<>() {
				@Override
				public MethodMember innerValue() {
					return declaredMethod;
				}

				@Override
				public int accessFlags() {
					return declaredMethod.getAccess();
				}

				@Override
				public boolean isPolymorphic() {
					return false;
				}
			};
		});
	}

	@Override
	public JvmClassInfo innerValue() {
		return info;
	}

	@Override
	public int accessFlags() {
		return info.getAccess();
	}

	@Nullable
	@Override
	public ClassInfo<JvmClassInfo> superClass() {
		String superName = info.getSuperName();
		if (superName == null) return null;
		return superClassLookup.apply(superName);
	}

	@Nonnull
	@Override
	public List<ClassInfo<JvmClassInfo>> interfaces() {
		return interfacesLookup.apply(info.getInterfaces());
	}

	@Override
	public MemberInfo<MethodMember> getMethod(String name, String descriptor) {
		return methodLookup.apply(name, descriptor);
	}

	@Override
	public MemberInfo<FieldMember> getField(String name, String descriptor) {
		return fieldLookup.apply(name, descriptor);
	}
}