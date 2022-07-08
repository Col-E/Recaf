package me.coley.recaf.mapping.data;

/**
 * Base mapping key.
 *
 * @author xDark
 */
public abstract class AbstractMappingKey implements MappingKey {
    private String text;

    @Override
    public String getAsText() {
        String text = this.text;
        if (text == null) {
            return this.text = toText();
        }
        return text;
    }

    @Override
    public int compareTo(MappingKey o) {
        return getAsText().compareTo(o.getAsText());
    }

    protected abstract String toText();
}
