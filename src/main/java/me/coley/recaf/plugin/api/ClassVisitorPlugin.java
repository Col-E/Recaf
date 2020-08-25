package me.coley.recaf.plugin.api;

import org.objectweb.asm.ClassVisitor;

public interface ClassVisitorPlugin extends BasePlugin {
    /**
     * Intercepts the given {@link ClassVisitor}.
     *
     * @param visitor
     *      Visitor to intercept.
     * @return intercepted {@link ClassVisitor}.
     */
    ClassVisitor intercept(ClassVisitor visitor);
}
