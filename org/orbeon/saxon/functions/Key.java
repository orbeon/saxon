package net.sf.saxon.functions;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.DocumentOrderIterator;
import net.sf.saxon.sort.LocalOrderComparer;
import net.sf.saxon.style.ExpressionContext;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.StringValue;


public class Key extends SystemFunction implements MappingFunction, XSLTFunction {

    private static class KeyContextInfo {
        public DocumentInfo document;
        public XPathContext context;
        public int keyFingerprint;
    }

    private NamespaceResolver nsContext = null;
    private int keyFingerprint = -1;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    /**
    * Simplify: add a third implicit argument, the context document
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Key f = (Key)super.simplify(env);
        if (argument.length == 2) {
            f.addContextDocumentArgument(2, "key");
        }
        return f;
    }

    public void checkArguments(StaticContext env) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(env);
        argument[1] = ExpressionTool.unsorted(argument[1], false);
        if (argument[0] instanceof StringValue) {
            // common case, key name is supplied as a constant
            try {
                keyFingerprint = ((ExpressionContext)env).getFingerprint(((StringValue)argument[0]).getStringValue(), false);
            } catch (XPathException e) {
                StaticError err = new StaticError("Error in key name " +
                        ((StringValue)argument[0]).getStringValue() + ": " + e.getMessage());
                err.setLocator(this);
                err.setErrorCode("XT1260");
                throw err;
            }
            if (keyFingerprint==-1) {
                StaticError err = new StaticError("Key " +
                        ((StringValue)argument[0]).getStringValue() + " has not been defined");
                err.setLocator(this);
                err.setErrorCode("XT1260");
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

        Item arg2 = argument[2].evaluateItem(context);
        if (!(arg2 instanceof NodeInfo)) {
            dynamicError("When calling the key() function, the context item must be a node", "XT1270", context);
            return null;
        }
        NodeInfo origin = (NodeInfo)arg2;
        NodeInfo root = origin.getRoot();
        if (!(root instanceof DocumentInfo)) {
            dynamicError("In the key() function," +
                            " the node supplied in the third argument (or the context node if absent)" +
                            " must be in a tree whose root is a document node", "XT1270", context);
            return null;
        }
        DocumentInfo doc = (DocumentInfo)root;

        int fprint = keyFingerprint;
        if (fprint == -1) {
            String givenkeyname = argument[0].evaluateItem(context).getStringValue();
            try {
                fprint = context.getController().getNamePool().allocateLexicalQName(
                        givenkeyname, false, nsContext) & NamePool.FP_MASK;
            } catch (XPathException err) {
                dynamicError("Invalid key name: " + err.getMessage(), "XT1260", context);
            }
            if (fprint==-1) {
                dynamicError("Key '" + givenkeyname + "' has not been defined", "XT1260", context);
                return null;
            }
        }

        // If the second argument is a singleton, we evaluate the function
        // directly; otherwise we recurse to evaluate it once for each Item
        // in the sequence.

        Expression expression = argument[1];
        SequenceIterator allResults;
        if (Cardinality.allowsMany(expression.getCardinality())) {
            KeyContextInfo info = new KeyContextInfo();
            info.document = doc;
            info.context = context;
            info.keyFingerprint = fprint;

            SequenceIterator keys = argument[1].iterate(context);
            SequenceIterator allValues = new MappingIterator(keys, this, null, info);
            allResults = new DocumentOrderIterator(allValues, LocalOrderComparer.getInstance());
        } else {
            AtomicValue keyValue = (AtomicValue)argument[1].evaluateItem(context);
            if (keyValue == null) {
                return EmptyIterator.getInstance();
            }
            KeyManager keyManager = controller.getKeyManager();
            allResults = keyManager.selectByKey(fprint, doc, keyValue, context);
        }
        if (origin == doc) {
            return allResults;
        }
        return new MappingIterator(allResults, new SubtreeFilter(), null, origin);
    }



    /**
    * Implement the MappingFunction interface
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        KeyContextInfo k = (KeyContextInfo)info;
        KeyManager keyManager = k.context.getController().getKeyManager();
        return keyManager.selectByKey(
                k.keyFingerprint, k.document, (AtomicValue)item, k.context);
    }

    /**
     * Mapping class to filter nodes that have the origin node as an ancestor-or-self
     */

    private static class SubtreeFilter implements MappingFunction {

        // TODO: much more efficient implementations are possible, especially with the TinyTree

        public Object map(Item item, XPathContext context, Object info) throws XPathException {
            if (isAncestorOrSelf((NodeInfo)info, (NodeInfo)item)) {
                return item;
            } else {
                return null;
            }
        }

        /**
         * Test if one node is an ancestor-or-self of another
         * @param a the putative ancestor-or-self node
         * @param d the putative descendant node
         * @return true if a is an ancestor-or-self of d
         */

        private static boolean isAncestorOrSelf(NodeInfo a, NodeInfo d) {
            NodeInfo p = d;
            while (p != null) {
                if (a.isSameNodeInfo(p)) {
                    return true;
                }
                p = p.getParent();
            }
            return false;
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
