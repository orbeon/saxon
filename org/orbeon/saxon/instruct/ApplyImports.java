package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
* An xsl:apply-imports element in the stylesheet
*/

public class ApplyImports extends Instruction {

    WithParam[] actualParams = null;
    WithParam[] tunnelParams = null;
    private boolean backwardsCompatible;

    public ApplyImports(boolean backwardsCompatible) {
        this.backwardsCompatible = backwardsCompatible;
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
        return StandardNames.XSL_APPLY_IMPORTS;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception net.sf.saxon.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        WithParam.simplify(actualParams, env);
        WithParam.simplify(tunnelParams, env);
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
     * @exception net.sf.saxon.trans.XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        WithParam.analyze(actualParams, env, contextItemType);
        WithParam.analyze(tunnelParams, env, contextItemType);
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

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        WithParam.promoteParams(actualParams, offer);
        WithParam.promoteParams(tunnelParams, offer);
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(10);
        WithParam.getXPathExpressions(actualParams, list);
        WithParam.getXPathExpressions(tunnelParams, list);
        return list.iterator();
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        Template currentTemplate = context.getCurrentTemplate();
        if (currentTemplate==null) {
            DynamicError e = new DynamicError("There is no current template rule");
            e.setXPathContext(context);
            e.setErrorCode("XT0560");
            throw e;
        }

        int min = currentTemplate.getMinImportPrecedence();
        int max = currentTemplate.getPrecedence()-1;
        Mode mode = context.getCurrentMode();
        if (mode == null) {
            mode = controller.getRuleManager().getMode(Mode.DEFAULT_MODE);
        }
        if (context.getCurrentIterator()==null) {
            DynamicError e = new DynamicError("Cannot call xsl:apply-imports when there is no context item");
            e.setXPathContext(context);
            e.setErrorCode("XT0565");
            throw e;
        }
        Item currentItem = context.getCurrentIterator().current();
        if (!(currentItem instanceof NodeInfo)) {
            DynamicError e = new DynamicError("Cannot call xsl:apply-imports when context item is not a node");
            e.setXPathContext(context);
            e.setErrorCode("XT0565");
            throw e;
        }
        NodeInfo node = (NodeInfo)currentItem;
        Template nh = controller.getRuleManager().getTemplateRule(node, mode, min, max, context);

		if (nh==null) {             // use the default action for the node
            ApplyTemplates.defaultAction(node, params, tunnels, context, backwardsCompatible);
        } else {
            XPathContextMajor c2 = context.newContext();
            c2.setOrigin(this);
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnels);
            c2.openStackFrame(nh.getStackFrameMap());
            nh.process(c2);
        }
        return null;
                // We never treat apply-imports as a tail call, though we could
    }



    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "apply-imports");
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
