package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;

/**
* InstanceOf Expression: implements "Expr instance of data-type"
*/

public final class InstanceOfExpression extends UnaryExpression {

    ItemType targetType;
    int targetCardinality;

    public InstanceOfExpression(Expression source, SequenceType target) {
        super(source);
        targetType = target.getPrimaryType();
        if (targetType == null) {
            throw new IllegalArgumentException("Primary item type must not be null");
        }
        targetCardinality = target.getCardinality();
    }

    /**
    * Type-check the expression
    * @return the checked expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        if (operand instanceof Value) {
            return (AtomicValue)evaluateItem(null);
        }

        // See if we can get the answer by static analysis.

        if (Cardinality.subsumes(targetCardinality, operand.getCardinality())) {
            if (Type.isSubType(operand.getItemType(), targetType)) {
                return BooleanValue.get(true);
            }
        }
        return this;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((InstanceOfExpression)other).targetType &&
                targetCardinality == ((InstanceOfExpression)other).targetCardinality;
    }

     /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Determine the data type of the result of the InstanceOf expression
    */

    public ItemType getItemType() {
        return Type.BOOLEAN_TYPE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression as a boolean
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        int count = 0;
        while (true) {
            Item item = iter.next();
            if (item == null) break;
            count++;
            if (!targetType.matchesItem(item)) {
                return false;
            }
            if (count==2 && !Cardinality.allowsMany(targetCardinality)) {
                return false;
            }
        }
        if (count==0 && ((targetCardinality & StaticProperty.ALLOWS_ZERO)==0)) {
            return false;
        }
        return true;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "instance of " + targetType.toString(pool) + Cardinality.getOccurrenceIndicator(targetCardinality);
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "instance of" );
        operand.display(level+1, pool, out);
        out.println(ExpressionTool.indent(level+1) +
                targetType.toString(pool) +
                Cardinality.getOccurrenceIndicator(targetCardinality));
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
