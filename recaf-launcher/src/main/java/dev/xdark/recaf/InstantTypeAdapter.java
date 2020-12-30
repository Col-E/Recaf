package dev.xdark.recaf;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

/**
 * GSON {@link Instant} adapter.
 *
 * @author xDark
 */
public final class InstantTypeAdapter extends TypeAdapter<Instant> {
	@Override
	public void write(JsonWriter out, Instant value) throws IOException {
		out.value(value.toString());
	}

	@Override
	public Instant read(JsonReader in) throws IOException {
		return Instant.parse(in.nextString());
	}
}
