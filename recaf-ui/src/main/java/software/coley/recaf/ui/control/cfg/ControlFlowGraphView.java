package software.coley.recaf.ui.control.cfg;

import com.google.common.collect.HashBiMap;
import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxRectangleShape;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;
import dev.xdark.blw.asm.AsmBytecodeLibrary;
import dev.xdark.blw.asm.ClassWriterProvider;
import dev.xdark.blw.asm.internal.Util;
import dev.xdark.blw.classfile.ClassBuilder;
import dev.xdark.blw.classfile.Method;
import dev.xdark.blw.classfile.generic.GenericClassBuilder;
import dev.xdark.blw.classfile.generic.GenericMethod;
import dev.xdark.blw.code.CodeElement;
import dev.xdark.blw.code.generic.GenericCode;
import dev.xdark.blw.code.generic.GenericLabel;
import dev.xdark.blw.code.instruction.*;
import dev.xdark.blw.type.MethodType;
import dev.xdark.blw.type.Types;
import dev.xdark.blw.util.Reflectable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import me.darknet.assembler.compile.analysis.jvm.IndexedStraightforwardSimulation;
import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.printer.InstructionPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import software.coley.recaf.services.cfg.ControlFlowGraph;
import software.coley.recaf.services.cfg.ControlFlowVertex;
import software.coley.recaf.services.cfg.ControlFlowVertexReference;
import software.coley.recaf.ui.control.cfg.layout.PatchedHierarchicalLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.List;

@Dependent
public class ControlFlowGraphView {

    final ControlFlowHighlighter highlighter;

    @Inject
    public ControlFlowGraphView(ControlFlowHighlighter highlighter) {
        this.highlighter = highlighter;
    }

    public mxGraph createGraph(ControlFlowGraph cfg) {
        mxGraph graph = new mxGraph();
        graph.setHtmlLabels(true);
        graph.setAutoOrigin(true);
        graph.setAutoSizeCells(true);
        graph.setAllowLoops(true);
        graph.setAllowDanglingEdges(true);
        this.setStyles(graph);

        graph.getModel().beginUpdate();
        HashMap<ControlFlowVertex, Object> cells = new HashMap<>();
        Map<ControlFlowVertex, String> views = this.createVerticesViews(cfg);
        for (ControlFlowVertex vertex : cfg.getVertices()) {
            Object cell = graph.insertVertex(graph.getDefaultParent(), null, views.get(vertex),
                    10, 10, 80, 80, this.getCellStyle());
            graph.updateCellSize(cell);
            cells.put(vertex, cell);
        }

        for (ControlFlowVertex vertex : cfg.getVertices()) {
            Object cell = cells.get(vertex);

            for (ControlFlowVertexReference ref : vertex.getOutRefs()) {
                Object refCell = cells.get(ref.getVertex());
                if (refCell == null) {
                    continue;
                }

                graph.insertEdge(graph.getDefaultParent(), null, "", cell, refCell, this.getEdgeStyle(vertex, ref));
            }
        }

        PatchedHierarchicalLayout layout = new PatchedHierarchicalLayout(graph);
        layout.setFineTuning(true);
        layout.setIntraCellSpacing(20d);
        layout.setInterRankCellSpacing(50d);
        layout.setDisableEdgeStyle(true);
        layout.setParallelEdgeSpacing(100d);
        layout.setUseBoundingBox(true);
        layout.execute(graph.getDefaultParent());

        graph.getModel().endUpdate();

        return graph;
    }

    void setStyles(mxGraph graph) {
        Map<String, Object> edgeStyle = graph.getStylesheet().getDefaultEdgeStyle();
        edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
        edgeStyle.put(mxConstants.STYLE_ELBOW, mxConstants.ELBOW_VERTICAL);
        edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_DIAMOND);
        edgeStyle.put(mxConstants.STYLE_TARGET_PERIMETER_SPACING, 1d);
        edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1.25d);

        Map<String, Object> vertexStyle = graph.getStylesheet().getDefaultVertexStyle();
        vertexStyle.put(mxConstants.STYLE_AUTOSIZE, 1);
        vertexStyle.put(mxConstants.STYLE_SPACING, "5");
        vertexStyle.put(mxConstants.STYLE_ORTHOGONAL, "true");
        vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
        vertexStyle.put(mxConstants.STYLE_ARCSIZE, 5);
        vertexStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
        mxGraphics2DCanvas.putShape(mxConstants.SHAPE_RECTANGLE, new mxRectangleShape() {
            @Override
            protected int getArcSize(mxCellState state, double w, double h) {
                return 10;
            }
        });
        mxStylesheet stylesheet = new mxStylesheet();
        stylesheet.setDefaultEdgeStyle(edgeStyle);
        stylesheet.setDefaultVertexStyle(vertexStyle);

        graph.setStylesheet(stylesheet);
    }

    Map<ControlFlowVertex, String> createVerticesViews(ControlFlowGraph graph) {
        Map<ControlFlowVertex, String> views = new HashMap<>();
        Map<Integer, String> labelNames = new HashMap<>();
        Map<LabelNode, Integer> labelIndexes = new HashMap<>();

        int idx = 1;
        for (AbstractInsnNode insn : graph.getMethod().instructions.toArray()) {
            if (insn.getType() == AbstractInsnNode.LABEL) {
                labelIndexes.put((LabelNode) insn, idx);
                labelNames.put(idx, "label" + idx);
                idx++;
            }
        }

        IndexedStraightforwardSimulation simulation = new IndexedStraightforwardSimulation();
        for (ControlFlowVertex vertex : graph.getVertices()) {
            List<CodeElement> elements = vertex.getInsns()
                    .stream()
                    .map(insn -> liftInsn(insn, labelIndexes))
                    .filter(Objects::nonNull)
                    .toList();

            GenericCode code = new GenericCode(0, 0, elements, List.of(), List.of());
            GenericMethod method = new GenericMethod(0, null, null, null, null,
                    null, code, null, null, null);

            StringWriter writer = new StringWriter();
            PrintContext<PrintContext<?>> printContext = new PrintContext<>(" ", writer);

            InstructionPrinter printer = new InstructionPrinter(new PrintContext.CodePrint(printContext),
                    method.code(), new Variables(new TreeMap<>(), List.of()), labelNames);
            simulation.execute(printer, method);

            String view = this.highlighter.highlightToHtml("jasm", writer.toString());
            views.put(vertex, view);
        }


        return views;
    }

    String getCellStyle() {
        return "fillColor=#2f343d;fontColor=#F4FEFF;strokeColor=#343A44";
    }

    String getEdgeStyle(ControlFlowVertex vertex, ControlFlowVertexReference ref) {
        return "strokeColor=" + getEdgeColor(vertex, ref);
    }

    String getEdgeColor(ControlFlowVertex vertex, ControlFlowVertexReference ref) {
        switch (ref.getKind()) {
            case JUMP:
                return "#009231";
            case LABEL:
            case SWITCH_DEFAULT:
                return "#e63ce0";
            case GOTO: {
                if (vertex.getOutRefs().size() == 1) {
                    return "#777c85";
                }
                return "#ea2027";
            }
            case SWITCH:
                return "ffc312";
            default:
                return "#777c85";
        }
    }

    static CodeElement liftInsn(AbstractInsnNode insn, Map<LabelNode, Integer> labelIndexes) {
        if (insn instanceof LdcInsnNode ldc) {
            return Util.wrapLdcInsn(ldc.cst);
        } else if (insn instanceof MethodInsnNode min) {
            return Util.wrapMethodInsn(min.getOpcode(), min.owner, min.name, min.desc, false);
        } else if (insn instanceof FieldInsnNode fin) {
            return Util.wrapFieldInsn(fin.getOpcode(), fin.owner, fin.name, fin.desc);
        } else if (insn instanceof TypeInsnNode tin) {
            return Util.wrapTypeInsn(tin.getOpcode(), tin.desc);
        } else if (insn instanceof IntInsnNode iin) {
            return Util.wrapIntInsn(iin.getOpcode(), iin.operand);
        } else if (insn instanceof InsnNode in) {
            return Util.wrapInsn(in.getOpcode());
        } else if (insn instanceof InvokeDynamicInsnNode indy) {
            return Util.wrapInvokeDynamicInsn(indy.name, indy.desc, indy.bsm, indy.bsmArgs);
        } else if (insn instanceof IincInsnNode iin) {
            return new VariableIncrementInstruction(iin.var, iin.incr);
        } else if (insn instanceof LabelNode labelNode) {
            return getLabel(labelNode, labelIndexes);
        } else if (insn instanceof LineNumberNode line) {
            getLabel(line.start, labelIndexes).setLineNumber(line.line);
        } else if (insn instanceof JumpInsnNode jump) {
            if (jump.getOpcode() == Opcodes.GOTO) {
                return new ImmediateJumpInstruction(Opcodes.GOTO, getLabel(jump.label, labelIndexes));
            } else {
                return new ConditionalJumpInstruction(jump.getOpcode(), getLabel(jump.label, labelIndexes));
            }
        } else if (insn instanceof LookupSwitchInsnNode lsinsn) {
            return new LookupSwitchInstruction(
                    lsinsn.keys.stream().mapToInt(it -> it).toArray(),
                    getLabel(lsinsn.dflt, labelIndexes),
                    lsinsn.labels.stream().map(it -> ControlFlowGraphView.getLabel(it, labelIndexes)).toList()
            );
        } else if (insn instanceof TableSwitchInsnNode tsinsn) {
            return new TableSwitchInstruction(
                    tsinsn.min,
                    getLabel(tsinsn.dflt, labelIndexes),
                    tsinsn.labels.stream().map(it -> ControlFlowGraphView.getLabel(it, labelIndexes)).toList()
            );
        } else if (insn instanceof MultiANewArrayInsnNode manainsn) {
            return new AllocateMultiDimArrayInstruction(Types.arrayTypeFromDescriptor(manainsn.desc), manainsn.dims);
        } else if (insn instanceof VarInsnNode varinsn) {
            return new VarInstruction(varinsn.getOpcode(), varinsn.var);
        }

        return null;
    }

    static dev.xdark.blw.code.Label getLabel(LabelNode label, Map<LabelNode, Integer> labelIndexes) {
        GenericLabel genericLabel = new GenericLabel();
        genericLabel.setIndex(labelIndexes.getOrDefault(label, -1));
        return genericLabel;
    }

}
