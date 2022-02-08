package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.MethodModel;
import me.coley.recaf.util.AccessFlag;

import java.util.Collection;

/**
 * Method printing strategy for interface methods.
 *
 * @author Matt Coley
 */
public class InterfaceMethodPrintStrategy extends BasicMethodPrintStrategy {
	@Override
	protected void buildDeclarationFlags(StringBuilder sb, MethodModel model) {
		Collection<AccessFlag> flags = AccessFlag.getApplicableFlags(AccessFlag.Type.METHOD, model.getAccess());
		flags = AccessFlag.sort(AccessFlag.Type.METHOD, flags);
		flags.remove(AccessFlag.ACC_PUBLIC);
		flags.remove(AccessFlag.ACC_ABSTRACT);
		boolean isAbstract = AccessFlag.isAbstract(model.getAccess());
		if (!flags.isEmpty()) {
			String flagsStr = AccessFlag.toString(flags);
			if (!isAbstract)
				sb.append("default ");
			sb.append(flagsStr).append(' ');
		} else if (!isAbstract)
			sb.append("default ");
	}
}
