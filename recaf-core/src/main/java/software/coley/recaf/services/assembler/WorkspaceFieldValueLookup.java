package software.coley.recaf.services.assembler;

import dev.xdark.blw.code.instruction.FieldInstruction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.compile.analysis.Value;
import me.darknet.assembler.compile.analysis.Values;
import me.darknet.assembler.compile.analysis.jvm.FieldValueLookup;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Field value lookup for items in a {@link Workspace}.
 * <p/>
 * Its very basic, only looking at static fields default values.
 *
 * @author Matt Coley
 */
public class WorkspaceFieldValueLookup implements FieldValueLookup {
	private final Workspace workspace;
	private final FieldValueLookup parent;

	/**
	 * @param workspace
	 * 		Workspace to pull values from.
	 * @param parent
	 * 		Parent lookup if no such value could be found in the workspace.
	 */
	public WorkspaceFieldValueLookup(@Nonnull Workspace workspace, @Nullable FieldValueLookup parent) {
		this.workspace = workspace;
		this.parent = parent;
	}

	@Override
	@Nullable
	public Value accept(@Nonnull FieldInstruction fieldRef, @Nullable Value.ObjectValue context) {
		if (fieldRef.opcode() == Opcodes.GETSTATIC) {
			ClassPathNode owner = workspace.findClass(fieldRef.owner().internalName());
			if (owner != null) {
				FieldMember field = owner.getValue().getDeclaredField(fieldRef.name(), fieldRef.type().descriptor());
				if (field != null) {
					Object value = field.getDefaultValue();
					if (value instanceof Integer v)
						return Values.valueOf(v);
					if (value instanceof Long v)
						return Values.valueOf(v);
					if (value instanceof Float v)
						return Values.valueOf(v);
					if (value instanceof Double v)
						return Values.valueOf(v);
					if (value instanceof String v)
						return Values.valueOfString(v);
				}
			}
		}
		if (parent == null)
			return null;
		return parent.accept(fieldRef, context);
	}
}
