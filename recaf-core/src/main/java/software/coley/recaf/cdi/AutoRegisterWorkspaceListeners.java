package software.coley.recaf.cdi;

import jakarta.interceptor.InterceptorBinding;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;

import java.lang.annotation.*;

/**
 * Applied to bean implementations that want to automatically register workspace listeners:
 * <ul>
 *     <li>{@link WorkspaceOpenListener}</li>
 *     <li>{@link WorkspaceModificationListener}</li>
 *     <li>{@link WorkspaceCloseListener}</li>
 * </ul>
 *
 * @author Matt Coley
 */
@Inherited
@InterceptorBinding
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRegisterWorkspaceListeners {
}
