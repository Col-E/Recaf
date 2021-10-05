package me.coley.recaf.decompile.cfr;

import me.coley.recaf.util.logging.Logging;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.slf4j.Logger;

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
				return t -> logger.error("CFR Error: {}", t);
			case SUMMARY:
			case PROGRESS:
			default:
				return t -> {
				};
		}
	}

	private <T> void setDecompilation(T value) {
		decompile = value.toString();
	}

	/**
	 * @return Decompiled class content.
	 */
	public String getDecompilation() {
		return decompile;
	}
}
