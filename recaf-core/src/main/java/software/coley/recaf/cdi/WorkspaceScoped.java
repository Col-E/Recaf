package software.coley.recaf.cdi;

import jakarta.enterprise.context.NormalScope;
import software.coley.recaf.workspace.model.Workspace;

import java.lang.annotation.*;

/**
 * Applied to classes that operate in the lifecycle of an active {@link Workspace}.
 * <br>
 * Generally these classes represent independent services and components.
 *
 * @author Matt Coley
 */
@NormalScope
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface WorkspaceScoped {
}