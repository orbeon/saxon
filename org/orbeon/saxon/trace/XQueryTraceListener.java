package net.sf.saxon.trace;

import net.sf.saxon.style.StandardNames;

/**
 * A Simple trace listener for XQuery that writes messages (by default) to System.err
 */

public class XQueryTraceListener extends AbstractTraceListener {

    /**
     * Generate attributes to be included in the opening trace element
     */

    protected String getOpeningAttributes() {
        return "";
    }

    /**
     * Get the trace element tagname to be used for a particular construct. Return null for
     * trace events that are ignored by this trace listener.
     */

    protected String tag(int construct) {
        switch (construct) {
            case StandardNames.XSL_FUNCTION:
                return "function";
            case StandardNames.XSL_VARIABLE:
                return "variable";
            case StandardNames.XSL_ELEMENT:
                return "element";
            case StandardNames.XSL_ATTRIBUTE:
                return "attribute";
            case StandardNames.XSL_COMMENT:
                return "comment";
            case StandardNames.XSL_DOCUMENT:
                return "document";
            case StandardNames.XSL_PROCESSING_INSTRUCTION:
                return "processing-instruction";
            case StandardNames.XSL_TEXT:
                return "text";
            case StandardNames.XSL_NAMESPACE:
                return "namespace";
            case Location.LITERAL_RESULT_ELEMENT:
                return "element";
            case Location.LITERAL_RESULT_ATTRIBUTE:
                return "attribute";
            case Location.FUNCTION_CALL:
                //return "function-call";
                return null;
            case Location.FOR_EXPRESSION:
                return "for";
            case Location.LET_EXPRESSION:
                return "let";
            case Location.WHERE_CLAUSE:
                return "where";
            case Location.ORDER_BY_CLAUSE:
                return "sort";
            case Location.RETURN_EXPRESSION:
                return "return";
            case Location.TYPESWITCH_EXPRESSION:
                return "typeswitch";
            case Location.VALIDATE_EXPRESSION:
                return "validate";
            case Location.IF_EXPRESSION:
                return "if";
            case Location.THEN_EXPRESSION:
                return "then";
            case Location.ELSE_EXPRESSION:
                return "else";
            case Location.CASE_EXPRESSION:
                return "case";
            case Location.DEFAULT_EXPRESSION:
                return "default";
            case Location.TRACE_CALL:
                return "user-trace";
            default:
                //return "Other";
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