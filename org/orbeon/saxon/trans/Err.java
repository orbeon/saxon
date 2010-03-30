package org.orbeon.saxon.trans;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Whitespace;

/**
 * Class containing utility methods for handling error messages
 */

public class Err {

    public static final int ELEMENT = 1;
    public static final int ATTRIBUTE = 2;
    public static final int FUNCTION = 3;
    public static final int VALUE = 4;
    public static final int VARIABLE = 5;
    public static final int GENERAL = 6;
    public static final int URI = 7;

    /**
     * Add delimiters to represent variable information within an error message
     * @param cs the variable information to be delimited
     * @return the delimited variable information
     */
    public static String wrap(CharSequence cs) {
        return wrap(cs, GENERAL);
    }

    /**
     * Add delimiters to represent variable information within an error message
     * @param cs the variable information to be delimited
     * @param valueType the type of value, e.g. element name or attribute name
     * @return the delimited variable information
     */
    public static String wrap(CharSequence cs, int valueType) {
        if (cs == null) {
            return "(NULL)";
        }
        FastStringBuffer sb = new FastStringBuffer(40);
        int len = cs.length();
        for (int i=0; i<len; i++) {
            char c = cs.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
//                case '\\':
//                    sb.append("\\\\");
//                    break;
                default:
                    if (c < 32 || c > 255) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        while (hex.length() < 4) {
                            hex = "0" + hex;
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        String s;
        if (len > 30) {
            if (valueType == URI) {
                s = "..." + sb.toString().substring(len-30);
            } else {
                s = sb.toString().substring(0, 30) + "...";
            }
        } else {
            s = sb.toString();
        }
        switch (valueType) {
            case ELEMENT:
                return "<" + s + ">";
            case ATTRIBUTE:
                return "@" + s;
            case FUNCTION:
                return s + "()";
            case VARIABLE:
                return "$" + s;
            case VALUE:
                return "\"" + s + "\"";
            default:
                return "{" + s + "}";
        }
    }

    /**
     * Create a string representation of an item for use in an error message
     */

    public static CharSequence depict(Item item) {
        if (item instanceof AtomicValue) {
            CharSequence cs = item.getStringValueCS();
            if (item instanceof StringValue) {
                return '\"' + truncate30(cs).toString() + '\"';
            } else {
                return truncate30(cs);
            }
        } else {
            NodeInfo node = (NodeInfo)item;
            switch (node.getNodeKind()) {
                case Type.DOCUMENT:
                    return "document-node()";
                case Type.ELEMENT:
                    return "<" + node.getDisplayName() + "/>";
                case Type.ATTRIBUTE:
                    return "@" + node.getDisplayName();
                case Type.TEXT:
                    return "text(\"" + truncate30(node.getStringValue()) + "\")";
                case Type.COMMENT:
                    return "<!--" + truncate30(node.getStringValue()) + "-->";
                case Type.PROCESSING_INSTRUCTION:
                    return "<?" + node.getDisplayName() + "?>";
                case Type.NAMESPACE:
                    String prefix = node.getLocalPart();
                    return "xmlns" + (prefix.equals("") ? "" : ":" + prefix) + "=\"" + node.getStringValue() + '"';
                default:
                    return "";
            }
        }
    }

    private static CharSequence truncate30(CharSequence cs) {
        if (cs.length() <= 30) {
            return Whitespace.collapseWhitespace(cs);
        } else {
            return Whitespace.collapseWhitespace(cs.subSequence(0, 30)).toString() + "...";
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//