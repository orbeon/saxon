package org.orbeon.saxon.sort;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.StringValue;

/**
 * An AtomicComparer used for comparing strings, untypedAtomic values, and URIs using the Unicode codepoint
 * collation.
 * A CodepointCollatingComparer is used when it is known in advance that both operands will be of these
 * types, and when the collation is the unicode codepoint collation.
 * This enables all conversions and promotions to be bypassed: the string values of both operands
 * are simply extracted and passed to the collator for comparison.
 *
 * <p>The difference between using this class and using the underlying CodepointCollator directly is that
 * the compare() method in this class expects two instances of AtomicValue as its operands, whereas the
 * underlying class expects two instances of java.lang.String. This class makes use of the extra information
 * held in the wrapping StringValue object, specifically, the knowledge of whether the string contains
 * surrogate pairs.</p>
 *
 * @author Michael H. Kay
 *
 */

public class CodepointCollatingComparer implements AtomicComparer {

    private static CodepointCollator collator = CodepointCollator.getInstance();

    private static CodepointCollatingComparer THE_INSTANCE = new CodepointCollatingComparer();

    /**
     * Get the singular instance of this class
     * @return the singleton instance
     */

    public static CodepointCollatingComparer getInstance() {
        return THE_INSTANCE;
    }

    private CodepointCollatingComparer() {}


    /**
     * Supply the dynamic context in case this is needed for the comparison
     *
     * @param context the dynamic evaluation context
     * @return either the original AtomicComparer, or a new AtomicComparer in which the context
     *         is known. The original AtomicComparer is not modified
     */

    public AtomicComparer provideContext(XPathContext context) {
        return this;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. This must be either be an instance
    * of AtomicValue, or null to represent an empty sequence. Empty collates before non-empty.
    * @param b the second object to be compared. This must be either be an instance
    * of AtomicValue, or null to represent an empty sequence. 
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) {
        if (a == null) {
            return (b == null ? 0 : -1);
        } else if (b == null) {
            return +1;
        }
        StringValue as = (StringValue)a;
        StringValue bs = (StringValue)b;
        if (as.containsSurrogatePairs() || bs.containsSurrogatePairs()) {
            return collator.compareCS(as.getStringValueCS(), bs.getStringValueCS());
        } else {
            // optimize to use UTF-16 binary comparison
            return as.getStringValue().compareTo(bs.getStringValue());
        }
    }

    /**
    * Compare two AtomicValue objects for equality. The values must be instances of xs:string or a type
     * derived from xs:string. The method will also handle xs:untypedAtomic and xs:anyURI values.
    * @param a the first object to be compared.
    * @param b the second object to be compared.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if either value is not xs:string or a subtype
    */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        StringValue as = (StringValue)a;
        StringValue bs = (StringValue)b;
        return as.codepointEquals(bs);
    }

    /**
    * Get a comparison key for an object. This must satisfy the rule that if two objects are equal
    * under the XPath eq operator, then their comparison keys are equal under the Java equals()
    * function, and vice versa. There is no requirement that the
    * comparison keys should reflect the ordering of the underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) {
        StringValue as = (StringValue)a;
        return new ComparisonKey(StandardNames.XS_STRING, as.getStringValue());
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
// Contributor(s): none
//