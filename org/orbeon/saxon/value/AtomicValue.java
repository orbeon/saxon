package net.sf.saxon.value;

import net.sf.saxon.Err;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.type.*;

import java.io.PrintStream;


/**
 * A AtomicValue is a value that isn't a sequence and isn't a node.
 * More strictly, it is any sequence of length one whose content is not a node.
 */

public abstract class AtomicValue extends Value implements Item {

    /**
     * Test whether the type of this atomic value is a built-in type.
     * Default implementation returns true.
     */

    public boolean hasBuiltInType() {
        return true;
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
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        Item item = evaluateItem(context);
        if (item != null) {
            context.getReceiver().append(item, 0, NodeInfo.ALL_NAMESPACES);
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
     * @param requiredType type code of the required atomic type
     * @return the result of the conversion, if conversion was possible. This
     *         will always be an instance of the class corresponding to the type
     *         of value requested
     * @throws XPathException if conversion is not allowed for this
     *                        required type, or if the particular value cannot be converted
     */

    public final AtomicValue convert(int requiredType) throws XPathException {
        SchemaType schemaType = BuiltInSchemaFactory.getSchemaType(requiredType);
        if (schemaType instanceof BuiltInAtomicType) {
            AtomicValue val = convertPrimitive((BuiltInAtomicType)schemaType, true);
            if (val instanceof ErrorValue) {
                throw ((ErrorValue)val).getException();
            }
            return val;
        } else {
            throw new IllegalArgumentException(
                    "This method can only be used for conversion to a built-in atomic type");
        }
    };

    /**
     * Convert a value to another primitive data type, with control over how validation is
     * handled.
     *
     * @param requiredType type code of the required atomic type
     * @param validate     true if validation is required. If set to false, the caller guarantees that
     *                     the value is valid for the target data type, and that further validation is therefore not required.
     *                     Note that a validation failure may be reported even if validation was not requested.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     *         will be an ErrorValue. The caller must check for this condition. No exception is thrown, instead
     *         the exception will be encapsulated within the ErrorValue.
     */
    public abstract AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate);

    /**
     * Convert the value to a given type. The result of the conversion will be
     * an atomic value of the required type. This method works where the target
     * type is a built-in atomic type and also where it is a user-defined atomic
     * type.
     *
     * @param targetType the type to which the value is to be converted
     * @param context    the evaluation context
     * @param validate   true if validation is required, false if the caller already knows that the
     *                   value is valid
     * @return the value after conversion if successful; or an ErrorValue if conversion failed. The
     *         caller must check for this condition. Validation may fail even if validation was not requested.
     */

    public AtomicValue convert(AtomicType targetType, XPathContext context, boolean validate) {
        if (targetType instanceof BuiltInAtomicType) {
            return convertPrimitive((BuiltInAtomicType)targetType, validate);
        } else {
            CharSequence lexicalValue = getStringValueCS();
            AtomicValue v = convertPrimitive((BuiltInAtomicType)targetType.getPrimitiveItemType(), validate);
            if (v instanceof ErrorValue) {
                // conversion has failed
                return v;
            }
            return targetType.makeDerivedValue(v, lexicalValue, validate);
        }
    }

    /**
     * Get the length of the sequence
     *
     * @return always 1 for an atomic value
     */

    public final int getLength() {
        return 1;
    }

    /**
     * Evaluate the value (this simply returns the value unchanged)
     *
     * @param context the evaluation context (not used in this implementation)
     * @return the value, unchanged
     * @throws XPathException
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return this;
    }

    /**
     * Iterate over the (single) item in the sequence
     *
     * @param context the evaluation context (not used in this implementation)
     * @return a SequenceIterator that iterates over the single item in this
     *         value
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
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return getStringValue();
    }

    /**
     * Get the typed value of this item
     *
     * @return the typed value of the expression (which is this value)
     */

    public final SequenceIterator getTypedValue() {
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
     *         zero-length string
     */
    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        DynamicError err = new DynamicError("Effective boolean value is not defined for an atomic value of type " +
                (context == null ?
                "other than boolean, number, or string" :
                getItemType().toString(context.getController().getNamePool())));
        err.setIsTypeError(true);
        err.setXPathContext(context);
        throw err;
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
     * Check statically that the results of the expression are capable of constructing the content
     * of a given schema type.
     *
     * @param parentType The schema type
     * @param env        the static context
     * @param whole      true if this atomic value accounts for the entire content of the containing node
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression doesn't match the required content type
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (whole) {
            SimpleType stype = null;
            if (parentType instanceof SimpleType) {
                stype = (SimpleType)parentType;
            } else if (parentType instanceof ComplexType && ((ComplexType)parentType).isSimpleContent()) {
                stype = ((ComplexType)parentType).getSimpleContentType();
            }
            if (stype != null && !stype.isNamespaceSensitive()) {
                // Can't validate namespace-sensitive content statically
                XPathException err = stype.validateContent(getStringValueCS(), null);
                if (err != null) {
                    throw err;
                }
                return;
            }
        }
        if (parentType instanceof ComplexType &&
                !((ComplexType)parentType).isSimpleContent() &&
                !((ComplexType)parentType).isMixedContent() &&
                !Navigator.isWhite(getStringValueCS())) {
            StaticError err = new StaticError("Complex type " + parentType.getDescription() +
                    " does not allow text content " +
                    Err.wrap(getStringValueCS()));
            err.setIsTypeError(true);
            throw err;
        }
    }

    /**
     * Get string value. In general toString() for an atomic value displays the value as it would be
     * written in XPath: that is, as a literal if available, or as a call on a constructor function
     * otherwise.
     */

    public String toString() {
        return getItemType().toString() + " (\"" + getStringValueCS() + "\")";
    }

    /**
     * Diagnostic print of expression structure
     *
     * @param level the indentation level of the output
     * @param out
     */

    public final void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + toString());
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

