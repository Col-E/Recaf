package me.coley.recaf.workspace;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

public final class UrlResourceLocation extends ResourceLocation {
    private final URL url;

    /**
     * Create resource location.
     *
     * @param kind location kind.
     * @param url  the url provided.
     */
    public UrlResourceLocation(ResourceKind kind, URL url) {
        super(kind);
        this.url = url;
    }

    @Override
    public ResourceLocation normalize() {
        try {
            return new UrlResourceLocation(kind(), toUri().normalize().toURL());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public ResourceLocation concat(ResourceLocation other) {
        return null;
    }

    @Override
    public ResourceLocation toAbsolute() {
        return this;
    }

    @Override
    public boolean isAbsolute() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlResourceLocation that = (UrlResourceLocation) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public String toString() {
        return url.toString();
    }

    /*
     * That is a hacky trick,
     * but there is nothing else I came up with.
     */
    private URI toUri() {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
