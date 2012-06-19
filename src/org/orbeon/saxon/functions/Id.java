package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.LocalOrderComparer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.pattern.NodeKindTest;


/**
* The XPath id() function
* XPath 2.0 version: accepts any sequence as the first parameter; each item in the sequence
* is taken as an IDREFS value, that is, a space-separated list of ID values.
 * Also accepts an optional second argument to identify the target document, this
 * defaults to the context node.
*/


public class Id extends SystemFunction {

    private boolean isSingletonId = false;

    /**
    * Simplify: add a second implicit argument, the context document
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Id id = (Id)super.simplify(visitor);
        if (argument.length == 1) {
            id.addContextDocumentArgument(1, "id");
        }
        return id;
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
        isSingletonId = !Cardinality.allowsMany(argument[0].getCardinality());
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        int prop = StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
        if ((getNumberOfArguments() == 1) ||
                (argument[1].getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            prop |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        return prop;
    }


    /**
     * Add a representation of a doc() call or similar function to a PathMap.
     * This is a convenience method called by the addToPathMap() methods for doc(), document(), collection()
     * and similar functions. These all create a new root expression in the path map.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        argument[0].addToPathMap(pathMap, pathMapNodeSet);
        PathMap.PathMapNodeSet target = argument[1].addToPathMap(pathMap, pathMapNodeSet);
        // indicate that the function navigates to all elements in the document
        AxisExpression allElements = new AxisExpression(Axis.DESCENDANT, NodeKindTest.ELEMENT);
        allElements.setContainer(getContainer());
        target = target.createArc(allElements);
//        if (isStringValueUsed()) {
//            target.setAtomized();
//        }
        return target;
    }

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        NodeInfo arg1 = (NodeInfo)argument[1].evaluateItem(context);
        // TODO: test K2-SeqIDFunc-3: we are getting XPTY0020 instead of XPTY0004 when the context item is not a node
        arg1 = arg1.getRoot();
        if (arg1.getNodeKind() != Type.DOCUMENT) {
            dynamicError("In the id() function," +
                            " the tree being searched must be one whose root is a document node", "FODC0001", context);
            return null;
        }
        DocumentInfo doc = (DocumentInfo)arg1;

        if (isSingletonId) {
            AtomicValue arg = (AtomicValue)argument[0].evaluateItem(context);
            if (arg==null) {
                return EmptyIterator.getInstance();
            }
            String idrefs = arg.getStringValue();
            return getIdSingle(doc, idrefs);
        } else {
            SequenceIterator idrefs = argument[0].iterate(context);
            return getIdMultiple(doc, idrefs);
        }
    }

    /**
     * Get an iterator over the nodes that have an id equal to one of the values is a whitespace separated
     * string
     * @param doc The document to be searched
     * @param idrefs a string containing zero or more whitespace-separated ID values to be found in the document
     * @return an iterator over the nodes whose ID is one of the specified values
     * @throws XPathException
     */

    public static SequenceIterator getIdSingle(DocumentInfo doc, String idrefs) throws XPathException {
        boolean white = false;
        for (int i=idrefs.length()-1; i>=0; i--) {
            char c = idrefs.charAt(i);
            if (c <= 0x20 && (c == 0x20 || c == 0x09 || c == 0x0a || c == 0x0d)) {
                white = true;
                break;
            }
        }

        if (white) {
            StringTokenIterator tokens = new StringTokenIterator(idrefs);
            IdMappingFunction map = new IdMappingFunction();
            map.document = doc;
            SequenceIterator result = new MappingIterator(tokens, map);
            return new DocumentOrderIterator(result, LocalOrderComparer.getInstance());
        } else {
            return SingletonIterator.makeIterator(doc.selectID(idrefs));
        }
    }

    /**
     * Get an iterator over the nodes that have an id equal to one of the values is a set of whitespace separated
     * strings
     * @param doc The document to be searched
     * @param idrefs an iterator over a set of strings each of which is a string containing
     * zero or more whitespace-separated ID values to be found in the document
     * @return an iterator over the nodes whose ID is one of the specified values
     * @throws XPathException
     */

    public static SequenceIterator getIdMultiple(DocumentInfo doc, SequenceIterator idrefs) throws XPathException {
        IdMappingFunction map = new IdMappingFunction();
        map.document = doc;
        SequenceIterator result = new MappingIterator(idrefs, map);
        return new DocumentOrderIterator(result, LocalOrderComparer.getInstance());
    }

    private static class IdMappingFunction implements MappingFunction {

        public DocumentInfo document;

        /**
        * Evaluate the function for a single string value
        * (implements the MappingFunction interface)
        */

        public SequenceIterator map(Item item) throws XPathException {

            String idrefs = Whitespace.trim(item.getStringValueCS());

            // If this value contains a space, we need to break it up into its
            // separate tokens; if not, we can process it directly

            boolean white = false;
            for (int i=idrefs.length()-1; i>=0; i--) {
                char c = idrefs.charAt(i);
                if (c <= 0x20 && (c == 0x20 || c == 0x09 || c == 0x0a || c == 0x0d)) {
                    white = true;
                    break;
                }
            }

            if (white) {
                StringTokenIterator tokens = new StringTokenIterator(idrefs);
                IdMappingFunction submap = new IdMappingFunction();
                submap.document = document;
                return new MappingIterator(tokens, submap);

            } else {
                return SingletonIterator.makeIterator(document.selectID(idrefs));
            }
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
