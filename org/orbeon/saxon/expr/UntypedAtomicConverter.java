package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

/**
 * An UntypedAtomicConverter is an expression that converts any untypedAtomic items in
 * a sequence to a specified type
 */

public final class UntypedAtomicConverter extends UnaryExpression {

    private AtomicType requiredItemType;
    private boolean allConverted;

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
     * Determine the data type of the items returned by the expression
     *
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (allConverted) {
            return requiredItemType;
        } else {
            return Type.getCommonSuperType(requiredItemType, operand.getItemType(th), th);
        }
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        if (allConverted && requiredItemType.isNamespaceSensitive()) {
            StaticError err = new StaticError("Cannot convert untypedAtomic values to QNames or NOTATIONs");
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        operand = operand.typeCheck(env, contextItemType);
        if (operand instanceof Value) {
            return ((Value)SequenceExtent.makeSequenceExtent(iterate(env.makeEarlyEvaluationContext()))).reduce();
        }
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        ItemType type = operand.getItemType(th);
        if (type instanceof NodeTest) {
            return this;
        }
        if (type == Type.ANY_ATOMIC_TYPE || type instanceof AnyItemType ||
                type == Type.UNTYPED_ATOMIC_TYPE) {
            return this;
        }
        // the sequence can't contain any untyped atomic values, so there's no need for a converter
        ComputedExpression.setParentExpression(operand, getParentExpression());
        return operand;
    }


    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
     * Iterate over the sequence of values
     */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemMappingFunction converter = new ItemMappingFunction() {
            public Item map(Item item) throws XPathException {
                if (item instanceof UntypedAtomicValue) {
                    AtomicValue val = ((UntypedAtomicValue)item).convert(requiredItemType, context, true);
                    if (val instanceof ValidationErrorValue) {
                        ValidationException vex = ((ValidationErrorValue)val).getException();
                        if (vex.getLineNumber() == -1) {
                            vex.setLocator(ExpressionTool.getLocator(UntypedAtomicConverter.this));
                        }
                        throw vex;
                    }
                    return val;
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
            try {
                AtomicValue val = ((UntypedAtomicValue)item).convert(requiredItemType, context, true);
                if (val instanceof ValidationErrorValue) {
                    throw ((ValidationErrorValue)val).getException();
                }
                return val;
            } catch (XPathException e) {
                if (e.getLocator() == null) {
                    e.setLocator(this);
                }
                throw e;
            }
        } else {
            return item;
        }
    }

    /**
     * Implement the mapping function
     */

//    public Object map(Item item) throws XPathException {
//        if (item instanceof UntypedAtomicValue) {
//            Value val = ((UntypedAtomicValue)item).convert(requiredItemType, context, true);
//            if (val instanceof ValidationErrorValue) {
//                throw ((ValidationErrorValue)val).getException();
//            }
//            return val;
//        } else {
//            return item;
//        }
//    }

    /**
     * Give a string representation of the operator for use in diagnostics
     *
     * @param config
     * @return the operator, as a string
     */

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
