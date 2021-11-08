package org.orbeon.saxon.s9api;

import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.value.Cardinality;

/**
 * Represents one of the possible occurrence indicators in a SequenceType. The four standard values are
 * ONE (no occurrence indicator), ZERO_OR_ONE (?), ZERO_OR_MORE (*), ONE_OR_MORE (+). In addition the
 * value ZERO is supported, this is used only in the type empty-sequence() which matches an empty sequence
 * and nothing else.
 */
public enum OccurrenceIndicator {
    ZERO, ZERO_OR_ONE, ZERO_OR_MORE, ONE, ONE_OR_MORE;

    protected int getCardinality() {
        switch(this) {
            case ZERO:
                return StaticProperty.EMPTY;
            case ZERO_OR_ONE:
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            case ZERO_OR_MORE:
                return StaticProperty.ALLOWS_ZERO_OR_MORE;
            case ONE:
                return StaticProperty.ALLOWS_ONE;
            case ONE_OR_MORE:
                return StaticProperty.ALLOWS_ONE_OR_MORE;
            default:
                return StaticProperty.EMPTY;
        }
    }

    protected static OccurrenceIndicator getOccurrenceIndicator(int cardinality) {
        switch (cardinality) {
            case StaticProperty.EMPTY:
                    return ZERO;
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return ZERO_OR_ONE;
            case StaticProperty.ALLOWS_ZERO_OR_MORE:
                return ZERO_OR_MORE;
            case StaticProperty.ALLOWS_ONE:
                return ONE;
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return ONE_OR_MORE;
            default:
                return ZERO_OR_MORE;
        }
    }

    /**
     * Determine whether one occurrence indicator subsumes another. Specifically,
     * <code>A.subsumes(B)</code> is true if every sequence that satisfies the occurrence
     * indicator B also satisfies the occurrence indicator A.
     * @param other The other occurrence indicator
     * @return true if this occurrence indicator subsumes the other occurrence indicator
     * @since 9.1
     */

    public boolean subsumes(OccurrenceIndicator other) {
        return Cardinality.subsumes(getCardinality(), other.getCardinality());
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//
