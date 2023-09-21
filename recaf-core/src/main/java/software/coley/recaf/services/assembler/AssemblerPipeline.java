package software.coley.recaf.services.assembler;

import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.DeclarationParser;
import me.darknet.assembler.parser.ParsingResult;
import me.darknet.assembler.parser.Token;
import me.darknet.assembler.parser.Tokenizer;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;

import java.util.List;

public interface AssemblerPipeline<C extends ClassInfo, R extends ClassRepresentation> {

    default Result<List<Token>> tokenize(String input, String source) {
          return new Tokenizer().tokenize(source, input);
    }

    default ParsingResult<List<ASTElement>> roughParse(List<Token> tokens) {
         return new DeclarationParser().parseDeclarations(tokens);
    }

    Result<List<ASTElement>> concreteParse(List<ASTElement> elements);

    Result<List<ASTElement>> pipe(List<Token> tokens, C info);

    Result<C> assemble(List<ASTElement> elements, PathNode<?> node);

    Result<String> disassemble(ClassPathNode node);

    Result<String> disassemble(ClassMemberPathNode node);

    Result<String> disassemble(AnnotationPathNode node);

    default Result<String> disassemble(PathNode<?> node) {
        if (node instanceof ClassPathNode classPathNode)
            return disassemble(classPathNode);
        if (node instanceof ClassMemberPathNode classMemberPathNode)
            return disassemble(classMemberPathNode);
        if (node instanceof AnnotationPathNode annotationPathNode)
            return disassemble(annotationPathNode);
        return Result.err(Error.of("Unsupported node type: " + node.getClass().getName(), null));
    }

    R getRepresentation(C info);

    C getClassInfo(R representation);

    AssemblerPipelineConfig getConfig();


}
