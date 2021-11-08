package org.orbeon.saxon.functions;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.SingletonNode;
import org.orbeon.saxon.FeatureKeys;
import org.orbeon.saxon.Configuration;

/**
 * Implement the fn:doc() function - a simplified form of the Document function
 */

public class Doc extends SystemFunction {

    private String expressionBaseURI = null;
    private boolean readOnce = false;

    /**
     * Indicate that the document will be read once only (or that it should be treated as if it
     * is read once only. This means (a) the document will not be held in memory after all references
     * to it go out of scope, and (b) if the query or transformation tries to read it again, it will get a new
     * copy, with different node identities, and potentially with different content. It also means that the
     * document is eligible for document projection.
     * @param once true if this document is to be treated as being read once only
     */

    public void setReadOnce(boolean once) {
        readOnce = once;
    }

    /**
     * Ask whether this document has been marked as being read once only.
     * @return true if the document has been marked as being read once only
     */

    public boolean isReadOnce() {
        return readOnce;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }

    /**
     * Get the static base URI of the expression
     * @return the static base URI
     */

    public String getStaticBaseURI() {
        return expressionBaseURI;
    }

    /**
     * preEvaluate: this method suppresses compile-time evaluation unless a configuration option has been
     * set to allow early evaluation.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        Configuration config = visitor.getConfiguration();
        if (((Boolean)config.getConfigurationProperty(
                FeatureKeys.PRE_EVALUATE_DOC_FUNCTION)).booleanValue()) {
            try {
                AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(null);
                if (hrefVal==null) {
                    return null;
                }
                String href = hrefVal.getStringValue();
                if (href.indexOf('#') >= 0) {
                    return this;
                }
                NodeInfo item = Document.preLoadDoc(href, expressionBaseURI, config, this);
                if (item!=null) {
                    return new Literal(new SingletonNode(item));
                }
            } catch (Exception err) {
                // ignore the exception and try again at run-time
                return this;
            }
        }
        return this;
    }

    public int computeCardinality() {
        return argument[0].getCardinality() & ~StaticProperty.ALLOWS_MANY;
    }


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return addDocToPathMap(pathMap, pathMapNodeSet);
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        Doc d = (Doc)super.copy();
        d.expressionBaseURI = expressionBaseURI;
        d.readOnce = readOnce;
        return d;
    }

    /**
     * Evaluate the expression
     * @param context the dynamic evaluation context
     * @return the result of evaluating the expression (a document node)
     * @throws XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return doc(context);
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.PEER_NODESET |
                StaticProperty.NON_CREATIVE |
                StaticProperty.SINGLE_DOCUMENT_NODESET;
        // Declaring it as a peer node-set expression avoids sorting of expressions such as
        // doc(XXX)/a/b/c
        // The doc() function might appear to be creative: but it isn't, because multiple calls
        // with the same arguments will produce identical results.
    }

    private NodeInfo doc(XPathContext context) throws XPathException {
        AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(context);
        if (hrefVal==null) {
            return null;
        }
        String href = hrefVal.getStringValue();
        NodeInfo item = Document.makeDoc(href, expressionBaseURI, context, this);
        if (item==null) {
            // we failed to read the document
            dynamicError("Failed to load document " + href, "FODC0005", context);
            return null;
        }
        return item;
    }

    /**
     * Copy the document identified by this expression to a given Receiver. This method is used only when it is
     * known that the document is being copied, because there is then no problem about node identity.
     * @param context the XPath dynamic context
     * @param out the destination to which the document will be sent
     */

    public void sendDocument(XPathContext context, Receiver out) throws XPathException {
        AtomicValue hrefVal = (AtomicValue)argument[0].evaluateItem(context);
        if (hrefVal==null) {
            return;
        }
        String href = hrefVal.getStringValue();
        try {
            Document.sendDoc(href, expressionBaseURI, context, this, out);
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            if (e.getErrorCodeLocalPart() == null) {
                e.setErrorCode("FODC0005");
            }
            throw e;
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
