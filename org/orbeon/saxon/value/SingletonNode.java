package net.sf.saxon.value;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.xpath.XPathException;

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
            node.copy(context.getReceiver(), NodeInfo.ALL_NAMESPACES, true, 0);
        }
    }


    /**
    * Determine the data type of the items in the expression
    * @return Type.NODE
    */

    public ItemType getItemType() {
        return AnyNodeTest.getInstance();
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
    * Set a flag to indicate whether the nodes are sorted. Used when the creator of the
    * node-set knows that they are already in document order.
    * @param isSorted true if the caller wishes to assert that the nodes are in document order
    * and do not need to be further sorted
    */

//    public void setSorted(boolean isSorted) {}

    /**
    * Test whether the value is known to be sorted
    * @return true if the value is known to be sorted in document order, false if it is not
    * known whether it is sorted.
    */

//    public boolean isSorted() {
//        return true;
//    }

    /**
    * Convert to string value
    * @return the value of the first node in the node-set if there
    * is one, otherwise an empty string
    */

//    public String getStringValue() {
//        if (node==null) {
//            return "";
//        } else {
//            return node.getStringValue();
//        }
//    }

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

    public void display(int depth, NamePool pool, PrintStream out) {
        if (node==null) {
            out.println(ExpressionTool.indent(depth) + "Empty node-set");
        } else {
            out.println(ExpressionTool.indent(depth) + "Node " + Navigator.getPath(node));
        }
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        return new SequenceExtent(iterate(null)).convertToJava(target, config, context);
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

