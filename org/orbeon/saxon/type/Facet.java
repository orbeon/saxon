package net.sf.saxon.type;

import net.sf.saxon.value.AtomicValue;

import java.io.Serializable;

/**
 * Represents an XML Schema Facet. Most facets are represented by subclasses of this
 * class, the only exception is whiteSpace.
*/

public abstract class Facet implements Serializable {

    public static final String ENUMERATION      = "enumeration";
    public static final String LENGTH           = "length";
    public static final String PATTERN          = "pattern";
    public static final String MAX_EXCLUSIVE    = "maxExclusive";
    public static final String MAX_INCLUSIVE    = "maxInclusive";
    public static final String MIN_EXCLUSIVE    = "minExclusive";
    public static final String MIN_INCLUSIVE    = "minInclusive";
    public static final String MAX_LENGTH       = "maxLength";
    public static final String MIN_LENGTH       = "minLength";
    public static final String WHITESPACE       = "whiteSpace";
    public static final String TOTALDIGITS      = "totalDigits";
    public static final String FRACTIONDIGITS   = "fractionDigits";

    /**
     * The name of this Facet
     */

    private String name    = null;

    /**
     * The value of this facet as a string
     */

    protected String value  = null;

    /**
     * Flag to indicate that the facet is fixed (cannot be overridden in a subtype)
     */

    private boolean fixed = false;

    /**
     * The values PRESERVE, REPLACE, and COLLAPSE represent the three options for whitespace
     * normalization. They are deliberately chosen in ascending strength order; given a number
     * of whitespace facets, only the strongest needs to be carried out.
     */

    public static final int PRESERVE = 0;
    public static final int REPLACE = 1;
    public static final int COLLAPSE = 2;

    /**
     * Test if the facet is defined in the schema with fixed="true"
     * @return true if fixed
     */

    public boolean isFixed() {
        return fixed;
    }

    /**
     * Set whether the facet is defined as fixed
     * @param fixed true if the facet is fixed
     */

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    /**
     * Returns the name of this Facet
     * @return the name of this Facet
     */

    public String getName() {
        return name;
    }

    /**
     * Returns the character (String) representation of this facet
     * @return the value of this facet
     */

    public String getValue() {
        return this.value;
    }

    /**
     * Returns the numeric value of this facet
     * @return a long representation of the value of this facet
    **/

    public long toLong() throws NumberFormatException {
        return Long.parseLong(value);
    }

    /**
     * Check that this facet is legal when used on a type derived by restriction
     * @param type the type on which the facet is defined
     * @param base the type from which the restricted type is derived
     * @throws SchemaException if the facet is not legal
     */

    public void checkFacetRestriction(SimpleType type, SimpleType base) throws SchemaException {
        // no-op (overridden in subclasses)
    }

    /**
     * Test whether this is a facet that can be used to test the normalized value of a
     * list type. (Specifically, this is true only for pattern facets and enumeration facets)
     */

    public boolean appliesToWholeList() {
        return false;
    }

    /**
     * Test whether an atomic value conforms to this facet
     * @param value  the value to be tested
     * @return true if the value conforms; false if it doesn't conform, or if the test
     * fails
     */

    public boolean testAtomicValue(AtomicValue value) {
        return true;
    }

    /**
     * Test whether the length of a list conforms to this facet.
     * Always true except for length facets
     */

    public boolean testLength(int count) {
        return true;
    }

    /**
     * Get the whitespace action required by this facet: one of PRESERVE, REPLACE, or COLLAPSE.
     * Returns a dummy value of PRESERVE for facets other than whitespace facets.
     */

    public int getWhitespaceAction() {
        return PRESERVE;
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//