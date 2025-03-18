package software.coley.recaf.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to beans to enable eager initialization, which is to say they and their dependencies get created as soon as
 * possible depending on the {@link #value() value of the intended} {@link InitializationStage}. This will result in
 * the bean's {@code @Inject} annotated constructor being called.
 * <p/>
 * Alternatively, you could also observe the events {@link InitializationEvent} or {@link UiInitializationEvent}
 * in a method with {@link Observes}. This would allow you to separate the initialization logic from the constructor
 * and have it reside in a separate method.
 * <p/>
 * <b>NOTE:</b> Beans are not eagerly initialized while in a test environment.
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
	 *
	 * @return When the initialization should occur.
	 */
	InitializationStage value() default InitializationStage.IMMEDIATE;
}
