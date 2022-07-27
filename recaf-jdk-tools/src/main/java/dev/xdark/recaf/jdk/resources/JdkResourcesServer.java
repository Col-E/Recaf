package dev.xdark.recaf.jdk.resources;

import dev.xdark.recaf.jdk.BootClassLoaderDelegate;
import dev.xdark.recaf.jdk.ToolHelper;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Serverside component of jdk resources tool.
 *
 * @author xDark
 */
public class JdkResourcesServer {
	private static Path classpathJar;

	private final ServerSocket server;
	private final Socket client;
	private final StreamPair streamPair;
	private final Process process;

	private JdkResourcesServer(ServerSocket server, Socket client, Process process) throws IOException {
		this.server = server;
		this.client = client;
		this.process = process;
		streamPair = new StreamPair(client.getInputStream(), client.getOutputStream());
	}

	/**
	 * Requestes resource from remote JVM.
	 *
	 * @param resourcePath
	 * 		Remote resource path.
	 *
	 * @return resource content or {@code null},
	 * if not found.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public byte[] requestResource(String resourcePath) throws IOException {
		StreamPair streamPair = this.streamPair;
		DataOutputStream out = streamPair.out;
		out.writeByte(ToolConstant.RESOURCE_REQUEST);
		out.writeUTF(resourcePath);
		out.flush();
		DataInputStream in = streamPair.in;
		byte response = in.readByte();
		switch (response) {
			case ToolConstant.TRANSFER_RESOURCE_NOT_FOUND:
				return null;
			case ToolConstant.TRANSFER_BLOCK_ERROR:
				throw new IllegalStateException(in.readUTF());
			case ToolConstant.TRANSFER_BLOCK_START:
				int blockSize = in.readInt();
				ServerOutputData outputData = new ServerOutputData();
				byte[] block = new byte[blockSize];
				int toTransfer = blockSize;
				while (true) {
					if (toTransfer == 0) {
						byte state = in.readByte();
						switch (state) {
							case ToolConstant.TRANSFER_BLOCK_OK:
								// Continue reading
								toTransfer = blockSize;
								break;
							case ToolConstant.TRANSFER_BLOCK_ERROR:
								throw new IllegalStateException(in.readUTF());
							case ToolConstant.TRANSFER_BLOCK_COMPLETE:
								return outputData.getData();
							case ToolConstant.TRANSFER_BLOCK_PAD:
								int padding = in.readInt();
								outputData.removePadding(padding);
								return outputData.getData();
							default:
								shutdown();
								throw new IllegalStateException("Unknown state: " + state);
						}
					}
					int read = in.read(block, 0, toTransfer);
					if (read > 0) {
						outputData.write(block, 0, read);
						toTransfer -= read;
					}
				}
			default:
				shutdown();
				throw new IllegalStateException("Unknown command: " + response);
		}
	}

	/**
	 * Shuts down server.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public void shutdown() throws IOException {
		StreamPair streamPair = this.streamPair;
		try {
			DataOutputStream out = streamPair.out;
			out.writeByte(ToolConstant.EXIT);
			out.flush();
		} finally {
			close(streamPair.out);
			close(streamPair.in);
			close(client);
			close(server);
			// No point in keeping process alive
			Process process = this.process;
			if (process != null) {
				process.destroy();
			}
		}
	}

	/**
	 * @param port
	 * 		Remote port.
	 *
	 * @return created server.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public static JdkResourcesServer open(int port) throws IOException {
		ServerSocket socket = new ServerSocket();
		socket.bind(new InetSocketAddress(port));
		socket.setSoTimeout(10_000);
		Socket client1 = socket.accept();
		socket.setSoTimeout(0);
		return new JdkResourcesServer(socket, client1, null);
	}

	public static JdkResourcesServer start(Path jdkExecutable) throws IOException {
		if (!Files.isRegularFile(jdkExecutable)) {
			throw new IllegalStateException("JDK executable does not exist");
		}
		// Prepare jar
		Path classpathJar = JdkResourcesServer.classpathJar;
		if (classpathJar == null) {
			classpathJar = Files.createTempFile("recaf-jdk-resources", ".jar");
			classpathJar.toFile().deleteOnExit();
			JdkResourcesServer.classpathJar = classpathJar;
			ToolHelper.prepareJar(classpathJar, Arrays.asList(JdkResources.class, BootClassLoaderDelegate.class));
		}
		// Find open port
		ServerSocket socket = new ServerSocket(0);
		socket.setSoTimeout(10_000);
		// Prepare process
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(jdkExecutable.getParent().toFile());
		builder.command(jdkExecutable.toString(),
				"-cp", classpathJar.toString(),
				JdkResources.class.getName(), Integer.toString(socket.getLocalPort()));
		Process process = builder.start();
		Socket client = socket.accept();
		socket.setSoTimeout(0);
		return new JdkResourcesServer(socket, client, process);
	}

	private static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static final class StreamPair {
		final DataInputStream in;
		final DataOutputStream out;

		StreamPair(InputStream in, OutputStream out) {
			this.in = new DataInputStream(in);
			this.out = new DataOutputStream(out);
		}
	}

	private static final class ServerOutputData extends ByteArrayOutputStream {
		void removePadding(int padding) {
			count -= padding;
		}

		byte[] getData() {
			byte[] buf = this.buf;
			int count = this.count;
			return buf.length == count ? buf : Arrays.copyOf(buf, count);
		}
	}
}
