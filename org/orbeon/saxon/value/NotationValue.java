package net.sf.saxon.value;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;


/**
 * An xs:NOTATION value.
 */

public final class NotationValue extends QNameValue {

    /*
    * Constructor
     * @param namePool The name pool containing the specified name code
     * @param nameCode The name code identifying this name in the name pool
     */

    public NotationValue(NamePool namePool, int nameCode) {
        super(namePool, nameCode);
    }

    /**
     * Constructor
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     */

    public NotationValue(String prefix, String uri, String localName) throws XPathException {
        super(prefix, uri, localName);
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @return an AtomicValue, a value of the required type
     * @throws net.sf.saxon.xpath.XPathException if the conversion is not possible
     */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch (requiredType) {
            case Type.ATOMIC:
            case Type.ITEM:
            case Type.QNAME:
            case Type.STRING:
            case Type.UNTYPED_ATOMIC:
                return super.convert(requiredType, context);
            default:
                DynamicError err = new DynamicError("Cannot convert NOTATION to " +
                        StandardNames.getDisplayName(requiredType));
                err.setXPathContext(context);
                err.setErrorCode("FORG0001");
                throw err;
        }
    }

    /**
     * Return the type of the expression
     * @return Type.NOTATION (always)
     */

    public ItemType getItemType() {
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

