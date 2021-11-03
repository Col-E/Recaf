package me.coley.recaf.decompile.mapleir.ripped;

import com.google.common.collect.Iterators;
import org.mapleir.flowgraph.ExceptionRange;
import org.mapleir.flowgraph.FlowGraph;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.code.Expr;
import org.mapleir.ir.code.Opcode;
import org.mapleir.ir.code.Stmt;
import org.mapleir.ir.code.expr.*;
import org.mapleir.ir.code.expr.invoke.DynamicInvocationExpr;
import org.mapleir.ir.code.expr.invoke.InitialisedObjectExpr;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.ir.code.stmt.*;
import org.mapleir.ir.code.stmt.copy.AbstractCopyStmt;
import org.mapleir.stdlib.util.*;
import org.mapleir.stdlib.collections.graph.algorithms.*;
import org.mapleir.ir.cfg.*;
import org.mapleir.stdlib.collections.graph.*;
import org.mapleir.flowgraph.edges.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author cts/rcx
 */
public class MapleIrInstructionPrinter {
    private final org.mapleir.asm.MethodNode mapleNode;
    private final MethodNode mNode;
    private final MapleIrTypeAndName[] args;

    public MapleIrInstructionPrinter(org.mapleir.asm.MethodNode mapleNode, MapleIrTypeAndName[] args) {
        this.mapleNode = mapleNode;
        this.mNode = mapleNode.node;
        this.args = args;
    }

    public ArrayList<String> createPrint() {
        final ControlFlowGraph cfg = ControlFlowGraphBuilder.build(mapleNode);
        /*if (this.parent.getParent().getSettings().getEntry("simplify-arithmetic").getBool()) {
            this.deobfuscator.simplifyArithmetic(cfg);
        }
        if (this.parent.getParent().getSettings().getEntry("kill-dead-code").getBool()) {
            this.deobfuscator.killDeadCode(cfg);
        }*/
        BoissinotDestructor.leaveSSA(cfg);
        LocalsReallocator.realloc(cfg);
        final TabbedStringWriter sw = new TabbedStringWriter();
        this.printCode(sw, cfg);
        return new ArrayList<String>(Arrays.asList(sw.toString().split(System.lineSeparator())));
    }

    private void printCode(final TabbedStringWriter sw, final ControlFlowGraph cfg) {
        final List<BasicBlock> verticesInOrder = new ExtendedDfs<BasicBlock>(cfg, 64) {
            @Override
            protected Iterable<? extends FastGraphEdge<BasicBlock>> order(final Set<? extends FastGraphEdge<BasicBlock>> edges) {
                return () -> Iterators.concat((Iterator)edges.stream().filter(e -> !(e instanceof ImmediateEdge)).iterator(), (Iterator)edges.stream().filter(ImmediateEdge.class::isInstance).iterator());
            }
        }.run(cfg.getEntries().iterator().next()).getTopoOrder();
        for (final BasicBlock b : verticesInOrder) {
            this.printBlock(sw, b);
        }
    }

    private void printBlock(final TabbedStringWriter sw, final BasicBlock b) {
        sw.print(b.getDisplayName()).print(":");
        int handlerCount = 0;
        for (final ExceptionRange<BasicBlock> erange : b.getGraph().getRanges()) {
            if (erange.containsVertex(b)) {
                if (handlerCount++ > 0) {
                    sw.newline();
                }
                else {
                    sw.print(" ");
                }
                sw.print("// Exception handler: Block ").print(erange.getHandler().getDisplayName()).print(" [");
                int typeCount = 0;
                for (final Type exceptionType : erange.getTypes()) {
                    sw.print(exceptionType.getClassName());
                    if (++typeCount != erange.getTypes().size()) {
                        sw.print(", ");
                    }
                }
                sw.print("]");
            }
        }
        sw.tab();
        for (final Stmt stmt : b) {
            sw.newline();
            this.printStmt(sw, stmt);
        }
        sw.untab();
        sw.newline();
    }

    public void printStmt(final TabbedStringWriter sw, final Stmt stmt) {
        final int opcode = stmt.getOpcode();
        switch (opcode) {
            case Opcode.LOCAL_STORE:
            case Opcode.PHI_STORE: {
                final AbstractCopyStmt cvs = (AbstractCopyStmt)stmt;
                this.printExpr(sw, cvs.getVariable());
                sw.print(" = ");
                if (cvs.isSynthetic()) {
                    final int varIndex = cvs.getVariable().getIndex();
                    if (varIndex == 0 && !Modifier.isStatic(this.mNode.access)) {
                        sw.print("this");
                    }
                    else {
                        final int refIndex = varIndex - (Modifier.isStatic(this.mNode.access) ? 0 : 1);
                        if (refIndex >= 0 && refIndex < this.args.length) {
                            sw.print(this.args[refIndex].name);
                        }
                    }
                    break;
                }
                this.printExpr(sw, cvs.getExpression());
                break;
            }
            case Opcode.ARRAY_STORE: {
                final ArrayStoreStmt ars = (ArrayStoreStmt)stmt;
                final Expr arrayExpr = ars.getArrayExpression();
                final Expr indexExpr = ars.getIndexExpression();
                final Expr valexpr = ars.getValueExpression();
                final int accessPriority = Expr.Precedence.ARRAY_ACCESS.ordinal();
                final int basePriority = arrayExpr.getPrecedence();
                if (basePriority > accessPriority) {
                    sw.print('(');
                }
                this.printExpr(sw, arrayExpr);
                if (basePriority > accessPriority) {
                    sw.print(')');
                }
                sw.print('[');
                this.printExpr(sw, indexExpr);
                sw.print(']');
                sw.print(" = ");
                this.printExpr(sw, valexpr);
                break;
            }
            case Opcode.FIELD_STORE: {
                final FieldStoreStmt fss = (FieldStoreStmt)stmt;
                final Expr valExpr = fss.getValueExpression();
                if (!fss.isStatic()) {
                    final int selfPriority = Expr.Precedence.MEMBER_ACCESS.ordinal();
                    final Expr instanceExpr = fss.getInstanceExpression();
                    final int basePriority2 = instanceExpr.getPrecedence();
                    if (basePriority2 > selfPriority) {
                        sw.print('(');
                    }
                    this.printExpr(sw, instanceExpr);
                    if (basePriority2 > selfPriority) {
                        sw.print(')');
                    }
                }
                else {
                    sw.print(fss.getOwner().replace("/", "."));
                }
                sw.print('.');
                sw.print(fss.getName());
                sw.print(" = ");
                this.printExpr(sw, valExpr);
                break;
            }
            case Opcode.COND_JUMP: {
                final ConditionalJumpStmt cjs = (ConditionalJumpStmt)stmt;
                sw.print("if (");
                this.printExpr(sw, cjs.getLeft());
                sw.print(" ").print(cjs.getComparisonType().getSign()).print(" ");
                this.printExpr(sw, cjs.getRight());
                sw.print(")");
                sw.tab().newline().print("goto ").print(cjs.getTrueSuccessor().getDisplayName()).untab();
                break;
            }
            case Opcode.UNCOND_JUMP: {
                final UnconditionalJumpStmt ujs = (UnconditionalJumpStmt)stmt;
                sw.print("goto ").print(ujs.getTarget().getDisplayName());
                break;
            }
            case Opcode.THROW: {
                final ThrowStmt ts = (ThrowStmt)stmt;
                sw.print("throw ");
                this.printExpr(sw, ts.getExpression());
                break;
            }
            case Opcode.MONITOR: {
                final MonitorStmt ms = (MonitorStmt)stmt;
                switch (ms.getMode()) {
                    case ENTER: {
                        sw.print("synchronized_enter ");
                        break;
                    }
                    case EXIT: {
                        sw.print("synchronized_exit ");
                        break;
                    }
                }
                this.printExpr(sw, ms.getExpression());
                break;
            }
            case Opcode.POP: {
                final PopStmt ps = (PopStmt)stmt;
                this.printExpr(sw, ps.getExpression());
                break;
            }
            case Opcode.RETURN: {
                final ReturnStmt rs = (ReturnStmt)stmt;
                sw.print("return");
                if (rs.getExpression() != null) {
                    sw.print(" ");
                    this.printExpr(sw, rs.getExpression());
                    break;
                }
                break;
            }
            case Opcode.SWITCH_JUMP: {
                final SwitchStmt ss = (SwitchStmt)stmt;
                sw.print("switch(");
                this.printExpr(sw, ss.getExpression());
                sw.print(") {");
                sw.tab();
                for (final Map.Entry<Integer, BasicBlock> e : ss.getTargets().entrySet()) {
                    sw.newline().print("case ").print(String.valueOf(e.getKey())).print(": goto ").print(e.getValue().getDisplayName());
                }
                sw.newline().print(".default: goto ").print(ss.getDefaultTarget().getDisplayName());
                sw.untab().newline().print("}");
                break;
            }
            default: {
                throw new UnsupportedOperationException("Got: " + Opcode.opname(opcode));
            }
        }
    }

    private String literalToString(final Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof String) {
            return "\"" + o.toString() + "\"";
        }
        return o.toString();
    }

    private void printExpr(final TabbedStringWriter sw, final Expr e) {
        final int opcode = e.getOpcode();
        switch (opcode) {
            case Opcode.CONST_LOAD: {
                final ConstantExpr ce = (ConstantExpr)e;
                sw.print(this.literalToString(ce.getConstant()));
                break;
            }
            case Opcode.LOCAL_LOAD: {
                final VarExpr ve = (VarExpr)e;
                sw.print(ve.getLocal().toString());
                break;
            }
            case Opcode.FIELD_LOAD: {
                final FieldLoadExpr fle = (FieldLoadExpr)e;
                if (fle.isStatic()) {
                    sw.print(fle.getOwner().replace("/", "."));
                }
                else {
                    final Expr instanceExpr = fle.getInstanceExpression();
                    final int selfPriority = fle.getPrecedence();
                    final int basePriority = instanceExpr.getPrecedence();
                    if (basePriority > selfPriority) {
                        sw.print('(');
                    }
                    this.printExpr(sw, instanceExpr);
                    if (basePriority > selfPriority) {
                        sw.print(')');
                    }
                }
                sw.print(".").print(fle.getName());
                break;
            }
            case Opcode.ARRAY_LOAD: {
                final ArrayLoadExpr ale = (ArrayLoadExpr)e;
                final Expr arrayExpr = ale.getArrayExpression();
                final Expr indexExpr = ale.getIndexExpression();
                final int selfPriority2 = ale.getPrecedence();
                final int expressionPriority = arrayExpr.getPrecedence();
                if (expressionPriority > selfPriority2) {
                    sw.print('(');
                }
                this.printExpr(sw, arrayExpr);
                if (expressionPriority > selfPriority2) {
                    sw.print(')');
                }
                sw.print('[');
                this.printExpr(sw, indexExpr);
                sw.print(']');
                break;
            }
            case Opcode.INVOKE: {
                final InvocationExpr ie = (InvocationExpr)e;
                if (ie.isDynamic()) {
                    sw.print("dynamic_invoke<");
                    sw.print(((DynamicInvocationExpr)ie).getProvidedFuncType().getClassName().replace("/", "."));
                    sw.print(">(");
                }
                if (ie.isStatic()) {
                    sw.print(ie.getOwner().replace("/", "."));
                }
                else {
                    final int memberAccessPriority = Expr.Precedence.MEMBER_ACCESS.ordinal();
                    final Expr instanceExpression = ie.getPhysicalReceiver();
                    final int instancePriority = instanceExpression.getPrecedence();
                    if (instancePriority > memberAccessPriority) {
                        sw.print('(');
                    }
                    this.printExpr(sw, instanceExpression);
                    if (instancePriority > memberAccessPriority) {
                        sw.print(')');
                    }
                }
                sw.print('.').print(ie.getName()).print('(');
                final Expr[] args = ie.getPrintedArgs();
                for (int i = 0; i < args.length; ++i) {
                    this.printExpr(sw, args[i]);
                    if (i + 1 < args.length) {
                        sw.print(", ");
                    }
                }
                sw.print(')');
                if (ie.isDynamic()) {
                    sw.print(')');
                    break;
                }
                break;
            }
            case Opcode.ARITHMETIC: {
                final ArithmeticExpr ae = (ArithmeticExpr)e;
                final Expr left = ae.getLeft();
                final Expr right = ae.getRight();
                final int selfPriority2 = ae.getPrecedence();
                final int leftPriority = left.getPrecedence();
                final int rightPriority = right.getPrecedence();
                if (leftPriority > selfPriority2) {
                    sw.print('(');
                }
                this.printExpr(sw, left);
                if (leftPriority > selfPriority2) {
                    sw.print(')');
                }
                sw.print(" " + ae.getOperator().getSign() + " ");
                if (rightPriority > selfPriority2) {
                    sw.print('(');
                }
                this.printExpr(sw, right);
                if (rightPriority > selfPriority2) {
                    sw.print(')');
                    break;
                }
                break;
            }
            case Opcode.NEGATE: {
                final NegationExpr ne = (NegationExpr)e;
                final Expr expr = ne.getExpression();
                final int selfPriority = ne.getPrecedence();
                final int exprPriority = expr.getPrecedence();
                sw.print('-');
                if (exprPriority > selfPriority) {
                    sw.print('(');
                }
                this.printExpr(sw, expr);
                if (exprPriority > selfPriority) {
                    sw.print(')');
                    break;
                }
                break;
            }
            case Opcode.CLASS_OBJ: {
                final AllocObjectExpr aoe = (AllocObjectExpr)e;
                sw.print("new ").print(aoe.getType().getClassName().replace("/", "."));
                break;
            }
            case Opcode.INIT_OBJ: {
                final InitialisedObjectExpr ioe = (InitialisedObjectExpr)e;
                sw.print("new ");
                sw.print(ioe.getOwner().replace("/", "."));
                sw.print('(');
                final Expr[] args = ioe.getParameterExprs();
                for (int i = 0; i < args.length; ++i) {
                    final boolean needsComma = i + 1 < args.length;
                    this.printExpr(sw, args[i]);
                    if (needsComma) {
                        sw.print(", ");
                    }
                }
                sw.print(')');
                break;
            }
            case Opcode.NEW_ARRAY: {
                final NewArrayExpr nae = (NewArrayExpr)e;
                final Type type = nae.getType();
                sw.print("new " + type.getElementType().getClassName());
                final Expr[] bounds = nae.getBounds();
                for (int dim = 0; dim < type.getDimensions(); ++dim) {
                    sw.print('[');
                    if (dim < bounds.length) {
                        this.printExpr(sw, bounds[dim]);
                    }
                    sw.print(']');
                }
                break;
            }
            case Opcode.ARRAY_LEN: {
                final ArrayLengthExpr ale2 = (ArrayLengthExpr)e;
                final Expr expr = ale2.getExpression();
                final int selfPriority = ale2.getPrecedence();
                final int expressionPriority2 = expr.getPrecedence();
                if (expressionPriority2 > selfPriority) {
                    sw.print('(');
                }
                this.printExpr(sw, expr);
                if (expressionPriority2 > selfPriority) {
                    sw.print(')');
                }
                sw.print(".length");
                break;
            }
            case Opcode.CAST: {
                final CastExpr ce2 = (CastExpr)e;
                final Expr expr = ce2.getExpression();
                final int selfPriority = ce2.getPrecedence();
                final int exprPriority = expr.getPrecedence();
                sw.print('(');
                final Type type2 = ce2.getType();
                sw.print(type2.getClassName());
                sw.print(')');
                if (exprPriority > selfPriority) {
                    sw.print('(');
                }
                this.printExpr(sw, expr);
                if (exprPriority > selfPriority) {
                    sw.print(')');
                    break;
                }
                break;
            }
            case Opcode.INSTANCEOF: {
                final InstanceofExpr ioe2 = (InstanceofExpr)e;
                final Expr expr = ioe2.getExpression();
                final int selfPriority = ioe2.getPrecedence();
                final int expressionPriority2 = expr.getPrecedence();
                if (expressionPriority2 > selfPriority) {
                    sw.print('(');
                }
                this.printExpr(sw, expr);
                if (expressionPriority2 > selfPriority) {
                    sw.print(')');
                }
                sw.print(" instanceof ");
                sw.print(ioe2.getType().getClassName());
                break;
            }
            case Opcode.COMPARE: {
                final ComparisonExpr ce3 = (ComparisonExpr)e;
                sw.print("compare(");
                this.printExpr(sw, ce3.getLeft());
                switch (ce3.getComparisonType()) {
                    case CMP: {
                        sw.print("==");
                        break;
                    }
                    case LT: {
                        sw.print("<");
                        break;
                    }
                    case GT: {
                        sw.print(">");
                        break;
                    }
                }
                this.printExpr(sw, ce3.getRight());
                sw.print(")");
                break;
            }
            case Opcode.CATCH: {
                sw.print("catch()");
                break;
            }
            default: {
                throw new UnsupportedOperationException("Got: " + Opcode.opname(opcode));
            }
        }
    }
}
