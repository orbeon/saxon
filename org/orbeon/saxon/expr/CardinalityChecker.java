package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.ObjectValue;

/**
* A CardinalityChecker implements the cardinality checking of "treat as": that is,
* it returns the supplied sequence, checking that its cardinality is correct
*/

public final class CardinalityChecker extends UnaryExpression implements MappingFunction {

    private int requiredCardinality = -1;
    private RoleLocator role;

    /**
    * Constructor
    */

    public CardinalityChecker(Expression sequence, int cardinality, RoleLocator role) {
        super(sequence);
        this.requiredCardinality = cardinality;
        this.role = role;
        computeStaticProperties();
        adoptChildExpression(sequence);
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        if (requiredCardinality == StaticProperty.ALLOWS_ZERO_OR_MORE) {
            return operand;
        }
        //int x = operand.getCardinality();
        if (Cardinality.subsumes(requiredCardinality, operand.getCardinality())) {
            return operand;
        }
        return this;
    }

    /**
     * Set the error code to be returned (this is used when evaluating the functions such
     * as exactly-one() which have their own error codes)
     */

    public void setErrorCode(String code) {
        role.setErrorCode(code);
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);

        ObjectValue stopper;
        if (!Cardinality.allowsZero(requiredCardinality)) {
            // To check for an empty sequence, we add a special item to the base
            // iteration, to ensure that the mapping function gets called at least
            // once. This item will cause an error if it is the first in the sequence,
            // and will be ignored otherwise.
            stopper = new ObjectValue(this);
            base = new AppendIterator(base, stopper, context);
        }
        return new MappingIterator(base, this, null, base);
    }

    /**
    * Mapping function
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        int pos = ((SequenceIterator)info).position();
        if (item instanceof ObjectValue && ((ObjectValue)item).getObject() == this) {
            // we've hit the stopper object
            if (pos==1) {
                 typeError("An empty sequence is not allowed as the " +
                         role.getMessage(), role.getErrorCode(), context);
            }
            // don't include the stopper in the result
            return null;
        }
        if (pos==2 && !Cardinality.allowsMany(requiredCardinality)) {
            typeError(
                    "A sequence of more than one item is not allowed as the " +
                    role.getMessage(), role.getErrorCode(), context);
            return null;
        }
        return item;
    }

    /**
    * Evaluate as an Item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        Item item = null;
        while (true) {
            Item nextItem = iter.next();
            if (nextItem == null) break;
            if (item != null) {
                typeError("A sequence of more than one item is not allowed as the " +
                    role.getMessage(), role.getErrorCode(), context);
                return null;
            }
            item = nextItem;
        }
        if (item == null && !Cardinality.allowsZero(requiredCardinality)) {
            typeError("An empty sequence is not allowed as the " +
                    role.getMessage(), role.getErrorCode(), context);
            return null;
        }
        return item;
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
    */

	public ItemType getItemType() {
	    return operand.getItemType();
	}

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        return requiredCardinality;
	}

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return operand.getSpecialProperties();
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return super.equals(other) &&
                this.requiredCardinality == ((CardinalityChecker)other).requiredCardinality;
    }

    /**
    * Diagnostic print of expression structure
    */

    public String displayOperator(NamePool pool) {
        return "checkCardinality (" + Cardinality.toString(requiredCardinality) + ')';
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
