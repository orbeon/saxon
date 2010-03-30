package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.*;

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
     * @param cardinality the cardinality of a sequence
     * @return true if the cardinality allows the sequence to contain more than one item
     */

    public static boolean allowsMany(int cardinality) {
        return (cardinality & StaticProperty.ALLOWS_MANY) != 0;
    }

    /**
     * Determine whether multiple occurrences are not only allowed, but likely.
     * This returns false for an expression that is the atomization of a singleton
     * node, since in that case it's quite unusual for the typed value to be a
     * non-singleton sequence.
     * @param expression an expression
     * @return true if multiple occurrences are not only allowed, but likely. Return
     * false if multiple occurrences are unlikely, even though they might be allowed.
     * This is typically the case for the atomized sequence that is obtained by atomizing
     * a singleton node.
     */

    public static boolean expectsMany(Expression expression) {
        if (expression instanceof VariableReference) {
            Binding b = ((VariableReference)expression).getBinding();
            if (b instanceof LetExpression) {
                return expectsMany(((LetExpression)b).getSequence());
            }
        }
        if (expression instanceof LazyExpression) {
            return expectsMany(((LazyExpression)expression).getBaseExpression());
        }
        if (expression instanceof Atomizer) {
            return expectsMany(((Atomizer)expression).getBaseExpression());
        }
        if (expression instanceof FilterExpression) {
            return expectsMany(((FilterExpression)expression).getBaseExpression());
        }
        return allowsMany(expression.getCardinality());
    }

    /**
     * Determine whether empty sequence is allowed
     * @param cardinality the cardinality of a sequence
     * @return true if the cardinality allows the sequence to be empty
     */

    public static boolean allowsZero(int cardinality) {
        return (cardinality & StaticProperty.ALLOWS_ZERO) != 0;
    }

    /**
    * Form the union of two cardinalities. The cardinality of the expression "if (c) then e1 else e2"
    * is the union of the cardinalities of e1 and e2.
    * @param c1 a cardinality
    * @param c2 another cardinality
    * @return the cardinality that allows both c1 and c2
    */

    public static int union(int c1, int c2) {
        int r = c1 | c2;
        // eliminate disallowed options
        if (r == (StaticProperty.ALLOWS_MANY |
                    StaticProperty.ALLOWS_ZERO ))
            r = StaticProperty.ALLOWS_ZERO_OR_MORE;
        return r;
    }


     /**
      * Add two cardinalities
      * @param c1 the first cardinality
      * @param c2 the second cardinality
      * @return the cardinality of a sequence formed by concatenating the sequences whose cardinalities
      * are c1 and c2
     */

    public static int sum(int c1, int c2) {
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
    * Test if one cardinality subsumes another. Cardinality c1 subsumes c2 if every option permitted
    * by c2 is also permitted by c1.
    * @param c1 a cardinality
    * @param c2 another cardinality
    * @return true if if every option permitted
    * by c2 is also permitted by c1.
    */

    public static boolean subsumes(int c1, int c2) {
        return (c1|c2)==c1;
    }



    /**
     * Multiply two cardinalities
     * @param c1 the first cardinality
     * @param c2 the second cardinality
     * @return the product of the cardinalities, that is, the cardinality of the sequence
     * "for $x in S1 return S2", where c1 is the cardinality of S1 and c2 is the cardinality of S2
     */

    public static int multiply(int c1, int c2) {
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
     * Display the cardinality as a string
     * @param cardinality the cardinality value to be displayed
     * @return the representation as a string, for example "zero or one", "zero or more"
     *
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
            case StaticProperty.ALLOWS_MANY:
                return "more than one";
            default:
                return "code " + cardinality;
        }
    }

    /**
     * Get the occurence indicator representing the cardinality
     * @param cardinality the cardinality value
     * @return the occurrence indicator, for example "*", "+", "?", "".
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
            case StaticProperty.EMPTY:
                return "\u00B0";
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
