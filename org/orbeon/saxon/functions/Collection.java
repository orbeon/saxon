package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.XPathException;

/**
 * Implement the fn:collection() function. The Saxon implementation loads an XML
 * document called the collection catalogue, which acts as an index of the collection.
 *
 * <p>The structure of this index is:
 * <pre>
 * &lt;collection&gt;
 *    &lt;doc href="doc1.xml"&gt;
 *    &lt;doc href="doc2.xml"&gt;
 *    &lt;doc href="doc3.xml"&gt;
 * &lt;/collection&gt;
 * </pre></p>
 *
 * <p>The document URIs are resolved relative to the base URI of the doc element
 * in the catalogue document.</p>
 */

public class Collection extends SystemFunction implements MappingFunction {

    private String expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
        }
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // Zero-argument function: Saxon does not support a default collection

        if (getNumberOfArguments() == 0) {
            dynamicError("There is no default collection", "FODC0002", context);
        }

        // First read the catalog document

        String href = argument[0].evaluateItem(context).getStringValue();

        DocumentInfo catalog =
                (DocumentInfo) Document.makeDoc(href, expressionBaseURI, context);
        if (catalog==null) {
            // we failed to read the catalogue
            dynamicError("Failed to load collection catalogue " + href, context);
            return null;
        }

        // Now return an iterator over the documents that it refers to

        SequenceIterator iter =
                catalog.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);
        NodeInfo top;
        while (true) {
            top = (NodeInfo)iter.next();
            if (top == null) break;
            if (!("collection".equals(top.getLocalPart()) &&
                    top.getURI().equals("") )) {
                dynamicError("collection catalogue must contain top-level element <collection>", context);
            }
            break;
        }

        SequenceIterator documents =
                top.iterateAxis(Axis.CHILD, NodeKindTest.ELEMENT);

        return new MappingIterator(documents, this, context, null);

    }

    /**
     * Map from doc elements in the catalogue document to nodes
     * returned in the result
     * @param item A doc element in the catalogue document
     * @param context The dynamic evaluation context
     * @param info not used (set to null)
     * @return the document or element referenced by the @href attribute of the doc
     * element in the catalogue
     * @throws XPathException if the document cannot be retrieved or parsed, unless
     * error recovery has been chosen.
     */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        NodeInfo element = (NodeInfo)item;
        if (!("doc".equals(element.getLocalPart()) &&
                element.getURI().equals("") )) {
            dynamicError("children of <collection> element must be <doc> elements", context);
        }
        String href = Navigator.getAttributeValue(element, "", "href");
        if (href==null) {
            dynamicError("<doc> element in catalogue has no @href attribute", context);
        }

        NodeInfo target = Document.makeDoc(href, element.getBaseURI(), context);
        return target;
    }

    // TODO: provide control over error recovery (etc) through options in the catalog file.

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
