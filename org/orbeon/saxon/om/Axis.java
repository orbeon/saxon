package org.orbeon.saxon.om;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.type.Type;

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
     * The class is never instantiated
     */

    private Axis() {
    }

    /**
     * Resolve an axis name into a symbolic constant representing the axis
     *
     * @param name
     * @exception org.orbeon.saxon.trans.StaticError
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

    /**
     * The following table indicates the combinations of axis and node-kind that always
     * return an empty result.
     */

    private static final int DOC = 1<<Type.DOCUMENT;
    private static final int ELE = 1<<Type.ELEMENT;
    private static final int ATT = 1<<Type.ATTRIBUTE;
    private static final int TEX = 1<<Type.TEXT;
    private static final int PIN = 1<<Type.PROCESSING_INSTRUCTION;
    private static final int COM = 1<<Type.COMMENT;
    private static final int NAM = 1<<Type.NAMESPACE;

    private static int[] voidAxisTable = {
         DOC,                       // ANCESTOR
         0,                         // ANCESTOR_OR_SELF;
         DOC|ATT|TEX|PIN|COM|NAM,   // ATTRIBUTE;
         ATT|TEX|PIN|COM|NAM,       // CHILD;
         ATT|TEX|PIN|COM|NAM,       // DESCENDANT;
         0,                         // DESCENDANT_OR_SELF;
         DOC,                       // FOLLOWING;
         DOC|ATT|NAM,               // FOLLOWING_SIBLING;
         DOC|ATT|TEX|PIN|COM|NAM,   // NAMESPACE;
         DOC,                       // PARENT;
         DOC,                       // PRECEDING;
         DOC|ATT|NAM,               // PRECEDING_SIBLING;
         0,                         // SELF;
    };

    public static boolean isAlwaysEmpty(int axis, int nodeKind) {
        return (voidAxisTable[axis] & (1<<nodeKind)) != 0;
    }

    /**
     * The following table indicates the kinds of node found on each axis
     */

    private static int[] nodeKindTable = {
             DOC|ELE,                       // ANCESTOR
             DOC|ELE|ATT|TEX|PIN|COM|NAM,   // ANCESTOR_OR_SELF;
             ATT,                           // ATTRIBUTE;
             ELE|TEX|PIN|COM,               // CHILD;
             ELE|TEX|PIN|COM,               // DESCENDANT;
             DOC|ELE|ATT|TEX|PIN|COM|NAM,   // DESCENDANT_OR_SELF;
             ELE|TEX|PIN|COM,               // FOLLOWING;
             ELE|TEX|PIN|COM,               // FOLLOWING_SIBLING;
             NAM,                           // NAMESPACE;
             DOC|ELE,                       // PARENT;
             DOC|ELE|TEX|PIN|COM,           // PRECEDING;
             ELE|TEX|PIN|COM,               // PRECEDING_SIBLING;
             DOC|ELE|ATT|TEX|PIN|COM|NAM,   // SELF;
        };

    /**
     * Determine whether a given kind of node can be found on a given axis
     */

    public static boolean containsNodeKind(int axis, int nodeKind) {
        return (nodeKindTable[axis] & (1<<nodeKind)) != 0;
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
