package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.Configuration;

import java.io.PrintStream;

/**
* A value of type xs:hexBinary
*/

public class HexBinaryValue extends AtomicValue {

    private byte[] binaryValue;


    /**
    * Constructor: create a hexBinary value from a supplied string, in which
     * each octet is represented by a pair of values from 0-9, a-f, A-F
    */

    public HexBinaryValue(CharSequence s) throws XPathException {
        if ((s.length() & 1) != 0) {
            DynamicError err = new DynamicError(
                    "A hexBinary value must contain an even number of characters");
            err.setErrorCode("FORG0001");
            throw err;
        }
        binaryValue = new byte[s.length() / 2];
        for (int i=0; i<binaryValue.length; i++) {
            binaryValue[i] = (byte)((fromHex(s.charAt(2*i))<<4) +
                    (fromHex(s.charAt(2*i+1))));
        }
    }

    /**
     * Constructor: create a hexBinary value from a given array of bytes
     */

    public HexBinaryValue(byte[] value) {
        this.binaryValue = value;
    }

   /**
     * Get the binary value
     */

    public byte[] getBinaryValue() {
        return binaryValue;
    }

    /**
     * Decode a single hex digit
     * @param c the hex digit
     * @return the numeric value of the hex digit
     * @throws XPathException if it isn't a hex digit
     */

    private int fromHex(char c) throws XPathException {
        int d = "0123456789ABCDEFabcdef".indexOf(c);
        if (d > 15) {
            d = d - 6;
        }
        if (d < 0) {
            DynamicError err = new DynamicError("Invalid hexadecimal digit");
            err.setErrorCode("FORG0001");
            throw err;
        }
        return d;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type
    * @throws XPathException if the conversion is not possible
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch(requiredType) {
        case Type.HEX_BINARY:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;
        case Type.STRING:
            return new StringValue(getStringValue());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        case Type.BASE64_BINARY:
            return new Base64BinaryValue(binaryValue);

        default:
            DynamicError err = new DynamicError("Cannot convert hexBinarry to " +
                                     StandardNames.getDisplayName(requiredType));
            err.setXPathContext(context);
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    /**
    * Convert to string
    * @return the canonical representation.
    */

    public String getStringValue() {
        String digits = "0123456789ABCDEF";
        StringBuffer sb = new StringBuffer(binaryValue.length * 2);
        for (int i=0; i<binaryValue.length; i++) {
            sb.append(digits.charAt((binaryValue[i]>>4)&0xf));
            sb.append(digits.charAt(binaryValue[i]&0xf));
        }
        return sb.toString();
    }


    /**
    * Determine the data type of the exprssion
    * @return Type.HEX_BINARY_TYPE
    */

    public ItemType getItemType() {
        return Type.HEX_BINARY_TYPE;
    }

    /**
     * Get the number of octets in the value
     */

    public int getLengthInOctets() {
        return binaryValue.length;
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {

        // TODO: review this
        if (target.isAssignableFrom(HexBinaryValue.class)) {
            return this;
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            Object o = super.convertToJava(target, config, context);
            if (o == null) {
                throw new DynamicError("Conversion of hexBinary to " + target.getName() +
                        " is not supported");
            }
            return o;
        }
    }


    /**
    * Test if the two hexBinary values are equal.
    */

    public boolean equals(Object other) {
        HexBinaryValue v2;
        if (other instanceof HexBinaryValue) {
            v2 = (HexBinaryValue)other;
        } else if (other instanceof AtomicValue) {
            try {
                v2 = (HexBinaryValue)((AtomicValue)other).convert(Type.HEX_BINARY, null);
            } catch (XPathException err) {
                return false;
            }
        } else {
            return false;
        }
        if (binaryValue.length != v2.binaryValue.length) {
            return false;
        };
        for (int i = 0; i < binaryValue.length; i++) {
            if (binaryValue[i] != v2.binaryValue[i]) {
                return false;
            };
        }
        return true;
    }

     public int hashCode() {
        return Base64BinaryValue.byteArrayHashCode(binaryValue);
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
// The Initial Developer of the Original Code is Michael H. Kay, Saxonica.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

