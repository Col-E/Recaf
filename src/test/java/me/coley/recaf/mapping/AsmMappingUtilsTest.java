package me.coley.recaf.mapping;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the solely String-based methods of {@link AsmMappingUtils}.
 */
public class AsmMappingUtilsTest {
    @Test
    public void testSimpleKeyToKeyTransformation() {
        Map<String, String> inputMapping = new HashMap<>();
        inputMapping.put("calc/Calculator.MAX_DEPTH", "MAX_DEPTH_LEVEL");
        inputMapping.put("calc/Calculator.evaluate(ILjava/lang/String;)D", "doEvaluate");

        Map<String, String> resultMapping = AsmMappingUtils.transformAsmMappingValuesToKeyFormat(inputMapping);

        assertEquals(2, resultMapping.size());
        assertEquals("calc/Calculator.MAX_DEPTH_LEVEL", resultMapping.get("calc/Calculator.MAX_DEPTH"));
        assertEquals("calc/Calculator.doEvaluate(ILjava/lang/String;)D",
                resultMapping.get("calc/Calculator.evaluate(ILjava/lang/String;)D"));
    }

    @Test
    public void testApplyMappingToExistingOnRenamedClass() {
        Map<String, String> aggregateMapping = new HashMap<>();
        aggregateMapping.put("calc/Calculator", "renamed/MyCalc");

        Map<String, String> additionalMapping = new HashMap<>();
        additionalMapping.put("renamed/MyCalc.evaluate(ILjava/lang/String;)D", "doEvaluate");
        additionalMapping.put("renamed/MyCalc.MAX_DEPTH", "MAX_DEPTH_LEVEL");

        AsmMappingUtils.applyMappingToExisting(aggregateMapping, additionalMapping);

        assertEquals(3, aggregateMapping.size());
        assertEquals("renamed/MyCalc", aggregateMapping.get("calc/Calculator"));
        assertEquals("MAX_DEPTH_LEVEL", aggregateMapping.get("calc/Calculator.MAX_DEPTH"));
        assertEquals("doEvaluate", aggregateMapping.get("calc/Calculator.evaluate(ILjava/lang/String;)D"));
    }

    @Test
    public void testApplyMappingToExistingClassRename() {
        Map<String, String> aggregateMapping = new HashMap<>();
        aggregateMapping.put("calc/Calculator.evaluate(ILjava/lang/String;)D", "doEvaluate");
        aggregateMapping.put("calc/Calculator.MAX_DEPTH", "MAX_DEPTH_LEVEL");

        Map<String, String> additionalMapping = new HashMap<>();
        additionalMapping.put("calc/Calculator", "renamed/MyCalc");

        AsmMappingUtils.applyMappingToExisting(aggregateMapping, additionalMapping);

        assertEquals(3, aggregateMapping.size());
        assertEquals("renamed/MyCalc", aggregateMapping.get("calc/Calculator"));
        assertEquals("MAX_DEPTH_LEVEL", aggregateMapping.get("calc/Calculator.MAX_DEPTH"));
        assertEquals("doEvaluate", aggregateMapping.get("calc/Calculator.evaluate(ILjava/lang/String;)D"));
    }

    @Test
    public void testApplyMappingToExistingTransitiveRenames() {
        Map<String, String> aggregateMapping = new HashMap<>();
        aggregateMapping.put("calc/Calculator.evaluate(ILjava/lang/String;)D", "doEvaluate");
        aggregateMapping.put("calc/Calculator.MAX_DEPTH", "MAX_DEPTH_LEVEL");
        aggregateMapping.put("calc/Calculator", "renamed/MyCalc");

        Map<String, String> additionalMapping = new HashMap<>();
        additionalMapping.put("renamed/MyCalc", "renamed2/MyCalc2");

        AsmMappingUtils.applyMappingToExisting(aggregateMapping, additionalMapping);

        assertEquals(3, aggregateMapping.size());
        assertEquals("renamed2/MyCalc2", aggregateMapping.get("calc/Calculator"));
        assertEquals("MAX_DEPTH_LEVEL", aggregateMapping.get("calc/Calculator.MAX_DEPTH"));
        assertEquals("doEvaluate", aggregateMapping.get("calc/Calculator.evaluate(ILjava/lang/String;)D"));
    }
}