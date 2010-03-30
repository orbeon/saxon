package org.orbeon.saxon.value;

import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.NoDynamicContextException;
import org.orbeon.saxon.type.*;


/**
 * The AtomicValue class corresponds to the concept of an atomic value in the
 * XPath 2.0 data model. Atomic values belong to one of the 19 primitive types
 * defined in XML Schema; or they are of type xs:untypedAtomic; or they are
 * "external objects", representing a Saxon extension to the XPath 2.0 type system.
 * <p/>
 * The AtomicValue class contains some methods that are suitable for applications
 * to use, and many others that are designed for internal use by Saxon itself.
 * These have not been fully classified. At present, therefore, none of the methods on this
 * class should be considered to be part of the public Saxon API.
 * <p/>
 *
 * @author Michael H. Kay
 */

public abstract class AtomicValue extends Value implements Item, GroundedValue, ConversionResult {

    protected AtomicType typeLabel;

    /**
     * Set the type label on this atomic value. Note that this modifies the value, so it must only called
     * if the caller is confident that the value is not shared. In other cases,
     * use {@link #copyAsSubType(org.orbeon.saxon.type.AtomicType)}
     *
     * @param type the type label to be set
     */

    public void setTypeLabel(AtomicType type) {
        typeLabel = type;
    }


    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * An implementation must be provided for all atomic types.
     * <p/>
     * <p>In the case of data types that are partially ordered, the returned Comparable extends the standard
     * semantics of the compareTo() method by returning the value {@link #INDETERMINATE_ORDERING} when there
     * is no defined order relationship between two given values. This value is also returned when two values
     * of different types are compared.</p>
     *
     * @return a Comparable that follows XML Schema comparison rules
     */

    public abstract Comparable getSchemaComparable();

    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     * type is unordered; in other cases the returned value will be a Comparable.
     * @param collator the collation to be used when comparing strings
     * @param context the XPath dynamic evaluation context, used in cases where the comparison is context
     * sensitive
     * @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     *         with respect to this atomic value. If ordered is specified, the result will either be null if
     *         no ordering is defined, or will be a Comparable
     * @throws NoDynamicContextException if the comparison depends on dynamic context information that
     * is not available, for example implicit timezone
     */

    public abstract Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) 
            throws NoDynamicContextException;

    /**
     * The equals() methods on atomic values is defined to follow the semantics of eq when applied
     * to two atomic values. When the other operand is not an atomic value, the result is undefined
     * (may be false, may be an exception). When the other operand is an atomic value that cannot be
     * compared with this one, the method must throw a ClassCastException.
     *
     * <p>The hashCode() method is consistent with equals().</p>
     * @param o the other value
     * @return true if the other operand is an atomic value and the two values are equal as defined
     * by the XPath eq operator
     */

    public abstract boolean equals(Object o);

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return getStringValue();
    }

    /**
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        context.getReceiver().append(this, 0, NodeInfo.ALL_NAMESPACES);
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     *
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     *         numbered zero. If n is negative or >= the length of the sequence, returns null.
     */

    public final Item itemAt(int n) {
        return (n == 0 ? this : null);
    }


    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @param th The TypeHierarchy. Can be null if the target is an AtomicValue,
     *           except in the case where it is an external ObjectValue.
     * @return for the default implementation: AnyItemType (not known)
     */

    public ItemType getItemType(TypeHierarchy th) {
        return typeLabel;
    }

    /**
     * Determine the data type of the value. This
     * delivers the same answer as {@link #getItemType}, except in the case of external objects
     * (instances of {@link ObjectValue}, where the method may deliver a less specific type.
     *
     * @return for the default implementation: AnyItemType (not known)
     */

    public AtomicType getTypeLabel() {
        return typeLabel;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is xs:anyAtomicType.
     *
     * @return the primitive type
     */

    public abstract BuiltInAtomicType getPrimitiveType();

    /**
     * Determine the static cardinality
     *
     * @return code identifying the cardinality
     * @see org.orbeon.saxon.value.Cardinality
     */

    public final int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Convert the value to a given type. The result of the conversion will be an
     * atomic value of the required type. This method works only where the target
     * type is a built-in type.
     *
     * @param schemaType the required atomic type
     * @param context the XPath dynamic context
     * @return the result of the conversion, if conversion was possible. This
     *         will always be an instance of the class corresponding to the type
     *         of value requested
     * @throws XPathException if conversion is not allowed for this
     *                        required type, or if the particular value cannot be converted
     */

    public final AtomicValue convert(AtomicType schemaType, XPathContext context) throws XPathException {
        // Note this method is used from XQuery compiled code
        return convert(schemaType, true, context).asAtomic();
    }

    /**
     * Convert a value to either (a) another primitive type, or (b) another built-in type derived
     * from the current primitive type, with control over how validation is
     * handled.
     *
     * @param requiredType the required atomic type. This must either be a primitive type, or a built-in
     *                     type derived from the same primitive type as this atomic value.
     * @param validate     true if validation is required. If set to false, the caller guarantees that
     *                     the value is valid for the target data type, and that further validation
     *                     is therefore not required.
     *                     Note that a validation failure may be reported even if validation was not requested.
     * @param context      The conversion context to be used. This is required at present only when converting to
     *                     xs:Name or similar types: it determines the NameChecker to be used.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     *         will be a ValidationFailure. The caller must check for this condition. No exception is thrown, instead
     *         the exception information will be encapsulated within the ValidationFailure.
     */
    protected abstract ConversionResult convertPrimitive(
            BuiltInAtomicType requiredType, boolean validate, XPathContext context);

    /**
     * Convert the value to a given type. The result of the conversion will be
     * an atomic value of the required type. This method works where the target
     * type is a built-in atomic type and also where it is a user-defined atomic
     * type.
     *
     * @param targetType the type to which the value is to be converted
     * @param validate   true if validation is required, false if the caller already knows that the
     *                   value is valid
     * @param context    provides access to conversion context
     * @return the value after conversion if successful; or a {@link ValidationFailure} if conversion failed. The
     *         caller must check for this condition. Validation may fail even if validation was not requested.
     */

    public final ConversionResult convert(AtomicType targetType, boolean validate, XPathContext context) {
        if (targetType.isPrimitiveType()) {
            return convertPrimitive((BuiltInAtomicType)targetType, validate, context);
        } else if (targetType.isAbstract()) {
            return new ValidationFailure("Cannot convert to an abstract type");
        } else if (targetType.isExternalType()) {
            return new ValidationFailure("Cannot convert to an external type");
        } else {
            BuiltInAtomicType primitiveType = (BuiltInAtomicType)targetType.getPrimitiveItemType();
            ConversionResult cr = convertPrimitive(primitiveType, validate, context);
            if (cr instanceof ValidationFailure) {
                return cr;
            }
            CharSequence lexicalValue;
            if (primitiveType.getFingerprint() == StandardNames.XS_STRING) {
                int whitespaceAction = targetType.getWhitespaceAction(
                        context == null ? null : context.getConfiguration().getTypeHierarchy());
                CharSequence cs = Whitespace.applyWhitespaceNormalization(
                        whitespaceAction, ((StringValue)cr).getStringValueCS());
                cr = new StringValue(cs);
                lexicalValue = cs;
            } else {
                lexicalValue = getCanonicalLexicalRepresentation();
            }
            ValidationFailure vf = targetType.validate((AtomicValue)cr, lexicalValue,
                    (context == null ? Name10Checker.getInstance() : context.getConfiguration().getNameChecker()));
            if (vf != null) {
                return vf;
            }
            return ((AtomicValue)cr).copyAsSubType(targetType);
        }
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     * @return the copied value
     */

    public abstract AtomicValue copyAsSubType(AtomicType typeLabel);

    /**
     * Test whether the value is the double/float value NaN
     * @return true if the value is float NaN or double NaN; otherwise false
     */

    public boolean isNaN() {
        return false;
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
     * Iterate over the (single) item in the sequence
     *
     * @return a SequenceIterator that iterates over the single item in this
     *         value
     */

    public final SequenceIterator iterate() {
        return SingletonIterator.makeIterator(this);
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

    public final SequenceIterator getTypedValue() {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Get the effective boolean value of the value
     *
     * @return true, unless the value is boolean false, numeric zero, or
     *         zero-length string
     */
    public boolean effectiveBooleanValue() throws XPathException {
        XPathException err = new XPathException("Effective boolean value is not defined for an atomic value of type " +
                Type.displayTypeName(this));
        err.setIsTypeError(true);
        err.setErrorCode("FORG0006");
        throw err;
        // unless otherwise specified in a subclass
    }

    /**
     * Method to extract components of a value. Implemented by some subclasses,
     * but defined at this level for convenience
     *
     * @param component identifies the required component, as a constant defined in class
     *                  {@link org.orbeon.saxon.functions.Component}, for example {@link org.orbeon.saxon.functions.Component#HOURS}
     * @return the value of the requested component of this value
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
     * @throws org.orbeon.saxon.trans.XPathException
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
                ValidationFailure err = stype.validateContent(
                        getStringValueCS(), null, env.getConfiguration().getNameChecker());
                if (err != null) {
                    throw err.makeException();
                }
                return;
            }
        }
        if (parentType instanceof ComplexType &&
                !((ComplexType)parentType).isSimpleContent() &&
                !((ComplexType)parentType).isMixedContent() &&
                !Whitespace.isWhite(getStringValueCS())) {
            XPathException err = new XPathException("Complex type " + parentType.getDescription() +
                    " does not allow text content " +
                    Err.wrap(getStringValueCS()));
            err.setIsTypeError(true);
            throw err;
        }
    }


    /**
     * Calling this method on a ConversionResult returns the AtomicValue that results
     * from the conversion if the conversion was successful, and throws a ValidationException
     * explaining the conversion error otherwise.
     * <p/>
     * <p>Use this method if you are calling a conversion method that returns a ConversionResult,
     * and if you want to throw an exception if the conversion fails.</p>
     *
     * @return the atomic value that results from the conversion if the conversion was successful
     */

    public AtomicValue asAtomic() {
        return this;
    }


    /**
     * Get a subsequence of the value
     *
     * @param start  the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. If min is
     */

    public GroundedValue subsequence(int start, int length) {
        if (start <= 0 && start + length > 0) {
            return this;
        } else {
            return EmptySequence.getInstance();
        }
    }

    /**
     * Get string value. In general toString() for an atomic value displays the value as it would be
     * written in XPath: that is, as a literal if available, or as a call on a constructor function
     * otherwise.
     */

    public String toString() {
        return typeLabel.toString() + " (\"" + getStringValueCS() + "\")";
    }


    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target  the required target class
     * @param context the XPath dynamic evaluation context
     * @return the (boxed) Java object that best represents the XPath value
     */

//    public final Object convertToJava(Class target, XPathContext context) throws XPathException {
//        return convertAtomicToJava(target, context);
//    }

    /**
     * Convert an atomic value to a Java object. Abstract method implemented by all subclasses
     * @param target  the required target class
     * @param context the XPath dynamic evaluation context
     * @return the (boxed) Java object that best represents the XPath value
     */

    //public abstract Object convertAtomicToJava(Class target, XPathContext context) throws XPathException;
    // TODO: delete this method and its implementations
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

