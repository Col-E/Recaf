package me.coley.recaf.parse.bytecode.exception;

import me.coley.recaf.parse.bytecode.RValue;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.function.BiPredicate;

public interface PostValidator extends BiPredicate<MethodNode, Frame<RValue>[]> {

}
