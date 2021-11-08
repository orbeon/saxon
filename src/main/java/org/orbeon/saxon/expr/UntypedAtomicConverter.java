package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

/**
 * An UntypedAtomicConverter is an expression that converts any untypedAtomic items in
 * a sequence to a specified type
 */

public final class UntypedAtomicConverter extends UnaryExpression {

    private AtomicType requiredItemType;
    private boolean allConverted;
    private boolean singleton = false;

    /**
     * Constructor
     *
     * @param sequence         this must be a sequence of atomic values. This is not checked; a ClassCastException
     *                         will occur if the precondition is not satisfied.
     * @param requiredItemType the item type to which untypedAtomic items in the sequence should be converted,
     *                         using the rules for "cast as".
     * @param allConverted     true if the result of this expression is a sequence in which all items
     *                         belong to the required type
     */

    public UntypedAtomicConverter(Expression sequence, AtomicType requiredItemType, boolean allConverted) {
        super(sequence);
        this.requiredItemType = requiredItemType;
        this.allConverted = allConverted;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
     * Get the item type to which untyped atomic items must be converted
     * @return the required item type
     */

    public ItemType getRequiredItemType() {
        return requiredItemType;
    }

    /**
     * Determine whether all items are to be converted, or only the subset that are untypedAtomic
     * @return true if all items are to be converted
     */

    public boolean areAllItemsConverted() {
        return allConverted;
    }

    /**
     * Determine the data type of the items returned by the expression
     *
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        ItemType it = operand.getItemType(th);
        singleton = it.isAtomicType() && !Cardinality.allowsMany(operand.getCardinality());
        if (allConverted) {
            return requiredItemType;
        } else {
            return Type.getCommonSuperType(requiredItemType, operand.getItemType(th), th);
        }
    }

    public int computeCardinality() {
        if (singleton) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return super.computeCardinality();
        }
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (allConverted && requiredItemType.isNamespaceSensitive()) {
            XPathException err = new XPathException("Cannot convert untypedAtomic values to QNames or NOTATIONs");
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand instanceof Literal) {
            return Literal.makeLiteral(
                    ((Value)SequenceExtent.makeSequenceExtent(
                            iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))).reduce());
        }
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        ItemType type = operand.getItemType(th);
        if (type instanceof NodeTest) {
            return this;
        }
        if (type.equals(BuiltInAtomicType.ANY_ATOMIC) || type instanceof AnyItemType ||
                type.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            return this;
        }
        // the sequence can't contain any untyped atomic values, so there's no need for a converter
        return operand;
    }

//    public Expression promote(PromotionOffer offer) throws XPathException {
//        // Don't try to loop-lift an UntypedAtomicConverter. This is because the type-checking on the
//        // resulting variable is not good enough to prevent a new UntypedAtomicConverter being added, ad infinitum.
//        // Bug 1930319
//        operand = doPromotion(operand, offer);
//        return this;
//    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE | StaticProperty.NOT_UNTYPED;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new UntypedAtomicConverter(getBaseExpression().copy(), requiredItemType, allConverted);
    }

    /**
     * Iterate over the sequence of values
     */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemMappingFunction converter = new ItemMappingFunction() {
            public Item map(Item item) throws XPathException {
                if (item instanceof UntypedAtomicValue) {
                    ConversionResult val = ((UntypedAtomicValue)item).convert(requiredItemType, true, context);
                    if (val instanceof ValidationFailure) {
                        ValidationFailure vex = (ValidationFailure)val;
                        vex.setLocator(UntypedAtomicConverter.this);
                        throw vex.makeException();
                    }
                    return (AtomicValue)val;
                } else {
                    return item;
                }
            }
        };
        return new ItemMappingIterator(base, converter);
    }


    /**
     * Evaluate as an Item. This should only be called if the UntypedAtomicConverter has cardinality zero-or-one
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item == null) {
            return null;
        }
        if (item instanceof UntypedAtomicValue) {
            ConversionResult val = ((UntypedAtomicValue)item).convert(requiredItemType, true, context);
            if (val instanceof ValidationFailure) {
                ValidationFailure err = (ValidationFailure)val;
                err.setLocator(this);
                throw err.makeException();
            } else {
                return (AtomicValue)val;
            }
        } else {
            return item;
        }
    }

     /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("convertUntypedAtomic");
        out.emitAttribute("to", requiredItemType.toString(out.getConfiguration().getNamePool()));
        out.emitAttribute("all", allConverted ? "true" : "false");
        operand.explain(out);
        out.endElement();
    }
    protected String displayOperator(Configuration config) {
        return "convert untyped atomic items to " + requiredItemType.toString(config.getNamePool());
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
