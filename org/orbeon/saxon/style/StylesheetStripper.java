package net.sf.saxon.style;
import net.sf.saxon.event.Stripper;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;

/**
  * The StylesheetStripper refines the Stripper class to do stripping of
  * whitespace nodes on a stylesheet. This is handled specially (a) because
  * it is done at compile time, so there is no Controller available, and (b)
  * because the rules are very simple
  * @author Michael H. Kay
  */

public class StylesheetStripper extends Stripper
{
    //   A child of xsl:text is preserved regardless of the setting of xml:space

    int xsl_text;   // fingerprint of name "xsl:text"

    //    Any child of one of the following elements is removed from the tree,
    //    regardless of any xml:space attributes:
    //
    //    xsl:analyze-string
    //    xsl:apply-imports
    //    xsl:apply-templates
    //    xsl:attribute-set
    //    xsl:call-template
    //    xsl:character-map
    //    xsl:choose
    //    xsl:next-match
    //    xsl:stylesheet
    //    xsl:transform

    int[] specials = new int[10];

    public Stripper getAnother() {
        StylesheetStripper s = new StylesheetStripper();
        s.xsl_text = xsl_text;
        s.specials = specials;
        return s;
    }

	/**
	* Set the rules appropriate for whitespace-stripping in a stylesheet
	*/

	public void setStylesheetRules(NamePool namePool) {
	    xsl_text = namePool.getFingerprint(NamespaceConstant.XSLT, "text");
	    specials[0] = namePool.getFingerprint(NamespaceConstant.XSLT, "analyze-string");
	    specials[1] = namePool.getFingerprint(NamespaceConstant.XSLT, "apply-imports");
	    specials[2] = namePool.getFingerprint(NamespaceConstant.XSLT, "apply-templates");
	    specials[3] = namePool.getFingerprint(NamespaceConstant.XSLT, "attribute-set");
	    specials[4] = namePool.getFingerprint(NamespaceConstant.XSLT, "call-template");
	    specials[5] = namePool.getFingerprint(NamespaceConstant.XSLT, "character-map");
	    specials[6] = namePool.getFingerprint(NamespaceConstant.XSLT, "choose");
	    specials[7] = namePool.getFingerprint(NamespaceConstant.XSLT, "next-match");
	    specials[8] = namePool.getFingerprint(NamespaceConstant.XSLT, "stylesheet");
	    specials[9] = namePool.getFingerprint(NamespaceConstant.XSLT, "transform");
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param nameCode identifies the element being tested
    */

    public byte isSpacePreserving(int nameCode) {
        int fp = nameCode & 0xfffff;
        if (fp == xsl_text) {
            return ALWAYS_PRESERVE;
        };
        for (int i = 0; i < specials.length; i++) {
            if (fp == specials[i]) {
                return ALWAYS_STRIP;
            }
        }
        return STRIP_DEFAULT;
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types.
     * This version of the method is useful in cases where getting the namecode of the
     * element is potentially expensive, e.g. with DOM nodes.
    * @param element Identifies the element whose whitespace is possibly to
     * be preserved
    * @return true if the element is in the set of white-space preserving element types
    */

    public byte isSpacePreserving(NodeInfo element) {
        return isSpacePreserving(element.getNameCode());
    }

    /**
    * Handle a text node
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException {
        // assume adjacent chunks of text are already concatenated
        super.characters(chars, locationId, properties);
    }


}   // end of class StylesheetStripper

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
