package net.sf.saxon;

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
        String s = cs.toString();
        s = s.replace('\n', ' ');
        s = s.replace('\t', ' ');
        s = s.replace('\r', ' ');
        if (s.length() > 30) {
            s = s.substring(0, 30) + "...";
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