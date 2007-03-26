package org.orbeon.saxon.value;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.Token;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.Configuration;

import java.io.PrintStream;

/**
* A value that is a sequence containing zero or one nodes
*/

public class SingletonNode extends Value {

    protected NodeInfo node = null;


    /**
     * Create a node-set containing zero or one nodes
     * @param node The node to be contained in the node-set, or null if the node-set
     * is to be empty
    */

    public SingletonNode(NodeInfo node) {
        this.node = node;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    /**
      * Process the instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(XPathContext context) throws XPathException {
        if (node != null) {
            context.getReceiver().append(node, 0, NodeInfo.ALL_NAMESPACES);
        }
    }


    /**
     * Determine the data type of the items in the expression. This method determines the most
     * precise type that it can, because it is called when testing that the node conforms to a required
     * type.
     * @return the most precise possible type of the node.
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                // Need to know whether the document is well-formed and if so what the element type is
                AxisIterator iter = node.iterateAxis(Axis.CHILD);
                ItemType elementType = null;
                while (true) {
                    NodeInfo n = (NodeInfo)iter.next();
                    if (n==null) {
                        break;
                    }
                    int kind = n.getNodeKind();
                    if (kind==Type.TEXT) {
                        elementType = null;
                        break;
                    } else if (kind==Type.ELEMENT) {
                        if (elementType != null) {
                            elementType = null;
                            break;
                        }
                        elementType = new SingletonNode(n).getItemType(th);
                    }
                }
                if (elementType == null) {
                    return NodeKindTest.DOCUMENT;
                } else {
                    return new DocumentNodeTest((NodeTest)elementType);
                }

            case Type.ELEMENT:
                int eltype = node.getTypeAnnotation();
                if (eltype == -1 || eltype == StandardNames.XDT_UNTYPED || eltype == StandardNames.XS_ANY_TYPE) {
                    return new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool());
                } else {
                    return new CombinedNodeTest(
                            new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool()),
                            Token.INTERSECT,
                            new ContentTypeTest(Type.ELEMENT, node.getConfiguration().getSchemaType(eltype),
                                    node.getConfiguration()));
                }

            case Type.ATTRIBUTE:
                int attype = node.getTypeAnnotation();
                if (attype == -1 || attype == StandardNames.XDT_UNTYPED_ATOMIC) {
                    return new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool());
                } else {
                    return new CombinedNodeTest(
                            new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool()),
                            Token.INTERSECT,
                            new ContentTypeTest(Type.ATTRIBUTE, node.getConfiguration().getSchemaType(attype),
                                    node.getConfiguration()));
                }

            case Type.TEXT:
                return NodeKindTest.TEXT;

            case Type.COMMENT:
                return NodeKindTest.COMMENT;

            case Type.PROCESSING_INSTRUCTION:
                 return NodeKindTest.PROCESSING_INSTRUCTION;

            case Type.NAMESPACE:
                return NodeKindTest.NAMESPACE;

            default:
                throw new IllegalArgumentException("Unknown node kind " + node.getNodeKind());
        }
    }

    /**
    * Determine the static cardinality
    */

    public int getCardinality() {
        if (node==null) {
            return StaticProperty.EMPTY;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return (node==null ? 0 : 1);
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    public Item itemAt(int n) throws XPathException {
        if (n==0 && node!=null) {
            return node;
        } else {
            return null;
        }

    }

    /**
    * Get the node that forms the node-set. Return null if there is none.
    */

    public NodeInfo getNode() {
        return node;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int getSpecialProperties() {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.NON_CREATIVE;
    }

    /**
    * Return an enumeration of this nodeset value.
    */

    public SequenceIterator iterate(XPathContext context) {
        return SingletonIterator.makeIterator(node);
    }

    /**
    * Evaluate as an item
    */

    public Item evaluateItem(XPathContext context) {
        return node;
    }

    /**
     * Get the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) {
        return (node != null);
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. For QNames and NOTATIONS, or lists
     * containing them, it fails.
     */

    public String getStringValue() {
        return (node==null ? "" : node.getStringValue());
    }

    /**
      * Evaluate an expression as a String. This function must only be called in contexts
      * where it is known that the expression will return a single string (or where an empty sequence
      * is to be treated as a zero-length string). Implementations should not attempt to convert
      * the result to a string, other than converting () to "". This method is used mainly to
      * evaluate expressions produced by compiling an attribute value template.
      *
      * @exception XPathException if any dynamic error occurs evaluating the
      *     expression
      * @param context The context in which the expression is to be evaluated
      * @return the value of the expression, evaluated in the current context.
      *     The expression must return a string or (); if the value of the
      *     expression is (), this method returns "".
      */

     public String evaluateAsString(XPathContext context) throws XPathException {
         if (node==null) return "";
         return node.getStringValue();
     }


    /**
    * Diagnostic display
    */

    public void display(int depth, PrintStream out, Configuration config) {
        if (node==null) {
            out.println(ExpressionTool.indent(depth) + "Empty node-set");
        } else {
            out.println(ExpressionTool.indent(depth) + "Node " + Navigator.getPath(node));
        }
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (node == null) {
            return null;
        }
        if (target.isAssignableFrom(node.getClass())) {
            return node;
        }
        if (target == String.class) {
            return node.getStringValue();
        }
        return super.convertToJava(target, context);
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

