package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.Mode;
import org.orbeon.saxon.trans.Rule;
import org.orbeon.saxon.trans.XPathException;

import java.util.Arrays;

/**
* An xsl:next-match element in the stylesheet
*/

public class NextMatch extends ApplyImports {

    boolean useTailRecursion;

    public NextMatch(boolean backwardsCompatible, boolean useTailRecursion) {
        super(backwardsCompatible);
        this.useTailRecursion = useTailRecursion;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_NEXT_MATCH;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();

        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        Rule currentRule = context.getCurrentTemplateRule();
        if (currentRule==null) {
            XPathException e = new XPathException("There is no current template rule");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0560");
            throw e;
        }
        Mode mode = context.getCurrentMode();
        if (mode == null) {
            mode = controller.getRuleManager().getDefaultMode();
        }
        if (context.getCurrentIterator()==null) {
            XPathException e = new XPathException("There is no context item");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0565");
            throw e;
        }
        Item currentItem = context.getCurrentIterator().current();
        if (!(currentItem instanceof NodeInfo)) {
            XPathException e = new XPathException("Cannot call xsl:next-match when context item is not a node");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0565");
            throw e;
        }
        NodeInfo node = (NodeInfo)currentItem;
        Rule rule = controller.getRuleManager().getNextMatchHandler(node, mode, currentRule, context);

		if (rule==null) {             // use the default action for the node
            ApplyTemplates.defaultAction(node, params, tunnels, context, false, getLocationId());
        } else if (useTailRecursion) {
            //Template nh = (Template)rule.getAction();
            // clear all the local variables: they are no longer needed
            Arrays.fill(context.getStackFrame().getStackFrameValues(), null);
            return new NextMatchPackage(rule, params, tunnels, context);
        } else {
            Template nh = (Template)rule.getAction();
            XPathContextMajor c2 = context.newContext();
            c2.setOrigin(this);
            c2.openStackFrame(nh.getStackFrameMap());
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnels);
            nh.apply(c2, rule);
        }
        return null;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("nextMatch");
        if (actualParams != null && actualParams.length > 0) {
            out.startSubsidiaryElement("withParams");
            WithParam.displayExpressions(actualParams, out);
            out.endSubsidiaryElement();
        }
        if (tunnelParams != null && tunnelParams.length > 0) {
            out.startSubsidiaryElement("tunnelParams");
            WithParam.displayExpressions(tunnelParams, out);
            out.endSubsidiaryElement();
        }
        out.endElement();
    }

    /**
    * A NextMatchPackage is an object that encapsulates the name of a template to be called,
    * the parameters to be supplied, and the execution context. This object can be returned as a tail
    * call, so that the actual call is made from a lower point on the stack, allowing a tail-recursive
    * template to execute in a finite stack size
    */

    private class NextMatchPackage implements TailCall {

        private Rule rule;
        private ParameterSet params;
        private ParameterSet tunnelParams;
        private XPathContext evaluationContext;

        /**
         * Construct a NextMatchPackage that contains information about a call.
         * @param rule the rule identifying the Template to be called
         * @param params the parameters to be supplied to the called template
         * @param tunnelParams the tunnel parameter supplied to the called template
         * @param evaluationContext saved context information from the Controller (current mode, etc)
         * which must be reset to ensure that the template is called with all the context information
         * intact
         */

        public NextMatchPackage(Rule rule,
                                   ParameterSet params,
                                   ParameterSet tunnelParams,
                                   XPathContext evaluationContext) {
            this.rule = rule;
            this.params = params;
            this.tunnelParams = tunnelParams;
            this.evaluationContext = evaluationContext;
        }

        /**
        * Process the template call encapsulated by this package.
        * @return another TailCall. This will never be the original call, but it may be the next
        * recursive call. For example, if A calls B which calls C which calls D, then B may return
        * a TailCall to A representing the call from B to C; when this is processed, the result may be
        * a TailCall representing the call from C to D.
         * @throws XPathException if a dynamic error occurs
        */

        public TailCall processLeavingTail() throws XPathException {
            Template nh = (Template)rule.getAction();
            XPathContextMajor c2 = evaluationContext.newContext();
            c2.setOrigin(NextMatch.this);
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnelParams);
            c2.openStackFrame(nh.getStackFrameMap());

            // System.err.println("Tail call on template");

            return nh.applyLeavingTail(c2, rule);
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
