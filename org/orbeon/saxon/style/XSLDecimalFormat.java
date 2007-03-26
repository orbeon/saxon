package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.NamespaceException;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.trans.DecimalFormatManager;
import org.orbeon.saxon.trans.DecimalSymbols;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;

import java.util.Arrays;
import java.util.HashMap;

/**
* Handler for xsl:decimal-format elements in stylesheet. <br>
*/

public class XSLDecimalFormat extends StyleElement {

    boolean prepared = false;

    String name;
    String decimalSeparator;
    String groupingSeparator;
    String infinity;
    String minusSign;
    String NaN;
    String percent;
    String perMille;
    String zeroDigit;
    String digit;
    String patternSeparator;

    public void prepareAttributes() throws XPathException {

        if (prepared) {
            return;
        }
        prepared = true;

		AttributeCollection atts = getAttributeList();

        for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		name = atts.getValue(a).trim();
        	} else if (f==StandardNames.DECIMAL_SEPARATOR) {
        		decimalSeparator = atts.getValue(a);
        	} else if (f==StandardNames.GROUPING_SEPARATOR) {
        		groupingSeparator = atts.getValue(a);
        	} else if (f==StandardNames.INFINITY) {
        		infinity = atts.getValue(a);
        	} else if (f==StandardNames.MINUS_SIGN) {
        		minusSign = atts.getValue(a);
        	} else if (f==StandardNames.NAN) {
        		NaN = atts.getValue(a);
        	} else if (f==StandardNames.PERCENT) {
        		percent = atts.getValue(a);
        	} else if (f==StandardNames.PER_MILLE) {
        		perMille = atts.getValue(a);
        	} else if (f==StandardNames.ZERO_DIGIT) {
        		zeroDigit = atts.getValue(a);
        	} else if (f==StandardNames.DIGIT) {
        		digit = atts.getValue(a);
        	} else if (f==StandardNames.PATTERN_SEPARATOR) {
        		patternSeparator = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
    }

    public void validate() throws XPathException {
        checkTopLevel(null);
        checkEmpty();
    }

    public DecimalSymbols makeDecimalFormatSymbols() throws XPathException {
        DecimalSymbols d = new DecimalSymbols();
        DecimalFormatManager.setDefaults(d);
        if (decimalSeparator!=null) {
            d.decimalSeparator = (toChar(decimalSeparator));
        }
        if (groupingSeparator!=null) {
            d.groupingSeparator = (toChar(groupingSeparator));
        }
        if (infinity!=null) {
            d.infinity = (infinity);
        }
        if (minusSign!=null) {
            d.minusSign = (toChar(minusSign));
        }
        if (NaN!=null) {
            d.NaN = (NaN);
        }
        if (percent!=null) {
            d.percent = (toChar(percent));
        }
        if (perMille!=null) {
            d.permill = (toChar(perMille));
        }
        if (zeroDigit!=null) {
            d.zeroDigit = (toChar(zeroDigit));
            checkZeroDigit();
        }
        if (digit!=null) {
            d.digit = (toChar(digit));
        }
        if (patternSeparator!=null) {
            d.patternSeparator = (toChar(patternSeparator));
        }
        checkDistinctRoles(d);
        return d;
    }

    /**
     * Check that no character is used in more than one role
     * @throws XPathException
     */

    private void checkDistinctRoles(DecimalSymbols dfs) throws XPathException {
        HashMap map = new HashMap(20);
        Integer c = new Integer(dfs.decimalSeparator);
        map.put(c, StandardNames.DECIMAL_SEPARATOR);

        c = new Integer(dfs.groupingSeparator);
        if (map.get(c) != null) {
            duplicate(StandardNames.GROUPING_SEPARATOR, (String)map.get(c));
        }
        map.put(c, StandardNames.GROUPING_SEPARATOR);

        c = new Integer(dfs.percent);
        if (map.get(c) != null) {
            duplicate(StandardNames.PERCENT, (String)map.get(c));
        }
        map.put(c, StandardNames.PERCENT);

        c = new Integer(dfs.permill);
        if (map.get(c) != null) {
            duplicate(StandardNames.PER_MILLE, (String)map.get(c));
        }
        map.put(c, StandardNames.PER_MILLE);

        c = new Integer(dfs.zeroDigit);
        if (map.get(c) != null) {
            duplicate(StandardNames.ZERO_DIGIT, (String)map.get(c));
        }
        map.put(c, StandardNames.ZERO_DIGIT);

        c = new Integer(dfs.digit);
        if (map.get(c) != null) {
            duplicate(StandardNames.DIGIT, (String)map.get(c));
        }
        map.put(c, StandardNames.DIGIT);

        c = new Integer(dfs.patternSeparator);
        if (map.get(c) != null) {
            duplicate(StandardNames.PATTERN_SEPARATOR, (String)map.get(c));
        }
        map.put(c, StandardNames.PATTERN_SEPARATOR);
    }

    private void duplicate(String role1, String role2) throws XPathException {
        compileError("The same character is used as the " + role1 +
                " and as the " + role2, "XTSE1300");
    }

    /**
     * Check that the character declared as a zero-digit is indeed a valid zero-digit
     * @throws XPathException
     */

    public void checkZeroDigit() throws XPathException {
        int d;
        if (zeroDigit.length() == 1) {
            d = zeroDigit.charAt(0);
        } else {
            d = StringValue.expand(zeroDigit)[0];
        }
        if (Arrays.binarySearch(zeroDigits, d) < 0) {
            compileError(
                    "The value of the zero-digit attribute must be a Unicode digit with value zero",
                    "XTSE1295");
        }
    }

    static int[] zeroDigits = {0x0030, 0x0660, 0x06f0, 0x0966, 0x09e6, 0x0a66, 0x0ae6, 0x0b66, 0x0be6, 0x0c66,
                               0x0ce6, 0x0d66, 0x0e50, 0x0ed0, 0x0f20, 0x1040, 0x17e0, 0x1810, 0x1946, 0x19d0,
                               0xff10, 0x104a0, 0x1d7ce, 0x1d7d8, 0x1d7e2, 0x1d7ec, 0x1d7f6};

    public void register() throws XPathException
    {
        prepareAttributes();
        DecimalSymbols d = makeDecimalFormatSymbols();
        DecimalFormatManager dfm = getPrincipalStylesheet().getDecimalFormatManager();
        if (name==null) {
            try {
                dfm.setDefaultDecimalFormat(d, getPrecedence());
            } catch (StaticError err) {
                compileError(err.getMessage(), err.getErrorCodeLocalPart());
            }
        } else {
            try {
                makeNameCode(name);   // checks for reserved namespaces
                String[] parts = getConfiguration().getNameChecker().getQNameParts(name);
	            String uri = getURIForPrefix(parts[0], false);
                try {
                    dfm.setNamedDecimalFormat(uri, parts[1], d, getPrecedence());
                } catch (StaticError err) {
                    compileError(err.getMessage(), err.getErrorCodeLocalPart());
                }
            } catch (XPathException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XTSE0020");
            } catch (QNameException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XTSE0020");
            } catch (NamespaceException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XTSE0280");
            }
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        return null;
    }

    /**
     * Get the Unicode codepoint corresponding to a String, which must represent a single Unicode character
     * @param s the input string, representing a single Unicode character, perhaps as a surrogate pair
     * @return
     * @throws XPathException
     */
    private int toChar(String s) throws XPathException {
        int[] e = StringValue.expand(s);
        if (e.length!=1)
            compileError("Attribute \"" + s + "\" should be a single character", "XTSE0020");
        return e[0];
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
