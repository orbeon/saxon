package net.sf.saxon.style;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.tree.AttributeCollection;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.NamespaceException;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.expr.Expression;
import javax.xml.transform.TransformerConfigurationException;

import java.text.DecimalFormatSymbols;

/**
* Handler for xsl:decimal-format elements in stylesheet. <br>
*/

public class XSLDecimalFormat extends StyleElement {

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

    public Expression compile(Executable exec) throws TransformerConfigurationException
    {
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

        DecimalFormatManager dfm = getPrincipalStylesheet().getDecimalFormatManager();
        if (name==null) {
            try {
                dfm.setDefaultDecimalFormat(d);
            } catch (TransformerConfigurationException err) {
                compileError(err.getMessage());
            }
        } else {
            try {
                makeNameCode(name);   // checks for reserved namespaces
                String[] parts = Name.getQNameParts(name);
	            String uri = getURIForPrefix(parts[0], false);
                try {
                    dfm.setNamedDecimalFormat(uri, parts[1], d);
                } catch (TransformerConfigurationException err) {
                    compileError(err.getMessage());
                }
            } catch (XPathException err) {
                compileError("Invalid decimal format name. " + err.getMessage());
            } catch (QNameException err) {
                compileError("Invalid decimal format name. " + err.getMessage());
            } catch (NamespaceException err) {
                compileError("Invalid decimal format name. " + err.getMessage());
            }
        }
        return null;
    }

    private char toChar(String s) throws TransformerConfigurationException {
        if (s.length()!=1)
            compileError("Attribute \"" + s + "\" should be a single character");
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
