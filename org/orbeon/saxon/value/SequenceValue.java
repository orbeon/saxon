package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.dom.DOMNodeList;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A SequenceValue object represents a sequence whose members
 * are all AtomicValues or Nodes.
 */


public abstract class SequenceValue extends Value {

    /**
     * Materialize the SequenceValue as a SequenceExtent
     */

    public SequenceExtent materialize() throws XPathException {
        return new SequenceExtent(iterate(null));
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD;
    }

    /**
      * Process the instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = iterate(context);
        SequenceReceiver out = context.getReceiver();
        while (true) {
            Item it = iter.next();
            if (it==null) break;
            out.append(it, 0);
        }
    }

    /**
     * Determine the data type of the items in the expression, if possible
     * @return AnyItemType (not known)
     */

    public ItemType getItemType() {
        return AnyItemType.getInstance();
    }

    /**
     * Determine the cardinality
     */

    public int getCardinality() {
        try {
            SequenceIterator iter = iterate(null);
            Item next = iter.next();
            if (next == null) {
                return StaticProperty.EMPTY;
            } else {
                if (iter.next() != null) {
                    return StaticProperty.ALLOWS_ONE_OR_MORE;
                } else {
                    return StaticProperty.EXACTLY_ONE;
                }
            }
        } catch (XPathException err) {
            // can't actually happen
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     */

    public Item itemAt(int n) throws XPathException {
        if (n < 0) return null;
        int i = 0;        // indexing is zero-based
        SequenceIterator iter = iterate(null);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                return null;
            }
            if (i++ == n) {
                return item;
            }
        }
    }

    /**
     * Evaluate as a singleton item (or empty sequence)
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return iterate(context).next();
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list.
     * @throws XPathException The method can fail if evaluation of the value
     * has been deferred, and if a failure occurs during the deferred evaluation.
     * No failure is possible in the case of an AtomicValue.
     */

    public String getStringValue() throws XPathException {
        StringBuffer sb = new StringBuffer(1024);
        SequenceIterator iter = iterate(null);
        Item item = iter.next();
        if (item != null) {
            while (true) {
                sb.append(item.getStringValue());
                item = iter.next();
                if (item == null) {
                    break;
                }
                sb.append(' ');
            }
        }
        return sb.toString();
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
     * @exception ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public String evaluateAsString(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue) evaluateItem(context);
        if (value == null) return "";
        return value.getStringValue();
    }


    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        SequenceIterator it = iterate(context);
        Item first = it.next();
        if (first == null) {
            return false;
        }
        if (first instanceof NodeInfo) {
            return true;
        } else {
            if (first instanceof BooleanValue) {
                return ((BooleanValue) first).getBooleanValue() || (it.next() != null);
            } else if (first instanceof StringValue) {
                return (!"".equals(first.getStringValue())) || (it.next() != null);
            } else if (first instanceof NumericValue) {
                // first==first is a test for NaN
                return (it.next() != null) || (!(first.equals(DoubleValue.ZERO)) && first.equals(first));
            } else {
                return true;
            }
        }
    }

    /**
     * Compare two sequence values for equality. This supports identity constraints in XML Schema,
     * which allow list-valued elements and attributes to participate in key and uniqueness constraints.
     * This method returns false if any error occurs during the comparison, or if any of the items
     * in either sequence is a node rather than an atomic value.
     */

    public boolean equals(Object obj) {
        try {
            if (obj instanceof Value) {
                SequenceIterator iter1 = iterate(null);
                SequenceIterator iter2 = ((Value)obj).iterate(null);
                while (true) {
                    Item item1 = iter1.next();
                    Item item2 = iter2.next();
                    if (item1 == null && item2 == null) {
                        return true;
                    }
                    if (item1 == null || item2 == null) {
                        return false;
                    }
                    if (item1 instanceof NodeInfo || item2 instanceof NodeInfo) {
                        return false;
                    }
                    if (!item1.equals(item2)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } catch (XPathException e) {
            return false;
        }
    }

    /**
     * Return a hash code to support the equals() function
     */

    public int hashCode() {
        try {
            int hash = 0x06639662;  // arbitrary seed
            SequenceIterator iter = iterate(null);
            while (true) {
                Item item = iter.next();
                if (item == null) {
                    return hash;
                }
                hash ^= item.hashCode();
            }
        } catch (XPathException e) {
            return 0;
        }
    }

    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {

        if (target == Object.class) {
            List list = new ArrayList(20);
            return convertToJavaList(list, config, context);

        } else if (target.isAssignableFrom(SequenceValue.class)) {
            return this;

        } else if (target.isAssignableFrom(SequenceIterator.class)) {
            return iterate(null);

        } else if (Collection.class.isAssignableFrom(target)) {
            Collection list;
            if (target.isAssignableFrom(ArrayList.class)) {
                list = new ArrayList(100);
            } else {
                try {
                    list = (Collection)target.newInstance();
                } catch (InstantiationException e) {
                    DynamicError de = new DynamicError("Cannot instantiate collection class " + target);
                    de.setXPathContext(context);
                    throw de;
                } catch (IllegalAccessException e) {
                    DynamicError de = new DynamicError("Cannot access collection class " + target);
                    de.setXPathContext(context);
                    throw de;
                }
            }
            return convertToJavaList(list, config, context);
        } else if (target.isArray()) {
            Class component = target.getComponentType();
            if (component.isAssignableFrom(Item.class) ||
                    component.isAssignableFrom(NodeInfo.class) ||
                    component.isAssignableFrom(DocumentInfo.class) ||
                    component.isAssignableFrom(Node.class)) {
                SequenceExtent extent = materialize();
                int length = extent.getLength();
                Object array = Array.newInstance(component, length);
                for (int i=0; i<length; i++) {
                    try {
                        Array.set(array, i, extent.itemAt(i));
                    } catch (IllegalArgumentException err) {
                        DynamicError d = new DynamicError(
                                "Cannot convert item in sequence to the component type of the Java array", err);
                        d.setXPathContext(context);
                        throw d;
                    }
                }
                return array;
            } else {
                // try atomizing the sequence
                SequenceIterator it = new Atomizer(this, config).iterate(context);
                int length;
                if (it instanceof LastPositionFinder) {
                    length = ((LastPositionFinder)it).getLastPosition();
                } else {
                    SequenceExtent extent = new SequenceExtent(it);
                    length = extent.getLength();
                    it = extent.iterate(context);
                }
                Object array = Array.newInstance(component, length);
                for (int i=0; i<length; i++) {
                    try {
                        AtomicValue val = (AtomicValue)it.next();
                        Object jval = val.convertToJava(component, config, context);
                        Array.set(array, i, jval);
                    } catch (XPathException err) {
                        DynamicError d = new DynamicError(
                                "Cannot convert item in atomized sequence to the component type of the Java array", err);
                        d.setXPathContext(context);
                        throw d;
                    }
                }
                return array;
            }

        } else if (target.isAssignableFrom(NodeList.class)) {
            return DOMNodeList.checkAndMake(materialize());
        } else if (target.isAssignableFrom(Item.class) ||
                target.isAssignableFrom(NodeInfo.class) ||
                target.isAssignableFrom(DocumentInfo.class) ||
                target.isAssignableFrom(Node.class)) {

            // try passing the first item in the sequence
            SequenceIterator iter = iterate(null);
            Item first = null;
            while (true) {
                Item next = iter.next();
                if (next == null) {
                    break;
                }
                if (first != null) {
                    DynamicError err = new DynamicError("Sequence contains more than one value; Java method expects only one");
                    err.setXPathContext(context);
                    throw err;
                }
                first = next;
            }
            if (first == null) {
                // sequence is empty; pass a Java null
                return null;
            }
            if (target.isAssignableFrom(first.getClass())) {
                // covers Item, NodeInfo and DOM Node
                return first;
            }
            Object n = first;
            while (n instanceof VirtualNode) {
                // If we've got a wrapper around a DOM or JDOM node, and the user wants a DOM
                // or JDOM node, we unwrap it
                Object vn = ((VirtualNode) n).getUnderlyingNode();
                if (target.isAssignableFrom(vn.getClass())) {
                    return vn;
                } else {
                    n = vn;
                }
            }
            throw new DynamicError("Cannot convert supplied XPath value to the required type for the extension function");
        } else {
            // try atomizing the value
            SequenceIterator it = new Atomizer(this, config).iterate(context);
            Item first = null;
            while (true) {
                Item next = it.next();
                if (next == null) {
                    break;
                }
                if (first != null) {
                    DynamicError err = new DynamicError("Sequence contains more than one value; Java method expects only one");
                    err.setXPathContext(context);
                    throw err;
                }
                first = next;
            }
            if (first == null) {
                // sequence is empty; pass a Java null
                return null;
            }
            if (target.isAssignableFrom(first.getClass())) {
                return first;
            } else {
                return ((AtomicValue)first).convertToJava(target, config, context);
            }
        }
    }

    private Collection convertToJavaList(Collection list, Configuration config, XPathContext context) throws XPathException {
        // TODO: with JDK 1.5, check to see if the item type of the list is constrained
        SequenceIterator iter = iterate(null);
        while (true) {
            Item it = iter.next();
            if (it == null) {
                if (list.size() == 0) {
                    // map empty sequence to null
                    return null;
                } else {
                    return list;
                }
            }
            if (it instanceof AtomicValue) {
                list.add(((AtomicValue)it).convertToJava(Object.class, config, context));
            } else if (it instanceof VirtualNode) {
                list.add(((VirtualNode)it).getUnderlyingNode());
            } else {
                list.add(it);
            }
        }
    }

    /**
     * Diagnostic display of the expression
     */

    public void display(int level, NamePool pool, PrintStream out) {
        try {
            out.println(ExpressionTool.indent(level) + "sequence of " +
                    getItemType().toString() + " (");
            SequenceIterator iter = iterate(null);
            while (true) {
                Item it = iter.next();
                if (it == null) {
                    break;
                }
                if (it instanceof NodeInfo) {
                    out.println(ExpressionTool.indent(level + 1) + "node " + Navigator.getPath(((NodeInfo)it)));
                } else {
                    out.println(ExpressionTool.indent(level + 1) + it.toString());
                }
            }
            out.println(ExpressionTool.indent(level) + ')');
        } catch (XPathException err) {
            out.println(ExpressionTool.indent(level) + "(*error*)");
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
