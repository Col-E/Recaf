package software.coley.recaf.util.analysis.gen;

import software.coley.recaf.util.analysis.eval.InstanceFactory;
import software.coley.recaf.util.analysis.eval.InstanceMapper;

/**
 * Generator for items in {@link InstanceFactory} to generate {@link InstanceMapper} implementations static constructors for supported types.
 *
 * @author Matt Coley
 */
public class InstanceStaticMapperGenerator extends GenUtils {
	/**
	 * Generator for method definitions.
	 */
	public static void main(String[] args) {
		// TODO: When we add support for types that can be constructed via static factory methods,
		//       add support for generating mappers for those as well.
		//        - Example: List.of(...)
	}
}
