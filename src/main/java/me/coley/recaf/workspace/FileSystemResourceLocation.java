package me.coley.recaf.workspace;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Represents file system resource location.
 *
 * @author xxDark
 */
public final class FileSystemResourceLocation extends ResourceLocation {
    private final Path path;

    /**
     * Create the file system location.
     *
     * @param kind kind of the resource.
     * @param path file system path.
     */
    public FileSystemResourceLocation(ResourceKind kind, Path path) {
        super(kind);
        this.path = path;
    }

    @Override
    public ResourceLocation normalize() {
        return new FileSystemResourceLocation(kind(), path.normalize());
    }

    @Override
    public ResourceLocation concat(ResourceLocation other) {
        // We can only concat fs location or literal
        Path path = null;
        if (other instanceof FileSystemResourceLocation) {
            path = ((FileSystemResourceLocation) other).path;
            if (!this.path.getFileSystem().equals(path.getFileSystem())) {
                throw new IllegalStateException("File systems mismatch!");
            }
        } else if (other instanceof LiteralResourceLocation) {
            path = Paths.get(((LiteralResourceLocation) other).getLiteral());
        }
        if (path == null) {
            throw new IllegalArgumentException("Can only concat with file system paths or literals!");
        }
        return new FileSystemResourceLocation(kind(), this.path.resolve(path));
    }

    @Override
    public ResourceLocation toAbsolute() {
        return new FileSystemResourceLocation(kind(), path.toAbsolutePath());
    }

    @Override
    public boolean isAbsolute() {
        return path.isAbsolute();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSystemResourceLocation that = (FileSystemResourceLocation) o;
        return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
