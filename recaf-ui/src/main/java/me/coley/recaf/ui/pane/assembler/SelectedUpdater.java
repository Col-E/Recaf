package me.coley.recaf.ui.pane.assembler;

import javafx.beans.value.ObservableValue;
import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.AbstractDefinition;
import me.coley.recaf.assemble.pipeline.AssemblerPipeline;
import me.coley.recaf.ui.control.code.bytecode.AssemblerArea;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

public class SelectedUpdater {

    private static final Logger logger = Logging.get(SelectedUpdater.class);
    private final AssemblerPipeline pipeline;
    private final AssemblerArea assemblerArea;

    public SelectedUpdater(AssemblerPipeline pipeline, AssemblerArea assemblerArea) {
        this.pipeline = pipeline;
        this.assemblerArea = assemblerArea;
    }

    public void addCaretPositionListener(ObservableValue<Integer> caretObserver) {
        caretObserver.addListener((observable, oldValue, newValue) -> {
            Unit unit = pipeline.getUnit();
            if (unit == null)
                return;
            if(!unit.isClass())
                return;

            Element elem = unit.getDefinition().getChildAt(newValue);
            if (elem == null)
                return;
            if (!(elem instanceof AbstractDefinition))
                return;

            // finally, update the child
            unit.getDefinitionAsClass().setCurrentDefinition((AbstractDefinition) elem);
        });
    }

}
