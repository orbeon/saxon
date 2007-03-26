package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.type.*;


/**
 * A value conforming to one of the built-in subtypes of String, specifically
 * normalizedString, token, language, Name, NCName, ID, IDREF, ENTITY, NMTOKEN.
 * This class doesnt' handle the types derived by list: IDREFS, NMTOKENS, ENTITIES.
 */

public final class RestrictedStringValue extends StringValue {

    private int type;

    /**
     * Factory method to create a restricted string value from a string
     * @param value the String value. Null is taken as equivalent to "".
     * @param checker a NameChecker if validation is required,
     *        null if the caller already knows that the value is valid
     * @return either the required RestrictedStringValue if the value is valid, or an ErrorValue encapsulating
     * the error message if not.
     */

    public static AtomicValue makeRestrictedString(CharSequence value, int type, NameChecker checker) {
        RestrictedStringValue rsv = new RestrictedStringValue();
        rsv.type = type;
        if (value == null) {
            rsv.value = "";
        } else if (type == Type.NORMALIZED_STRING) {
            rsv.value = Whitespace.normalizeWhitespace(value);
        } else if (type == Type.TOKEN) {
            rsv.value = Whitespace.collapseWhitespace(value);
        } else {
            rsv.value = Whitespace.trimWhitespace(value);
            if (checker != null) {
                ValidationException err = rsv.validate(checker);
                if (err == null) {
                    return rsv;
                } else {
                    return new ValidationErrorValue(err);
                }
            } else {
                return rsv;
            }
        }
        return rsv;
    }

    /**
     * Validate that the string conforms to the rules for its type
     * @return null if the value is OK, otherwise a DynamicError containing details of the failure
     */

    private ValidationException validate(NameChecker checker) {
        switch (type) {

            case Type.TOKEN:
                return null;
            case Type.NORMALIZED_STRING:
                return null;
            case Type.LANGUAGE:
                String regex =
                        "(([a-z]|[A-Z])([a-z]|[A-Z])|" // ISO639Code
                        + "([iI]-([a-z]|[A-Z])+)|"     // IanaCode
                        + "([xX]-([a-z]|[A-Z])+))"     // UserCode
                        + "(-([a-z]|[A-Z])+)*";        // Subcode
                if (!java.util.regex.Pattern.matches(regex, value.toString())) {
                    ValidationException err = new ValidationException("The value '" + value + "' is not a valid xs:language");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            case Type.NAME:
                // replace any colons by underscores and then test if it's a valid NCName
                FastStringBuffer buff = new FastStringBuffer(value.length());
                buff.append(value.toString());
                for (int i = 0; i < buff.length(); i++) {
                    if (buff.charAt(i) == ':') {
                        buff.setCharAt(i, '_');
                    }
                }
                if (!checker.isValidNCName(buff.toString())) {
                    ValidationException err = new ValidationException("The value '" + value + "' is not a valid Name");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            case Type.NCNAME:
            case Type.ID:
            case Type.IDREF:
            case Type.ENTITY:
                if (!checker.isValidNCName(value.toString())) {
                    ValidationException err = new ValidationException("The value '" + value + "' is not a valid NCName");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            case Type.NMTOKEN:
                if (!XMLChar.isValidNmtoken(value.toString())) {
                    ValidationException err = new ValidationException("The value '" + value + "' is not a valid NMTOKEN");
                    err.setErrorCode("FORG0001");
                    return err;
                }
                return null;
            default:
                throw new IllegalArgumentException("Unknown string value type " + type);
        }
    }


    /**
     * Return the type of the expression
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return (AtomicType) BuiltInSchemaFactory.getSchemaType(type);
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        int req = requiredType.getPrimitiveType();
        if (req == Type.STRING) {
            return StringValue.makeStringValue(value);
        } else if (req == Type.UNTYPED_ATOMIC) {
            return new UntypedAtomicValue(value);
        } else {
            return super.convertPrimitive(requiredType, validate, context);
        }
    }

    public String toString() {
        return getItemType(null).toString() + '(' + super.toString() + ')';
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

