package org.orbeon.saxon.functions;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.LocalOrderComparer;
import org.orbeon.saxon.style.ExpressionContext;
import org.orbeon.saxon.trans.*;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;


public class KeyFn extends SystemFunction implements XSLTFunction {

    private NamespaceResolver nsContext = null;
    private KeyDefinitionSet staticKeySet = null; // null if name resolution is done at run-time
    private transient boolean checked = false;
    private transient boolean internal = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    /**
     * Get the key name, if known statically. If not known statically, return null.
     * @return the key name if known, otherwise null
     */

    public StructuredQName getStaticKeyName() {
        return (staticKeySet == null ? null : staticKeySet.getKeyName());
    }

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        try {
            return super.typeCheck(visitor, contextItemType);
        } catch (XPathException err) {
            if ("XPDY0002".equals(err.getErrorCodeLocalPart())) {
                XPathException e = new XPathException("Cannot call the key() function when there is no context node");
                e.setErrorCode("XTDE1270");
                e.maybeSetLocation(this);
                throw e;
            }
            throw err;
        }
    }

    /**
     * Non-standard constructor to create an internal call on key() with a known key definition
     * @param keySet the set of KeyDefinitions (always a single KeyDefinition)
     * @param name the name allocated to the key (first argument of the function)
     * @param value the value being searched for (second argument of the function)
     * @param doc the document being searched (third argument)
     * @return a call on the key() function
     */

    public static KeyFn internalKeyCall(KeyDefinitionSet keySet, String name, Expression value, Expression doc) {
        KeyFn k = new KeyFn();
        k.argument = new Expression[] {new StringLiteral(name), value, doc};
        k.staticKeySet = keySet;
        k.checked = true;
        k.internal = true;
        k.setDetails(StandardFunction.getFunction("key", 3));
        k.setFunctionName(FN_KEY);
        k.adoptChildExpression(value);
        k.adoptChildExpression(doc);
        return k;
    }

    private final static StructuredQName FN_KEY = new StructuredQName("fn", NamespaceConstant.FN, "key");

    /**
     * Simplify: add a third implicit argument, the context document
     * @param visitor the expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        if (!internal && !(visitor.getStaticContext() instanceof ExpressionContext)) {
            throw new XPathException("The key() function is available only in XPath expressions within an XSLT stylesheet");
        }
        KeyFn f = (KeyFn)super.simplify(visitor);
        if (argument.length == 2) {
            f.addContextDocumentArgument(2, "key");
        }
        return f;
    }

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[1] = ExpressionTool.unsorted(opt, argument[1], false);
        if (argument[0] instanceof StringLiteral) {
            // common case, key name is supplied as a constant
            StructuredQName keyName;
            try {
                keyName = ((ExpressionContext)visitor.getStaticContext()).getStructuredQName(
                        ((StringLiteral)argument[0]).getStringValue(), false);
            } catch (XPathException e) {
                XPathException err = new XPathException("Error in key name " +
                        ((StringLiteral)argument[0]).getStringValue() + ": " + e.getMessage());
                err.setLocator(this);
                err.setErrorCode("XTDE1260");
                throw err;
            }
            staticKeySet = visitor.getExecutable().getKeyManager().getKeyDefinitionSet(keyName);
            if (staticKeySet == null) {
                XPathException err = new XPathException("Key " +
                        ((StringLiteral)argument[0]).getStringValue() + " has not been defined");
                err.setLocator(this);
                err.setErrorCode("XTDE1260");
                throw err;
            }
        } else {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
        }
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * a property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        int prop = StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
        if ((getNumberOfArguments() == 2) ||
                (argument[2].getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            prop |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        return prop;
    }

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor the expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        argument[0].addToPathMap(pathMap, pathMapNodeSet);
        argument[1].addToPathMap(pathMap, pathMapNodeSet);
        PathMap.PathMapNodeSet target = argument[2].addToPathMap(pathMap, pathMapNodeSet);
        // indicate that the function navigates to all nodes in the containing document
        AxisExpression root = new AxisExpression(Axis.ANCESTOR_OR_SELF, NodeKindTest.DOCUMENT);
        root.setContainer(getContainer());
        target = target.createArc(root);
        AxisExpression allElements = new AxisExpression(Axis.DESCENDANT, AnyNodeTest.getInstance());
        allElements.setContainer(getContainer());
        return target.createArc(allElements);
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        KeyFn k = (KeyFn)super.copy();
        k.nsContext = nsContext;
        k.staticKeySet = staticKeySet;
        k.internal = internal;
        k.checked = checked;
        return k;
    }

    /**
    * Enumerate the results of the expression
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        Controller controller = context.getController();

        Item arg2;
        try {
            arg2 = argument[2].evaluateItem(context);
        } catch (XPathException e) {
            if ("XPDY0002".equals(e.getErrorCodeLocalPart())) {
                dynamicError("Cannot call the key() function when there is no context item", "XTDE1270", context);
                return null;
            } else if ("XPDY0050".equals(e.getErrorCodeLocalPart())) {
                dynamicError("In the key() function," +
                            " the node supplied in the third argument (or the context node if absent)" +
                            " must be in a tree whose root is a document node", "XTDE1270", context);
                return null;
            } else if ("XPTY0020".equals(e.getErrorCodeLocalPart())) {
                dynamicError("Cannot call the key() function when the context item is an atomic value",
                        "XTDE1270", context);
                return null;
            }
            throw e;
        }

        NodeInfo origin = (NodeInfo)arg2;
        NodeInfo root = origin.getRoot();
        if (root.getNodeKind() != Type.DOCUMENT) {
            dynamicError("In the key() function," +
                            " the node supplied in the third argument (or the context node if absent)" +
                            " must be in a tree whose root is a document node", "XTDE1270", context);
            return null;
        }
        DocumentInfo doc = (DocumentInfo)root;

        KeyDefinitionSet selectedKeySet = staticKeySet;
        if (selectedKeySet == null) {
            String givenkeyname = argument[0].evaluateItem(context).getStringValue();
            StructuredQName qName = null;
            try {
                qName = StructuredQName.fromLexicalQName(
                            givenkeyname, false,
                            controller.getConfiguration().getNameChecker(),
                            nsContext);
            } catch (XPathException err) {
                dynamicError("Invalid key name: " + err.getMessage(), "XTDE1260", context);
            }
            selectedKeySet = controller.getKeyManager().getKeyDefinitionSet(qName);
            if (selectedKeySet == null) {
                dynamicError("Key '" + givenkeyname + "' has not been defined", "XTDE1260", context);
                return null;
            }
        }

//        if (internal) {
//            System.err.println("Using key " + fprint + " on doc " + doc);
//        }

        // If the second argument is a singleton, we evaluate the function
        // directly; otherwise we recurse to evaluate it once for each Item
        // in the sequence.

        Expression expression = argument[1];
        SequenceIterator allResults;
        if (Cardinality.allowsMany(expression.getCardinality())) {
            final XPathContext keyContext = context;
            final DocumentInfo document = doc;
            final KeyManager keyManager = controller.getKeyManager();
            final KeyDefinitionSet keySet = selectedKeySet;
            MappingFunction map = new MappingFunction() {
                // Map a value to the sequence of nodes having that value as a key value
                public SequenceIterator map(Item item) throws XPathException {
                    return keyManager.selectByKey(
                            keySet, document, (AtomicValue)item, keyContext);
                }
            };

            SequenceIterator keys = argument[1].iterate(context);
            SequenceIterator allValues = new MappingIterator(keys, map);
            allResults = new DocumentOrderIterator(allValues, LocalOrderComparer.getInstance());
        } else {
            try {
                AtomicValue keyValue = (AtomicValue)argument[1].evaluateItem(context);
                if (keyValue == null) {
                    return EmptyIterator.getInstance();
                }
                KeyManager keyManager = controller.getKeyManager();
                allResults = keyManager.selectByKey(selectedKeySet, doc, keyValue, context);
            } catch (XPathException e) {
                e.maybeSetLocation(this);
                throw e;
            }
        }
        if (origin == doc) {
            return allResults;
        }
        SubtreeFilter filter = new SubtreeFilter();
        filter.origin = origin;
        return new ItemMappingIterator(allResults, filter);
    }


    /**
     * Mapping class to filter nodes that have the origin node as an ancestor-or-self
     */

    private static class SubtreeFilter implements ItemMappingFunction {

        public NodeInfo origin;

        public Item map(Item item) throws XPathException {
            if (Navigator.isAncestorOrSelf(origin, (NodeInfo)item)) {
                return item;
            } else {
                return null;
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
