package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.ConversionContext;

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
*/

public final class AnyURIValue extends StringValue {

    public static final AnyURIValue EMPTY_URI = new AnyURIValue("");

    /**
    * Constructor
    * @param value the String value. Null is taken as equivalent to "".
    */

    public AnyURIValue(CharSequence value) {
        this.value = (value==null ? "" : trimWhitespace(value).toString());
    }

    /**
    * Convert to target data type
    * @param requiredType integer code representing the item type required
    * @param conversion
     * @return the result of the conversion, or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, ConversionContext conversion) {
        int req = requiredType.getPrimitiveType();
        switch(req) {
        case Type.ATOMIC:
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
            return RestrictedStringValue.makeRestrictedString(value, req, validate);

        default:
            ValidationException err = new ValidationException("Cannot convert anyURI to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("FORG0001");
            return new ValidationErrorValue(err);
        }
    }

    /**
    * Return the type of the expression
    * @return Type.ANY_URI_TYPE (always)
    */

    public ItemType getItemType() {
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
     * Get the effective boolean value of the value
     *
     * @param context the evaluation context (not used in this implementation)
     * @return true, unless the value is boolean false, numeric zero, or
     *     zero-length string
     */
    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        DynamicError err = new DynamicError(
                "Effective boolean value is not defined for a value of type xs:anyURI");
        err.setIsTypeError(true);
        err.setXPathContext(context);
        throw err;
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

