package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;

/**
* A NumericPromoter performs numeric promotion on each item in a supplied sequence
*/

public final class NumericPromoter extends UnaryExpression implements MappingFunction {

    private int requiredType; // always xs:float or xs:double

    /**
    * Constructor
    * @param sequence this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredType the item type to which all items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public NumericPromoter(Expression sequence, int requiredType) {
        super(sequence);
        this.requiredType = requiredType;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (operand instanceof Value) {
            return new SequenceExtent(iterate(null));
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        return this;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return new MappingIterator(base, this, null, null);
    }

    /**
    * Evaluate as an Item. This should only be called if the expression has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return promote(((AtomicValue)item), context);
    }

    /**
    * Implement the mapping function
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        return promote(((AtomicValue)item), context);
    }

    /**
     * Perform the promotion
     */

    private AtomicValue promote(AtomicValue value, XPathContext context) throws XPathException {
        AtomicValue v = value.getPrimitiveValue();
        if (!(v instanceof NumericValue || v instanceof UntypedAtomicValue)) {
            DynamicError err = new DynamicError("Cannot promote non-numeric value to " + getItemType().toString());
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        return v.convert(requiredType, context);
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
    */

	public ItemType getItemType() {
        if (requiredType == StandardNames.XS_DOUBLE) {
            return Type.DOUBLE_TYPE;
        } else {
	        return Type.FLOAT_TYPE;
        }
	}

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredType == ((NumericPromoter)other).requiredType;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "promote items to " + getItemType().toString(pool);
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
