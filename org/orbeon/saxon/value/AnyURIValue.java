package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.functions.EscapeURI;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;



/**
* An XPath value of type xs:anyURI.
 *
 * <p>This is implemented as a subtype of StringValue even though xs:anyURI is not a subtype of
 * xs:string in the XPath type hierarchy. This enables type promotion from URI to String to happen
 * automatically in most cases where it is appropriate.</p>
 *
 * <p>This implementation of xs:anyURI allows any string to be contained in the value space. It is possible
 * to validate that the string is a "valid URI" in the sense of XML Schema Part 2 (which refers to the XLink
 * specification and to RFC 2396); however, this validation is optional, and is not carried out by default.
 * In particular, there is no constraint that namespace URIs, collation URIs, and the like should be valid
 * URIs. However, casting from strings to xs:anyURI does invoke validation.</p>
*/

public final class AnyURIValue extends StringValue {

    public static final AnyURIValue EMPTY_URI = new AnyURIValue("");

    /**
    * Constructor
    * @param value the String value. Null is taken as equivalent to "".
    */

    public AnyURIValue(CharSequence value) {
        this.value = (value==null ? "" : Whitespace.trimWhitespace(value).toString());
    }

    /**
     * Check whether a string consititutes a valid URI
     */

    public static boolean isValidURI(CharSequence value) {

        String sv = value.toString().trim();

        // Allow zero-length strings (RFC2396 is ambivalent on this point)
        if (sv.length() == 0) {
            return true;
        }

        // Allow a string if the java.net.URI class accepts it
        try {
            new URI(sv);
            return true;
        } catch (URISyntaxException e) {
            // keep trying
            // TODO: it's expensive to throw exceptions on a success path. Perhaps keep a cache of valid URIs.
        }

        // Allow a string if it can be escaped into a form that java.net.URI accepts
        sv = EscapeURI.iriToUri(sv).toString();
        try {
            new URI(sv);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
    * Convert to target data type
    * @param requiredType integer code representing the item type required
    * @param context
     * @return the result of the conversion, or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        int req = requiredType.getPrimitiveType();
        switch(req) {
        case Type.ANY_ATOMIC:
        case Type.ITEM:
        case Type.ANY_URI:
            return this;
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(value);
        case Type.STRING:
            return new StringValue(value);
        case Type.NORMALIZED_STRING:
        case Type.TOKEN:
        case Type.LANGUAGE:
        case Type.NAME:
        case Type.NCNAME:
        case Type.ID:
        case Type.IDREF:
        case Type.ENTITY:
        case Type.NMTOKEN:
            return RestrictedStringValue.makeRestrictedString(value, req,
                    (validate ? context.getConfiguration().getNameChecker() : null));

        default:
            ValidationException err = new ValidationException("Cannot convert anyURI to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return new ValidationErrorValue(err);
        }
    }

    /**
    * Return the type of the expression
    * @return Type.ANY_URI_TYPE (always)
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.ANY_URI_TYPE;
    }

    /**
     * Determine if two AnyURIValues are equal, according to XML Schema rules. (This method
     * is not used for XPath comparisons, which are always under the control of a collation.)
     * @throws ClassCastException if the values are not comparable
     */

    public boolean equals(Object other) {
        // Force a ClassCastException if the other value isn't an anyURI or derived from anyURI
        AnyURIValue otherVal = (AnyURIValue) ((AtomicValue) other).getPrimitiveValue();
        // cannot use equals() directly on two unlike CharSequences
        return getStringValue().equals(otherVal.getStringValue());
    }

    /**
    * Convert to Java object (for passing to external functions)
    * @param target the Java class to which conversion is required
    * @return the result of the conversion
    * @throws XPathException if conversion to this target type is not possible
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target==Object.class) {
            return value;
        } else if (target.isAssignableFrom(StringValue.class)) {
            return this;
        } else if (target==URI.class) {
            try {
                return new URI(value.toString());
            } catch (URISyntaxException err) {
                throw new DynamicError("The anyURI value '" + value + "' is not an acceptable Java URI");
            }
        } else if (target==URL.class) {
            try {
                return new URL(value.toString());
            } catch (MalformedURLException err) {
                throw new DynamicError("The anyURI value '" + value + "' is not an acceptable Java URL");
            }
        } else if (target==String.class) {
            return value;
        } else if (target==CharSequence.class) {
            return value;
        } else {
             Object o = super.convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of anyURI to " + target.getName() +
                        " is not supported");
            }
            return o;
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

