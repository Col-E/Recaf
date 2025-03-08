package software.coley.recaf.services.json;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GsonProvider}.
 */
class GsonProviderTest {
	private GsonProvider provider;

	@BeforeEach
	void setup() {
		provider = new GsonProvider(new GsonProviderConfig());
	}

	@Test
	void addTypeSerializer() {
		// The default gson should match our default in the provider given instance.
		Gson defaultGson = new Gson();
		Gson baseline = provider.getGson();
		assertThat(baseline.getAdapter(Foo.class)).hasSameClassAs(defaultGson.getAdapter(Foo.class));
		assertThat(baseline.getAdapter(Fizz.class)).hasSameClassAs(defaultGson.getAdapter(Fizz.class));

		// If we add two type serializers, we should see them in our newly provided instance.
		DummyModelSerializer ourSerializer = new DummyModelSerializer();
		provider.addTypeSerializer(Foo.class, ourSerializer);
		provider.addTypeSerializer(Fizz.class, ourSerializer);
		Gson afterSerialization = provider.getGson();
		TypeAdapter<Foo> adapterFoo = afterSerialization.getAdapter(Foo.class);
		TypeAdapter<Fizz> adapterFizz = afterSerialization.getAdapter(Fizz.class);

		// They should not be the same as our first assertion's type.
		assertThat(adapterFoo).doesNotHaveSameClassAs(defaultGson.getAdapter(Foo.class));
		assertThat(adapterFizz).doesNotHaveSameClassAs(defaultGson.getAdapter(Fizz.class));
	}

	@Test
	void addTypeSerializerAndDeserializer() {
		DummyModelSerializer ourSerializer = new DummyModelSerializer();
		DummyModelDeserializer ourDeserializer = new DummyModelDeserializer();

		provider.addTypeSerializer(Foo.class, ourSerializer);
		provider.addTypeDeserializer(Foo.class, ourDeserializer);
		Gson gson = provider.getGson();

		String json = gson.toJson(new Foo("hello"));
		Foo foo = gson.fromJson(json, Foo.class);

		assertThat(foo)
				.isInstanceOf(Foo.class)
				.matches(deserializedFoo -> deserializedFoo.display().equals("hello"));
	}

	static class DummyModelSerializer implements JsonSerializer<DummyModel> {
		@Override
		public JsonElement serialize(DummyModel model, Type type, JsonSerializationContext context) {
			return new JsonPrimitive(model.getClass().getSimpleName() + ":" + model.display());
		}
	}

	static class DummyModelDeserializer implements JsonDeserializer<DummyModel> {
		@Override
		public DummyModel deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
			String string = element.getAsString();
			if (string.startsWith("Foo"))
				return new Foo(string.substring(4));
			else if (string.startsWith("Fizz"))
				return new Fizz(string.substring(5));
			throw new JsonParseException("No type provided in encoded string: " + string);
		}
	}

	interface DummyModel {
		String display();
	}

	record Foo(String bar) implements DummyModel {
		@Override
		public String display() {
			return bar;
		}
	}

	record Fizz(String buzz) implements DummyModel {
		@Override
		public String display() {
			return buzz;
		}
	}
}