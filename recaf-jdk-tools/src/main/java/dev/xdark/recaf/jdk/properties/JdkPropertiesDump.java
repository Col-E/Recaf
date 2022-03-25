package dev.xdark.recaf.jdk.properties;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * Process entrypoint for {@link JdkProperties}.
 * 
 * @author xDark
 */
public class JdkPropertiesDump {
	public static void main(String[] args) {
		try (DataOutputStream dos = new DataOutputStream(Files.newOutputStream(Paths.get(args[0])))) {
			Properties properties = System.getProperties();
			// Write system properties
			dos.writeShort(properties.size());
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				dos.writeUTF(entry.getKey().toString());
				dos.writeUTF(entry.getValue().toString());
			}
			// Write environment variables
			Map<String, String> env = System.getenv();
			dos.writeShort(env.size());
			for (Map.Entry<String, String> entry : env.entrySet()) {
				dos.writeUTF(entry.getKey());
				dos.writeUTF(entry.getValue());
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
}
