package org.orbeon.saxon.s9api;

/**
 * WhitespaceStrippingPolicy is an enumeration class defining the possible policies for handling
 * whitespace text nodes in a source document.
 */

public enum WhitespaceStrippingPolicy {
    /**
     * The value NONE indicates that all whitespace text nodes are retained
     */
    NONE,
    /**
     * The value IGNORABLE indicates that whitespace text nodes in element-only content are
     * discarded. Content is element-only if it is defined by a schema or DTD definition that
     * does not allow mixed or PCDATA content.
     */
    IGNORABLE,
    /**
     * The value ALL indicates that all whitespace-only text nodes are discarded.
     */
    ALL,
    /**
     * UNSPECIFIED means that no other value has been specifically requested.
     */
    UNSPECIFIED
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

