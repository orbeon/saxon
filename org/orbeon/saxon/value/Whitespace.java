package org.orbeon.saxon.value;

import org.orbeon.saxon.functions.NormalizeSpace;

/**
 * This class provides helper methods and constants for handling whitespace
 */
public class Whitespace {

    private Whitespace() {}


    /**
     * The values PRESERVE, REPLACE, and COLLAPSE represent the three options for whitespace
     * normalization. They are deliberately chosen in ascending strength order; given a number
     * of whitespace facets, only the strongest needs to be carried out.
     */

    public static final int PRESERVE = 0;
    public static final int REPLACE = 1;
    public static final int COLLAPSE = 2;

    /**
     * Apply schema-defined whitespace normalization to a string
     * @param action the action to be applied: one of PRESERVE, REPLACE, or COLLAPSE
     * @param value the value to be normalized
     * @return the value after normalization
     */

    public static CharSequence applyWhitespaceNormalization(int action, CharSequence value) {
        switch (action) {
            case PRESERVE:
                return value;
            case REPLACE:
                StringBuffer sb = new StringBuffer(value.length());
                for (int i=0; i<value.length(); i++) {
                    if ("\n\r\t".indexOf(value.charAt(i)) >= 0) {
                        sb.append(' ');
                    } else {
                        sb.append(value.charAt(i));
                    }
                }
                return sb;
            case COLLAPSE:
                return NormalizeSpace.normalize(value.toString());
            default:
                throw new IllegalArgumentException("Unknown whitespace facet value");
        }
    }
}
