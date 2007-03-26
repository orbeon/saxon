package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.*;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.Value;

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
    private boolean implicitSelect;

    public ApplyTemplates(  Expression select,
                            boolean useCurrentMode,
                            boolean useTailRecursion,
                            Mode mode,
                            boolean backwardsCompatible,
                            boolean implicitSelect) {
        this.select = select;
        this.useCurrentMode = useCurrentMode;
        this.useTailRecursion = useTailRecursion;
        this.mode = mode;
        this.backwardsCompatible = backwardsCompatible;
        this.implicitSelect = implicitSelect;
        adoptChildExpression(select);
    }

   /**
     * Set the actual parameters on the call
     */

    public void setActualParameters(
                        WithParam[] actualParams,
                        WithParam[] tunnelParams ) {
        this.actualParams = actualParams;
        this.tunnelParams = tunnelParams;
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

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        WithParam.typeCheck(actualParams, env, contextItemType);
        WithParam.typeCheck(tunnelParams, env, contextItemType);
        try {
            select = select.typeCheck(env, contextItemType);
        } catch (XPathException e) {
            if (implicitSelect) {
                if ("XPTY0020".equals(e.getErrorCodeLocalPart())) {
                    DynamicError err = new DynamicError(
                            "Cannot apply-templates to child nodes when the context item is an atomic value");
                    err.setErrorCode("XTTE0510");
                    err.setIsTypeError(true);
                    throw err;
                } else if ("XPDY0002".equals(e.getErrorCodeLocalPart())) {
                    DynamicError err = new DynamicError(
                            "Cannot apply-templates to child nodes when the context item is undefined");
                    err.setErrorCode("XTTE0510");
                    err.setIsTypeError(true);
                    throw err;
                }
            }
            throw e;
        }
        adoptChildExpression(select);
        if (select instanceof EmptySequence) {
            return EmptySequence.getInstance();
        }
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        WithParam.optimize(opt, actualParams, env, contextItemType);
        WithParam.optimize(opt, tunnelParams, env, contextItemType);
        select = select.typeCheck(env, contextItemType);  // More info available second time around
        select = select.optimize(opt, env, contextItemType);
        adoptChildExpression(select);
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
                    ExpressionTool.lazyEvaluate(select, context, 1),
                    thisMode, params, tunnels, c2, getLocationId());
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
            TailCall tc = applyTemplates(iter, thisMode, params, tunnels, c2, backwardsCompatible, getLocationId());
            while (tc != null) {
                tc = tc.processLeavingTail();
            }
        } catch (StackOverflowError e) {
            DynamicError err = new DynamicError(
                    "Too many nested apply-templates calls. The stylesheet may be looping.");
            err.setErrorCode(SaxonErrorCode.SXLM0001);
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
     * @param locationId
     * @return a TailCall returned by the last template to be invoked, or null,
     *     indicating that there are no outstanding tail calls.
     */

    public static TailCall applyTemplates(SequenceIterator iterator,
                                          Mode mode,
                                          ParameterSet parameters,
                                          ParameterSet tunnelParameters,
                                          XPathContextMajor context,
                                          boolean backwardsCompatible,
                                          int locationId)
                                throws XPathException {
        Controller controller = context.getController();
        TailCall tc = null;

        XPathContextMajor c2 = context;

        // Iterate over this sequence

        if (controller.isTracing()) {

            c2.setCurrentIterator(iterator);
            c2.setCurrentMode(mode);
            while(true) {

                NodeInfo node = (NodeInfo)iterator.next();
                        // We can assume it's a node - we did static type checking
                if (node == null) {
                    break;
                }
                // process any tail calls returned from previous nodes
                while (tc != null) {
                    tc = tc.processLeavingTail();
                }

                // find the template rule for this node
                Rule rule = controller.getRuleManager().getTemplateRule(node, mode, c2);

                if (rule==null) {             // Use the default action for the node
                                            // No need to open a new stack frame!
                    defaultAction(node, parameters, tunnelParameters, c2, backwardsCompatible, locationId);

                } else {
                    Template template = (Template)rule.getAction();
                    TraceListener traceListener = controller.getTraceListener();
                    c2.setLocalParameters(parameters);
                    c2.setTunnelParameters(tunnelParameters);
                    c2.openStackFrame(template.getStackFrameMap());
                    traceListener.startCurrentItem(node);
                    tc = template.applyLeavingTail(c2, rule);
                    traceListener.endCurrentItem(node);
                }
            }

        } else {    // not tracing

            c2.setCurrentIterator(iterator);
            c2.setCurrentMode(mode);
            boolean lookahead = (iterator.getProperties() & SequenceIterator.LOOKAHEAD) != 0;
            while(true) {

                // process any tail calls returned from previous nodes. We need to do this before changing
                // the context. If we have a LookaheadIterator, we can tell whether we're positioned at the
                // end without changing the current position, and we can then return the last tail call to
                // the caller and execute it further down the stack, reducing the risk of running out of stack
                // space. In other cases, we need to execute the outstanding tail calls before moving the iterator

                if (tc != null) {
                    if (lookahead && !((LookaheadIterator)iterator).hasNext()) {
                        break;
                    }
                    do {
                        tc = tc.processLeavingTail();
                    } while (tc != null);
                }

                NodeInfo node = (NodeInfo)iterator.next();
                        // We can assume it's a node - we did static type checking
                if (node == null) {
                    break;
                }

                // find the template rule for this node

                Rule rule = controller.getRuleManager().getTemplateRule(node, mode, c2);

                if (rule==null) {             // Use the default action for the node
                                            // No need to open a new stack frame!
                    defaultAction(node, parameters, tunnelParameters, c2, backwardsCompatible, locationId);

                } else {
                    Template template = (Template)rule.getAction();
                    c2.openStackFrame(template.getStackFrameMap());
                    c2.setLocalParameters(parameters);
                    c2.setTunnelParameters(tunnelParameters);
                    tc = template.applyLeavingTail(c2, rule);
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
     * @param locationId location of the instruction (apply-templates, apply-imports etc) that caused
     * the built-in template to be invoked
     * @exception XPathException if any dynamic error occurs
     */

    public static void defaultAction(NodeInfo node,
                                     ParameterSet parameters,
                                     ParameterSet tunnelParams,
                                     XPathContext context,
                                     boolean backwardsCompatible,
                                     int locationId) throws XPathException {
        switch(node.getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                SequenceIterator iter = node.iterateAxis(Axis.CHILD);
                XPathContextMajor c2 = context.newContext();
                c2.setOrigin(builtInDetails);
	            TailCall tc = applyTemplates(
                        iter, context.getCurrentMode(), parameters, tunnelParams, c2, backwardsCompatible, locationId);
                while (tc != null) {
                    tc = tc.processLeavingTail();
                }
	            return;
	        case Type.TEXT:
	            // NOTE: I tried changing this to use the text node's copy() method, but
	            // performance was worse
	        case Type.ATTRIBUTE:
	            context.getReceiver().characters(node.getStringValueCS(), locationId, 0);
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
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (WithParam.replaceXPathExpression(actualParams, original, replacement)) {
            found = true;
        }
        if (WithParam.replaceXPathExpression(tunnelParams, original, replacement)) {
            found = true;
        }
                return found;
    }


    /**
     * Get the select expression
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = doPromotion(select, offer);
        WithParam.promoteParams(actualParams, offer);
        WithParam.promoteParams(tunnelParams, offer);
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        String modeName = "";
        if (!mode.isDefaultMode()) {
            modeName = " mode=" + config.getNamePool().getDisplayName(mode.getModeNameCode());
        }
        out.println(ExpressionTool.indent(level) + "apply-templates" + modeName + " select=");

        select.display(level+1, out, config);
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
        private int locationId;

        ApplyTemplatesPackage(ValueRepresentation selectedNodes,
                                     Mode mode,
                                     ParameterSet params,
                                     ParameterSet tunnelParams,
                                     XPathContextMajor context,
                                     int locationId
                                     ) {
            this.selectedNodes = selectedNodes;
            this.mode = mode;
            this.params = params;
            this.tunnelParams = tunnelParams;
            this.evaluationContext = context;
            this.locationId = locationId;
        }

        public TailCall processLeavingTail() throws XPathException {
            TailCall tc = applyTemplates(
                    Value.getIterator(selectedNodes),
                    mode, params, tunnelParams, evaluationContext, false, locationId);
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
