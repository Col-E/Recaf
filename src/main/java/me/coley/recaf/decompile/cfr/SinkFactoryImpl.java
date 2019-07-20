package me.coley.recaf.decompile.cfr;

import org.benf.cfr.reader.api.OutputSinkFactory;
import org.pmw.tinylog.Logger;

import java.util.*;

/**
 * Cfr logging/output sinker.
 *
 * @author Matt
 */
public class SinkFactoryImpl implements OutputSinkFactory {
	private String decompile;

	@Override
	public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
		return Arrays.asList(SinkClass.values());
	}

	@Override
	public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
		switch(sinkType) {
			case JAVA:
				return t -> setDecompilation(t);
			case EXCEPTION:
				return t -> Logger.error(t);
			case SUMMARY:
			case PROGRESS:
			default:
				return t -> {};
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
