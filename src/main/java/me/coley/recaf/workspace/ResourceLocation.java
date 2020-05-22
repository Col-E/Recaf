package me.coley.recaf.workspace;

/**
 * Location of a Java resource.
 *
 * @author xxDark
 */
public abstract class ResourceLocation {
    private final ResourceKind kind;

    /**
     * Create resource location.
     *
     * @param kind location kind.
     */
    public ResourceLocation(ResourceKind kind) {
        this.kind = kind;
    }

    /**
     * @return Type of referenced resource.
     */
    public ResourceKind kind() {
        return kind;
    }

    /**
     * @return {@link ResourceLocation} with redundant name elements eliminated.
     */
    public abstract ResourceLocation normalize();

    /**
     * @param other other location.
     * @return result of concating two locations together.
     */
    public abstract ResourceLocation concat(ResourceLocation other);

    /**
     * @return absolute location of this location.
     */
    public abstract ResourceLocation toAbsolute();

    /**
     * @return {@code true} if this location is absolute,
     * {@code false} otherwise.
     */
    public abstract boolean isAbsolute();

    @Override
    public abstract boolean equals(Object other);

    @Override
    public abstract String toString();
}
