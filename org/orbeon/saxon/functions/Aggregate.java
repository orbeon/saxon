package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

/**
* This class implements the sum(), avg(), count() functions,
*/

public class Aggregate extends SystemFunction {

    public static final int SUM = 0;
    public static final int AVG = 1;
    public static final int COUNT = 4;

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(StaticContext env) throws XPathException {
        super.checkArguments(env);
        Optimizer opt = env.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], true);
    }

    /**
     * Determine the item type of the value returned by the function
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        switch (operation) {
            case COUNT:
                return super.getItemType(th);
            case SUM: {
                //ItemType base = argument[0].getItemType();
                ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
                if (base == Type.UNTYPED_ATOMIC_TYPE) {
                    base = Type.DOUBLE_TYPE;
                }
                if (Cardinality.allowsZero(argument[0].getCardinality())) {
                    if (argument.length == 1) {
                        return Type.getCommonSuperType(base, Type.INTEGER_TYPE, th);
                    } else {
                        return Type.getCommonSuperType(base, argument[1].getItemType(th), th);
                    }
                } else {
                    return base;
                }
            }
            case AVG: {
                ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
                if (base == Type.UNTYPED_ATOMIC_TYPE) {
                    return Type.DOUBLE_TYPE;
                } else if (base.getPrimitiveType() == Type.INTEGER) {
                    return Type.DECIMAL_TYPE;
                } else {
                    return base;
                }
            }
            default:
                throw new AssertionError("Unknown aggregate operation");
        }
    }


    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        // Note: these functions do not need to sort the underlying sequence,
        // but they do need to de-duplicate it
        switch (operation) {
            case COUNT:
                SequenceIterator iter = argument[0].iterate(context);
                return new IntegerValue(count(iter));
            case SUM:
                return total(argument[0].iterate(context), context);
            case AVG:
                return average(argument[0].iterate(context), context);
            default:
                throw new UnsupportedOperationException("Unknown aggregate function");
        }
    }

    /**
    * Calculate total
    */

    private AtomicValue total(SequenceIterator iter, XPathContext context) throws XPathException {
        AtomicValue sum = (AtomicValue)iter.next();
        if (sum == null) {
            // the sequence is empty
            if (argument.length == 2) {
                return (AtomicValue)argument[1].evaluateItem(context);
            } else {
                return IntegerValue.ZERO;
            }
        }
        if (!sum.hasBuiltInType()) {
            sum = sum.getPrimitiveValue();
        }
        if (sum instanceof UntypedAtomicValue) {
            try {
                sum = sum.convert(Type.DOUBLE, context);
            } catch (XPathException e) {
                if (e.getLocator() == null || e.getLocator().getLineNumber() == -1) {
                    e.setLocator(this);
                }
                throw e;
            }
        }
        if (sum instanceof NumericValue) {
            while (true) {
                AtomicValue nextVal = (AtomicValue)iter.next();
                if (nextVal == null) {
                    return sum;
                }
                AtomicValue next = nextVal.getPrimitiveValue();
                if (next instanceof UntypedAtomicValue) {
                    next = next.convert(Type.DOUBLE, context);
                } else if (!(next instanceof NumericValue)) {
                    DynamicError err =
                            new DynamicError("Input to sum() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(this);
                    throw err;
                }
                sum = ((NumericValue)sum).arithmetic(Token.PLUS, (NumericValue)next, context);
                if (((NumericValue)sum).isNaN()) {
                    // take an early bath, once we've got a NaN it's not going to change
                    // TODO: it could change from a float NaN to a double NaN...
                    return sum;
                }
            }
        } else if (sum instanceof DurationValue) {
            while (true) {
                AtomicValue nextVal = (AtomicValue)iter.next();
                if (nextVal == null) {
                    return sum;
                }
                AtomicValue next = nextVal.getPrimitiveValue();
                if (!(next instanceof DurationValue)) {
                    DynamicError err =
                            new DynamicError("Input to sum() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(this);
                    throw err;
                }
                sum = ((DurationValue)sum).add((DurationValue)next);
            }
        } else {
            DynamicError err =
                    new DynamicError("Input to sum() contains a value that is neither numeric, nor a duration");
            err.setXPathContext(context);
            err.setErrorCode("FORG0006");
            err.setLocator(this);
            throw err;
        }
    }

    /**
    * Calculate average
    */

    private AtomicValue average(SequenceIterator iter, XPathContext context) throws XPathException {
        int count = 0;
        AtomicValue item = (AtomicValue)iter.next();
        if (item == null) {
            // the sequence is empty
            return null;
        }
        count++;
        if (!item.hasBuiltInType()) {
            item = item.getPrimitiveValue();
        }
        if (item instanceof UntypedAtomicValue) {
            try {
                item = item.convert(Type.DOUBLE, context);
            } catch (XPathException e) {
                if (e.getLocator() == null || e.getLocator().getLineNumber() == -1) {
                    e.setLocator(this);
                }
                throw e;
            }
        }
        if (item instanceof NumericValue) {
            while (true) {
                AtomicValue nextVal = (AtomicValue)iter.next();
                if (nextVal == null) {
                    return ((NumericValue)item).arithmetic(Token.DIV, new IntegerValue(count), context);
                }
                count++;
                AtomicValue next = nextVal.getPrimitiveValue();
                if (next instanceof UntypedAtomicValue) {
                    try {
                        next = next.convert(Type.DOUBLE, context);
                    } catch (XPathException e) {
                        if (e.getLocator() == null || e.getLocator().getLineNumber() == -1) {
                            e.setLocator(this);
                        }
                        throw e;
                    }
                } else if (!(next instanceof NumericValue)) {
                    DynamicError err =
                            new DynamicError("Input to avg() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(this);
                    throw err;
                }
                item = ((NumericValue)item).arithmetic(Token.PLUS, (NumericValue)next, context);
                if (((NumericValue)item).isNaN()) {
                    // take an early bath, once we've got a NaN it's not going to change
                    // TODO: it could change from a float NaN to a double NaN...
                    return item;
                }
            }
        } else if (item instanceof DurationValue) {
            while (true) {
                AtomicValue nextVal = (AtomicValue)iter.next();
                if (nextVal == null) {
                    return ((DurationValue)item).multiply(1.0/count);
                }
                count++;
                AtomicValue next = nextVal.getPrimitiveValue();
                if (!(next instanceof DurationValue)) {
                    DynamicError err =
                            new DynamicError("Input to avg() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(this);
                    throw err;
                }
                item = ((DurationValue)item).add((DurationValue)next);
            }
        } else {
            DynamicError err =
                    new DynamicError("Input to avg() contains a value that is neither numeric, nor a duration");
            err.setXPathContext(context);
            err.setErrorCode("FORG0006");
            err.setLocator(this);
            throw err;
        }
    }


    /**
     * Get the number of items in a sequence identified by a SequenceIterator
     * @param iter The SequenceIterator. This method moves the current position
     * of the supplied iterator; if this isn't safe, make a copy of the iterator
     * first by calling getAnother(). The supplied iterator must be positioned
     * before the first item (there must have been no call on next()).
     * @return the number of items in the underlying sequence
     * @throws XPathException if a failure occurs reading the input sequence
     */

    public static int count(SequenceIterator iter) throws XPathException {
        if ((iter.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            return ((LastPositionFinder)iter).getLastPosition();
        } else {
            int n = 0;
            while (iter.next() != null) {
                n++;
            }
            return n;
        }
    }

    /**
     * Determine whether a given expression is a call to the count() function
     */

    public static boolean isCountFunction(Expression exp) {
        if (!(exp instanceof Aggregate)) return false;
        Aggregate ag = (Aggregate)exp;
        return ag.getNumberOfArguments() == 1 && ag.operation == COUNT;
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
