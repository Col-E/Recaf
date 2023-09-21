package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.printer.ClassPrinter;
import me.darknet.assembler.printer.JvmClassPrinter;
import me.darknet.assembler.printer.MemberPrinter;
import me.darknet.assembler.printer.Printer;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@ApplicationScoped
public class JvmAssemblerPipeline extends AbstractAssemblerPipeline<JvmClassInfo, JavaClassRepresentation> {

    private final ASTProcessor processor = new ASTProcessor(BytecodeFormat.JVM);
    private final String name = "JVM";

    @Inject
    public JvmAssemblerPipeline(@Nonnull AssemblerPipelineGeneralConfig config) {
        super(config, null);
    }

    @Override
    public Result<List<ASTElement>> concreteParse(List<ASTElement> elements) {
        return processor.processAST(elements);
    }

    @Override
    public Result<List<ASTElement>> pipe(List<Token> tokens, JvmClassInfo info) {
        var result = roughParse(tokens);
        if(result.isOk()) {
            return concreteParse(result.get());
        } else {
            return result;
        }
    }

    @Override
    public Result<JvmClassInfo> assemble(List<ASTElement> elements, JvmClassInfo info) {
        JvmCompiler compiler = new JvmCompiler();
        return null;
    }

    @Override
    public Result<String> disassemble(ClassPathNode node) {
        return classPrinter(node).map(this::print);
    }

    @Override
    public Result<String> disassemble(ClassMemberPathNode node) {
        return memberPrinter(node).map(this::print);
    }

    @Override
    public Result<String> disassemble(AnnotationPathNode node) {
        return annotationPrinter(node).map(this::print);
    }

    @Override
    public JavaClassRepresentation getRepresentation(JvmClassInfo info) {
        return new JavaClassRepresentation(info.getBytecode());
    }

    @Override
    public JvmClassInfo getClassInfo(JavaClassRepresentation representation) {
        return new JvmClassInfoBuilder().withBytecode(representation.data()).build();
    }

    @Override
    protected Result<ClassPrinter> classPrinter(ClassPathNode node) {
        try {
            return Result.ok(new JvmClassPrinter(new ByteArrayInputStream(node.getValue().asJvmClass().getBytecode())));
        } catch (IOException e) {
            return Result.exception(e);
        }
    }

    @Override
    public AssemblerPipelineConfig getConfig() {
        return null;
    }
}
