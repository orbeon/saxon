package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.LocalOrderComparer;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.type.Type;


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
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Id id = (Id)super.simplify(env);
        if (argument.length == 1) {
            id.addContextDocumentArgument(1, "id");
        }
        return id;
    }

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        Optimizer opt = env.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
        isSingletonId = !Cardinality.allowsMany(argument[0].getCardinality());
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
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
    * Evaluate the function to return an iteration of selected nodes.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        NodeInfo arg1 = (NodeInfo)argument[1].evaluateItem(context);
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
            if (idrefs.indexOf(0x20)>=0 ||
                    idrefs.indexOf(0x09)>=0 ||
                    idrefs.indexOf(0x0a)>=0 ||
                    idrefs.indexOf(0x0d)>=0) {
                StringTokenIterator tokens = new StringTokenIterator(idrefs);
                IdMappingFunction map = new IdMappingFunction();
                map.document = doc;
                SequenceIterator result = new MappingIterator(tokens, map);
                return new DocumentOrderIterator(result, LocalOrderComparer.getInstance());
            } else {
                return SingletonIterator.makeIterator(doc.selectID(idrefs));
            }
        } else {
            SequenceIterator idrefs = argument[0].iterate(context);
            IdMappingFunction map = new IdMappingFunction();
            map.document = doc;
            SequenceIterator result = new MappingIterator(idrefs, map);
            return new DocumentOrderIterator(result, LocalOrderComparer.getInstance());
        }
    }

    private static class IdMappingFunction implements MappingFunction {

        public DocumentInfo document;

        /**
        * Evaluate the function for a single string value
        * (implements the MappingFunction interface)
        */

        public Object map(Item item) throws XPathException {

            String idrefs = item.getStringValue().trim();

            // If this value contains a space, we need to break it up into its
            // separate tokens; if not, we can process it directly

            if (idrefs.indexOf(0x20)>=0 ||
                    idrefs.indexOf(0x09)>=0 ||
                    idrefs.indexOf(0x0a)>=0 ||
                    idrefs.indexOf(0x0d)>=0) {
                StringTokenIterator tokens = new StringTokenIterator(idrefs);
                IdMappingFunction submap = new IdMappingFunction();
                submap.document = document;
                return new MappingIterator(tokens, submap);

            } else {
                return document.selectID(idrefs);
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
