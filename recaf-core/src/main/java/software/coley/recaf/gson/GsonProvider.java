package software.coley.recaf.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GsonProvider {

    private static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableJdkUnsafe()
            .serializeNulls()
            .create();

    @Inject
    public GsonProvider() {
        registerAdapters();
    }

    public Gson getGson() {
        return gson;
    }

    public void addTypeAdapter(Class<?> type, Object adapter) {
        gson = gson.newBuilder().registerTypeAdapter(type, adapter).create();
    }

    public <T> void addTypeAdapter(Class<T> type, TypeAdapter<T> adapter) {
        gson = gson.newBuilder().registerTypeAdapter(type, adapter).create();
    }

    private void registerAdapters() {
        // register recaf type adapters
    }

}
