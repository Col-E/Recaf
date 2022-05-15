package me.coley.recaf.assemble.transformer;

import me.coley.recaf.assemble.ast.arch.Annotation;
import me.coley.recaf.assemble.ast.arch.ConstVal;
import me.coley.recaf.assemble.ast.arch.ThrownException;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold attributes about {@link me.coley.recaf.assemble.ast.arch.MemberDefinition}s that appear before
 * the actual definition.
 */
public class Attributes {

    private Signature signature;
    private final List<ThrownException> thrownExceptions = new ArrayList<>();
    private final List<Annotation> annotations = new ArrayList<>();


    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public List<ThrownException> getThrownExceptions() {
        return thrownExceptions;
    }

    public void addThrownException(ThrownException exception) {
        thrownExceptions.add(exception);
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(Annotation annotation) {
        annotations.add(annotation);
    }

    public void clear() {
        signature = null;
        thrownExceptions.clear();
        annotations.clear();
    }

}
