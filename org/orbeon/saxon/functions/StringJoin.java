package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.FunctionCall;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.StringValue;

/**
* xf:string-join(string* $sequence, string $separator)
*/

public class StringJoin extends SystemFunction {

    /**
    * Type-check the expression. This simplifies the expression if the first argument is a singleton:
    * important as this is common for attribute value templates.
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression exp = super.analyze(env, contextItemType);
        if (exp instanceof StringJoin) {
            return ((StringJoin)exp).simplifySingleton(env);
        } else {
            return exp;
        }
    }

    private Expression simplifySingleton(StaticContext env) {
        int card = argument[0].getCardinality();
        if (!Cardinality.allowsMany(card)) {
            if (Cardinality.allowsZero(card)) {
                FunctionCall f = SystemFunction.makeSystemFunction("string", 1, env.getNamePool());
                Expression[] args = {argument[0]};
                f.setArguments(args);
                return f;
            } else {
                return argument[0];
            }
        }
        return this;
    }

    public Item evaluateItem(XPathContext c) throws XPathException {

        // This rather tortuous code is designed to ensure that we don't evaluate the
        // separator argument unless there are at least two items in the sequence.

        SequenceIterator iter = argument[0].iterate(c);
        Item it = iter.next();
        if (it==null) {
            return StringValue.EMPTY_STRING;
        }

        String first = it.getStringValue();

        it = iter.next();
        if (it==null) {
            return new StringValue(first);
        }

        FastStringBuffer sb = new FastStringBuffer(1024);
        sb.append(first);

        // Type checking ensures that the separator is not an empty sequence
        CharSequence sep = argument[1].evaluateItem(c).getStringValueCS();
        sb.append(sep);
        sb.append(it.getStringValueCS());

        while (true) {
            it = iter.next();
            if (it == null) {
                return new StringValue(sb.condense());
            }
            sb.append(sep);
            sb.append(it.getStringValueCS());
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
