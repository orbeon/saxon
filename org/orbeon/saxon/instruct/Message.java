package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Emitter;
import org.orbeon.saxon.event.TreeReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NoNodeTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;

import javax.xml.transform.OutputKeys;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
* An xsl:message element in the stylesheet.
*/

public class Message extends Instruction {

    // TODO: JAXP 1.3 specifies that xsl:message output is written to the ErrorListener

    private Expression terminate;
    private Expression select;

    public Message(Expression select, Expression terminate) {
        this.terminate = terminate;
        this.select = select;
        adoptChildExpression(terminate);
        adoptChildExpression(select);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        if (terminate != null) {
            terminate = terminate.simplify(env);
        }
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.typeCheck(env, contextItemType);
        adoptChildExpression(select);
        if (terminate != null) {
            terminate = terminate.typeCheck(env, contextItemType);
            adoptChildExpression(terminate);
        }
        return this;
    }

   public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.optimize(opt, env, contextItemType);
        adoptChildExpression(select);
        if (terminate != null) {
            terminate = terminate.optimize(opt, env, contextItemType);
            adoptChildExpression(terminate);
        }
        return this;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_MESSAGE;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return NoNodeTest.getInstance();
    }

    public int getCardinality() {
        return StaticProperty.EMPTY;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
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
        if (select != null) {
            select = doPromotion(select, offer);
        }
        if (terminate != null) {
            terminate = doPromotion(terminate, offer);
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(2);
        if (select != null) {
            list.add(select);
        }
        if (terminate != null) {
            list.add(terminate);
        }
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
        if (terminate == original) {
            terminate = replacement;
            found = true;
        }
                return found;
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        Emitter emitter = controller.getMessageEmitter();
        if (emitter==null) {
            emitter = controller.makeMessageEmitter();
        }
        if (emitter.getWriter()==null) {
            emitter.setWriter(new OutputStreamWriter(System.err));
        }

        TreeReceiver rec = new TreeReceiver(emitter);

        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        Properties props = new Properties();
        props.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
        emitter.setOutputProperties(props);
        c2.changeOutputDestination(props, rec, false, Configuration.XSLT, Validation.PRESERVE, null);

        if (select != null) {
            SequenceIterator iter = select.iterate(c2);
            while (true) {
                Item item = iter.next();
                if (item == null) {
                    break;
                }
                rec.append(item, locationId, NodeInfo.ALL_NAMESPACES);
            }
        }
        rec.close();

        if (terminate != null) {
            String term = terminate.evaluateAsString(context);
            if (term.equals("no")) {
                // no action
            } else if (term.equals("yes")) {
                throw new TerminationException(
                        "Processing terminated by xsl:message at line " + getLineNumber() +
                        " in " + ExpressionLocation.truncateURI(getSystemId()));
            } else {
                DynamicError e = new DynamicError("The terminate attribute of xsl:message must be 'yes' or 'no'");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                throw e;
            }
        }
        return null;
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
        out.println(ExpressionTool.indent(level) + "message");
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
