package dev.xdark.recaf.jdk.resources;

import dev.xdark.recaf.jdk.BootClassLoaderDelegate;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

/**
 * Clientside component of jdk resources tool.
 * 
 * @author xDark
 */
public class JdkResources {
	public static void main(String[] args) {
		Socket socket = new Socket();
		try {
			socket.setSoTimeout(10_000);
			socket.connect(new InetSocketAddress(Integer.parseInt(args[0])));
			socket.setSoTimeout(0);
			byte[] buf = new byte[ToolConstant.TRANSFER_BUFFER];
			BootClassLoaderDelegate delegate = new BootClassLoaderDelegate();
			try (DataInputStream in = new DataInputStream(socket.getInputStream());
				 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
				while (true) {
					byte command = in.readByte();
					switch (command) {
						case ToolConstant.EXIT:
							out.writeByte(ToolConstant.EXIT);
							out.flush();
							return;
						case ToolConstant.RESOURCE_REQUEST:
							String resourcePath = in.readUTF();
							URL url = delegate.getResource(resourcePath);
							if (url == null) {
								out.writeByte(ToolConstant.TRANSFER_RESOURCE_NOT_FOUND);
							} else {
								InputStream resource;
								try {
									resource = url.openStream();
								} catch (IOException ex) {
									out.writeByte(ToolConstant.TRANSFER_BLOCK_ERROR);
									writeError(out, ex);
									break;
								}
								out.writeByte(ToolConstant.TRANSFER_BLOCK_START);
								int toTransfer = ToolConstant.TRANSFER_BUFFER;
								out.writeInt(toTransfer); // Write block size
								try {
									int read;
									while ((read = resource.read(buf, 0, toTransfer)) != -1) {
										// If we consumed whole block but still have data left,
										// send signal to the client.
										if (toTransfer == 0) {
											toTransfer = ToolConstant.TRANSFER_BUFFER;
											out.writeByte(ToolConstant.TRANSFER_BLOCK_OK);
										}
										// Write data to the client
										out.write(buf, 0, read);
										toTransfer -= read;
									}
									if (toTransfer == 0) {
										// If toTransfer is 0, it means that we transferred exactly
										// ToolConstant#TRANSFER_BUFFER*N blocks
										out.writeByte(ToolConstant.TRANSFER_BLOCK_COMPLETE);
									} else {
										// Write padding
										out.write(buf, 0, toTransfer);
										// Tell client that we wrote some padding
										out.writeByte(ToolConstant.TRANSFER_BLOCK_PAD);
										out.writeInt(toTransfer);
									}
								} catch (IOException ex) {
									// Write padding & send TRANSFER_BLOCK_ERROR
									out.write(buf, 0, toTransfer);
									out.writeByte(ToolConstant.TRANSFER_BLOCK_ERROR);
									writeError(out, ex);
								} finally {
									close(resource);
								}
								break;
							}
					}
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	private static void writeError(DataOutputStream dos, Exception ex) throws IOException {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		String error = writer.toString();
		dos.writeUTF(error);
	}

	private static void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException ignored) {
			}
		}
	}
}
