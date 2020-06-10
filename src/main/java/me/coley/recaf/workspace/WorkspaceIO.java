package me.coley.recaf.workspace;

import com.eclipsesource.json.*;
import me.coley.recaf.util.IOUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * IO Utilities for {@link Workspace}.
 *
 * @author Matt
 */
public class WorkspaceIO {
	/**
	 * @param json
	 * 		Json file.
	 *
	 * @return Workspace loaded from a json config.
	 *
	 * @throws Exception
	 * 		Thrown if the path could not be read or parsed.
	 */
	public static Workspace fromJson(Path json) throws Exception {
		return fromJson(String.join("", Files.readAllLines(json, StandardCharsets.UTF_8)));
	}

	/**
	 * @param json
	 * 		Json text.
	 *
	 * @return Workspace loaded from a json string.
	 *
	 * @throws Exception
	 * 		Thrown if the json could not be parsed.
	 */
	public static Workspace fromJson(String json) throws Exception {
		JsonObject root = Json.parse(json).asObject();
		JsonObject jprimary = root.get("primary").asObject();
		JavaResource primary = deserializeResource(jprimary);
		List<JavaResource> libraries = new ArrayList<>();
		if(root.names().contains("libraries")) {
			JsonArray jlibraries = root.get("libraries").asArray();
			for(JsonValue value : jlibraries)
				libraries.add(deserializeResource(value.asObject()));
		}
		return new Workspace(primary, libraries);
	}

	/**
	 * @param workspace
	 * 		Workspace instance.
	 *
	 * @return Json text representation of a workspace.
	 */
	public static String toJson(Workspace workspace) {
		JsonObject root = Json.object();
		JsonObject jprimary = serializeResource(workspace.getPrimary());
		JsonArray jlibraries = new JsonArray();
		for(JavaResource library : workspace.getLibraries())
			jlibraries.add(serializeResource(library));
		root.add("primary", jprimary);
		root.add("libraries", jlibraries);
		return root.toString(WriterConfig.PRETTY_PRINT);
	}

	/**
	 * @param resource
	 * 		Resource reference.
	 *
	 * @return Json representation of the reference.
	 */
	private static JsonObject serializeResource(JavaResource resource) {
		JsonObject root = serializeBase(resource);
		serializeExtras(resource, root);
		return root;
	}

	/**
	 * @param jresource
	 * 		Json representation of a resource reference.
	 *
	 * @return Resource reference.
	 *
	 * @throws IllegalArgumentException
	 * 		Thrown if the json object was malformed or if instantiation of the resource failed.
	 * @throws IOException
	 * 		Thrown when the resource's source failed to be loaded.
	 */
	private static JavaResource deserializeResource(JsonObject jresource) throws IllegalArgumentException, IOException {
		JavaResource resource = deserializeBase(jresource);
		deserializeExtras(resource, jresource);
		return resource;
	}

	/**
	 * Serialize core values <i>(kind/kind-source)</i>
	 *
	 * @param resource
	 * 		Resource to serialize.
	 *
	 * @return Json of resource.
	 */
	private static JsonObject serializeBase(JavaResource resource) {
		JsonObject root = Json.object();
		ResourceKind kind = resource.getKind();
		switch(kind) {
			case CLASS:
				Path clazz = ((ClassResource) resource).getPath();
				root.add("kind", "class");
				root.add("source", IOUtil.toString(clazz));
				break;
			case JAR:
				Path jar = ((JarResource) resource).getPath();
				root.add("kind", "jar");
				root.add("source", IOUtil.toString(jar));
				break;
			case WAR:
				Path war = ((WarResource) resource).getPath();
				root.add("kind", "war");
				root.add("source", IOUtil.toString(war));
				break;
			case DIRECTORY:
				Path dir = ((DirectoryResource) resource).getPath();
				root.add("kind", "directory");
				root.add("source", IOUtil.toString(dir));
				break;
			case MAVEN:
				MavenResource maven = (MavenResource) resource;
				root.add("kind", "maven");
				root.add("source", maven.getCoords());
				break;
			case URL:
				UrlResource url = (UrlResource) resource;
				root.add("kind", "url");
				root.add("source", url.getUrl().toString());
				break;
			case INSTRUMENTATION:
				root.add("kind", "instrumentation");
				root.add("source", "n/a");
				break;
			case DEBUGGER:
				root.add("kind", "debugger");
				root.add("source", "n/a");
				break;
			case EMPTY:
				root.add("kind", "empty");
				root.add("source", "n/a");
				break;
			default:
				throw new IllegalStateException("Unsupported kind: " + kind);
		}
		return root;
	}

	/**
	 * Deserialize core values <i>(kind/kind-source)</i>
	 *
	 * @param jresource
	 * 		Json to deserialize.
	 *
	 * @return Deserialized resource..
	 */
	private static JavaResource deserializeBase(JsonObject jresource) throws IOException {
		String kind = jresource.getString("kind", null);
		if (kind == null)
			throw new IllegalArgumentException("Invalid resource, kind not specified!");
		String source = jresource.getString("source", null);
		if (source == null)
			throw new IllegalArgumentException("Invalid resource, source not specified!");
		JavaResource resource = null;
		switch(kind) {
			case "class":
			case "jar":
			case "war":
			case "directory":
				Path path = Paths.get(source);
				if (Files.exists(path))
					resource = FileSystemResource.of(path);
				break;
			case "maven":
				String[] args = source.split(":");
				if (args.length != 3)
					throw new IllegalArgumentException("Invalid resource, maven source format invalid: " +
							source);
				resource = new MavenResource(args[0], args[1], args[2]);
				break;
			case "url":
				try {
					URL url = new URL(source);
					resource = new UrlResource(url);
				} catch(MalformedURLException ex) {
					throw new IllegalArgumentException("Invalid resource, url source format invalid: " +
							source, ex);
				}
				break;
			case "empty":
				resource = new EmptyResource();
				break;
			case "debugger":
			case "instrumentation":
				// Do nothing. Can't be deserialized.
				break;
			default:
				throw new IllegalStateException("Unsupported kind: " + kind);
		}
		if (resource == null)
			throw new IllegalStateException("Failed to load resource: " + kind + "/" + source);
		return resource;
	}


	/**
	 * Serialize non-core attributes.
	 *
	 * @param resource
	 * 		Resource to serialize.
	 * @param jresource
	 * 		Json to append data to.
	 */
	private static void serializeExtras(JavaResource resource, JsonObject jresource) {
		if (resource.getSkippedPrefixes().size() > 0) {
			JsonArray skipped = Json.array(resource.getSkippedPrefixes().toArray(new String[0]));
			jresource.add("skipped", skipped);
		}
		if (resource.getClassSourcePath() != null) {
			jresource.add("attach-src", resource.getClassSourcePath().toAbsolutePath().toString());
		}
		if (resource.getClassDocsPath() != null) {
			jresource.add("attach-docs", resource.getClassDocsPath().toAbsolutePath().toString());
		}
	}


	/**
	 * Deserialize non-core attributes.
	 *
	 * @param resource
	 * 		Resource to derialize.
	 * @param jresource
	 * 		Json to read data from.
	 */
	private static void deserializeExtras(JavaResource resource, JsonObject jresource) throws  IOException {
		if (resource == null)
			return;
		JsonValue value = jresource.get("skipped");
		if (value != null) {
			List<String> skipped = new ArrayList<>();
			value.asArray().forEach(val -> skipped.add(val.asString()));
			resource.setSkippedPrefixes(skipped);
		}
		value = jresource.get("attach-src");
		if (value != null) {
			File src = new File(value.asString());
			if (src.exists())
				resource.setClassSources(src.toPath());
		}
		value = jresource.get("attach-docs");
		if (value != null) {
			File docs = new File(value.asString());
			if (docs.exists())
				resource.setClassDocs(docs.toPath());
		}
	}
}
