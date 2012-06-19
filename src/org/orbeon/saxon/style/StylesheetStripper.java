package org.orbeon.saxon.style;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;

import java.util.Arrays;

/**
  * The StylesheetStripper refines the Stripper class to do stripping of
  * whitespace nodes on a stylesheet. This is handled specially (a) because
  * it is done at compile time, so there is no Controller available, and (b)
  * because the rules are very simple
  * @author Michael H. Kay
  */

public class StylesheetStripper extends Stripper
{


    //    Any child of one of the following elements is removed from the tree,
    //    regardless of any xml:space attributes. Note that this array must be in numeric
    //    order for binary chop to work correctly.

    private static final int[] specials = {
        StandardNames.XSL_ANALYZE_STRING,
        StandardNames.XSL_APPLY_IMPORTS,
        StandardNames.XSL_APPLY_TEMPLATES,
        StandardNames.XSL_ATTRIBUTE_SET,
        StandardNames.XSL_CALL_TEMPLATE,
        StandardNames.XSL_CHARACTER_MAP,
        StandardNames.XSL_CHOOSE,
        StandardNames.XSL_NEXT_MATCH,
        StandardNames.XSL_STYLESHEET,
        StandardNames.XSL_TRANSFORM
    };

    public Stripper getAnother() {
        return new StylesheetStripper();
    }

    /**
    * Decide whether an element is in the set of white-space preserving element types
    * @param nameCode identifies the element being tested
    */

    public byte isSpacePreserving(int nameCode) {
        int fp = nameCode & 0xfffff;
        if (fp == StandardNames.XSL_TEXT) {
            return ALWAYS_PRESERVE;
        }

        if (Arrays.binarySearch(specials, fp) >= 0) {
            return ALWAYS_STRIP;
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

    public byte isSpacePreserving(NodeInfo element) throws XPathException {
        return isSpacePreserving(element.getNameCode());
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
