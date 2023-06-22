package software.coley.recaf.services.callgraph;

import dev.xdark.jlinker.ClassInfo;
import dev.xdark.jlinker.LinkResolver;
import dev.xdark.jlinker.Resolution;
import dev.xdark.jlinker.Result;
import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.util.MemoizedFunctions;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Memoized implementation of {@link LinkResolver}.
 *
 * @author Amejonah
 */
public class CachedLinkResolver implements LinkResolver<JvmClassInfo, MethodMember, FieldMember> {
	private final LinkResolver<JvmClassInfo, MethodMember, FieldMember> backedResolver = LinkResolver.jvm();
	private final Function<ClassInfo<JvmClassInfo>, BiFunction<String, String, Result<Resolution<JvmClassInfo, MethodMember>>>>
			virtualMethodResolver = MemoizedFunctions.memoize(
			c -> MemoizedFunctions.memoize((name, descriptor) -> backedResolver.resolveVirtualMethod(c, name, descriptor))
	);
	private final Function<ClassInfo<JvmClassInfo>, BiFunction<String, String, Result<Resolution<JvmClassInfo, MethodMember>>>>
			staticMethodResolver = MemoizedFunctions.memoize(
			c -> MemoizedFunctions.memoize((name, descriptor) -> backedResolver.resolveStaticMethod(c, name, descriptor))
	);
	private final Function<ClassInfo<JvmClassInfo>, BiFunction<String, String, Result<Resolution<JvmClassInfo, MethodMember>>>>
			interfaceMethodResolver = MemoizedFunctions.memoize(
			c -> MemoizedFunctions.memoize((name, descriptor) -> backedResolver.resolveInterfaceMethod(c, name, descriptor))
	);
	private final Function<ClassInfo<JvmClassInfo>, BiFunction<String, String, Result<Resolution<JvmClassInfo, FieldMember>>>>
			virtualFieldResolver = MemoizedFunctions.memoize(
			c -> MemoizedFunctions.memoize((name, descriptor) -> backedResolver.resolveVirtualField(c, name, descriptor))
	);
	private final Function<ClassInfo<JvmClassInfo>, BiFunction<String, String, Result<Resolution<JvmClassInfo, FieldMember>>>>
			staticFieldResolver = MemoizedFunctions.memoize(
			c -> MemoizedFunctions.memoize((name, descriptor) -> backedResolver.resolveStaticField(c, name, descriptor))
	);

	@Override
	public Result<Resolution<JvmClassInfo, MethodMember>> resolveStaticMethod(@Nonnull ClassInfo<JvmClassInfo> owner,
																			  @Nonnull String name, @Nonnull String descriptor, boolean itf) {
		return staticMethodResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<JvmClassInfo, MethodMember>> resolveVirtualMethod(@Nonnull ClassInfo<JvmClassInfo> owner,
																			   @Nonnull String name, @Nonnull String descriptor) {
		return virtualMethodResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<JvmClassInfo, MethodMember>> resolveInterfaceMethod(@Nonnull ClassInfo<JvmClassInfo> owner,
																				 @Nonnull String name, @Nonnull String descriptor) {
		return interfaceMethodResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<JvmClassInfo, FieldMember>> resolveStaticField(@Nonnull ClassInfo<JvmClassInfo> owner,
																			@Nonnull String name, @Nonnull String descriptor) {
		return staticFieldResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<JvmClassInfo, FieldMember>> resolveVirtualField(@Nonnull ClassInfo<JvmClassInfo> owner,
																			 @Nonnull String name, @Nonnull String descriptor) {
		return virtualFieldResolver.apply(owner).apply(name, descriptor);
	}
}
