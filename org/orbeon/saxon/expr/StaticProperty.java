package org.orbeon.saxon.expr;

/**
* This class contains constants identifying dependencies that an XPath expression
* might have on its context.
*/

public abstract class StaticProperty {

    /**
    * Bit setting: Expression depends on current() item
    */

    public static final int DEPENDS_ON_CURRENT_ITEM = 1;

    /**
    * Bit setting: Expression depends on context item
    */

    public static final int DEPENDS_ON_CONTEXT_ITEM = 1<<1;

    /**
    * Bit setting: Expression depends on position()
    */

    public static final int DEPENDS_ON_POSITION = 1<<2;

    /**
    * Bit setting: Expression depends on last()
    */

    public static final int DEPENDS_ON_LAST = 1<<3;

    /**
    * Bit setting: Expression depends on the document containing the context node
    */

    public static final int DEPENDS_ON_CONTEXT_DOCUMENT = 1<<4;

    /**
    * Bit setting: Expression depends on current-group() and/or current-grouping-key()
    */

    public static final int DEPENDS_ON_CURRENT_GROUP = 1<<5;
    
    /**
    * Bit setting: Expression depends on regex-group() 
    */

    public static final int DEPENDS_ON_REGEX_GROUP = 1<<6;    

    /**
    * Combination of bits representing dependencies on the XSLT context
    */

    public static final int DEPENDS_ON_XSLT_CONTEXT =
            DEPENDS_ON_CURRENT_ITEM |
            DEPENDS_ON_CURRENT_GROUP |
            DEPENDS_ON_REGEX_GROUP;

    /**
    * Combination of bits representing dependencies on the focus
    */

    public static final int DEPENDS_ON_FOCUS =
            DEPENDS_ON_CONTEXT_ITEM |
            DEPENDS_ON_POSITION |
            DEPENDS_ON_LAST |
            DEPENDS_ON_CONTEXT_DOCUMENT;

    /**
    * Combination of bits representing dependencies on the focus, but excluding dependencies
    * on the current document
    */

    public static final int DEPENDS_ON_NON_DOCUMENT_FOCUS =
            DEPENDS_ON_CONTEXT_ITEM |
            DEPENDS_ON_POSITION |
            DEPENDS_ON_LAST;

    /**
     * Mask to select all the dependency bits
     */

    public static final int DEPENDENCY_MASK =
            DEPENDS_ON_CONTEXT_DOCUMENT |
            DEPENDS_ON_CONTEXT_ITEM |
            DEPENDS_ON_CURRENT_GROUP |
            DEPENDS_ON_REGEX_GROUP |
            DEPENDS_ON_CURRENT_ITEM |
            DEPENDS_ON_FOCUS;

    /*
    * Bit set if an empty sequence is allowed
    */

    public static final int ALLOWS_ZERO = 1<<8;

    /**
    * Bit set if a single value is allowed
    */

    public static final int ALLOWS_ONE = 1<<9;

    /**
    * Bit set if multiple values are allowed
    */

    public static final int ALLOWS_MANY = 1<<10;

    /**
     * Mask for all cardinality bits
     */

    public static final int CARDINALITY_MASK =
            ALLOWS_ZERO | ALLOWS_ONE | ALLOWS_MANY;

    /**
    * Occurence indicator for "one or more" (+)
    */

    public static final int ALLOWS_ONE_OR_MORE =
            ALLOWS_ONE | ALLOWS_MANY;

    /**
    * Occurence indicator for "zero or more" (*)
    */

    public static final int ALLOWS_ZERO_OR_MORE =
            ALLOWS_ZERO | ALLOWS_ONE | ALLOWS_MANY;

    /**
    * Occurence indicator for "zero or one" (?)
    */

    public static final int ALLOWS_ZERO_OR_ONE =
            ALLOWS_ZERO | ALLOWS_ONE;

    /**
    * Occurence indicator for "exactly one" (default occurrence indicator)
    */

    public static final int EXACTLY_ONE = ALLOWS_ONE;

    /**
    * Occurence indicator when an empty sequence is required
    */

    public static final int EMPTY = ALLOWS_ZERO;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression whose item type is node, when the nodes in the result are
     * guaranteed all to be in the same document as the context node. For
     * expressions that return values other than nodes, the setting is undefined.
     */

    public static final int CONTEXT_DOCUMENT_NODESET = 1<<16;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression whose item type is node, when the nodes in the result are
     * in document order.
     */

    public static final int ORDERED_NODESET = 1<<17;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers items in the reverse of the correct order, when unordered
     * retrieval is requested.
     */

    public static final int REVERSE_DOCUMENT_ORDER = 1<<18;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers a set of nodes with the guarantee that no node in the
     * set will be an ancestor of any other. This property is useful in deciding whether the
     * results of a path expression are pre-sorted. The property is only used in the case where
     * the NATURALLY_SORTED property is true, so there is no point in setting it in other cases.
     */

    public static final int PEER_NODESET = 1<<19;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers a set of nodes with the guarantee that every node in the
     * result will be a descendant or self, or attribute or namespace, of the context node
     */

    public static final int SUBTREE_NODESET = 1<<20;

    /**
     * Expression property: this bit is set by getProperties() in the case of
     * an expression that delivers a set of nodes with the guarantee that every node in the
     * result will be an attribute or namespace of the context node
     */

    public static final int ATTRIBUTE_NS_NODESET = 1<<21;

    /**
     * Expression property: this bit is set in the case of an expression that
     * may return newly created nodes, or a value that depends on the identity
     * of newly created nodes (for example generate-id(new-node())). Such expressions
     * cannot be moved out of loops unless they are used in a context where the
     * identity of the nodes is known to be immaterial, e.g. if the nodes are
     * immediately atomized.
     */

    public static final int CREATES_NEW_NODES = 1<<22;

    /**
     * Mask for "special properties": that is, all properties other than cardinality
     * and dependencies
     */

    public static final int SPECIAL_PROPERTY_MASK =
            CONTEXT_DOCUMENT_NODESET |
            ORDERED_NODESET |
            REVERSE_DOCUMENT_ORDER |
            PEER_NODESET |
            SUBTREE_NODESET |
            ATTRIBUTE_NS_NODESET |
            CREATES_NEW_NODES;

    // This class is not instantiated
    private StaticProperty() {}
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
