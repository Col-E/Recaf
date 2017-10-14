package me.coley.recaf.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * An extension of DataInputStream that allows access to the protected position
 * index.
 */
public class IndexableDataStream extends DataInputStream {
	private PositionExposer exposer;

	public IndexableDataStream(byte[] data) {
		super(new PositionExposer(data));
		this.exposer = ((PositionExposer) in);
	}

	public int getIndex() {
		return exposer.getIndex();
	}

	public void reset(int len) {
		exposer.reset(len);
	}

	private static final class PositionExposer extends ByteArrayInputStream {
		public PositionExposer(byte[] data) {
			super(data);
		}

		public void reset(int len) {
			pos -= len;
		}

		public int getIndex() {
			return pos;
		}
	}
}
