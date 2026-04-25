package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.code.Code;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.util.collect.primitive.Object2IntMap;

import java.util.Set;

/**
 * Summary of method features used for similarity comparison.
 *
 * @param parameterTypes
 * 		Parameter types of the method.
 * @param returnType
 * 		Returned type of the method.
 * @param thrownTypes
 * 		Thrown types of the method.
 * @param trigrams
 * 		Trigrams of normalized instructions.
 * @param controlFlowVector
 * 		Vector of control-flow metrics in the following order:
 * 			<ol>
 * 			   <li>{@code blockCount}</li>
 * 			   <li>{@code edgeCount}</li>
 * 			   <li>{@code branchCount}</li>
 * 			   <li>{@code switchCount}</li>
 * 			   <li>{@code handlerCount}</li>
 * 			   <li>{@code exitCount}</li>
 * 			   <li>{@code cyclomaticComplexity}</li>
 * 			</ol>
 * 		See {@link MethodInstructionNormalizer#computeControlFlowVector(MethodNode)}
 * 		and {@link MethodInstructionNormalizer#computeControlFlowVector(Code)}.
 *
 * @author Matt Coley
 */
public record MethodFingerprint(@Nonnull Type[] parameterTypes,
                                @Nonnull Type returnType,
                                @Nonnull Set<String> thrownTypes,
                                @Nonnull Object2IntMap<String> trigrams,
                                @Nonnull long[] controlFlowVector) {}
