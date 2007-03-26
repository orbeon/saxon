package org.orbeon.saxon.event;

import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.sort.IntHashSet;


/**
 * XHTMLIndenter: This class indents XHTML elements, by adding whitespace
 * character data where appropriate. This class differs from its superclass,
 * HTMLIndenter, only in the way it classifies elements as being inline or
 * formatted elements: unlike the HTML indenter, it requires the element names
 * to be in lower case and to be in the XHTML namespace.
 *
 * @author Michael Kay
*/


public class XHTMLIndenter extends HTMLIndenter {

    private IntHashSet inlineTagSet;
    private IntHashSet formattedTagSet;



    public XHTMLIndenter() {
    }

    /**
     * Classify an element name as inline, formatted, or both or neither.
     * This method is overridden in the XHTML indenter
     * @param nameCode the element name
     * @return a bit-significant integer containing flags IS_INLINE and/or IS_FORMATTED
     */

    protected int classifyTag(int nameCode) {
        if (inlineTagSet == null) {
            NamePool pool = getNamePool();
            inlineTagSet = new IntHashSet(50);
            formattedTagSet = new IntHashSet(10);
            for (int i=0; i<inlineTags.length; i++) {
                int nc = pool.allocate("", NamespaceConstant.XHTML, inlineTags[i]);
                inlineTagSet.add(nc);
            }
            for (int i=0; i<formattedTags.length; i++) {
                int nc = pool.allocate("", NamespaceConstant.XHTML, formattedTags[i]);
                formattedTagSet.add(nc);
            }
        }
        int r = 0;
        int key = nameCode & NamePool.FP_MASK;
        if (inlineTagSet.contains(key)) {
            r |= IS_INLINE;
        }
        if (formattedTagSet.contains(key)) {
            r |= IS_FORMATTED;
        }
        return r;
    }

};

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

