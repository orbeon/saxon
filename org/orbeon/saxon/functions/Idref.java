package net.sf.saxon.functions;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sort.DocumentOrderIterator;
import net.sf.saxon.sort.LocalOrderComparer;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.StringValue;


public class Idref extends SystemFunction implements MappingFunction {

    private class KeyContextInfo {
        public DocumentInfo document;
        public XPathContext context;
    }

    /**
    * Simplify: add a second implicit argument, the context document
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Idref f = (Idref)super.simplify(env);
        f.addContextDocumentArgument(1, "idref");
        return f;
    }

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        argument[0] = ExpressionTool.unsorted(argument[0], false);
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
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Enumerate the results of the expression
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        Controller controller = context.getController();

        Item arg2 = argument[1].evaluateItem(context);
        if (!(arg2 instanceof DocumentInfo)) {
            dynamicError("In the idref() function," +
                            " the context node must be in a tree whose root is a document node", context);
            return null;
        }
        DocumentInfo doc = (DocumentInfo)arg2;

        int fprint = StandardNames.XS_IDREFS;

        // If the argument is a singleton, we evaluate the function
        // directly; otherwise we recurse to evaluate it once for each Item
        // in the sequence.

        Expression expression = argument[0];
        if (!Cardinality.allowsMany(expression.getCardinality())) {
            AtomicValue keyValue = (AtomicValue)argument[0].evaluateItem(context);
            if (keyValue == null) {
                return EmptyIterator.getInstance();
            }
            KeyManager keyManager = controller.getKeyManager();
            return keyManager.selectByKey(fprint, doc, keyValue, context);

        } else {
            KeyContextInfo info = new KeyContextInfo();
            info.document = doc;
            info.context = context;

            SequenceIterator keys = argument[0].iterate(context);
            SequenceIterator allValues = new MappingIterator(keys, this, null, info);
            return new DocumentOrderIterator(allValues, LocalOrderComparer.getInstance());
        }
    }



    /**
    * Implement the MappingFunction interface
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        KeyContextInfo k = (KeyContextInfo)info;
        KeyManager keyManager = k.context.getController().getKeyManager();
        AtomicValue keyValue;
        if (item instanceof AtomicValue) {
            keyValue = (AtomicValue)item;
        } else {
            keyValue = new StringValue(item.getStringValue());
        }
        return keyManager.selectByKey(
                StandardNames.XS_IDREFS, k.document, keyValue, k.context);

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
