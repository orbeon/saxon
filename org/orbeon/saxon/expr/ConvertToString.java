package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;

/**
* A ConvertToString expression performs the string conversion required for string arguments
* in backwards compatibility mode. If given an empty sequence, it returns an empty string; otherwise,
* it returns the string value of the first item in the sequence.
*/

public final class ConvertToString extends UnaryExpression {

    /**
    * Constructor
    * @param base A sequence expression denoting sequence whose first item is to be returned
    */

    public ConvertToString(Expression base) {
        super(base);
    }

    /**
    * Get the data type of the items returned
    */

    public ItemType getItemType() {
        return Type.STRING_TYPE;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        if (operand.getCardinality() == StaticProperty.EXACTLY_ONE &&
                Type.isSubType(operand.getItemType(), Type.STRING_TYPE)) {
            return operand;
        }
        return this;
    }

    /**
    * Get the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        Item it = iter.next();
        if (it == null) {
            return StringValue.EMPTY_STRING;
        } else {
            return new StringValue(it.getStringValue());
        }
    }

    /**
    * Diagnostic print of expression structure
    */

    public String displayOperator(NamePool pool) {
        return "convert to string";
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
