package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;

/**
* An AtomicSequenceConverter is an expression that performs a cast on each member of
* a supplied sequence
*/

public final class AtomicSequenceConverter extends UnaryExpression implements MappingFunction {

    private AtomicType reqItemType;
    private int requiredPrimitiveType;

    /**
    * Constructor
    * @param sequence this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredItemType the item type to which all items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public AtomicSequenceConverter(Expression sequence, AtomicType requiredItemType) {
        super(sequence);
        this.reqItemType = requiredItemType;
        this.requiredPrimitiveType = requiredItemType.getPrimitiveType();
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
        if (Type.isSubType(operand.getItemType(), reqItemType)) {
            return operand;
        } else if (!Cardinality.allowsMany(operand.getCardinality())) {
            CastExpression cast = new CastExpression(operand, reqItemType,
                                        (operand.getCardinality() & StaticProperty.ALLOWS_ZERO) != 0);
            ExpressionTool.copyLocationInfo(this, cast);
            cast.setParentExpression(getParentExpression());
            return cast;
        } else {
            return this;
        }
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return new MappingIterator(base, this, null, null);
    }

    /**
    * Evaluate as an Item. This should only be called if the AtomicSequenceConverter has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return ((AtomicValue)item).convert(requiredPrimitiveType);
    }

    /**
    * Implement the mapping function
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        return ((AtomicValue)item).convert(requiredPrimitiveType);
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
    */

	public ItemType getItemType() {
	    return reqItemType;
	}

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        return operand.getCardinality();
	}

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredPrimitiveType == ((AtomicSequenceConverter)other).requiredPrimitiveType;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "convert items to " + reqItemType.toString(pool);
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
