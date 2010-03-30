package org.orbeon.saxon.functions;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.LocalOrderComparer;
import org.orbeon.saxon.trans.KeyDefinitionSet;
import org.orbeon.saxon.trans.KeyManager;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;


public class Idref extends SystemFunction {

    private KeyDefinitionSet idRefKey;

    /**
    * Simplify: add a second implicit argument, the context document
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Idref f = (Idref)super.simplify(visitor);
        f.addContextDocumentArgument(1, "idref");
        return f;
    }


    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(visitor, contextItemType);
        idRefKey = visitor.getExecutable().getKeyManager().getKeyDefinitionSet(
                    StandardNames.getStructuredQName(StandardNames.XS_IDREFS));
        return e;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], false);
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
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Idref i2 = (Idref)super.copy();
        i2.idRefKey = idRefKey;
        return i2;
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
        // indicate that the function navigates to all nodes in the document
        AxisExpression allElements = new AxisExpression(Axis.DESCENDANT, AnyNodeTest.getInstance());
        allElements.setContainer(getContainer());
        target = target.createArc(allElements);
//        if (isStringValueUsed()) {
//            target.setAtomized();
//        }
        return target;
    }


    /**
    * Enumerate the results of the expression
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        Controller controller = context.getController();

        NodeInfo arg2 = (NodeInfo)argument[1].evaluateItem(context);
        arg2 = arg2.getRoot();
        if (arg2.getNodeKind() != Type.DOCUMENT) {
            dynamicError("In the idref() function," +
                            " the tree being searched must be one whose root is a document node", "FODC0001", context);
            return null;
        }
        DocumentInfo doc = (DocumentInfo)arg2;

        // If the argument is a singleton, we evaluate the function
        // directly; otherwise we recurse to evaluate it once for each Item
        // in the sequence.

        Expression expression = argument[0];
        if (Cardinality.allowsMany(expression.getCardinality())) {
            SequenceIterator keys = argument[0].iterate(context);
            return getIdrefMultiple(doc, keys, context);

        } else {
            AtomicValue keyValue = (AtomicValue)argument[0].evaluateItem(context);
            if (keyValue == null) {
                return EmptyIterator.getInstance();
            }
            KeyManager keyManager = controller.getKeyManager();
            return keyManager.selectByKey(idRefKey, doc, keyValue, context);

        }
    }

    /**
     * Get the result when multiple idref values are supplied. Note this is also called from
     * compiled XQuery code.
     * @param doc the document to be searched
     * @param keys the idref values supplied
     * @param context the dynamic execution context
     * @return iterator over the result of the function
     * @throws XPathException
     */

    public static SequenceIterator getIdrefMultiple(DocumentInfo doc, SequenceIterator keys, XPathContext context)
    throws XPathException {
        IdrefMappingFunction map = new IdrefMappingFunction();
        map.document = doc;
        map.keyContext = context;
        map.keyManager =  context.getController().getKeyManager();
        map.keySet = map.keyManager.getKeyDefinitionSet(StandardNames.getStructuredQName(StandardNames.XS_IDREFS));
        SequenceIterator allValues = new MappingIterator(keys, map);
        return new DocumentOrderIterator(allValues, LocalOrderComparer.getInstance());
    }

    private static class IdrefMappingFunction implements MappingFunction {
        public DocumentInfo document;
        public XPathContext keyContext;
        public KeyManager keyManager;
        public KeyDefinitionSet keySet;

        /**
        * Implement the MappingFunction interface
        */

        public SequenceIterator map(Item item) throws XPathException {
            KeyManager keyManager = keyContext.getController().getKeyManager();
            AtomicValue keyValue;
            if (item instanceof AtomicValue) {
                keyValue = (AtomicValue)item;
            } else {
                keyValue = new StringValue(item.getStringValue());
            }
            return keyManager.selectByKey(keySet, document, keyValue, keyContext);

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
