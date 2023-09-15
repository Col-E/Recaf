package software.coley.recaf.services;

import jakarta.annotation.Nonnull;

/**
 * Outline of a service.
 *
 * @author Matt Coley
 */
public interface Service {
	/**
	 * @return A unique string for identifying the service.
	 */
	@Nonnull
	String getServiceId();

	/**
	 * @return The config instance for the service.
	 */
	@Nonnull
	ServiceConfig getServiceConfig();
}
