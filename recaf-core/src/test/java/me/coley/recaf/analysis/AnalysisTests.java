package me.coley.recaf.analysis;

import me.coley.recaf.TestUtils;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.source.JarContentSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnalysisTests extends TestUtils {
    @Test
    void test() throws IOException {
        Resource demoGame = new Resource(new JarContentSource(jarsDir.resolve("DemoGame.jar")));
        demoGame.read();

        ClassReferenceAnalyzer analyzer = new ClassReferenceAnalyzer(new Resources(demoGame));

        for (ClassInfo info : demoGame.getClasses().values()) {
            boolean shouldBeUnreferenced = info.getName().startsWith("game/unreferencedclasses");
            boolean isClassUnreferenced = !analyzer.isClassReferenced(info);
            assertEquals(shouldBeUnreferenced, isClassUnreferenced, info.getName());
        }
    }
}
