package me.coley.recaf.search.query;

import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.result.ResultBuilder;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A query that looks for definitions of types and members in classes.<br>
 * Not all parameters are required, for instance only including the {@link #owner} will
 * yield all definitions within the class.
 *
 * @author Matt Coley
 */
public class DeclarationQuery implements Query {
	private final String owner;
	private final String name;
	private final String desc;
	private final TextMatchMode mode;

	/**
	 * @param owner
	 * 		The class defining the declared member.
	 * @param name
	 * 		The name of the declared member.
	 * @param desc
	 * 		The type descriptor of the declared member.
	 * @param mode
	 * 		The matching strategy of the query against the declared type texts.
	 */
	public DeclarationQuery(String owner, String name, String desc, TextMatchMode mode) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
		this.mode = mode;
	}

	@Override
	public QueryVisitor createVisitor(Resource resource, QueryVisitor delegate) {
		return new DecClassVisitor(resource, delegate);
	}

	private void whenMatched(String owner, String name, String desc, Consumer<ResultBuilder> builderConsumer) {
		if (StringUtil.isAnyNullOrEmpty(this.owner, owner) || mode.match(this.owner, owner)) {
			if (StringUtil.isAnyNullOrEmpty(this.name, name) || mode.match(this.name, name)) {
				if (StringUtil.isAnyNullOrEmpty(this.desc, desc) || mode.match(this.desc, desc)) {
					builderConsumer.accept(ResultBuilder.declaration(owner, name, desc));
				}
			}
		}
	}

	@Override
	public String toString() {
		List<String> lines = new ArrayList<>();
		if (owner != null)
			lines.add("owner=" + owner);
		if (name != null)
			lines.add("name=" + name);
		if (desc != null)
			lines.add("desc=" + desc);
		return "Declarations [" + String.join(", ", lines) + ']';
	}

	private class DecClassVisitor extends QueryVisitor {
		public DecClassVisitor(Resource resource, QueryVisitor delegate) {
			super(resource, delegate);
		}

		@Override
		public void visitFile(FileInfo fileInfo) {
			// no-op
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodInfo methodInfo = currentClass.findMethod(name, desc);
			if (methodInfo != null)
				whenMatched(currentClass.getName(), name, desc,
						builder -> addMethod(builder, methodInfo.getName(), methodInfo.getDescriptor()));
			return null;
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			FieldInfo fieldInfo = currentClass.findField(name, desc);
			if (fieldInfo != null)
				whenMatched(currentClass.getName(), name, desc,
						builder -> addField(builder, fieldInfo.getName(), fieldInfo.getDescriptor()));
			return null;
		}
	}
}