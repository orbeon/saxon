package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.TransformerConfigurationException;
import java.text.DecimalFormatSymbols;
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

    public void prepareAttributes() throws TransformerConfigurationException {

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

    public void validate() throws TransformerConfigurationException {
        checkTopLevel(null);
        checkEmpty();
    }

    public DecimalFormatSymbols makeDecimalFormatSymbols() throws TransformerConfigurationException {
        DecimalFormatSymbols d = new DecimalFormatSymbols();
        DecimalFormatManager.setDefaults(d);
        if (decimalSeparator!=null) {
            d.setDecimalSeparator(toChar(decimalSeparator));
        }
        if (groupingSeparator!=null) {
            d.setGroupingSeparator(toChar(groupingSeparator));
        }
        if (infinity!=null) {
            d.setInfinity(infinity);
        }
        if (minusSign!=null) {
            d.setMinusSign(toChar(minusSign));
        }
        if (NaN!=null) {
            d.setNaN(NaN);
        }
        if (percent!=null) {
            d.setPercent(toChar(percent));
        }
        if (perMille!=null) {
            d.setPerMill(toChar(perMille));
        }
        if (zeroDigit!=null) {
            d.setZeroDigit(toChar(zeroDigit));
        }
        if (digit!=null) {
            d.setDigit(toChar(digit));
        }
        if (patternSeparator!=null) {
            d.setPatternSeparator(toChar(patternSeparator));
        }
        checkDistinctRoles(d);
        return d;
    }

    /**
     * Check that no character is used in more than one role
     * @throws TransformerConfigurationException
     */

    private void checkDistinctRoles(DecimalFormatSymbols dfs) throws TransformerConfigurationException {
        HashMap map = new HashMap(20);
        Character c = new Character(dfs.getDecimalSeparator());
        map.put(c, StandardNames.DECIMAL_SEPARATOR);

        c = new Character(dfs.getGroupingSeparator());
        if (map.get(c) != null) {
            duplicate(StandardNames.GROUPING_SEPARATOR, (String)map.get(c));
        }
        map.put(c, StandardNames.GROUPING_SEPARATOR);

        c = new Character(dfs.getPercent());
        if (map.get(c) != null) {
            duplicate(StandardNames.PERCENT, (String)map.get(c));
        }
        map.put(c, StandardNames.PERCENT);

        c = new Character(dfs.getPerMill());
        if (map.get(c) != null) {
            duplicate(StandardNames.PER_MILLE, (String)map.get(c));
        }
        map.put(c, StandardNames.PER_MILLE);

        c = new Character(dfs.getZeroDigit());
        if (map.get(c) != null) {
            duplicate(StandardNames.ZERO_DIGIT, (String)map.get(c));
        }
        map.put(c, StandardNames.ZERO_DIGIT);

        c = new Character(dfs.getDigit());
        if (map.get(c) != null) {
            duplicate(StandardNames.DIGIT, (String)map.get(c));
        }
        map.put(c, StandardNames.DIGIT);

        c = new Character(dfs.getPatternSeparator());
        if (map.get(c) != null) {
            duplicate(StandardNames.PATTERN_SEPARATOR, (String)map.get(c));
        }
        map.put(c, StandardNames.PATTERN_SEPARATOR);
    }

    private void duplicate(String role1, String role2) throws TransformerConfigurationException {
        compileError("The same character is used as the " + role1 +
                " and as the " + role2, "XT1300");
    }

    public void register() throws TransformerConfigurationException
    {
        prepareAttributes();
        DecimalFormatSymbols d = makeDecimalFormatSymbols();
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
                String[] parts = Name.getQNameParts(name);
	            String uri = getURIForPrefix(parts[0], false);
                try {
                    dfm.setNamedDecimalFormat(uri, parts[1], d, getPrecedence());
                } catch (StaticError err) {
                    compileError(err.getMessage(), err.getErrorCodeLocalPart());
                }
            } catch (XPathException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XT0020");
            } catch (QNameException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XT0020");
            } catch (NamespaceException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XT0280");
            }
        }
    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        return null;
    }

    private char toChar(String s) throws TransformerConfigurationException {
        if (s.length()!=1)
            compileError("Attribute \"" + s + "\" should be a single character", "XT0020");
        return s.charAt(0);
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
