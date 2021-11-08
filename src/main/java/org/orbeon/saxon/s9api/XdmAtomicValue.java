package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ConversionResult;
import org.orbeon.saxon.type.ValidationException;
import org.orbeon.saxon.value.*;

import java.math.BigDecimal;
import java.net.URI;

/**
 * The class XdmAtomicValue represents an item in an XPath 2.0 sequence that is an atomic value.
 * The value may belong to any of the 19 primitive types defined in XML Schema, or to a type
 * derived from these primitive types, or the XPath 2.0 type xs:untypedAtomic. The type may
 * be either a built-in type or a user-defined type.
 *
 * <p>An <code>XdmAtomicValue</code> is immutable.</p>
 */
public class XdmAtomicValue extends XdmItem {

    protected XdmAtomicValue(AtomicValue value) {
        super(value);
    }

    /**
     * Create an <tt>xs:boolean</tt> atomic value
     * @param value the boolean value, true or false
     */

    public XdmAtomicValue(boolean value) {
        super(BooleanValue.get(value));
    }

    /**
     * Create an <tt>xs:integer</tt> atomic value
     * @param value the <tt>xs:integer</tt> value, as a long
     */

    public XdmAtomicValue(long value) {
        super(Int64Value.makeIntegerValue(value));
    }

    /**
     * Create an <tt>xs:decimal</tt> atomic value
     * @param value the <tt>xs:decimal</tt> value, as a BigDecimal
     */

    public XdmAtomicValue(BigDecimal value) {
        super(new DecimalValue(value));
    }

    /**
     * Create an <tt>xs:double</tt> atomic value
     * @param value the <tt>xs:double</tt> value, as a double
     */

    public XdmAtomicValue(double value) {
        super(new DoubleValue(value));
    }

    /**
     * Create an <tt>xs:float</tt> atomic value
     * @param value the <tt>xs:float</tt> value, as a float
     */

    public XdmAtomicValue(float value) {
        super(new FloatValue(value));
    }

    /**
     * Create an <tt>xs:string</tt> atomic value
     * @param value the <tt>xs:string</tt> value, as a string
     */

    public XdmAtomicValue(String value) {
        super(new StringValue(value));
    }

    /**
     * Create an <tt>xs:anyURI</tt> atomic value
     * @param value the <tt>xs:anyURI</tt> value, as a URI
     */

    public XdmAtomicValue(URI value) {
        super(new AnyURIValue(value.toString()));
    }

    /**
     * Create an <tt>xs:QName</tt> atomic value
     * @param value the <tt>xs:QName</tt> value, as a QName
     */

    public XdmAtomicValue(QName value) {
        super(new QNameValue(value.getStructuredQName(), BuiltInAtomicType.QNAME));
    }

    /**
     * Construct an atomic value given its lexical representation and the name of the required
     * built-in atomic type.
     * <p>This method cannot be used to construct values that are namespace-sensitive (QNames and Notations)</p>
     * @param lexicalForm the value in the lexical space of the target data type. More strictly, the input
     * value before the actions of the whitespace facet for the target data type are applied.
     * @param type the required atomic type. This must either be one of the built-in
     * atomic types defined in XML Schema, or a user-defined type whose definition appears
     * in a schema that is known to the Processor. It must not be an abstract type.
     * @throws SaxonApiException if the type is unknown, or is not atomic, or is namespace-sensitive;
     * or if the value supplied in <tt>lexicalForm</tt> is not in the lexical space of the specified atomic
     * type.
     */

    public XdmAtomicValue(String lexicalForm, ItemType type) throws SaxonApiException {
        org.orbeon.saxon.type.ItemType it = type.getUnderlyingItemType();
        if (!it.isAtomicType()) {
            throw new SaxonApiException("Requested type is not atomic");
        }
        if (((AtomicType)it).isAbstract()) {
            throw new SaxonApiException("Requested type is an abstract type");
        }
        Configuration config = type.getProcessor().getUnderlyingConfiguration();
        ConversionResult result = new StringValue(lexicalForm).convert(
                (AtomicType)it, true, config.getConversionContext());
        try {
            setValue(result.asAtomic());
        } catch (ValidationException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get the result of converting the atomic value to a string. This has the same
     * effect as the XPath string() function.
     */

    public String toString() {
        return getStringValue();
    }

    /**
     * Get the primitive type of this atomic value, as a QName. The primitive types for this purpose are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is xs:anyAtomicType.
     * @return a QName naming the primitive type of this atomic value. This will always be an atomic type.
     */

    public QName getPrimitiveTypeName() {
        AtomicValue value = (AtomicValue)getUnderlyingValue();
        BuiltInAtomicType type = value.getPrimitiveType();
        return new QName(type.getQualifiedName());
    }

    /**
     * Get the value as a Java object of the nearest equivalent type.
     *
     * <p>The result type is as follows:</p>
     *
     * <table>
     * <tr><th>XPath type</th>      <th>Java class</th></tr>
     * <tr><td>xs:string</td>       <td>String</td></tr>
     * <tr><td>xs:integer</td>      <td>java.math.BigInteger</td></tr>
     * <tr><td>xs:decimal</td>      <td>java.math.BigDecimal</td></tr>
     * <tr><td>xs:double</td>       <td>Double</td></tr>
     * <tr><td>xs:float</td>        <td>Float</td></tr>
     * <tr><td>xs:boolean</td>      <td>Boolean</td></tr>
     * <tr><td>xs:QName</td>        <td>QName</td></tr>
     * <tr><td>xs:anyURI</td>       <td>String</td></tr>
     * <tr><td>xs:untypedAtomic</td><td>String</td></tr>
     * <tr><td>Other types</td>     <td>currently String, but this may change in the future</td></tr>
     * </table>
     * @return the value, converted to a Java object of a suitable type
     */

    @SuppressWarnings({"AutoBoxing"})
    public Object getValue() {
        AtomicValue av = (AtomicValue)getUnderlyingValue();
        if (av instanceof StringValue) {
            return av.getStringValue();
        } else if (av instanceof IntegerValue) {
            return ((IntegerValue)av).asBigInteger();
        } else if (av instanceof DoubleValue) {
            return ((DoubleValue)av).getDoubleValue();
        } else if (av instanceof FloatValue) {
            return ((FloatValue)av).getFloatValue();
        } else if (av instanceof BooleanValue) {
            return ((BooleanValue)av).getBooleanValue();
        } else if (av instanceof DecimalValue) {
            return ((DecimalValue)av).getDecimalValue();
        } else if (av instanceof QNameValue) {
            QNameValue q = (QNameValue)av;
            return new QName(q.getPrefix(), q.getNamespaceURI(), q.getLocalName());
        } else {
            return av.getStringValue();
        }
    }

    /**
     * Get the value converted to a boolean using the XPath casting rules
     * @return the result of converting to a boolean (Note: this is not the same as the
     * effective boolean value).
     * @throws SaxonApiException if the value cannot be cast to a boolean
     */

    public boolean getBooleanValue() throws SaxonApiException {
        AtomicValue av = (AtomicValue)getUnderlyingValue();
        if (av instanceof BooleanValue) {
            return ((BooleanValue)av).getBooleanValue();
        } else if (av instanceof NumericValue) {
            return !av.isNaN() && ((NumericValue)av).signum() != 0;
        } else if (av instanceof StringValue) {
            String s = ((StringValue)av).getStringValue();
            return "1".equals(s) || "true".equals(s);
        } else {
            throw new SaxonApiException("Cannot cast item to a boolean");
        }
    }

    /**
     * Get the value converted to an integer using the XPath casting rules
     * @return the result of converting to an integer
     * @throws SaxonApiException if the value cannot be cast to an integer
     */

    public long getLongValue() throws SaxonApiException {
        AtomicValue av = (AtomicValue)getUnderlyingValue();
        if (av instanceof BooleanValue) {
            return ((BooleanValue)av).getBooleanValue() ? 0L : 1L;
        } else if (av instanceof NumericValue) {
            try {
                return ((NumericValue)av).longValue();
            } catch (XPathException e) {
                throw new SaxonApiException("Cannot cast item to an integer");
            }
        } else if (av instanceof StringValue) {
            return (long)Value.stringToNumber(av.getStringValueCS());
        } else {
            throw new SaxonApiException("Cannot cast item to an integer");
        }
    }

    /**
     * Get the value converted to a double using the XPath casting rules
     * @return the result of converting to a double
     * @throws SaxonApiException if the value cannot be cast to a double
     */

    public double getDoubleValue() throws SaxonApiException {
        AtomicValue av = (AtomicValue)getUnderlyingValue();
        if (av instanceof BooleanValue) {
            return ((BooleanValue)av).getBooleanValue() ? 0.0 : 1.0;
        } else if (av instanceof NumericValue) {
            return ((NumericValue)av).getDoubleValue();
        } else if (av instanceof StringValue) {
            return Value.stringToNumber(av.getStringValueCS());
        } else {
            throw new SaxonApiException("Cannot cast item to a double");
        }
    }

    /**
     * Get the value converted to a decimal using the XPath casting rules
     * @return the result of converting to a decimal
     * @throws SaxonApiException if the value cannot be cast to a double
     */

    public BigDecimal getDecimalValue() throws SaxonApiException {
        AtomicValue av = (AtomicValue)getUnderlyingValue();
        if (av instanceof BooleanValue) {
            return ((BooleanValue)av).getBooleanValue() ? BigDecimal.ZERO : BigDecimal.ONE;
        } else if (av instanceof NumericValue) {
            try {
                return ((NumericValue)av).getDecimalValue();
            } catch (XPathException e) {
                throw new SaxonApiException("Cannot cast item to a decimal");
            }
        } else if (av instanceof StringValue) {
            return new BigDecimal(av.getStringValueCS().toString());
        } else {
            throw new SaxonApiException("Cannot cast item to a decimal");
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

