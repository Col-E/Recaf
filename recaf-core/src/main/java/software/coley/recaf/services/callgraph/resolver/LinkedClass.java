package software.coley.recaf.services.callgraph.resolver;

import dev.xdark.jlinker.ClassModel;
import dev.xdark.jlinker.FieldDescriptor;
import dev.xdark.jlinker.FieldDescriptorString;
import dev.xdark.jlinker.FieldModel;
import dev.xdark.jlinker.MethodDescriptor;
import dev.xdark.jlinker.MethodDescriptorString;
import dev.xdark.jlinker.MethodModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.MemoizedFunctions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * JLinker wrapper of {@link software.coley.recaf.info.ClassInfo}.
 *
 * @author Matt Coley
 */
public class LinkedClass implements ClassModel<LinkedClass.LinkedMethod, LinkedClass.LinkedField> {
	private static final DebuggingLogger logger = Logging.get(LinkedClass.class);
	private final ClassInfo info;
	private final Function<String, LinkedClass> superClassLookup;
	private final Function<List<String>, List<LinkedClass>> interfacesLookup;
	private final BiFunction<String, FieldDescriptor, LinkedField> fieldLookup;
	private final BiFunction<String, MethodDescriptor, LinkedMethod> methodLookup;

	public LinkedClass(@Nonnull ClassLookup lookup, @Nonnull Function<ClassInfo, LinkedClass> linker, @Nonnull ClassInfo info) {
		this.info = info;

		superClassLookup = MemoizedFunctions.memoize((String superName) -> {
			var superClass = lookup.get(superName);
			if (superClass == null) {
				logger.debugging(l -> l.warn("Lookup failed for super-class: {}", superName));
				return null;
			}
			return linker.apply(superClass);
		});

		interfacesLookup = MemoizedFunctions.memoize((List<String> interfaces) -> {
			if (interfaces.isEmpty())
				return Collections.emptyList();
			List<LinkedClass> values = new ArrayList<>();
			for (String itf : interfaces) {
				var itfInfo = lookup.get(itf);
				if (itfInfo == null)
					logger.debugging(l -> l.warn("Lookup failed for interface: {}", itf));
				else
					values.add(linker.apply(itfInfo));
			}
			return values;
		});

		fieldLookup = MemoizedFunctions.memoize((name, descriptor) -> {
			FieldMember declaredField = info.getDeclaredField(name, descriptor.toString());
			if (declaredField == null) {
				logger.debugging(l -> l.warn("Missing declared field: {} {}", descriptor, name));
				return null;
			}
			return new LinkedField(this, declaredField);
		});

		methodLookup = MemoizedFunctions.memoize((name, descriptor) -> {
			MethodMember declaredMethod = info.getDeclaredMethod(name, descriptor.toString());
			if (declaredMethod == null) {
				logger.debugging(l -> l.warn("Missing declared method: {}{}", name, descriptor));
				return null;
			}
			return new LinkedMethod(this, declaredMethod);
		});
	}

	@Nonnull
	public ClassInfo innerValue() {
		return info;
	}

	@Override
	public @Nonnull String name() {
		return info.getName();
	}

	@Override
	public int accessFlags() {
		return info.getAccess();
	}

	@Nullable
	@Override
	public LinkedClass superClass() {
		String superName = info.getSuperName();
		if (superName == null)
			return null;
		return superClassLookup.apply(superName);
	}

	@Nonnull
	@Override
	public Iterable<? extends LinkedClass> interfaces() {
		return interfacesLookup.apply(info.getInterfaces());
	}

	@Override
	public LinkedMethod findMethod(@Nonnull String name, @Nonnull MethodDescriptor descriptor) {
		return methodLookup.apply(name, descriptor);
	}

	@Override
	public LinkedField findField(@Nonnull String name, @Nonnull FieldDescriptor descriptor) {
		return fieldLookup.apply(name, descriptor);
	}

	@Nullable
	public LinkedMethod getMethod(@Nonnull String name, @Nonnull String descriptor) {
		return findMethod(name, MethodDescriptorString.of(descriptor));
	}

	@Nullable
	public LinkedField getField(@Nonnull String name, @Nonnull String descriptor) {
		return findField(name, FieldDescriptorString.of(descriptor));
	}

	public record LinkedMethod(@Nonnull LinkedClass owner, @Nonnull MethodMember method) implements MethodModel {
		@Override
		public int accessFlags() {
			return method.getAccess();
		}
	}

	public record LinkedField(@Nonnull LinkedClass owner, @Nonnull FieldMember field) implements FieldModel {
		@Override
		public int accessFlags() {
			return field.getAccess();
		}
	}
}
