package org.orbeon.saxon.functions;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.DocumentOrderIterator;
import org.orbeon.saxon.sort.LocalOrderComparer;
import org.orbeon.saxon.style.ExpressionContext;
import org.orbeon.saxon.trans.KeyManager;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.StaticError;
import org.orbeon.saxon.xpath.XPathException;


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
            keyFingerprint = ((ExpressionContext)env).getFingerprint(((StringValue)argument[0]).getStringValue(), false);
            if (keyFingerprint==-1) {
                throw new StaticError("Key " + ((StringValue)argument[0]).getStringValue() + " is not defined");
            }
        } else {
            // we need to save the namespace context
            nsContext = env.getNamespaceResolver();
        }
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        int prop = StaticProperty.ORDERED_NODESET | StaticProperty.NON_CREATIVE;
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
        NodeInfo root= ((NodeInfo)arg2).getRoot();
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
                fprint = nsContext.getFingerprint(givenkeyname, false, context.getController().getNamePool());
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
        if (Cardinality.allowsMany(expression.getCardinality())) {
            KeyContextInfo info = new KeyContextInfo();
            info.document = doc;
            info.context = context;
            info.keyFingerprint = fprint;

            SequenceIterator keys = argument[1].iterate(context);
            SequenceIterator allValues = new MappingIterator(keys, this, null, info);
            return new DocumentOrderIterator(allValues, LocalOrderComparer.getInstance());
        } else {
            AtomicValue keyValue = (AtomicValue)argument[1].evaluateItem(context);
            if (keyValue == null) {
                return EmptyIterator.getInstance();
            }
            KeyManager keyManager = controller.getKeyManager();
            return keyManager.selectByKey(fprint, doc, keyValue, context);

        }
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
