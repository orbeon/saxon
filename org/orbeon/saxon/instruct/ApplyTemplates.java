package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.Value;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
* An instruction representing an xsl:apply-templates element in the stylesheet
*/

public class ApplyTemplates extends Instruction {

    private Expression select;
    private WithParam[] actualParams = null;
    private WithParam[] tunnelParams = null;
    private boolean useCurrentMode = false;
    private boolean useTailRecursion = false;
    private Mode mode;
    private boolean backwardsCompatible;

    public ApplyTemplates(  Expression select,
                            WithParam[] actualParams,
                            WithParam[] tunnelParams,
                            boolean useCurrentMode,
                            boolean useTailRecursion,
                            Mode mode,
                            boolean backwardsCompatible) {
        this.select = select;
        this.actualParams = actualParams;
        this.tunnelParams = tunnelParams;
        this.useCurrentMode = useCurrentMode;
        this.useTailRecursion = useTailRecursion;
        this.mode = mode;
        this.backwardsCompatible = backwardsCompatible;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_APPLY_TEMPLATES;
    }

    /**
     * Set additional trace properties appropriate to the kind of instruction. This
     * implementation adds the mode attribute
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
        details.setProperty("mode", mode);
        return details;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        WithParam.simplify(actualParams, env);
        WithParam.simplify(tunnelParams, env);
        select = select.simplify(env);
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @exception XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        WithParam.analyze(actualParams, env, contextItemType);
        WithParam.analyze(tunnelParams, env, contextItemType);
        select = select.analyze(env, contextItemType);
        if (select instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }
        return this;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true (which is almost invariably the case, so it's not worth
     * doing any further analysis to find out more precisely).
     */

    public final boolean createsNewNodes() {
        return true;
    }

    public void process(XPathContext context) throws XPathException {
        apply(context, false);
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        return apply(context, useTailRecursion);
    }

    private TailCall apply(XPathContext context, boolean returnTailCall) throws XPathException {
        Mode thisMode = mode;
        if (useCurrentMode) {
            thisMode = context.getCurrentMode();
        }

        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        if (returnTailCall) {
            XPathContextMajor c2 = context.newContext();
            c2.setOrigin(this);
            return new ApplyTemplatesPackage(
                    ExpressionTool.lazyEvaluate(select, context, false),
                    thisMode, params, tunnels, c2);
        }

        // Get an iterator to iterate through the selected nodes in original order

        SequenceIterator iter = select.iterate(context);

        // Quick exit if the iterator is empty

        if (iter instanceof EmptyIterator) {
            return null;
        }

        // process the selected nodes now
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        try {
            TailCall tc = applyTemplates(iter, thisMode, params, tunnels, c2, backwardsCompatible);
            while (tc != null) {
                tc = tc.processLeavingTail(c2);
            }
        } catch (StackOverflowError e) {
            DynamicError err = new DynamicError(
                    "Too many nested apply-templates calls. The stylesheet is probably looping.");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        return null;

    }

    /**
     * Process selected nodes using the handlers registered for a particular
     * mode.
     *
     * @exception XPathException if any dynamic error occurs
     * @param iterator an Iterator over the nodes to be processed, in the
     *     correct (sorted) order
     * @param mode Identifies the processing mode. It should match the
     *     mode defined when the element handler was registered using
     *     setHandler with a mode parameter. Set this parameter to null to
     *     invoke the default mode.
     * @param parameters A ParameterSet containing the parameters to
     *     the handler/template being invoked. Specify null if there are no
     *     parameters.
     * @param tunnelParameters A ParameterSet containing the parameters to
     *     the handler/template being invoked. Specify null if there are no
     *     parameters.
     * @param context A newly-created context object
     * @return a TailCall returned by the last template to be invoked, or null,
     *     indicating that there are no outstanding tail calls.
     */

    public static TailCall applyTemplates( SequenceIterator iterator,
                                Mode mode,
                                ParameterSet parameters,
                                ParameterSet tunnelParameters,
                                XPathContextMajor context,
                                boolean backwardsCompatible)
                                throws XPathException {
        Controller controller = context.getController();
        TailCall tc = null;

        XPathContextMajor c2 = context;

        // Iterate over this sequence

        if (controller.isTracing()) {

            c2.setCurrentIterator(iterator);
            c2.setCurrentMode(mode);
            while(true) {
                // process any tail calls returned from previous nodes
                while (tc != null) {
                    tc = tc.processLeavingTail(c2);
                }

                NodeInfo node = (NodeInfo)iterator.next();
                        // We can assume it's a node - we did static type checking
                if (node == null) break;

                // find the node handler [i.e., the template rule] for this node
                Template eh = controller.getRuleManager().getTemplateRule(node, mode, c2);

                if (eh==null) {             // Use the default action for the node
                                            // No need to open a new stack frame!
                    defaultAction(node, parameters, tunnelParameters, c2, backwardsCompatible);

                } else {
                    if (tunnelParameters != null || eh.needsStackFrame()) {
                        TraceListener traceListener = controller.getTraceListener();
                        c2.setLocalParameters(parameters);
                        c2.setTunnelParameters(tunnelParameters);
                        c2.openStackFrame(eh.getStackFrameMap());
                        traceListener.startCurrentItem(node);
                        tc = eh.processLeavingTail(c2);
                        traceListener.endCurrentItem(node);
                    } else {
                        TraceListener traceListener = controller.getTraceListener();
                        traceListener.startCurrentItem(node);
                        tc = eh.processLeavingTail(c2);
                        traceListener.endCurrentItem(node);
                    }
                }
            }

        } else {    // not tracing

            c2.setCurrentIterator(iterator);
            c2.setCurrentMode(mode);
            while(true) {

                // process any tail calls returned from previous nodes
                while (tc != null) {
                    tc = tc.processLeavingTail(c2);
                }

                NodeInfo node = (NodeInfo)iterator.next();
                        // We can assume it's a node - we did static type checking
                if (node == null) break;

                // find the template rule for this node

                Template eh = controller.getRuleManager().getTemplateRule(node, mode, c2);

                if (eh==null) {             // Use the default action for the node
                                            // No need to open a new stack frame!
                    defaultAction(node, parameters, tunnelParameters, c2, backwardsCompatible);

                } else {
                    if (tunnelParameters != null || eh.needsStackFrame()) {
                        c2.openStackFrame(eh.getStackFrameMap());
                        c2.setLocalParameters(parameters);
                        c2.setTunnelParameters(tunnelParameters);
                        tc = eh.processLeavingTail(c2);
                    } else {
                        tc = eh.processLeavingTail(c2);
                    }
                }
            }
        }
        // return the TailCall returned from the last node processed
        return tc;
    }

    /**
     * Perform the built-in template action for a given node.
     *
     * @param node the node to be processed
     * @param parameters the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param backwardsCompatible true if in 1.0 mode (currently makes no difference)
     * @exception XPathException if any dynamic error occurs
     */

    public static void defaultAction(NodeInfo node,
                               ParameterSet parameters,
                               ParameterSet tunnelParams,
                               XPathContext context,
                               boolean backwardsCompatible) throws XPathException {
        switch(node.getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                SequenceIterator iter = node.iterateAxis(Axis.CHILD);
                XPathContextMajor c2 = context.newContext();
                c2.setOrigin(builtInDetails);
	            TailCall tc = applyTemplates(
                        iter, context.getCurrentMode(), parameters, tunnelParams, c2, backwardsCompatible);
                while (tc != null) {
                    tc = tc.processLeavingTail(c2);
                }
	            return;
	        case Type.TEXT:
	            // NOTE: I tried changing this to use the text node's copy() method, but
	            // performance was worse
	        case Type.ATTRIBUTE:
	            context.getReceiver().characters(node.getStringValueCS(), 0, 0);
	            return;
	        case Type.COMMENT:
	        case Type.PROCESSING_INSTRUCTION:
	        case Type.NAMESPACE:
	            // no action
	            return;
        }
    }

    /**
     * Instruction details for the built-in template
     */

    private static InstructionDetails builtInDetails = new InstructionDetails();
    static {
        builtInDetails.setConstructType(Location.BUILT_IN_TEMPLATE);
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(10);
        list.add(select);
        WithParam.getXPathExpressions(actualParams, list);
        WithParam.getXPathExpressions(tunnelParams, list);
        return list.iterator();
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = select.promote(offer);
        WithParam.promoteParams(actualParams, offer);
        WithParam.promoteParams(tunnelParams, offer);
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "apply-templates " + "select=");
        select.display(level+1, pool, out);
    }

    /**
    * An ApplyTemplatesPackage is an object that encapsulates the sequence of nodes to be processed,
    * the mode, the parameters to be supplied, and the execution context. This object can be returned as a tail
    * call, so that the actual call is made from a lower point on the stack, allowing a tail-recursive
    * template to execute in a finite stack size
    */

    private static class ApplyTemplatesPackage implements TailCall {

        private ValueRepresentation selectedNodes;
        private Mode mode;
        private ParameterSet params;
        private ParameterSet tunnelParams;
        private XPathContextMajor evaluationContext;

        public ApplyTemplatesPackage(ValueRepresentation selectedNodes,
                                     Mode mode,
                                     ParameterSet params,
                                     ParameterSet tunnelParams,
                                     XPathContextMajor context
                                     ) {
            this.selectedNodes = selectedNodes;
            this.mode = mode;
            this.params = params;
            this.tunnelParams = tunnelParams;
            this.evaluationContext = context;
        }

        public TailCall processLeavingTail(XPathContext context) throws XPathException {
            TailCall tc = applyTemplates(
                    Value.getIterator(selectedNodes), mode, params, tunnelParams, evaluationContext, false);
            return tc;
        }
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
