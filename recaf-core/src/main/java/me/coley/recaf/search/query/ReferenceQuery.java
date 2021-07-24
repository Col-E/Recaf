package me.coley.recaf.search.query;

import com.google.common.base.Strings;
import me.coley.recaf.RecafConstants;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.result.ResultBuilder;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.INVOKEDYNAMIC;
import static org.objectweb.asm.Opcodes.LDC;

/**
 * A query that looks for references to a member in all accessible locations.<br>
 * Not all parameters are required, for instance only including the {@link #owner} will
 * yield all references to the class.
 *
 * @author Matt Coley
 */
public class ReferenceQuery implements Query {
	private static final Logger logger = Logging.get(ReferenceQuery.class);
	private final String owner;
	private final String name;
	private final String desc;
	private final TextMatchMode mode;

	/**
	 * @param owner
	 * 		The class defining the referenced member.
	 * @param name
	 * 		The name of the referenced member.
	 * @param desc
	 * 		The type descriptor of the referenced member.
	 * @param mode
	 * 		The matching strategy of the query against the reference type texts.
	 */
	public ReferenceQuery(String owner, String name, String desc, TextMatchMode mode) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.mode = mode;
	}

	@Override
	public QueryVisitor createVisitor(Resource resource, QueryVisitor delegate) {
		return new RefClassVisitor(resource, delegate);
	}

	private void whenMatched(String owner, String name, String desc, Consumer<ResultBuilder> builderConsumer) {
		if (Strings.isNullOrEmpty(this.owner) || mode.match(this.owner, owner)) {
			if (Strings.isNullOrEmpty(this.name) || mode.match(this.name, name)) {
				if ((Strings.isNullOrEmpty(this.desc) || mode.match(this.desc, desc))) {
					builderConsumer.accept(ResultBuilder.reference(owner, name, desc));
				}
			}
		}
	}

	private class RefClassVisitor extends QueryVisitor {
		public RefClassVisitor(Resource resource, QueryVisitor delegate) {
			super(resource, delegate);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			MethodInfo methodInfo = currentClass.findMethod(name, desc);
			if (methodInfo != null) {
				return new RefMethodVisitor(mv, methodInfo);
			} else {
				logger.error("Failed to lookup method for query: {}.{}{}", currentClass.getName(), name, desc);
				return mv;
			}
		}


		private class RefMethodVisitor extends MethodVisitor {
			private final MethodInfo methodInfo;

			public RefMethodVisitor(MethodVisitor delegate, MethodInfo methodInfo) {
				super(RecafConstants.ASM_VERSION, delegate);
				this.methodInfo = methodInfo;
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				super.visitFieldInsn(opcode, owner, name, desc);
				whenMatched(owner, name, desc,
						builder -> addMethodInsn(builder, methodInfo.getName(), methodInfo.getDescriptor(), opcode));
			}

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
				super.visitMethodInsn(opcode, owner, name, desc, isInterface);
				whenMatched(owner, name, desc,
						builder -> addMethodInsn(builder, methodInfo.getName(), methodInfo.getDescriptor(), opcode));
			}

			@Override
			public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsmHandle,
											   Object... bootstrapMethodArguments) {
				super.visitInvokeDynamicInsn(name, descriptor, bsmHandle, bootstrapMethodArguments);
				whenMatched(bsmHandle.getOwner(), bsmHandle.getName(), bsmHandle.getDesc(),
						builder -> addMethodInsn(builder, methodInfo.getName(),
								methodInfo.getDescriptor(), INVOKEDYNAMIC));
				for (Object bsmArg : bootstrapMethodArguments) {
					if (bsmArg instanceof Handle) {
						Handle handle = (Handle) bsmArg;
						whenMatched(handle.getOwner(), handle.getName(), handle.getDesc(),
								builder -> addMethodInsn(builder, methodInfo.getName(),
										methodInfo.getDescriptor(), INVOKEDYNAMIC));
					} else if (bsmArg instanceof ConstantDynamic) {
						ConstantDynamic dynamic = (ConstantDynamic) bsmArg;
						Handle handle = dynamic.getBootstrapMethod();
						whenMatched(handle.getOwner(), handle.getName(), handle.getDesc(),
								builder -> addMethodInsn(builder, methodInfo.getName(),
										methodInfo.getDescriptor(), INVOKEDYNAMIC));
					}
				}
			}

			@Override
			public void visitLdcInsn(Object value) {
				super.visitLdcInsn(value);
				if (value instanceof Handle) {
					Handle handle = (Handle) value;
					whenMatched(handle.getOwner(), handle.getName(), handle.getDesc(),
							builder -> addMethodInsn(builder, methodInfo.getName(),
									methodInfo.getDescriptor(), LDC));
				}
			}
		}
	}
}