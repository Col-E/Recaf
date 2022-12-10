package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAttributable extends BaseElement implements Attributable {

    private final List<Annotation> annotations = new ArrayList<>();
    private Signature signature;
    private boolean deprecated;

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations.clear();
        this.annotations.addAll(annotations);
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public void addAnnotation(Annotation annotation) {
        annotations.add(annotation);
    }

    protected String buildDefString(PrintContext context) {
        StringBuilder sb = new StringBuilder();
        if (deprecated)
            sb.append(context.fmtKeyword("deprecated")).append("\n");
        if (signature != null)
            sb.append(getSignature().print(context));
        for (Annotation annotation : annotations)
            sb.append(annotation.print(context));
        return sb.toString();
    }

}
