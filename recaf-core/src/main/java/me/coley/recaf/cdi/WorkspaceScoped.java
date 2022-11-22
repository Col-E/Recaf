package me.coley.recaf.cdi;

import jakarta.enterprise.context.NormalScope;
import me.coley.recaf.workspace.WorkspaceCloseListener;

import java.lang.annotation.*;

/**
 * Applied to classes that operate in the lifecycle of an active {@link me.coley.recaf.workspace.Workspace}.
 * <br>
 * Generally these classes represent independent services and components.
 *
 * @see WorkspaceCloseListener Implemented when the component should update internal state when the operating workspace changes.
 */
@NormalScope
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface WorkspaceScoped {
}