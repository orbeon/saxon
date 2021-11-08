package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.charcode.UTF16;


/**
* An xsl:output-character element in the stylesheet. <br>
*/

public class XSLOutputCharacter extends StyleElement {

    private int codepoint = -1;
        // the character to be substituted, as a Unicode codepoint (may be > 65535)
    private String replacementString = null;

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.CHARACTER) {
                String s = atts.getValue(a);
                switch (s.length()) {
                    case 0:
                        compileError("character attribute must not be zero-length", "XTSE0020");
                        codepoint = 256; // for error recovery
                        break;
                    case 1:
                        codepoint = s.charAt(0);
                        break;
                    case 2:
                        if (UTF16.isHighSurrogate(s.charAt(0)) &&
                                UTF16.isLowSurrogate(s.charAt(1))) {
                            codepoint = UTF16.combinePair(s.charAt(0), s.charAt(1));
                        } else {
                            compileError("character attribute must be a single XML character", "XTSE0020");
                            codepoint = 256; // for error recovery
                        }
                        break;
                    default:
                        compileError("character attribute must be a single XML character", "XTSE0020");
                        codepoint = 256; // for error recovery
                }
        	} else if (f==StandardNames.STRING) {
        		replacementString = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (codepoint==-1) {
            reportAbsence("character");
            return;
        }

        if (replacementString==null) {
            reportAbsence("string");
            return;
        }

    }

    public void validate() throws XPathException {
        if (!(getParent() instanceof XSLCharacterMap)) {
            compileError("xsl:output-character may appear only as a child of xsl:character-map", "XTSE0010");
        };
    }

    public Expression compile(Executable exec) throws XPathException {
        return null;
    }

    public int getCodePoint() {
        return codepoint;
    }

    public String getReplacementString() {
        return replacementString;
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
