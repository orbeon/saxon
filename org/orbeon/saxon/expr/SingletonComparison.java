package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.sort.AtomicComparer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;

import java.util.Comparator;


/**
* Class to handle comparisons of singletons. Unlike ValueComparison, this class
* converts untyped atomic values to the type of the other argument, and returns false
 * (rather than ()) if either operand is ().
*/

public class SingletonComparison extends BinaryExpression {

    private AtomicComparer comparer;

    public SingletonComparison(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    public void setComparator(Comparator comp) {
        if (comp instanceof AtomicComparer) {
            comparer = (AtomicComparer)comp;
        } else {
            comparer = new AtomicComparer(comp);
        }
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }
    /**
    * Type-check the expression
    * @return the checked expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        //backwardsCompatible = env.isInBackwardsCompatibleMode();
        return super.analyze(env, contextItemType);
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
    }

    /**
    * Evaluate the expression in a given context
    * @param context the given context for evaluation
    * @return a BooleanValue representing the result of the numeric comparison of the two operands
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression in a boolean context
    * @param context the given context for evaluation
    * @return a boolean representing the result of the numeric comparison of the two operands
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        AtomicValue v1 = (AtomicValue)operand0.evaluateItem(context);
        if (v1==null) return false;
        AtomicValue v2 = (AtomicValue)operand1.evaluateItem(context);
        if (v2==null) return false;

        try {
            return GeneralComparison.compare(v1, operator, v2, comparer, context);
        } catch (DynamicError e) {
            // re-throw the exception with location information added
            if (e.getXPathContext() == null) {
                e.setXPathContext(context);
            }
            if (e.getLocator() == null) {
                e.setLocator(this);
            }
            throw e;
        }
    }

    protected String displayOperator() {
        return "singleton " + super.displayOperator();
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
