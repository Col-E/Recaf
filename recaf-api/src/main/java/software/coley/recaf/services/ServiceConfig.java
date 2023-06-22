package software.coley.recaf.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.ConfigContainer;

/**
 * Base type for a service's config type.
 * When a service implementation uses {@link Inject} to inject the config instance some rules should be followed:
 * <ul>
 *     <li>The type injected is the implementation type, not {@link ServiceConfig}</li>
 *     <li>The config implementation type is annotated with {@link ApplicationScoped} so that it is shared
 *     among all instances of a service.</li>
 * </ul>
 *
 * @author Matt Coley
 */
public interface ServiceConfig extends ConfigContainer {
}
