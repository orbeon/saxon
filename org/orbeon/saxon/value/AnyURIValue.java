package net.sf.saxon.value;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.Configuration;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.PrintStream;



/**
* A string value
*/

public final class AnyURIValue extends AtomicValue {

    private String value;     // may be zero-length, will never be null

    /**
    * Constructor
    * @param value the String value. Null is taken as equivalent to "".
    */

    public AnyURIValue(CharSequence value) {
        this.value = (value==null ? "" : trimWhitespace(value).toString());
    }

    /**
    * Get the string value as a String
    * @return the string value
    */

    public String getStringValue() {
        return value;
    }

    /**
    * Convert to target data type
    * @param requiredType integer code representing the item type required
    * @return the result of the conversion
    * @throws XPathException if the conversion is not allowed
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch(requiredType) {
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
            return new RestrictedStringValue(value, requiredType);
        default:
            DynamicError err = new DynamicError("Cannot convert anyURI to " +
                                     StandardNames.getDisplayName(requiredType));
            err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    /**
    * Return the type of the expression
    * @return Type.ANY_URI (always)
    */

    public ItemType getItemType() {
        return Type.ANY_URI_TYPE;
    }


    /**
    * Determine if two anyURI values are equal
    * @return true if the two values are equal
    * @throws ClassCastException if the other value is not an AnyURIValue
    */

    public boolean equals(Object other) {
        return this.value.equals(((AnyURIValue)other).value);
    }

    public int hashCode() {
        return value.hashCode();
    }


    /**
    * Convert to Java object (for passing to external functions)
    * @param target the Java class to which conversion is required
    * @return the result of the conversion
    * @throws XPathException if conversion to this target type is not possible
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target==Object.class) {
            return value;
        } else if (target.isAssignableFrom(StringValue.class)) {
            return this;
        } else if (target==URI.class) {
            try {
                return new URI(value);
            } catch (URISyntaxException err) {
                throw new DynamicError("The anyURI value '" + value + "' is not an acceptable Java URI");
            }
        } else if (target==URL.class) {
            try {
                return new URL(value);
            } catch (MalformedURLException err) {
                throw new DynamicError("The anyURI value '" + value + "' is not an acceptable Java URL");
            }
        } else if (target==String.class) {
            return value;
        } else if (target==CharSequence.class) {
            return value;
        } else {
             Object o = super.convertToJava(target, config, context);
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

