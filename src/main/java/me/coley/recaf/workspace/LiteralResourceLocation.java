package me.coley.recaf.workspace;

import java.util.Objects;

/**
 * Represents literal resource location.
 *
 * @author xxDark
 */
public final class LiteralResourceLocation extends ResourceLocation {
    private final String literal;

    /**
     * No public constructions are allowed.
     */
    private LiteralResourceLocation(ResourceKind kind, String literal) {
        super(kind);
        this.literal = literal;
    }

    @Override
    public ResourceLocation normalize() {
        return this;
    }

    @Override
    public ResourceLocation concat(ResourceLocation other) {
        if (!(other instanceof LiteralResourceLocation)) {
            throw new IllegalArgumentException("Cannot concat with non-literal location!");
        }
        return new LiteralResourceLocation(kind(), literal + ((LiteralResourceLocation) other).literal);
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
        LiteralResourceLocation that = (LiteralResourceLocation) o;
        return Objects.equals(literal, that.literal);
    }

    @Override
    public int hashCode() {
        return literal.hashCode();
    }

    @Override
    public String toString() {
        return literal;
    }

    /**
     * @return backing literal.
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * @param kind    kind of the location.
     * @param literal location literal.
     * @return new literal resource location.
     */
    public static ResourceLocation ofKind(ResourceKind kind, String literal) {
        return new LiteralResourceLocation(kind, literal);
    }
}
