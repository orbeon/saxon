package net.sf.saxon.om;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.StaticError;

/**
 * An axis, that is a direction of navigation in the document structure.
 */

public final class Axis  {

    /**
     * Constant representing the ancestor axis
     */

    public static final byte ANCESTOR           = 0;
    /** Constant representing the ancestor-or-self axis
     */
    public static final byte ANCESTOR_OR_SELF   = 1;
    /** Constant representing the attribute axis
     */
    public static final byte ATTRIBUTE          = 2;
    /** Constant representing the child axis
     */
    public static final byte CHILD              = 3;
    /** Constant representing the descendant axis
     */
    public static final byte DESCENDANT         = 4;
    /** Constant representing the descendant-or-self axis
     */
    public static final byte DESCENDANT_OR_SELF = 5;
    /** Constant representing the following axis
     */
    public static final byte FOLLOWING          = 6;
    /** Constant representing the following-sibling axis
     */
    public static final byte FOLLOWING_SIBLING  = 7;
    /** Constant representing the namespace axis
     */
    public static final byte NAMESPACE          = 8;
    /** Constant representing the parent axis
     */
    public static final byte PARENT             = 9;
    /** Constant representing the preceding axis
     */
    public static final byte PRECEDING          = 10;
    /** Constant representing the preceding-sibling axis
     */
    public static final byte PRECEDING_SIBLING  = 11;
    /** Constant representing the self axis
     */
    public static final byte SELF               = 12;

    // preceding-or-ancestor axis gives all preceding nodes including ancestors,
    // in reverse document order

    /** Constant representing the preceding-or-ancestor axis. This axis is used internally by the xsl:number implementation, it returns the union of the preceding axis and the ancestor axis.
     */
    public static final byte PRECEDING_OR_ANCESTOR = 13;


    /**
     * Table indicating the principal node type of each axis
     */

    public static final short[] principalNodeType =
    {
        Type.ELEMENT,       // ANCESTOR
        Type.ELEMENT,       // ANCESTOR_OR_SELF;
        Type.ATTRIBUTE,     // ATTRIBUTE;
        Type.ELEMENT,       // CHILD;
        Type.ELEMENT,       // DESCENDANT;
        Type.ELEMENT,       // DESCENDANT_OR_SELF;
        Type.ELEMENT,       // FOLLOWING;
        Type.ELEMENT,       // FOLLOWING_SIBLING;
        Type.NAMESPACE,     // NAMESPACE;
        Type.ELEMENT,       // PARENT;
        Type.ELEMENT,       // PRECEDING;
        Type.ELEMENT,       // PRECEDING_SIBLING;
        Type.ELEMENT,       // SELF;
        Type.ELEMENT,       // PRECEDING_OR_ANCESTOR;
    };

    /**
     * Table indicating for each axis whether it is in forwards document order
     */

    public static final boolean[] isForwards =
    {
        false,          // ANCESTOR
        false,          // ANCESTOR_OR_SELF;
        true,           // ATTRIBUTE;
        true,           // CHILD;
        true,           // DESCENDANT;
        true,           // DESCENDANT_OR_SELF;
        true,           // FOLLOWING;
        true,           // FOLLOWING_SIBLING;
        false,          // NAMESPACE;
        true,           // PARENT;
        false,          // PRECEDING;
        false,          // PRECEDING_SIBLING;
        true,           // SELF;
        false,          // PRECEDING_OR_ANCESTOR;
    };

    /**
     * Table indicating for each axis whether it is in reverse document order
     */

    public static final boolean[] isReverse =
    {
        true,           // ANCESTOR
        true,           // ANCESTOR_OR_SELF;
        false,          // ATTRIBUTE;
        false,          // CHILD;
        false,          // DESCENDANT;
        false,          // DESCENDANT_OR_SELF;
        false,          // FOLLOWING;
        false,          // FOLLOWING_SIBLING;
        false,          // NAMESPACE;
        true,           // PARENT;
        true,           // PRECEDING;
        true,           // PRECEDING_SIBLING;
        true,           // SELF;
        true,           // PRECEDING_OR_ANCESTOR;
    };

    /**
     * Table indicating for each axis whether it is a peer axis. An axis is a peer
     * axis if no node on the axis is an ancestor of another node on the axis.
     */

    public static final boolean[] isPeerAxis =
    {
        false,          // ANCESTOR
        false,          // ANCESTOR_OR_SELF;
        true,           // ATTRIBUTE;
        true,           // CHILD;
        false,          // DESCENDANT;
        false,          // DESCENDANT_OR_SELF;
        false,          // FOLLOWING;
        true,           // FOLLOWING_SIBLING;
        true,           // NAMESPACE;
        true,           // PARENT;
        false,          // PRECEDING;
        true,           // PRECEDING_SIBLING;
        true,           // SELF;
        false,          // PRECEDING_OR_ANCESTOR;
    };

    /**
     * Table indicating for each axis whether it is contained within the subtree
     * rooted at the origin node.
     */

    public static final boolean[] isSubtreeAxis =
    {
        false,          // ANCESTOR
        false,          // ANCESTOR_OR_SELF;
        true,           // ATTRIBUTE;
        true,           // CHILD;
        true,           // DESCENDANT;
        true,           // DESCENDANT_OR_SELF;
        false,          // FOLLOWING;
        false,          // FOLLOWING_SIBLING;
        true,           // NAMESPACE;
        false,          // PARENT;
        false,          // PRECEDING;
        false,          // PRECEDING_SIBLING;
        true,           // SELF;
        false,          // PRECEDING_OR_ANCESTOR;
    };

    /**
     * Table giving the name each axis
     */

    public static final String[] axisName =
    {
        "ancestor",             // ANCESTOR
        "ancestor-or-self",     // ANCESTOR_OR_SELF;
        "attribute",            // ATTRIBUTE;
        "child",                // CHILD;
        "descendant",           // DESCENDANT;
        "descendant-or-self",   // DESCENDANT_OR_SELF;
        "following",            // FOLLOWING;
        "following-sibling",    // FOLLOWING_SIBLING;
        "namespace",            // NAMESPACE;
        "parent",               // PARENT;
        "preceding",            // PRECEDING;
        "preceding-sibling",    // PRECEDING_SIBLING;
        "self",                 // SELF;
        "preceding-or-ancestor",// PRECEDING_OR_ANCESTOR;
    };

    /**
     * Resolve an axis name into a symbolic constant representing the axis
     *
     * @param name
     * @exception net.sf.saxon.xpath.StaticError
     * @return integer value representing the named axis
     */

    public static byte getAxisNumber(String name) throws StaticError {
        if (name.equals("ancestor"))                return ANCESTOR;
        if (name.equals("ancestor-or-self"))        return ANCESTOR_OR_SELF;
        if (name.equals("attribute"))               return ATTRIBUTE;
        if (name.equals("child"))                   return CHILD;
        if (name.equals("descendant"))              return DESCENDANT;
        if (name.equals("descendant-or-self"))      return DESCENDANT_OR_SELF;
        if (name.equals("following"))               return FOLLOWING;
        if (name.equals("following-sibling"))       return FOLLOWING_SIBLING;
        if (name.equals("namespace"))               return NAMESPACE;
        if (name.equals("parent"))                  return PARENT;
        if (name.equals("preceding"))               return PRECEDING;
        if (name.equals("preceding-sibling"))       return PRECEDING_SIBLING;
        if (name.equals("self"))                    return SELF;
        // preceding-or-ancestor cannot be used in an XPath expression
        throw new StaticError("Unknown axis name: " + name);
    }



}

/*
    // a list for any future cut-and-pasting...
    ANCESTOR
    ANCESTOR_OR_SELF;
    ATTRIBUTE;
    CHILD;
    DESCENDANT;
    DESCENDANT_OR_SELF;
    FOLLOWING;
    FOLLOWING_SIBLING;
    NAMESPACE;
    PARENT;
    PRECEDING;
    PRECEDING_SIBLING;
    SELF;
*/


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
