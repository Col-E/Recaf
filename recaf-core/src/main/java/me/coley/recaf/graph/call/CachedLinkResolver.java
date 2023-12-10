package me.coley.recaf.graph.call;

import dev.xdark.jlinker.LinkResolver;
import dev.xdark.jlinker.Resolution;
import dev.xdark.jlinker.Result;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.decompile.PostDecompileInterceptor;
import me.coley.recaf.util.MemoizedFunction;
import org.checkerframework.checker.units.qual.C;

import java.util.function.BiFunction;
import java.util.function.Function;

class CachedLinkResolver implements LinkResolver<ClassInfo, MethodInfo, FieldInfo> {

	LinkResolver<ClassInfo, MethodInfo, FieldInfo> backedResolver = LinkResolver.jvm();

	private final Function<dev.xdark.jlinker.ClassInfo<ClassInfo>, BiFunction<String, String, Result<Resolution<ClassInfo, MethodInfo>>>>
			virtualMethodResolver = MemoizedFunction.memoize(
			c -> MemoizedFunction.memoize((name, descriptor) -> backedResolver.resolveVirtualMethod(c, name, descriptor))
	);
	private final Function<dev.xdark.jlinker.ClassInfo<ClassInfo>, BiFunction<String, String, Result<Resolution<ClassInfo, MethodInfo>>>>
			staticMethodResolver = MemoizedFunction.memoize(
			c -> MemoizedFunction.memoize((name, descriptor) -> backedResolver.resolveStaticMethod(c, name, descriptor))
	);
	private final Function<dev.xdark.jlinker.ClassInfo<ClassInfo>, BiFunction<String, String, Result<Resolution<ClassInfo, MethodInfo>>>>
			interfaceMethodResolver = MemoizedFunction.memoize(
			c -> MemoizedFunction.memoize((name, descriptor) -> backedResolver.resolveInterfaceMethod(c, name, descriptor))
	);
	private final Function<dev.xdark.jlinker.ClassInfo<ClassInfo>, BiFunction<String, String, Result<Resolution<ClassInfo, FieldInfo>>>>
			virtualFieldResolver = MemoizedFunction.memoize(
			c -> MemoizedFunction.memoize((name, descriptor) -> backedResolver.resolveVirtualField(c, name, descriptor))
	);
	private final Function<dev.xdark.jlinker.ClassInfo<ClassInfo>, BiFunction<String, String, Result<Resolution<ClassInfo, FieldInfo>>>>
			staticFieldResolver = MemoizedFunction.memoize(
			c -> MemoizedFunction.memoize((name, descriptor) -> backedResolver.resolveStaticField(c, name, descriptor))
	);
	private final Function<dev.xdark.jlinker.ClassInfo<ClassInfo>, BiFunction<String, String, Result<Resolution<ClassInfo, MethodInfo>>>>
			specialMethodResolver = MemoizedFunction.memoize(
			c -> MemoizedFunction.memoize((name, descriptor) -> backedResolver.resolveSpecialMethod(c, name, descriptor))
	);

	@Override
	public Result<Resolution<ClassInfo, MethodInfo>> resolveStaticMethod(dev.xdark.jlinker.ClassInfo<ClassInfo> owner, String name, String descriptor, boolean itf) {
		return staticMethodResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<ClassInfo, MethodInfo>> resolveStaticMethod(dev.xdark.jlinker.ClassInfo<ClassInfo> owner, String name, String descriptor) {
		return resolveStaticMethod(owner, name, descriptor, false);
	}

	@Override
	public Result<Resolution<ClassInfo, MethodInfo>> resolveSpecialMethod(dev.xdark.jlinker.ClassInfo<ClassInfo> owner, String name, String descriptor, boolean itf) {
		return specialMethodResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<ClassInfo, MethodInfo>> resolveVirtualMethod(dev.xdark.jlinker.ClassInfo<ClassInfo> owner, String name, String descriptor) {
		return virtualMethodResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<ClassInfo, MethodInfo>> resolveInterfaceMethod(dev.xdark.jlinker.ClassInfo<ClassInfo> owner, String name, String descriptor) {
		return interfaceMethodResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<ClassInfo, FieldInfo>> resolveStaticField(dev.xdark.jlinker.ClassInfo<ClassInfo> owner, String name, String descriptor) {
		return staticFieldResolver.apply(owner).apply(name, descriptor);
	}

	@Override
	public Result<Resolution<ClassInfo, FieldInfo>> resolveVirtualField(dev.xdark.jlinker.ClassInfo<ClassInfo> owner, String name, String descriptor) {
		return virtualFieldResolver.apply(owner).apply(name, descriptor);
	}
}
