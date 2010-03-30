package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.ExternalObjectType;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.Configuration;

import javax.xml.transform.Source;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * This class together with its embedded subclasses handles conversion from Java values to XPath values.
 *
 * The general principle is to allocate a specific JPConverter at compile time wherever possible. If there
 * is insufficient type information to make this feasible, a general-purpose JPConverter is allocated, which
 * in turn allocates a more specific converter at run-time to do the actual work.
 */

public abstract class JPConverter implements Serializable {

    private static HashMap map = new HashMap();

    static {
        map.put(SequenceIterator.class, new FromSequenceIterator());
        map.put(ValueRepresentation.class, FromValueRepresentation.INSTANCE);
        map.put(String.class, FromString.INSTANCE);
        map.put(Boolean.class, FromBoolean.INSTANCE);
        map.put(boolean.class, FromBoolean.INSTANCE);
        map.put(Double.class, FromDouble.INSTANCE);
        map.put(double.class, FromDouble.INSTANCE);
        map.put(Float.class, FromFloat.INSTANCE);
        map.put(float.class, FromFloat.INSTANCE);
        map.put(BigDecimal.class, FromBigDecimal.INSTANCE);
        map.put(BigInteger.class, FromBigInteger.INSTANCE);
        map.put(Long.class, FromLong.INSTANCE);
        map.put(long.class, FromLong.INSTANCE);
        map.put(Integer.class, FromInt.INSTANCE);
        map.put(int.class, FromInt.INSTANCE);
        map.put(Short.class, FromShort.INSTANCE);
        map.put(short.class, FromShort.INSTANCE);
        map.put(Byte.class, FromByte.INSTANCE);
        map.put(byte.class, FromByte.INSTANCE);
        map.put(Character.class, FromCharacter.INSTANCE);
        map.put(char.class, FromCharacter.INSTANCE);
        //map.put(QName.class, new FromQName());
        map.put(URI.class, FromURI.INSTANCE);
        map.put(URL.class, FromURI.INSTANCE);
        map.put(Date.class, FromDate.INSTANCE);
        //map.put(Source.class, FromSource.INSTANCE);
        map.put(long[].class, FromLongArray.INSTANCE);
        map.put(int[].class, FromIntArray.INSTANCE);
        map.put(short[].class, FromShortArray.INSTANCE);
        map.put(byte[].class, FromByteArray.INSTANCE);
        map.put(char[].class, FromCharArray.INSTANCE);
        map.put(double[].class, FromDoubleArray.INSTANCE);
        map.put(float[].class, FromFloatArray.INSTANCE);
        map.put(boolean[].class, FromBooleanArray.INSTANCE);
        map.put(Collection.class, FromCollection.INSTANCE);
        //map.put(Object.class, FromExternalObject.INSTANCE);
    }

    public static JPConverter allocate(Class javaClass, Configuration config) {
        JPConverter c = (JPConverter)map.get(javaClass);
        if (c != null) {
            return c;
        }

        if (javaClass.getName().equals("javax.xml.namespace.QName")) {
            return FromQName.INSTANCE;
        }

        if (NodeInfo.class.isAssignableFrom(javaClass)) {
            return new FromValueRepresentation(AnyNodeTest.getInstance(), StaticProperty.ALLOWS_ZERO_OR_ONE);
        }

        if (Source.class.isAssignableFrom(javaClass)) {
            return FromSource.INSTANCE;
        }

        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            Class k = (Class)iter.next();
            if (k.isAssignableFrom(javaClass)) {
                return (JPConverter)map.get(k);
            }
        }

        List externalObjectModels = config.getExternalObjectModels();
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            JPConverter converter = model.getJPConverter(javaClass);
            if (converter != null) {
                return converter;
            }
        }

        if (javaClass.isArray()) {
            Class itemClass = javaClass.getComponentType();
            return new FromObjectArray(allocate(itemClass, config));
        }

        return new WrapExternalObject(new ExternalObjectType(javaClass, config));
    }

    /**
     * Convert a Java object to an equivalent XPath value
     * @param object the java object to be converted
     * @param context the XPath dynamic evaluation context
     * @return the XPath value resulting from the conversion
     * @throws XPathException if the conversion is not possible or if it fails
     */

    public abstract ValueRepresentation convert(Object object, XPathContext context) throws XPathException;

    /**
     * Get the item type of the XPath value that will result from the conversion
     * @return the XPath item type
     */

    public abstract ItemType getItemType();

    /**
     * Get the cardinality of the XPath value that will result from the conversion
     * @return the cardinality of the result
     */

    public int getCardinality() {
        // default implementation
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Generate a Java expression (as text) that can be used to implement this conversion when compiling
     * a query
     * @param var the Java variable that will hold the Java value to be converted
     * @param compiler provides callback services
     * @return a Java expression (as text) that implements the conversion. The result of the Java
     * expression must be of type ValueRepresentation, and if the cardinality of the converter is
     * exactly one then it must be of type Item.
     */

    public String compile(String var, CodeGeneratorService compiler) {
        throw new UnsupportedOperationException("Cannot compile converter " + getClass().getName());
    }

    public static class FromSequenceIterator extends JPConverter {
        public static FromSequenceIterator INSTANCE = new FromSequenceIterator();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return SequenceExtent.makeSequenceExtent((SequenceIterator)object);
        }
        public ItemType getItemType() {
            return AnyItemType.getInstance();
        }
        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    public static class FromValueRepresentation extends JPConverter {

        public static FromValueRepresentation INSTANCE =
                new FromValueRepresentation(AnyItemType.getInstance(), StaticProperty.ALLOWS_ZERO_OR_MORE);

        private ItemType resultType;
        private int cardinality;

        public FromValueRepresentation(ItemType resultType, int cardinality) {
            this.resultType = resultType;
            this.cardinality = cardinality;
        }

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return (object instanceof Closure ?
                    Value.asValue(SequenceExtent.makeSequenceExtent(((Closure)object).iterate())) :
                    (ValueRepresentation)object);
        }
        public ItemType getItemType() {
            return resultType;
        }
        public int getCardinality() {
            return cardinality;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return var;
        }
    }

    public static class FromString extends JPConverter {
        public static FromString INSTANCE = new FromString();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new StringValue((String)object);
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.STRING;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "new StringValue(" + var + ")";
        }
    }

    public static class FromBoolean extends JPConverter {
        public static FromBoolean INSTANCE = new FromBoolean();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return BooleanValue.get(((Boolean)object).booleanValue());
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.BOOLEAN;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "BooleanValue.get(" + var + ")";
        }
    }

    public static class FromDouble extends JPConverter {
        public static FromDouble INSTANCE = new FromDouble();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new DoubleValue(((Double)object).doubleValue());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.DOUBLE;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new DoubleValue(" + var + ")";
        }
    }

    public static class FromFloat extends JPConverter {
        public static FromFloat INSTANCE = new FromFloat();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new FloatValue(((Float)object).floatValue());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.FLOAT;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new FloatValue(" + var + ")";
        }
    }

    public static class FromBigDecimal extends JPConverter {
        public static FromBigDecimal INSTANCE = new FromBigDecimal();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new DecimalValue((BigDecimal)object);
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.DECIMAL;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new DecimalValue(" + var + ")";
        }
    }

    public static class FromBigInteger extends JPConverter {
        public static FromBigInteger INSTANCE = new FromBigInteger();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return IntegerValue.makeIntegerValue((BigInteger)object);
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.INTEGER;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "IntegerValue.makeIntegerValue(" + var + ")";
        }
    }

    public static class FromLong extends JPConverter {
        public static FromLong INSTANCE = new FromLong();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new Int64Value(((Long)object).longValue());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.INTEGER;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new Int64Value(" + var + ")";
        }
    }

    public static class FromInt extends JPConverter {
        public static FromInt INSTANCE = new FromInt();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new Int64Value(((Integer)object).intValue());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.INTEGER;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new Int64Value(" + var + ")";
        }
    }

    public static class FromShort extends JPConverter {
        public static FromShort INSTANCE = new FromShort();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new Int64Value(((Short)object).intValue());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.INTEGER;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new Int64Value(" + var + ")";
        }
    }

    public static class FromByte extends JPConverter {
        public static FromByte INSTANCE = new FromByte();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new Int64Value(((Byte)object).intValue());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.INTEGER;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new Int64Value(" + var + ")";
        }
    }

    public static class FromCharacter extends JPConverter {
        public static FromCharacter INSTANCE = new FromCharacter();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new StringValue(object.toString());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.STRING;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new StringValue(" + var + ".toString())";
        }
    }

    public static class FromQName extends JPConverter {
        public static FromQName INSTANCE = new FromQName();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return Value.makeQNameValue(object, context.getConfiguration());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.QNAME;
        }
        // TODO: compile method
    }

    public static class FromURI extends JPConverter {
        // also used for URL
        public static FromURI INSTANCE = new FromURI();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new AnyURIValue(object.toString());
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.ANY_URI;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new AnyURIValue(" + var + ".toString())";
        }
    }

    public static class FromDate extends JPConverter {
        public static FromDate INSTANCE = new FromDate();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return DateTimeValue.fromJavaDate((Date)object);
        }
        public ItemType getItemType() {
            return BuiltInAtomicType.DATE_TIME;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "DateTimeValue.fromJavaDate(" + var + ")";
        }
    }

    public static class WrapExternalObject extends JPConverter {

        public static WrapExternalObject INSTANCE = new WrapExternalObject(BuiltInAtomicType.ANY_ATOMIC);

        private ItemType resultType;

        public WrapExternalObject(ItemType resultType) {
            this.resultType = resultType;
        }

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return new ObjectValue(object);
        }
        public ItemType getItemType() {
            return resultType;
        }
        public String compile(String var, CodeGeneratorService compiler) {
            return "new ObjectValue(" + var + ")";
        }
    }

    public static class FromCollection extends JPConverter {

        public static FromCollection INSTANCE = new FromCollection();
        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((Collection)object).size()];
            int a = 0;
            for (Iterator i=((Collection)object).iterator(); i.hasNext(); ) {
                Object obj = i.next();
                JPConverter itemConverter = allocate(obj.getClass(), context.getConfiguration());
                try {
                    Item item = Value.asItem(itemConverter.convert(obj, context));
                    if (item != null) {
                        array[a++] = item;
                    }
                } catch (XPathException e) {
                    throw new XPathException(
                            "Returned Collection contains an object that cannot be converted to an Item ("
                                    + obj.getClass() + "): " + e.getMessage(),
                            SaxonErrorCode.SXJE0051);
                }
            }
            return new SequenceExtent(array, 0, a);
        }

        public ItemType getItemType() {
            return AnyItemType.getInstance();
        }

        /**
         * Get the cardinality of the XPath value that will result from the conversion
         * @return the cardinality of the result
         */

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
        // TODO: compile method
    }

    public static class FromSource extends JPConverter {

        public static FromSource INSTANCE = new FromSource();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return context.getConfiguration().buildDocument((Source)object);
        }

        public ItemType getItemType() {
            return AnyNodeTest.getInstance();
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return compiler.getContextVariableName() + ".getConfiguration().buildDocument(" + var + ")";
        }
    }

    public static class FromLongArray extends JPConverter {

        public static FromLongArray INSTANCE = new FromLongArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((long[])object).length];
            for (int i = 0; i < array.length; i++){
                array[i] = Int64Value.makeDerived(((long[])object)[i], BuiltInAtomicType.LONG);
            }
            return new SequenceExtent(array);
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.LONG;
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "JPConverter.FromLongArray.INSTANCE.convert("
                    + var + ", " + compiler.getContextVariableName() + ")";
        }
    }

    public static class FromIntArray extends JPConverter {

        public static FromIntArray INSTANCE = new FromIntArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((int[])object).length];
            for (int i = 0; i < array.length; i++){
                array[i] = Int64Value.makeDerived(((int[])object)[i], BuiltInAtomicType.INT);
            }
            return new SequenceExtent(array);
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.INT;
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "JPConverter.FromIntArray.INSTANCE.convert("
                    + var + ", " + compiler.getContextVariableName() + ")";
        }
    }

    public static class FromShortArray extends JPConverter {

        public static FromShortArray INSTANCE = new FromShortArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((short[])object).length];
            for (int i = 0; i < array.length; i++){
                array[i] = Int64Value.makeDerived(((short[])object)[i], BuiltInAtomicType.SHORT);
            }
            return new SequenceExtent(array);
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.SHORT;
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "JPConverter.FromShortArray.INSTANCE.convert("
                    + var + ", " + compiler.getContextVariableName() + ")";
        }
    }

    public static class FromByteArray extends JPConverter {

        public static FromByteArray INSTANCE = new FromByteArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((byte[])object).length];
            for (int i = 0; i < array.length; i++){
                array[i] = Int64Value.makeDerived(255 & (int)((byte[])object)[i], BuiltInAtomicType.BYTE);
            }
            return new SequenceExtent(array);
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.BYTE;
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "JPConverter.FromByteArray.INSTANCE.convert("
                    + var + ", " + compiler.getContextVariableName() + ")";
        }
    }

    public static class FromCharArray extends JPConverter {

        public static FromCharArray INSTANCE = new FromCharArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            return StringValue.makeStringValue(new String((char[])object));
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.STRING;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "StringValue.makeStringValue(new String((char[])" + var + "))";
        }
    }

    public static class FromDoubleArray extends JPConverter {

        public static FromDoubleArray INSTANCE = new FromDoubleArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((double[])object).length];
            for (int i = 0; i < array.length; i++){
                array[i] = new DoubleValue(((double[])object)[i]);
            }
            return new SequenceExtent(array);
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.DOUBLE;
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "JPConverter.FromDoubleArray.INSTANCE.convert("
                    + var + ", " + compiler.getContextVariableName() + ")";
        }
    }

    public static class FromFloatArray extends JPConverter {

        public static FromFloatArray INSTANCE = new FromFloatArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((float[])object).length];
            for (int i = 0; i < array.length; i++){
                array[i] = new DoubleValue(((float[])object)[i]);
            }
            return new SequenceExtent(array);
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.FLOAT;
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "JPConverter.FromFloatArray.INSTANCE.convert("
                    + var + ", " + compiler.getContextVariableName() + ")";
        }
    }

    public static class FromBooleanArray extends JPConverter {

        public static FromBooleanArray INSTANCE = new FromBooleanArray();

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Item[] array = new Item[((boolean[])object).length];
            for (int i = 0; i < array.length; i++){
                array[i] = BooleanValue.get(((boolean[])object)[i]);
            }
            return new SequenceExtent(array);
        }

        public ItemType getItemType() {
            return BuiltInAtomicType.BOOLEAN;
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        public String compile(String var, CodeGeneratorService compiler) {
            return "JPConverter.FromBooleanArray.INSTANCE.convert("
                    + var + ", " + compiler.getContextVariableName() + ")";
        }
    }

    public static class FromObjectArray extends JPConverter {

        private JPConverter itemConverter;

        public FromObjectArray(JPConverter itemConverter) {
            this.itemConverter = itemConverter;
        }

        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
            Object[] arrayObject = (Object[])object;
            Item[] newArray = new Item[arrayObject.length];
            int a = 0;
            for (int i = 0; i < arrayObject.length; i++){
                try {
                    Item newItem = Value.asItem(itemConverter.convert(arrayObject[i], context));
                    if (newItem != null) {
                        newArray[a++] = newItem;
                    }
                } catch (XPathException e) {
                    throw new XPathException(
                         "Returned array contains an object that cannot be converted to an Item (" +
                                arrayObject[i].getClass() + "): " + e.getMessage(),
                         SaxonErrorCode.SXJE0051);
                }
            }
            return new SequenceExtent(newArray, 0, a);
        }

        public ItemType getItemType() {
            return itemConverter.getItemType();
        }

        public int getCardinality() {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }

        // TODO: compile method       
    }


//    public static class General extends JPConverter {
//        public static General INSTANCE = new General();
//        public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
//            // fall back to old code
//            return Value.convertToBestFit(object, context.getConfiguration());
//        }
//        public ItemType getItemType() {
//            return AnyItemType.getInstance();
//        }
//    }



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

