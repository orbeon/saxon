package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.Type;

import javax.xml.transform.OutputKeys;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
* An xsl:message element in the stylesheet.
*/

public class Message extends Instruction {

    private Expression terminate;
    private Expression select;

    /**
     * Create an xsl:message instruction
     * @param select the expression that constructs the message (composite of the select attribute
     * and the contained sequence constructor)
     * @param terminate expression that calculates terminate = yes or no.
     */

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
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        terminate = visitor.simplify(terminate);
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.typeCheck(select, contextItemType);
        adoptChildExpression(select);
        if (terminate != null) {
            terminate = visitor.typeCheck(terminate, contextItemType);
            adoptChildExpression(terminate);
        }
        return this;
    }

   public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        adoptChildExpression(select);
        if (terminate != null) {
            terminate = visitor.optimize(terminate, contextItemType);
            adoptChildExpression(terminate);
        }
        return this;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_MESSAGE;
    }

    /**
     * Get the item type. To avoid spurious compile-time type errors, we falsely declare that the
     * instruction can return anything
     * @param th the type hierarchy cache
     * @return AnyItemType
     */
    public ItemType getItemType(TypeHierarchy th) {
        return AnyItemType.getInstance();
    }

    /**
     * Get the static cardinality. To avoid spurious compile-time type errors, we falsely declare that the
     * instruction returns zero or one items - this is always acceptable
     * @return zero or one
     */

    public int getCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_ONE;
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
        Receiver emitter = controller.getMessageEmitter();

        SequenceReceiver rec = new TreeReceiver(emitter);
        rec = new AttributeMasker(rec);

        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);
        Properties props = new Properties();
        props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        c2.changeOutputDestination(props, rec, false, Configuration.XSLT, Validation.PRESERVE, null);

        boolean abort = false;
        if (terminate != null) {
            String term = terminate.evaluateAsString(context).toString();
            if (term.equals("no")) {
                // no action
            } else if (term.equals("yes")) {
                abort = true;
            } else {
                XPathException e = new XPathException("The terminate attribute of xsl:message must be 'yes' or 'no'");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                throw e;
            }
        }


        rec.startDocument(abort ? ReceiverOptions.TERMINATE : 0);

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

        rec.endDocument();

        if (abort) {
            throw new TerminationException(
                    "Processing terminated by xsl:message at line " + getLineNumber() +
                    " in " + ExpressionLocation.truncateURI(getSystemId()));
        }
        return null;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("xslMessage");
        out.endElement();
    }

    private static class AttributeMasker extends ProxyReceiver {
        private boolean contentStarted = true;

        public AttributeMasker(SequenceReceiver next) {
            setPipelineConfiguration(next.getPipelineConfiguration());
            setUnderlyingReceiver(next);
        }

        public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
            contentStarted = false;
            super.startElement(nameCode, typeCode, locationId, properties);
        }

        public void startContent() throws XPathException {
            contentStarted = true;
            super.startContent();
        }


        public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
                throws XPathException {
            if (contentStarted) {
                String attName = getNamePool().getDisplayName(nameCode);
                processingInstruction("attribute", "name=\"" + attName + "\" value=\"" + value + "\"", 0, 0);
            } else {
                super.attribute(nameCode, typeCode, value, locationId, properties);
            }
        }

        public void namespace(int namespaceCode, int properties) throws XPathException {
            if (contentStarted) {
                String prefix = getNamePool().getPrefixFromNamespaceCode(namespaceCode);
                String uri = getNamePool().getURIFromNamespaceCode(namespaceCode);
                processingInstruction("namespace", "prefix=\"" + prefix + "\" uri=\"" + uri + "\"", 0, 0);
            } else {
                super.namespace(namespaceCode, properties);
            }
        }

        public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
            if (item instanceof NodeInfo) {
                int kind = ((NodeInfo)item).getNodeKind();
                if (kind == Type.ATTRIBUTE || kind == Type.NAMESPACE) {
                    ((NodeInfo)item).copy(this, NodeInfo.NO_NAMESPACES, false, 0);
                    return;
                }
            }
            ((SequenceReceiver)nextReceiver).append(item, locationId, copyNamespaces);
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
