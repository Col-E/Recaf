package me.coley.recaf.plugin.api;

import org.objectweb.asm.ClassVisitor;

/**
 * Allow plugins to intercept and modify {@link ClassVisitor}.
 *
 * @author xxDark
 */
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
