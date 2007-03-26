package org.orbeon.saxon.pattern;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.Nilled;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.tinytree.TinyTree;
import org.orbeon.saxon.type.*;

/**
 * NodeTest is an interface that enables a test of whether a node matches particular
 * conditions. ContentTypeTest tests for an element or attribute node with a particular
 * type annotation.
  *
  * @author Michael H. Kay
  */

public class ContentTypeTest extends NodeTest {

	private int kind;          // element or attribute
    private SchemaType schemaType;
    private int requiredType;
    private Configuration config;
    private boolean nillable = false;
    private boolean matchDTDTypes = false;

    /**
     * Create a ContentTypeTest
     * @param nodeKind the kind of nodes to be matched: always elements or attributes
     * @param schemaType the required type annotation, as a simple or complex schema type
     * @param config the Configuration, supplied because this KindTest needs access to schema information
     */

	public ContentTypeTest(int nodeKind, SchemaType schemaType, Configuration config) {
		this.kind = nodeKind;
        this.schemaType = schemaType;
        this.requiredType = schemaType.getFingerprint();
        if (requiredType == -1) {
            requiredType = StandardNames.XDT_UNTYPED;   // probably doesn't happen
        }
        this.config = config;
	}

    /**
     * Indicate whether nilled elements should be matched (the default is false)
     * @param nillable true if nilled elements should be matched
     */
    public void setNillable(boolean nillable) {
        this.nillable = nillable;
    }

    /**
     * The test is nillable if a question mark was specified as the occurrence indicator
     * @return true if the test is nillable
     */

    public boolean isNillable() {
        return nillable;
    }

    /**
     * Indicate whether DTD-derived content types should be matched (the default is false)
     * @param matched true if DTD-derived types should be matched. If false, DTD-derived types are treated
     * as untypedAtomic
     */

    public void setMatchDTDTypes(boolean matched) {
        this.matchDTDTypes = matched;
    }

    /**
     * Test whether DTD-derived content types should be matched (the default is false)
     * @return true if DTD-derived types should be matched. If false, DTD-derived types are treated
     * as untypedAtomic
     */

    public boolean matchesDTDTypes() {
        return matchDTDTypes;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public ItemType getSuperType(TypeHierarchy th) {
        return NodeKindTest.makeNodeKindTest(kind);
    }

    /**
    * Test whether this node test is satisfied by a given node
    * @param nodeKind The type of node to be matched
     * @param fingerprint identifies the expanded name of the node to be matched
     * @param annotation The actual content type of the node
     */

    public boolean matches(int nodeKind, int fingerprint, int annotation) {
        if (kind != nodeKind) {
            return false;
        }
        return matchesAnnotation(annotation);
    }

    /**
     * Test whether this node test is satisfied by a given node on a TinyTree. The node
     * must be a document, element, text, comment, or processing instruction node.
     * This method is provided
     * so that when navigating a TinyTree a node can be rejected without
     * actually instantiating a NodeInfo object.
     *
     * @param tree   the TinyTree containing the node
     * @param nodeNr the number of the node within the TinyTree
     * @return true if the node matches the NodeTest, otherwise false
     */

    public boolean matches(TinyTree tree, int nodeNr) {
        if (kind != tree.getNodeKind(nodeNr)) {
            return false;
        }
        return matchesAnnotation(tree.getTypeAnnotation(nodeNr))
            && (nillable || !tree.isNilled(nodeNr));
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return node.getNodeKind() == kind &&
                matchesAnnotation(node.getTypeAnnotation())
                && (nillable || !Nilled.isNilled(node));
    }

    private boolean matchesAnnotation(int annotation) {
        if (requiredType == StandardNames.XS_ANY_TYPE) {
            return true;
        }

        if (annotation == -1) {
            annotation = (kind==Type.ATTRIBUTE ? StandardNames.XDT_UNTYPED_ATOMIC : StandardNames.XDT_UNTYPED);
        }

        if (matchDTDTypes) {
            annotation = annotation & NamePool.FP_MASK;
        } else if (((annotation & NodeInfo.IS_DTD_TYPE) != 0)) {
            return (requiredType == StandardNames.XDT_UNTYPED_ATOMIC);
        }

        if (annotation == requiredType) {
            return true;
        }

        // see if the type annotation is a subtype of the required type


        try {
            SchemaType type = config.getSchemaType(annotation & NamePool.FP_MASK).getBaseType();
            if (type == null) {
                // only true if annotation = XS_ANY_TYPE
                return false;
            }
            ItemType actual = new ContentTypeTest(kind, type, config);
            return config.getTypeHierarchy().isSubType(actual, this);
        } catch (UnresolvedReferenceException e) {
            throw new IllegalStateException(e.getMessage());
        }
        //return false;
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return 0;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return kind;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<kind;
    }

    /**
     * Get the content type allowed by this NodeTest (that is, the type annotation of the matched nodes).
     * Return AnyType if there are no restrictions. The default implementation returns AnyType.
     */

    public SchemaType getContentType() {
        return schemaType;
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized (assuming that atomization succeeds)
     */

    public AtomicType getAtomizedItemType() {
        SchemaType type = config.getSchemaType(requiredType);
        if (type.isAtomicType()) {
            return (AtomicType)type;
        } else if (type instanceof ListType) {
            SimpleType mem = ((ListType)type).getItemType();
            if (mem.isAtomicType()) {
                return (AtomicType)mem;
            }
        } else if (type instanceof ComplexType && ((ComplexType)type).isSimpleContent()) {
            SimpleType ctype = ((ComplexType)type).getSimpleContentType();
            if (ctype.isAtomicType()) {
                return (AtomicType)ctype;
            } else if (ctype instanceof ListType) {
                SimpleType mem = ((ListType)ctype).getItemType();
                if (mem.isAtomicType()) {
                    return (AtomicType)mem;
                }
            }
        }
        return Type.ANY_ATOMIC_TYPE;
    }

    public String toString() {
        return (kind == Type.ELEMENT ? "element(*, " : "attribute(*, ") +
                        schemaType.getDescription() + ')';
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return kind<<20 ^ requiredType;
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof ContentTypeTest &&
                ((ContentTypeTest)other).kind == kind &&
                ((ContentTypeTest)other).schemaType == schemaType &&
                ((ContentTypeTest)other).requiredType == requiredType &&
                ((ContentTypeTest)other).nillable == nillable &&
                ((ContentTypeTest)other).matchDTDTypes == matchDTDTypes;
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
