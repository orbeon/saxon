package net.sf.saxon.trace;

import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.style.StandardNames;

/**
 * A Simple trace listener for XSLT that writes messages (by default) to System.err
 */

public class XSLTTraceListener extends AbstractTraceListener {

    /**
     * Generate attributes to be included in the opening trace element
     */

    protected String getOpeningAttributes() {
        return "xmlns:xsl=\"" + NamespaceConstant.XSLT + '\"';
    }

    /**
     * Get the trace element tagname to be used for a particular construct. Return null for
     * trace events that are ignored by this trace listener.
     */

    protected String tag(int construct) {
        if (construct < 1024) {
            return StandardNames.getDisplayName(construct);
        }
        switch (construct) {
            case Location.LITERAL_RESULT_ELEMENT:
                return "LRE";
            case Location.LITERAL_RESULT_ATTRIBUTE:
                return "ATTR";
            case Location.LET_EXPRESSION:
                return "xsl:variable";
            case Location.EXTENSION_INSTRUCTION:
                return "extension-instruction";
            case Location.TRACE_CALL:
                return "user-trace";
            default:
                return null;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//