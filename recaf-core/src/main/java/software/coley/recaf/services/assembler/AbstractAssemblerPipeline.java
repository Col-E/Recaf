package software.coley.recaf.services.assembler;

import com.google.common.reflect.ClassPath;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.ast.ElementType;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.printer.*;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

import java.util.List;

public abstract class AbstractAssemblerPipeline<C extends ClassInfo, R extends ClassRepresentation>
        implements AssemblerPipeline<C, R> {

    protected final PrintContext<?> context;
    protected final AssemblerPipelineConfig pipelineConfig;

    public AbstractAssemblerPipeline(AssemblerPipelineGeneralConfig config, AssemblerPipelineConfig pipelineConfig) {
        this.context = new PrintContext<>(config.getDisassemblyIndent().getValue());
        this.pipelineConfig = pipelineConfig;

        config.getDisassemblyIndent().addChangeListener((observable, oldVal, newVal) -> context.setIndentStep(newVal));
    }

    protected abstract Result<ClassPrinter> classPrinter(ClassPathNode location);
    protected abstract CompilerOptions<? extends CompilerOptions<?>> getCompilerOptions();
    protected abstract Compiler getCompiler();
    protected abstract InheritanceChecker getInheritanceChecker();
    protected abstract int getClassVersion(C info);

    protected Result<C> compile(List<ASTElement> elements, PathNode<?> node) {
        if(elements.isEmpty()) {
            return Result.err(Error.of("No elements to compile", null));
        }
        if(elements.size() != 1) {
            return Result.err(Error.of("Multiple elements to compile", elements.get(1).location()));
        }
        ASTElement element = elements.get(0);

        if(element == null) {
            return Result.err(Error.of("No element to compile", null));
        }

        ClassInfo classInfo = node.getValueOfType(ClassInfo.class);
        if(classInfo == null) {
            return Result.err(Error.of("Dangling member", null));
        }

        C info = (C) classInfo;

        CompilerOptions<? extends CompilerOptions<?>> options = getCompilerOptions();
        options.version(getClassVersion(info))
                .inheritanceChecker(getInheritanceChecker());

        if(element.type() != ElementType.CLASS) {
            options.overlay(getRepresentation(info));

            if(element.type() == ElementType.ANNOTATION) {
                // build annotation path
                String path = "this";
                PathNode<?> parent = node.getParent();
                if(parent instanceof ClassMemberPathNode memberPathNode) {
                    path += memberPathNode.isMethod() ? ".method." : ".field.";
                    path += memberPathNode.getValue().getName() + ".";
                    path += memberPathNode.getValue().getDescriptor();
                }

                Annotated annotated = node.getValueOfType(Annotated.class);

                if(annotated == null) {
                    return Result.err(Error.of("Dangling annotation", null));
                }

                AnnotationInfo annotation = (AnnotationInfo) node.getValue();

                path += annotated.getAnnotations().indexOf(annotation);

                options.annotationPath(path);

            }
        }

        Compiler compiler = getCompiler();

        return compiler.compile(elements, options).flatMap(res -> Result.ok(getClassInfo((R) res)));
    }

    protected Result<Printer> memberPrinter(ClassMemberPathNode node) {
        ClassPathNode owner = node.getParent();
        if(owner == null)
            return Result.err(Error.of("Dangling member", null));

        ClassMember member = node.getValue();
        return classPrinter(owner).flatMap((printer) -> {
            Printer memberPrinter = null;

            if(member.isMethod()) {
                memberPrinter = printer.method(member.getName(), member.getDescriptor());
            } else if(member.isField()) {
                memberPrinter = printer.field(member.getName(), member.getDescriptor());
            }

            if(memberPrinter == null) {
                return Result.err(Error.of("Failed to find member", null));
            } else {
                return Result.ok(memberPrinter);
            }
        });
    }

    protected Result<AnnotationPrinter> annotationPrinter(AnnotationPathNode node) {
        if(node.getParent() == null) {
            return Result.err(Error.of("Dangling annotation", null));
        }

        Object parent = node.getParent().getValue();
        Result<? extends Printer> parentPrinter;
        if(parent instanceof ClassPathNode classNode) {
            parentPrinter = classPrinter(classNode);
        } else if(parent instanceof ClassMemberPathNode classMember) {
            parentPrinter = memberPrinter(classMember);
        } else {
            return Result.err(Error.of("Invalid parent type", null));
        }

        AnnotationInfo annotation = node.getValue();

        if(parent instanceof Annotated annotated) {

            return parentPrinter.flatMap((printer) -> {
                if (printer instanceof AnnotationHolder holder) {
                    return Result.ok(holder.annotation(annotated.getAnnotations().indexOf(annotation)));
                } else {
                    return Result.err(Error.of("Parent is not an annotation holder", null));
                }
            });

        } else {
            return Result.err(Error.of("Parent cannot hold annotations", null));
        }
    }

    protected String print(Printer printer) {
        context.clear();
        printer.print(context);
        return context.toString();
    }

    @Override
    public AssemblerPipelineConfig getConfig() {
        return pipelineConfig;
    }



}
