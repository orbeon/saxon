package net.sf.saxon.value;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.xpath.XPathException;

import java.io.PrintStream;


/**
 * A AtomicValue is a value that isn't a sequence and isn't a node.
 * More strictly, it is any sequence of length one whose content is not a node.
 */

public abstract class AtomicValue extends Value implements Item {

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
        Item item = evaluateItem(context);
        if (item != null) {
            context.getReceiver().append(item, 0);
        }
    }

    /**
     * Determine the static cardinality
     *
     * @return code identifying the cardinality
     * @see net.sf.saxon.value.Cardinality
     */

    public final int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

     /**
     * Convert the value to a given type. The result of the conversion will be an
     * atomic value of the required type. This method works only where the target
     * type is a built-in type.
     *
     * @exception XPathException if conversion is not allowed for this
     *     required type, or if the particular value cannot be converted
     * @param requiredType type code of the required atomic type
      * @param context the evaluation context. Used primarily for error reporting,
      * since nearly all type conversions are context-independent. The value may be
      * null if the context is not known.
     * @return the result of the conversion, if conversion was possible. This
     *      will always be an instance of the class corresponding to the type
     *      of value requested
     */

    public abstract AtomicValue convert(int requiredType, XPathContext context) throws XPathException;

    /**
     * Convert the value to a given type. The result of the conversion will be
     * an atomic value of the required type. This method works where the target
     * type is a built-in atomic type and also where it is a user-defined atomic
     * type.
     */

    public final AtomicValue convert(AtomicType targetType, XPathContext context) throws XPathException {
        if (targetType.isBuiltIn()) {
            return convert(targetType.getFingerprint(), context);
        } else {
            String lexicalValue = getStringValue();
            AtomicValue v = convert(targetType.getPrimitiveType(), context);
            return DerivedAtomicValue.makeValue(v, lexicalValue, targetType, true);
        }
    }

    /**
     * Evaluate the value (this simply returns the value unchanged)
     *
     * @exception XPathException
     * @param context the evaluation context (not used in this implementation)
     * @return the value, unchanged
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return this;
    }

    /**
     * Iterate over the (single) item in the sequence
     *
     * @param context the evaluation context (not used in this implementation)
     * @return a SequenceIterator that iterates over the single item in this
     *     value
     */

    public final SequenceIterator iterate(XPathContext context) {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Evaluate as a string
     */

    public final String evaluateAsString(XPathContext context) {
        return getStringValue();
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. This method is refined for AtomicValues
     * so that it never throws an Exception.
     */

    public abstract String getStringValue();

    /**
     * Get the typed value of this item
     *
     * @return the typed value of the expression (which is this value)
     */

    public final SequenceIterator getTypedValue(Configuration config) {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Get the primitive value (the value in the value space). This returns an
     * AtomicValue of a class that would be used to represent the primitive value.
     * In effect this means that for built-in types, it returns the value itself,
     * but for user-defined type, it returns the primitive value minus the type
     * annotation. Note that getItemType() when applied to the result of this
     * function does not not necessarily return a primitive type: for example, this
     * function may return a value of type xdt:dayTimeDuration, which is not a
     * primitive type as defined by {@link net.sf.saxon.type.Type#isPrimitiveType(int)} 
     */

    public AtomicValue getPrimitiveValue() {
        // overridden where necessary
        return this;
    }

    /**
     * Get the effective boolean value of the value
     *
     * @param context the evaluation context (not used in this implementation)
     * @return true, unless the value is boolean false, numeric zero, or
     *     zero-length string
     */
    public boolean effectiveBooleanValue(XPathContext context) {
        return true;
        // unless otherwise specified in a subclass
    }

    /**
     * Method to extract components of a value. Implemented by some subclasses,
     * but defined at this level for convenience
     */

    public AtomicValue getComponent(int component) throws XPathException {
        throw new UnsupportedOperationException("Data type does not support component extraction");
    }

    /**
     * Get string value. In general toString() for an atomic value displays the value as it would be
     * written in XPath: that is, as a literal if available, or as a call on a constructor function
     * otherwise.
     */

    public String toString() {
        return getItemType().toString() + " (\"" + getStringValue() + "\")";
    }

    /**
    * Diagnostic print of expression structure
    * @param level the indentation level of the output
     * @param out
     */

    public final void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + toString() );
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, Configuration config, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(SequenceIterator.class)) {
            return SingletonIterator.makeIterator(this);
        }
        if (target.isAssignableFrom(SequenceValue.class)) {
            Item[] a = {this};
            return new SequenceExtent(a);
        }
        Item[] array = {this};
        return new SequenceExtent(array).convertToJava(target, config, context);
//        if (target.isAssignableFrom(List.class)) {
//            List list = new ArrayList();
//            list.add(this);
//            return list;
//        };
//        return null;
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

