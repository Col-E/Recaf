package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;
import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMemberDefinition extends BaseElement implements MemberDefinition{

    Modifiers modifiers;
    List<Annotation> annotations = new ArrayList<>();
    Signature signature;

    @Override
    public Modifiers getModifiers() {
        return modifiers;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(Annotation annotation) {
        annotations.add(annotation);
    }

    public Signature getSignature() {
        return signature;
    }

    public void setModifiers(Modifiers modifiers) {
        this.modifiers = modifiers;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }



}
