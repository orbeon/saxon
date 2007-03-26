package org.orbeon.saxon.functions;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.LocalOrderComparer;
import org.orbeon.saxon.style.ExpressionContext;
import org.orbeon.saxon.trans.KeyManager;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;


public class KeyFn extends SystemFunction implements XSLTFunction {

    private NamespaceResolver nsContext = null;
    private int keyFingerprint = -1;
    private transient boolean checked = false;
    private transient boolean internal = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        try {
            return super.typeCheck(env, contextItemType);
        } catch (XPathException err) {
            if ("XPDY0002".equals(err.getErrorCodeLocalPart())) {
                DynamicError e = new DynamicError("Cannot call the key() function when there is no context node");
                e.setErrorCode("XTDE1270");
                throw e;
            }
            throw err;
        }
    }

    /**
     * Non-standard constructor to create an internal call on key() with a known key fingerprint
     */

    public static KeyFn internalKeyCall(NamePool pool, int fingerprint, String name, Expression value, Expression doc) {
        KeyFn k = new KeyFn();
        Expression[] arguments = {new StringValue(name), value, doc};
        k.argument = arguments;
        k.keyFingerprint= fingerprint;
        k.checked = true;
        k.internal = true;
        k.setDetails(StandardFunction.getFunction("key", 3));
        k.setFunctionNameCode(pool.allocate("fn", NamespaceConstant.FN, "key"));
        k.adoptChildExpression(value);
        k.adoptChildExpression(doc);
        return k;
    }

    /**
    * Simplify: add a third implicit argument, the context document
    */

     public Expression simplify(StaticContext env) throws XPathException {
        if (!internal && !(env instanceof ExpressionContext)) {
            throw new StaticError("The key() function is available only in XPath expressions within an XSLT stylesheet");
        }
        KeyFn f = (KeyFn)super.simplify(env);
        if (argument.length == 2) {
            f.addContextDocumentArgument(2, "key");
        }
        return f;
    }

    public void checkArguments(StaticContext env) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(env);
        Optimizer opt = env.getConfiguration().getOptimizer();
        argument[1] = ExpressionTool.unsorted(opt, argument[1], false);
        if (argument[0] instanceof StringValue) {
            // common case, key name is supplied as a constant
            try {
                keyFingerprint = ((ExpressionContext)env).getFingerprint(((StringValue)argument[0]).getStringValue(), false);
            } catch (XPathException e) {
                StaticError err = new StaticError("Error in key name " +
                        ((StringValue)argument[0]).getStringValue() + ": " + e.getMessage());
                err.setLocator(this);
                err.setErrorCode("XTDE1260");
                throw err;
            }
            if (keyFingerprint==-1) {
                StaticError err = new StaticError("Key " +
                        ((StringValue)argument[0]).getStringValue() + " has not been defined");
                err.setLocator(this);
                err.setErrorCode("XTDE1260");
                throw err;
            }
        } else {
            // we need to save the namespace context
            nsContext = env.getNamespaceResolver();
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
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
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

        int fprint = keyFingerprint;
        if (fprint == -1) {
            String givenkeyname = argument[0].evaluateItem(context).getStringValue();
            try {
                fprint = controller.getNamePool().allocateLexicalQName(
                        givenkeyname, false, nsContext,
                        controller.getConfiguration().getNameChecker()) & NamePool.FP_MASK;
            } catch (XPathException err) {
                dynamicError("Invalid key name: " + err.getMessage(), "XTDE1260", context);
            }
            if (fprint==-1) {
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
            MappingFunction map = new MappingFunction() {
                // Map a value to the sequence of nodes having that value as a key value
                public Object map(Item item) throws XPathException {
                    return keyManager.selectByKey(
                            keyFingerprint, document, (AtomicValue)item, keyContext);
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
                allResults = keyManager.selectByKey(fprint, doc, keyValue, context);
            } catch (XPathException e) {
                if (e.getLocator() == null) {
                    e.setLocator(this);
                }
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
    * Implement the MappingFunction interface
    */

//    private static class KeyMappingFunction implements MappingFunction {
//
//        public XPathContext keyContext;
//        public int keyFingerprint;
//        public DocumentInfo document;
//
//        public Object map(Item item) throws XPathException {
//            KeyManager keyManager = keyContext.getController().getKeyManager();
//            return keyManager.selectByKey(
//                    keyFingerprint, document, (AtomicValue)item, keyContext);
//        }
//    }

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
