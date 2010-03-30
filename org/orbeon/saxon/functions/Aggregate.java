package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

import javax.xml.transform.SourceLocator;

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

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], true);
        // we don't care about the order of the results, but we do care about how many nodes there are
    }

    /**
     * Determine the item type of the value returned by the function
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        switch (operation) {
            case COUNT:
                return super.getItemType(th);
            case SUM: {
                //ItemType base = argument[0].getItemType();
                ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
                if (base.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    base = BuiltInAtomicType.DOUBLE;
                }
                if (Cardinality.allowsZero(argument[0].getCardinality())) {
                    if (argument.length == 1) {
                        return Type.getCommonSuperType(base, BuiltInAtomicType.INTEGER, th);
                    } else {
                        return Type.getCommonSuperType(base, argument[1].getItemType(th), th);
                    }
                } else {
                    return base;
                }
            }
            case AVG: {
                ItemType base = Atomizer.getAtomizedItemType(argument[0], false, th);
                if (base.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    return BuiltInAtomicType.DOUBLE;
                } else if (base.getPrimitiveType() == StandardNames.XS_INTEGER) {
                    return BuiltInAtomicType.DECIMAL;
                } else {
                    return base;
                }
            }
            default:
                throw new AssertionError("Unknown aggregate operation");
        }
    }

    /**
     * Determine the cardinality of the function.
     */

    public int computeCardinality() {
        if (operation == AVG && !Cardinality.allowsZero(argument[0].getCardinality())) {
            return StaticProperty.EXACTLY_ONE;
        } else {
            return super.computeCardinality();
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
                return new Int64Value(count(iter));
            case SUM:
                AtomicValue sum = total(argument[0].iterate(context), context, this);
                if (sum != null) {
                    return sum;
                } else {
                    // the sequence was empty
                    if (argument.length == 2) {
                        return argument[1].evaluateItem(context);
                    } else {
                        return Int64Value.ZERO;
                    }
                } 
            case AVG:
                return average(argument[0].iterate(context), context, this);
            default:
                throw new UnsupportedOperationException("Unknown aggregate function");
        }
    }

    /**
     * Calculate the total of a sequence.
     * @param iter iterator over the items to be totalled
     * @param context the XPath dynamic context
     * @param location location of the expression in the source for diagnostics
     * @return the total, according to the rules of the XPath sum() function, but returning null
     * if the sequence is empty. (It's then up to the caller to decide what the correct result is
     * for an empty sequence.
    */

    public static AtomicValue total(SequenceIterator iter, XPathContext context, SourceLocator location)
            throws XPathException {
        AtomicValue sum = (AtomicValue)iter.next();
        if (sum == null) {
            // the sequence is empty
           return null;
        }
//        if (!sum.hasBuiltInType()) {
//            sum = sum.getPrimitiveValue();
//        }
        if (sum instanceof UntypedAtomicValue) {
            try {
                sum = sum.convert(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
            } catch (XPathException e) {
                e.maybeSetLocation(location);
                throw e;
            }
        }
        if (sum instanceof NumericValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (next instanceof UntypedAtomicValue) {
                    next = next.convert(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
                } else if (!(next instanceof NumericValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                //sum = ((NumericValue)sum).arithmetic(Token.PLUS, (NumericValue)next, context);
                sum = ArithmeticExpression.compute(sum, Calculator.PLUS, next, context);
                if (sum.isNaN() && sum instanceof DoubleValue) {
                    // take an early bath, once we've got a double NaN it's not going to change
                    return sum;
                }
            }
        } else if (sum instanceof DurationValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return sum;
                }
                if (!(next instanceof DurationValue)) {
                    XPathException err = new XPathException("Input to sum() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                sum = ((DurationValue)sum).add((DurationValue)next);
            }
        } else {
            XPathException err = new XPathException("Input to sum() contains a value that is neither numeric, nor a duration");
            err.setXPathContext(context);
            err.setErrorCode("FORG0006");
            err.setLocator(location);
            throw err;
        }
    }

    /**
     * Calculate average
     * @param iter iterator over the items to be totalled
     * @param context the XPath dynamic context
     * @param location location of the expression in the source for diagnostics
     * @return the average of the values
    */

    public static AtomicValue average(SequenceIterator iter, XPathContext context, SourceLocator location)
            throws XPathException {
        int count = 0;
        AtomicValue item = (AtomicValue)iter.next();
        if (item == null) {
            // the sequence is empty
            return null;
        }
        count++;
        if (item instanceof UntypedAtomicValue) {
            try {
                item = item.convert(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
            } catch (XPathException e) {
                e.maybeSetLocation(location);
                throw e;
            }
        }
        if (item instanceof NumericValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    //return ((NumericValue)item).arithmetic(Token.DIV, new Int64Value(count), context);
                    return ArithmeticExpression.compute(item, Calculator.DIV, new Int64Value(count), context);
                }
                count++;
                if (next instanceof UntypedAtomicValue) {
                    try {
                        next = next.convert(BuiltInAtomicType.DOUBLE, true, context).asAtomic();
                    } catch (XPathException e) {
                        e.maybeSetLocation(location);
                        throw e;
                    }
                } else if (!(next instanceof NumericValue)) {
                    XPathException err = new XPathException("Input to avg() contains a mix of numeric and non-numeric values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                //item = ((NumericValue)item).arithmetic(Token.PLUS, (NumericValue)next, context);
                item = ArithmeticExpression.compute(item, Calculator.PLUS, next, context);
                if (item.isNaN() && item instanceof DoubleValue) {
                    // take an early bath, once we've got a double NaN it's not going to change
                    return item;
                }
            }
        } else if (item instanceof DurationValue) {
            while (true) {
                AtomicValue next = (AtomicValue)iter.next();
                if (next == null) {
                    return ((DurationValue)item).multiply(1.0/count);
                }
                count++;
                if (!(next instanceof DurationValue)) {
                    XPathException err = new XPathException("Input to avg() contains a mix of duration and non-duration values");
                    err.setXPathContext(context);
                    err.setErrorCode("FORG0006");
                    err.setLocator(location);
                    throw err;
                }
                item = ((DurationValue)item).add((DurationValue)next);
            }
        } else {
            XPathException err = new XPathException("Input to avg() contains a value that is neither numeric, nor a duration");
            err.setXPathContext(context);
            err.setErrorCode("FORG0006");
            err.setLocator(location);
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
     * @param exp an expression to be examined
     * @return true if the expression is a call to the count() function
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
