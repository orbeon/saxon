package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInSchemaFactory;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;


/**
 * A value conforming to one of the built-in subtypes of String, specifically
 * normalizedString, token, language, Name, NCName, ID, IDREF, ENTITY, NMTOKEN.
 * This class doesnt' handle the types derived by list: IDREFS, NMTOKENS, ENTITIES.
 */

public final class RestrictedStringValue extends StringValue {

    private int type;

    /**
     * Constructor
     * @param value the String value. Null is taken as equivalent to "".
     */

    public RestrictedStringValue(CharSequence value, int type) throws XPathException {
        this.type = type;
        if (value == null) {
            this.value = "";
        } else if (type == Type.NORMALIZED_STRING) {
            this.value = normalizeWhitespace(value);
        } else if (type == Type.TOKEN) {
            this.value = collapseWhitespace(value);
        } else {
            this.value = trimWhitespace(value);
            validate();
        }
    }

    /**
     * Validate that the string conforms to the rules for its type
     */

    private void validate() throws XPathException {
        switch (type) {

            case Type.TOKEN:
                return;
            case Type.NORMALIZED_STRING:
                return;
            case Type.LANGUAGE:
                String regex =
                        "(([a-z]|[A-Z])([a-z]|[A-Z])|" // ISO639Code
                        + "([iI]-([a-z]|[A-Z])+)|"     // IanaCode
                        + "([xX]-([a-z]|[A-Z])+))"     // UserCode
                        + "(-([a-z]|[A-Z])+)*";        // Subcode
                if (!java.util.regex.Pattern.matches(regex, value.toString())) {
                    DynamicError err = new DynamicError("The value '" + value + "' is not a valid xs:language");
                    err.setErrorCode("FORG0001");
                    throw err;
                }
                return;
            case Type.NAME:
                // replace any colons by underscores and then test if it's a valid NCName
                StringBuffer buff = new StringBuffer(value.length());
                buff.append(value);
                for (int i = 0; i < buff.length(); i++) {
                    if (buff.charAt(i) == ':') {
                        buff.setCharAt(i, '_');
                    }
                }
                if (!XMLChar.isValidNCName(buff.toString())) {
                    DynamicError err = new DynamicError("The value '" + value + "' is not a valid Name");
                    err.setErrorCode("FORG0001");
                    throw err;
                }
                return;
            case Type.NCNAME:
            case Type.ID:
            case Type.IDREF:
            case Type.ENTITY:
                if (!XMLChar.isValidNCName(value.toString())) {
                    DynamicError err = new DynamicError("The value '" + value + "' is not a valid NCName");
                    err.setErrorCode("FORG0001");
                    throw err;
                }
                return;
            case Type.NMTOKEN:
                if (!XMLChar.isValidNmtoken(value.toString())) {
                    DynamicError err = new DynamicError("The value '" + value + "' is not a valid NMTOKEN");
                    err.setErrorCode("FORG0001");
                    throw err;
                }
                return;
            default:
                throw new IllegalArgumentException("Unknown string value type " + type);
        }
    }


    /**
     * Return the type of the expression
     */

    public ItemType getItemType() {
        return (AtomicType) BuiltInSchemaFactory.getSchemaType(type);
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type
     * @throws XPathException if the conversion is not possible
     */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        if (requiredType == Type.STRING) {
            return new StringValue(value);
        } else if (requiredType == Type.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(value);
        } else {
            return super.convert(requiredType, context);
        }
    }

    public String toString() {
        return getItemType().toString() + "(" + super.toString() + ")";
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

