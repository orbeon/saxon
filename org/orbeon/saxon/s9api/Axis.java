package org.orbeon.saxon.s9api;

/**
 * This is an enumeration class containaing constants representing the thirteen XPath axes
 */
public enum Axis {
    ANCESTOR (org.orbeon.saxon.om.Axis.ANCESTOR),
    ANCESTOR_OR_SELF (org.orbeon.saxon.om.Axis.ANCESTOR_OR_SELF),
    ATTRIBUTE (org.orbeon.saxon.om.Axis.ATTRIBUTE),
    CHILD (org.orbeon.saxon.om.Axis.CHILD),
    DESCENDANT (org.orbeon.saxon.om.Axis.DESCENDANT),
    DESCENDANT_OR_SELF (org.orbeon.saxon.om.Axis.DESCENDANT_OR_SELF),
    FOLLOWING (org.orbeon.saxon.om.Axis.FOLLOWING),
    FOLLOWING_SIBLING (org.orbeon.saxon.om.Axis.FOLLOWING_SIBLING),
    PARENT (org.orbeon.saxon.om.Axis.PARENT),
    PRECEDING (org.orbeon.saxon.om.Axis.PRECEDING),
    PRECEDING_SIBLING (org.orbeon.saxon.om.Axis.PRECEDING_SIBLING),
    SELF (org.orbeon.saxon.om.Axis.SELF),
    NAMESPACE (org.orbeon.saxon.om.Axis.NAMESPACE);

    private byte number;

    /**
     * Create an Axis
     * @param number the internal axis number as defined in class {@link org.orbeon.saxon.om.Axis}
     */

    private Axis(byte number) {
        this.number = number;
    }

    /**
     * Get the axis number, as defined in class {@link org.orbeon.saxon.om.Axis}
     * @return the axis number
     */
    public byte getAxisNumber() {
        return number;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

