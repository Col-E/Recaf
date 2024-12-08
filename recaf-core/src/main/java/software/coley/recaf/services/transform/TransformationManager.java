package software.coley.recaf.services.transform;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.Bootstrap;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manager for tracking transformers.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class TransformationManager implements Service {
	public static final String SERVICE_ID = "transformation-manager";
	private static final Logger logger = Logging.get(TransformationManager.class);
	private final Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> jvmTransformerSuppliers = new IdentityHashMap<>();
	private final Map<String, Class<? extends JvmClassTransformer>> nameToJvmTransformerClass = new HashMap<>();
	private final TransformationManagerConfig config;

	/**
	 * Constructor for pulling transformer instances from the Recaf CDI container.
	 *
	 * @param config
	 * 		Manager config.
	 * @param jvmTransformers
	 * 		CDI provider of JVM transformer implementations.
	 */
	@Inject
	public TransformationManager(@Nonnull TransformationManagerConfig config, @Nonnull Instance<JvmClassTransformer> jvmTransformers) {
		this.config = config;
		for (Instance.Handle<JvmClassTransformer> handle : jvmTransformers.handles()) {
			Bean<JvmClassTransformer> bean = handle.getBean();
			Class<? extends JvmClassTransformer> transformerClass = Unchecked.cast(bean.getBeanClass());
			jvmTransformerSuppliers.put(transformerClass, () -> {
				// Even though our transformers may be @Dependent scoped, we need to do a new lookup each time we want
				// a new instance to get our desired scope behavior. If we re-use the instance handle that is injected
				// here then even @Dependent scoped beans will yield the same instance again and again.
				return Bootstrap.get().get(transformerClass);
			});
		}
	}

	/**
	 * Constructor for testing with pre-defined sets of transformers.
	 *
	 * @param jvmTransformerSuppliers
	 * 		Map of transformer classes to suppliers that generate instances of those classes.
	 */
	@VisibleForTesting
	public TransformationManager(@Nonnull Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> jvmTransformerSuppliers) {
		this.jvmTransformerSuppliers.putAll(jvmTransformerSuppliers);
		this.config = new TransformationManagerConfig();
	}

	/**
	 * @param transformerClass
	 * 		Class of transformer to register.
	 * @param transformerSupplier
	 * 		Supplier of transformer instances.
	 * @param <T>
	 * 		Transformer type.
	 */
	public <T extends JvmClassTransformer> void registerJvmClassTransformer(@Nonnull Class<T> transformerClass, @Nonnull Supplier<T> transformerSupplier) {
		jvmTransformerSuppliers.put(Unchecked.cast(transformerClass), Unchecked.cast(transformerSupplier));
	}

	/**
	 * @param transformerClass
	 * 		Class of transformer to unregister.
	 * @param <T>
	 * 		Transformer type.
	 */
	public <T extends JvmClassTransformer> void unregisterJvmClassTransformer(@Nonnull Class<T> transformerClass) {
		jvmTransformerSuppliers.remove(Unchecked.cast(transformerClass));
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public <T extends JvmClassTransformer> T newJvmTransformer(@Nonnull Class<T> type) throws TransformationException {
		try {
			Supplier<JvmClassTransformer> supplier = jvmTransformerSuppliers.get(type);
			if (supplier == null)
				throw new TransformationException("Requested transformer supplier for type '"
						+ type.getSimpleName() + "' but no associated supplier was registered");
			return (T) supplier.get();
		} catch (Throwable t) {
			throw new TransformationException("Requested transformer supplier for type '"
					+ type.getSimpleName() + "' could not be instantiated", t);
		}
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public TransformationManagerConfig getServiceConfig() {
		return config;
	}
}
