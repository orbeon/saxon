package net.sf.saxon.pattern;
import net.sf.saxon.Configuration;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.*;

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

    // TODO: need to consider "issue 309": what happens when the document contains
    // a type annotation that wasn't known at stylesheet compile time? It might be
    // a subtype of the required type.

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
        this.config = config;

	}

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public ItemType getSuperType() {
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

        if (requiredType == -1 || requiredType == StandardNames.XS_ANY_TYPE) {
            return true;
            // TODO: don't bother generating a CombinedNodeTest when the element at the head
            // of the substitution group has type xs:anyType (schvalid007)
        }

        if (annotation == requiredType) {
            return true;
        }

        if (annotation == -1) {
            if (nodeKind == Type.ELEMENT) {
                return (requiredType == StandardNames.XDT_UNTYPED);
            } else if (nodeKind == Type.ATTRIBUTE) {
                return (requiredType == StandardNames.XDT_UNTYPED_ATOMIC ||
                        requiredType == StandardNames.XDT_ANY_ATOMIC_TYPE ||
                        requiredType == StandardNames.XS_ANY_SIMPLE_TYPE);
            } else {
                return false;
            }
        }

        try {
            SchemaType type = config.getSchemaType(annotation & 0xfffff).getBaseType();
            while (type != null) {
                if (type.getFingerprint() == requiredType) {
                    return true;
                }
                type = type.getBaseType();
            }
        } catch (UnresolvedReferenceException e) {
            throw new IllegalStateException(e.getMessage());
        }
        return false;
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return matches(node.getNodeKind(), -1, node.getTypeAnnotation());
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
        if (type instanceof AtomicType) {
            return (AtomicType)type;
        } else if (type instanceof ListType) {
            SimpleType mem = ((ListType)type).getItemType();
            if (mem instanceof AtomicType) {
                return (AtomicType)mem;
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
