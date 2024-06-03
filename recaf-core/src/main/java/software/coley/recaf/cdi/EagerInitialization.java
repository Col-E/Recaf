package software.coley.recaf.cdi;

import jakarta.enterprise.context.ApplicationScoped;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to beans to enable eager initialization, which is to say they and their dependencies get created as soon as
 * possible depending on the {@link #value() value of the intended} {@link InitializationStage}.
 * <p/>
 * Beans are not eagerly initialized while in a test environment.
 *
 * @author Matt Coley
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EagerInitialization {
	/**
	 * Determines when to run early initialization.
	 * <br>
	 * Changing this value is mostly applicable to {@link ApplicationScoped} beans.
	 * Having the value set to {@link InitializationStage#IMMEDIATE} will result in the bean and <i>all</i> of
	 * its dependencies being created as soon as the application begins. For beans dealing with UI capabilities this
	 * will likely lead to problems. For those situations, use {@link InitializationStage#AFTER_UI_INIT} to delay
	 * initialization until after the UI has been populated.
	 * <br>
	 * {@link WorkspaceScoped} beans should not need to ever change this value since they are only created/scoped to the
	 * creation of new workspaces. When running the UI, a workspace will never be opened before the UI populates.
	 *
	 * @return When the initialization should occur.
	 */
	InitializationStage value() default InitializationStage.IMMEDIATE;
}
