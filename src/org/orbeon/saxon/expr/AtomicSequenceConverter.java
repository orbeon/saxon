package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.Value;

/**
* An AtomicSequenceConverter is an expression that performs a cast on each member of
* a supplied sequence
*/

public final class AtomicSequenceConverter extends UnaryExpression {

    private AtomicType requiredItemType;

    private BuiltInAtomicType requiredPrimitiveType;

    /**
    * Constructor
    * @param sequence this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredItemType the item type to which all items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public AtomicSequenceConverter(Expression sequence, AtomicType requiredItemType) {
        super(sequence);
        this.requiredItemType = requiredItemType;
        requiredPrimitiveType = (BuiltInAtomicType)requiredItemType.getPrimitiveItemType();
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
     * Get the required (target) primitive type
     * @return the required primitive type
     */

    public AtomicType getRequiredPrimitiveType() {
        return requiredPrimitiveType;
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (operand instanceof Literal) {
            ValueRepresentation val = SequenceExtent.makeSequenceExtent(
                    iterate(visitor.getStaticContext().makeEarlyEvaluationContext()));
            return Literal.makeLiteral(Value.asValue(val));
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (th.isSubType(operand.getItemType(th), requiredItemType)) {
            return operand;
        } else if (!Cardinality.allowsMany(operand.getCardinality())) {
            CastExpression cast = new CastExpression(operand, requiredItemType,
                                        (operand.getCardinality() & StaticProperty.ALLOWS_ZERO) != 0);
            ExpressionTool.copyLocationInfo(this, cast);
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
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new AtomicSequenceConverter(getBaseExpression().copy(), requiredItemType);
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemMappingFunction converter = new ItemMappingFunction() {
            public Item map(Item item) throws XPathException {
                return ((AtomicValue)item).convert(requiredPrimitiveType, true, context).asAtomic();
            }
        };
        return new ItemMappingIterator(base, converter);
    }

    /**
    * Evaluate as an Item. This should only be called if the AtomicSequenceConverter has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return ((AtomicValue)item).convert(requiredPrimitiveType, true, context).asAtomic();
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return requiredItemType;
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
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("convertItems");
        destination.emitAttribute("to", requiredItemType.toString(destination.getNamePool()));
        operand.explain(destination);
        destination.endElement();
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
