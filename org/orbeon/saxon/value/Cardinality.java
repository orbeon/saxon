package net.sf.saxon.value;

import net.sf.saxon.expr.StaticProperty;

/**
* This class contains static methods to manipulate the cardinality
* property of a type.
* Cardinality of expressions is denoted by one of the values ONE_OR_MORE, ZERO_OR_MORE,
* ZERO_OR_ONE, EXACTLY_ONE, or EMPTY. These are combinations of the three bit-significant
* values ALLOWS_ZERO, ALLOWS_ONE, and ALLOWS_MANY.
*/

public final class Cardinality {

    /**
    * Private constructor: no instances allowed
    */

    private Cardinality() {}

    /**
     * Determine whether multiple occurrences are allowed
     */

    public static final boolean allowsMany(int cardinality) {
        return (cardinality & StaticProperty.ALLOWS_MANY) != 0;
    }

    /**
     * Determine whether empty sequence is allowed
     */

    public static final boolean allowsZero(int cardinality) {
        return (cardinality & StaticProperty.ALLOWS_ZERO) != 0;
    }

    /**
    * Form the union of two cardinalities. The cardinality of the expression "if (c) then e1 else e2"
    * is the union of the cardinalities of e1 and e2.
    * @param c1 a cardinality
    * @param c2 another cardinality
    * @return the cardinality that allows both c1 and c2
    */

    public static final int union(int c1, int c2) {
        int r = c1 | c2;
        // eliminate disallowed options
        if (r == (StaticProperty.ALLOWS_MANY |
                    StaticProperty.ALLOWS_ZERO ))
            r = StaticProperty.ALLOWS_ZERO_OR_MORE;
        return r;
    }

    /**
     * Form the sum of two cardinalities
     */

    public static final int sum(int c1, int c2) {
        if (allowsMany(c1) || allowsMany(c2)) {
            return c1 | c2;
        }
        if (!allowsZero(c1) && !allowsZero(c2)) {
            return StaticProperty.ALLOWS_ONE_OR_MORE;
        } else {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
    * Test if one cardinality subsumes another. Cardinality c1 subsumes c2 if every option permitted
    * by c2 is also permitted by c1.
    * @param c1 a cardinality
    * @param c2 another cardinality
    * @return true if if every option permitted
    * by c2 is also permitted by c1.
    */

    public static final boolean subsumes(int c1, int c2) {
        return (c1|c2)==c1;
    }

    /**
     * Add two cardinalities
     */

    public static final int add(int c1, int c2) {
        if (c1==StaticProperty.EMPTY) {
            return c2;
        }
        if (c2==StaticProperty.EMPTY) {
            return c1;
        }
        boolean allowsZero = Cardinality.allowsZero(c1) && Cardinality.allowsZero(c2);
        return StaticProperty.ALLOWS_ONE_OR_MORE | (allowsZero ? StaticProperty.ALLOWS_ZERO : 0);
    }

    /**
     * Multiply two cardinalities
     */

    public static final int multiply(int c1, int c2) {
        if (c1==StaticProperty.EMPTY || c2==StaticProperty.EMPTY) {
            return StaticProperty.EMPTY;
        }
        if (c2==StaticProperty.EXACTLY_ONE) {
            return c1;
        }
        if (c1==StaticProperty.EXACTLY_ONE) {
            return c2;
        }
        if (c1==StaticProperty.ALLOWS_ZERO_OR_ONE && c2==StaticProperty.ALLOWS_ZERO_OR_ONE) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }
        if (c1==StaticProperty.ALLOWS_ONE_OR_MORE && c2==StaticProperty.ALLOWS_ONE_OR_MORE) {
            return StaticProperty.ALLOWS_ONE_OR_MORE;
        }
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
    * Display the cardinality
    */

    public static String toString(int cardinality) {
        switch (cardinality) {
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return "zero or one";
            case StaticProperty.EXACTLY_ONE:
                return "exactly one";
            case StaticProperty.ALLOWS_ZERO_OR_MORE:
                return "zero or more";
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return "one or more";
            case StaticProperty.EMPTY:
                return "exactly zero";
            default: throw new AssertionError("unknown cardinality value");
        }
    }

    /**
     * Get the occurence indicator representing the cardinality
     */

    public static String getOccurrenceIndicator(int cardinality) {
        switch (cardinality) {
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return "?";
            case StaticProperty.EXACTLY_ONE:
                return "";
            case StaticProperty.ALLOWS_ZERO_OR_MORE:
                return "*";
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return "+";
            default: throw new AssertionError("unknown cardinality value");
        }
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
