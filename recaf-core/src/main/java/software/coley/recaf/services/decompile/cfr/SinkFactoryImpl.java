package software.coley.recaf.services.decompile.cfr;

import jakarta.annotation.Nullable;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Cfr logging/output sinker.
 *
 * @author Matt
 */
public class SinkFactoryImpl implements OutputSinkFactory {
	private static final Logger logger = Logging.get(SinkFactoryImpl.class);
	private Throwable exception;
	private String decompile;

	@Override
	public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
		return Arrays.asList(SinkClass.values());
	}

	@Override
	public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
		switch (sinkType) {
			case JAVA:
				return this::setDecompilation;
			case EXCEPTION:
				return this::handleException;
			case SUMMARY:
			case PROGRESS:
			default:
				return t -> {
				};
		}
	}

	private <T> void handleException(T value) {
		logger.error("CFR Error: {}", value);
		if (value instanceof Throwable) {
			exception = (Throwable) value;
		}
	}

	private <T> void setDecompilation(T value) {
		decompile = value.toString();
	}

	/**
	 * @return Decompiled class content.
	 */
	@Nullable
	public String getDecompilation() {
		return decompile;
	}

	/**
	 * @return Failure reason.
	 */
	@Nullable
	public Throwable getException() {
		return exception;
	}
}
