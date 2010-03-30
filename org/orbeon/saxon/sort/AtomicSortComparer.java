package org.orbeon.saxon.sort;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;

/**
 * An AtomicComparer used for comparing atomic values of arbitrary item types. It encapsulates
 * a collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * The AtomicSortComparer is identical to the GenericAtomicComparer except for its handling
 * of NaN: it treats NaN values as lower than any other value, and as equal to each other.
 *
 * @author Michael H. Kay
 *
 */

public class AtomicSortComparer implements AtomicComparer {

    private StringCollator collator;
    private XPathContext context;
    private int itemType;

    /**
     * Factory method to get an atomic comparer suitable for sorting or for grouping (operations in which
     * NaN is considered equal to NaN)
     * @param collator Collating comparer to be used when comparing strings. This argument may be null
     * if the itemType excludes the possibility of comparing strings. If the method is called at compile
     * time, this should be a NamedCollation so that it can be cloned at run-time.
     * @param itemType the primitive item type of the values to be compared
     * @param context Dynamic context (may be an EarlyEvaluationContext)
     * @return a suitable AtomicComparer
     */

    public static AtomicComparer makeSortComparer(StringCollator collator, int itemType, XPathContext context) {
        switch (itemType) {
            case StandardNames.XS_STRING:
            case StandardNames.XS_UNTYPED_ATOMIC:
            case StandardNames.XS_ANY_URI:
                if (collator instanceof CodepointCollator) {
                    return CodepointCollatingComparer.getInstance();
                } else {
                    return new CollatingAtomicComparer(collator, Configuration.getPlatform());
                }
            case StandardNames.XS_INTEGER:
            case StandardNames.XS_DECIMAL:
                return DecimalSortComparer.getDecimalSortComparerInstance();
            case StandardNames.XS_DOUBLE:
            case StandardNames.XS_FLOAT:
            case StandardNames.XS_NUMERIC:
                return DoubleSortComparer.getInstance();
            case StandardNames.XS_DATE_TIME:
            case StandardNames.XS_DATE:
            case StandardNames.XS_TIME:
                return new CalendarValueComparer(context);
            default:
            // use the general-purpose comparer that handles all types
                return new AtomicSortComparer(collator, itemType, context);
        }

    }

    private AtomicSortComparer(StringCollator collator, int itemType, XPathContext context) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.context = context;
        this.itemType = itemType;
    }


    /**
     * Supply the dynamic context in case this is needed for the comparison
     *
     * @param context the dynamic evaluation context
     * @return either the original AtomicComparer, or a new AtomicComparer in which the context
     *         is known. The original AtomicComparer is not modified
     */

    public AtomicComparer provideContext(XPathContext context) {
        return new AtomicSortComparer(collator, itemType, context);
    }

    /**
     * Get the underlying StringCollator
     * @return the underlying collator
     */

    public StringCollator getStringCollator() {
        return collator;
    }

    /**
     * Get the requested item type
     * @return the item type
     */

    public int getItemType() {
        return itemType;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should normally be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) throws NoDynamicContextException {

        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (b == null) {
            return +1;
        }

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator, context);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator, context);
        } else if (a.isNaN()) {
            return (b.isNaN() ? 0 : -1);
        } else if (b.isNaN()) {
            return +1;
        } else if (a instanceof StringValue && b instanceof StringValue) {
            if (collator instanceof CodepointCollator) {
                return ((CodepointCollator)collator).compareCS(a.getStringValueCS(), b.getStringValueCS());
            } else {
                return collator.compareStrings(a.getStringValue(), b.getStringValue());
            }
        } else {
            Comparable ac = (Comparable)a.getXPathComparable(true, collator, context);
            Comparable bc = (Comparable)b.getXPathComparable(true, collator, context);
            if (ac == null || bc == null) {
                throw new ClassCastException("Values are not comparable (" +
                        Type.displayTypeName(a) + ", " + Type.displayTypeName(b) + ')');
            } else {
                return ac.compareTo(bc);
            }
        }
    }

    /**
     * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
     * values are compared by converting to the type of the other operand.
     *
     * @param a the first object to be compared. It is intended that this should be an instance
     *          of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
     *          collator is used to compare the values, otherwise the value must implement the equals() method.
     * @param b the second object to be compared. This must be comparable with the first object: for
     *          example, if one is a string, they must both be strings.
     * @return true if the values are equal, false if not
     * @throws ClassCastException if the objects are not comparable
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) throws NoDynamicContextException {
        return compareAtomicValues(a, b) == 0;
    }

    /**
    * Get a comparison key for an object. This must satisfy the rule that if two objects are equal,
    * then their comparison keys are equal, and vice versa. There is no requirement that the
    * comparison keys should reflect the ordering of the underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) throws NoDynamicContextException {
        if (a instanceof NumericValue) {
            if (((NumericValue)a).isNaN()) {
                // Deal with NaN specially. For this function, NaN is considered equal to itself
                return new ComparisonKey(StandardNames.XS_NUMERIC, COLLATION_KEY_NaN);
            } else {
                return new ComparisonKey(StandardNames.XS_NUMERIC, a);
            }
        } else if (a instanceof StringValue) {
            final Platform platform = Configuration.getPlatform();
            if (platform.canReturnCollationKeys(collator)) {
                return new ComparisonKey(StandardNames.XS_STRING,
                        collator.getCollationKey(a.getStringValue()));
            } else {
                return new ComparisonKey(StandardNames.XS_STRING, a);
            }
        } else if (a instanceof CalendarValue) {
            CalendarValue cv = (CalendarValue)a;
            if (cv.hasTimezone()) {
                return new ComparisonKey(a.getTypeLabel().getPrimitiveType(), a);
            } else {
                cv = (CalendarValue)cv.copyAsSubType((AtomicType)cv.getTypeLabel());
                cv.setTimezoneInMinutes(context.getImplicitTimezone());
                return new ComparisonKey(cv.getTypeLabel().getPrimitiveType(), cv);
            }
        } else if (a instanceof DurationValue) {
            // dayTimeDuration and yearMonthDuration are comparable in the special case of the zero duration
            return new ComparisonKey(StandardNames.XS_DURATION, a);
        } else {
            return new ComparisonKey(a.getTypeLabel().getPrimitiveType(), a);
        }
    }

    protected static StructuredQName COLLATION_KEY_NaN =
            new StructuredQName("saxon", "http://saxon.sf.net/collation-key", "NaN");
        // The logic here is to choose a value that compares equal to itself but not equal to any other
        // number. We use StructuredQName because it has a simple equals() method.

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
// Contributor(s): none
//