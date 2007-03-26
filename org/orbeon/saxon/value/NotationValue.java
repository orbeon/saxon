package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;


/**
 * An xs:NOTATION value.
 */

public final class NotationValue extends QNameValue {

   /**
     * Constructor
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     */

    public NotationValue(String prefix, String uri, String localName, NameChecker checker) throws XPathException {
        super(prefix, uri, localName, checker);
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
            case Type.ANY_ATOMIC:
            case Type.ITEM:
            case Type.QNAME:
            case Type.STRING:
            case Type.UNTYPED_ATOMIC:
                return super.convertPrimitive(requiredType, validate, context);
            case Type.NOTATION:
                return this;
            default:
                ValidationException err = new ValidationException("Cannot convert NOTATION to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                return new ValidationErrorValue(err);
        }
    }

    /**
     * Return the type of the expression
     * @return Type.NOTATION (always)
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.NOTATION_TYPE;
    }


     /**
     * The toString() method returns the name in the form QName("uri", "local")
     * @return the name in Clark notation: {uri}local
     */

    public String toString() {
        return "NOTATION(" + getClarkName() + ')';
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

