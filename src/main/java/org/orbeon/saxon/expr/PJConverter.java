package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.ExternalObjectType;
import org.orbeon.saxon.value.*;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This class together with its embedded subclasses handles conversion from XPath values
 * to Java values
 */
public abstract class PJConverter implements Serializable {

    private static HashMap jpmap = new HashMap();

    static {
        jpmap.put(boolean.class, SequenceType.SINGLE_BOOLEAN);
        jpmap.put(Boolean.class, SequenceType.OPTIONAL_BOOLEAN);
        jpmap.put(String.class, SequenceType.OPTIONAL_STRING);
        jpmap.put(CharSequence.class, SequenceType.OPTIONAL_STRING);
        // Mappings for long and int are chosen to avoid static type errors when
        // a Java method expecting long or int is called with an integer literal
        jpmap.put(long.class, SequenceType.SINGLE_INTEGER);
        jpmap.put(Long.class, SequenceType.OPTIONAL_INTEGER);
        jpmap.put(int.class, SequenceType.SINGLE_INTEGER);
        jpmap.put(Integer.class, SequenceType.OPTIONAL_INTEGER);
        jpmap.put(short.class, SequenceType.SINGLE_SHORT);
        jpmap.put(Short.class, SequenceType.OPTIONAL_SHORT);
        jpmap.put(byte.class, SequenceType.SINGLE_BYTE);
        jpmap.put(Byte.class, SequenceType.OPTIONAL_BYTE);
        jpmap.put(float.class, SequenceType.SINGLE_FLOAT);
        jpmap.put(Float.class, SequenceType.OPTIONAL_FLOAT);
        jpmap.put(double.class, SequenceType.SINGLE_DOUBLE);
        jpmap.put(Double.class, SequenceType.OPTIONAL_DOUBLE);
        jpmap.put(URI.class, SequenceType.OPTIONAL_ANY_URI);
        jpmap.put(URL.class, SequenceType.OPTIONAL_ANY_URI);
        jpmap.put(BigInteger.class, SequenceType.OPTIONAL_INTEGER);
        jpmap.put(BigDecimal.class, SequenceType.OPTIONAL_DECIMAL);
    }



    /**
     * Get the nearest XPath equivalent to a Java class. A function call will
     * be type-checked against an XPath function signature in which the Java classes
     * are replaced by their nearest equivalent XPath types
     * @param javaClass a Java class
     * @return the nearest equivalent XPath SequenceType
     */

    public static SequenceType getEquivalentItemType(Class javaClass) {
        return (SequenceType)jpmap.get(javaClass);
    }

    /**
     * Convert an XPath value to a Java value of a specified class
     * @param value the supplied XPath value
     * @param targetClass the class of the required Java value
     * @param context the XPath dynamic context
     * @return the corresponding Java value, which is guaranteed to be an instance of the
     * target class (except that an empty sequence is converted to null)
     * @throws XPathException if the conversion is not possible or fails
     */

    public abstract Object convert(ValueRepresentation value, Class targetClass, XPathContext context)
            throws XPathException;

    /**
     * Generate Java code to implement the type conversion
     * @param var the name of a variable whose value will be the XPath ValueRepresentation
     * to be converted
     * @param targetClass the required class of the Java value
     *@param compiler provides supporting services by callback @return the text of a Java expression whose result will be a Java object/value of the
     * required type
     */

    public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
        throw new UnsupportedOperationException(
                "Cannot generate Java code to support argument conversion for " + getClass());
    }

    /**
     * Factory method to instantiate a converter from a given XPath type to a given Java class
     * @param config the Saxon Configuration
     * @param itemType the item type of the XPath value to be converted
     * @param cardinality the cardinality of the XPath value to be converted
     * @param targetClass the Java class required for the conversion result
     * @return a suitable converter
     */

    public static PJConverter allocate(Configuration config, ItemType itemType,
                                       int cardinality, Class targetClass)
    throws XPathException {
        TypeHierarchy th = config.getTypeHierarchy();
        if (targetClass == SequenceIterator.class) {
            return ToSequenceIterator.INSTANCE;
        }
        if (targetClass == ValueRepresentation.class || targetClass == Item.class) {
            return Identity.INSTANCE;
        }
        if (targetClass == Value.class | targetClass == SequenceExtent.class) {
            return ToSequenceExtent.INSTANCE;
        }

        if (!itemType.isAtomicType()) {
            List externalObjectModels = config.getExternalObjectModels();
            for (int m=0; m<externalObjectModels.size(); m++) {
                ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
                PJConverter converter = model.getPJConverter(targetClass);
                if (converter != null) {
                    return converter;
                }
            }

            if (NodeInfo.class.isAssignableFrom(targetClass)) {
                return Identity.INSTANCE;
            }
        }

        if (Collection.class.isAssignableFrom(targetClass)) {
            return ToCollection.INSTANCE;
        }
        if (targetClass.isArray()) {
            PJConverter itemConverter =
                    allocate(config, itemType, StaticProperty.EXACTLY_ONE, targetClass.getComponentType());
            return new ToArray(itemConverter);
        }
        if (!Cardinality.allowsMany(cardinality)) {
            if (itemType.isAtomicType()) {
                if (th.isSubType(itemType, BuiltInAtomicType.STRING)) {
                    if (targetClass == Object.class || targetClass == String.class || targetClass == CharSequence.class) {
                        return StringValueToString.INSTANCE;
                    } else if (targetClass.isAssignableFrom(StringValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == char.class || targetClass == Character.class) {
                        return StringValueToChar.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (itemType == BuiltInAtomicType.UNTYPED_ATOMIC) {
                    if (targetClass == Object.class || targetClass == String.class || targetClass == CharSequence.class) {
                        return StringValueToString.INSTANCE;
                    } else if (targetClass.isAssignableFrom(UntypedAtomicValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.BOOLEAN)) {
                    if (targetClass == Object.class || targetClass == Boolean.class || targetClass == boolean.class) {
                        return BooleanValueToBoolean.INSTANCE;
                    } else if (targetClass.isAssignableFrom(BooleanValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.INTEGER)) {
                    if (targetClass == Object.class || targetClass == BigInteger.class) {
                        return IntegerValueToBigInteger.INSTANCE;
                    } else if (targetClass == long.class || targetClass == Long.class) {
                        return IntegerValueToLong.INSTANCE;
                    } else if (targetClass == int.class || targetClass == Integer.class) {
                        return IntegerValueToInt.INSTANCE;
                    } else if (targetClass == short.class || targetClass == Short.class) {
                        return IntegerValueToShort.INSTANCE;
                    } else if (targetClass == byte.class || targetClass == Byte.class) {
                        return IntegerValueToByte.INSTANCE;
                    } else if (targetClass == char.class || targetClass == Character.class) {
                        return IntegerValueToChar.INSTANCE;
                    } else if (targetClass == double.class || targetClass == Double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass == float.class || targetClass == Float.class) {
                        return NumericValueToFloat.INSTANCE;
                    } else if (targetClass == BigDecimal.class) {
                        return NumericValueToBigDecimal.INSTANCE;
                    } else if (targetClass.isAssignableFrom(IntegerValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DECIMAL)) {
                    if (targetClass == Object.class || targetClass == BigDecimal.class) {
                        return NumericValueToBigDecimal.INSTANCE;
                    } else if (targetClass == double.class || targetClass == Double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass == float.class || targetClass == Float.class) {
                        return NumericValueToFloat.INSTANCE;
                    } else if (targetClass.isAssignableFrom(DecimalValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.FLOAT)) {
                    if (targetClass == Object.class || targetClass == Float.class || targetClass == float.class) {
                        return NumericValueToFloat.INSTANCE;
                    } else if (targetClass == double.class || targetClass == Double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass.isAssignableFrom(FloatValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DOUBLE)) {
                    if (targetClass == Object.class || targetClass == Double.class || targetClass == double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass.isAssignableFrom(DoubleValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        return Atomic.INSTANCE;
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.ANY_URI)) {
                    if (targetClass == Object.class || URI.class.isAssignableFrom(targetClass)) {
                        return AnyURIValueToURI.INSTANCE;
                    } else if (URL.class.isAssignableFrom(targetClass)) {
                        return AnyURIValueToURL.INSTANCE;
                    } else if (targetClass == String.class || targetClass == CharSequence.class) {
                        return StringValueToString.INSTANCE;
                    } else if (targetClass.isAssignableFrom(AnyURIValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.QNAME)) {
                    if (targetClass == Object.class || targetClass.getName().equals("javax.xml.namespace.QName")) {
                        // TODO:JDK1.5: change the above to use the class name literally
                        return QualifiedNameValueToQName.INSTANCE;
                    } else if (targetClass.isAssignableFrom(QNameValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.NOTATION)) {
                    if (targetClass == Object.class || targetClass.getName().equals("javax.xml.namespace.QName")) {
                        // TODO:JDK1.5: change the above to use the class name literally
                        return QualifiedNameValueToQName.INSTANCE;
                    } else if (targetClass.isAssignableFrom(NotationValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DURATION)) {
                    if (targetClass.isAssignableFrom(DurationValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DATE_TIME)) {
                    if (targetClass.isAssignableFrom(DateTimeValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == java.util.Date.class) {
                        return CalendarValueToDate.INSTANCE;
                    } else if (targetClass == java.util.Calendar.class) {
                        return CalendarValueToCalendar.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DATE)) {
                    if (targetClass.isAssignableFrom(DateValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == java.util.Date.class) {
                        return CalendarValueToDate.INSTANCE;
                    } else if (targetClass == java.util.Calendar.class) {
                        return CalendarValueToCalendar.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.TIME)) {
                    if (targetClass.isAssignableFrom(TimeValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == java.util.Date.class) {
                        return CalendarValueToDate.INSTANCE;
                    } else if (targetClass == java.util.Calendar.class) {
                        return CalendarValueToCalendar.INSTANCE;                          
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_YEAR)) {
                    if (targetClass.isAssignableFrom(GYearValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_YEAR_MONTH)) {
                    if (targetClass.isAssignableFrom(GYearMonthValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_MONTH)) {
                    if (targetClass.isAssignableFrom(GMonthValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_MONTH_DAY)) {
                    if (targetClass.isAssignableFrom(GMonthDayValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_DAY)) {
                    if (targetClass.isAssignableFrom(GDayValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.BASE64_BINARY)) {
                    if (targetClass.isAssignableFrom(Base64BinaryValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.HEX_BINARY)) {
                    if (targetClass.isAssignableFrom(HexBinaryValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (itemType instanceof ExternalObjectType) {
                    return UnwrapExternalObject.INSTANCE;
                } else {
                    return Atomic.INSTANCE;
                }

            } else if (itemType instanceof EmptySequenceTest) {
                return ToNull.INSTANCE;

            } else if (itemType instanceof NodeTest) {
                if (NodeInfo.class.isAssignableFrom(targetClass)) {
                    return Identity.INSTANCE;
                } else {
                    return General.INSTANCE;
                }

            } else {
                // ItemType is item()
                return General.INSTANCE;
            }
        } else {
            // Cardinality allows many (but target type is not a collection)
            return General.INSTANCE;
        }
    }

    private static XPathException cannotConvert(ItemType source, Class target, Configuration config) {
        return new XPathException("Cannot convert from " + source.toString(config.getNamePool()) +
            " to " + target.getName());
    }

    /**
     * Static method to get a converter from an XPath sequence of nodes to the representation of a NodeList
     * in an external object model (this is really a special for DOM, which uses NodeList rather than general
     * purpose Java collection classes)
     */

    public static PJConverter allocateNodeListCreator(Configuration config, Object node) {
        List externalObjectModels = config.getExternalObjectModels();
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            PJConverter converter = model.getNodeListCreator(node);
            if (converter != null) {
                return converter;
            }
        }
        return ToCollection.INSTANCE;
    }

    public static class ToSequenceIterator extends PJConverter {

        public static ToSequenceIterator INSTANCE = new ToSequenceIterator();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return Value.asIterator(value);
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "Value.asIterator(" + var + ")";
        }
    }

    public static class ToNull extends PJConverter {

        public static ToNull INSTANCE = new ToNull();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return null;
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "null";
        }
    }

    public static class ToSequenceExtent extends PJConverter {

        public static ToSequenceExtent INSTANCE = new ToSequenceExtent();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return SequenceExtent.makeSequenceExtent(Value.asIterator(value));
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "SequenceExtent.makeSequenceExtent(Value.asIterator(" + var + "))";
        }
    }

    /**
     * Converter for use when the target class is a collection class. Also used when the target
     * class is Object
     */

    public static class ToCollection extends PJConverter {

        public static ToCollection INSTANCE = new ToCollection();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            Collection list;
            if (targetClass.isAssignableFrom(ArrayList.class)) {
                list = new ArrayList(100);
            } else {
                try {
                    list = (Collection)targetClass.newInstance();
                } catch (InstantiationException e) {
                    XPathException de = new XPathException("Cannot instantiate collection class " + targetClass);
                    de.setXPathContext(context);
                    throw de;
                } catch (IllegalAccessException e) {
                    XPathException de = new XPathException("Cannot access collection class " + targetClass);
                    de.setXPathContext(context);
                    throw de;
                }
            }
            Configuration config = context.getConfiguration();
            TypeHierarchy th = config.getTypeHierarchy();
            SequenceIterator iter = Value.asIterator(value);
            while (true) {
                Item it = iter.next();
                if (it == null) {
                    return list;
                }
                if (it instanceof AtomicValue) {
                    PJConverter pj = allocate(
                            config, ((AtomicValue)it).getItemType(th), StaticProperty.EXACTLY_ONE, Object.class);
                    list.add(pj.convert(it, Object.class, context));
                    //list.add(((AtomicValue)it).convertToJava(Object.class, context));
                } else if (it instanceof VirtualNode) {
                    list.add(((VirtualNode)it).getUnderlyingNode());
                } else {
                    list.add(it);
                }
            }
            //return Value.asValue(value).convertToJavaList(list, context);
        }

        // TODO: compile() method
    }

    /**
     * Converter for use when the target class is an array
     */

    public static class ToArray extends PJConverter {

        private PJConverter itemConverter;

        public ToArray(PJConverter itemConverter) {
            this.itemConverter = itemConverter;
        }

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            Class componentClass = targetClass.getComponentType();
            List list = new ArrayList(20);
            SequenceIterator iter = Value.asIterator(value);
            while (true) {
                Item item = iter.next();
                if (item == null) break;
                Object obj = itemConverter.convert(item, componentClass, context);
                if (obj != null) {
                    list.add(obj);
                }
            }
            Object array = Array.newInstance(componentClass, list.size());
            for (int i=0; i<list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
            //return list.toArray((Object[])array);
        }

        // TODO: compile() method
    }

    public static class Identity extends PJConverter {

        public static Identity INSTANCE = new Identity();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            if (value instanceof SingletonNode) {
                value = ((SingletonNode)value).getNode();
            }
            if (value instanceof VirtualNode) {
                Object obj = ((VirtualNode)value).getUnderlyingNode();
                if (targetClass.isAssignableFrom(obj.getClass())) {
                    return obj;
                }
            }
            if (targetClass.isAssignableFrom(value.getClass())) {
                return value;
            } else {
                ValueRepresentation val = Value.asValue(value).reduce();
                if (val instanceof SingletonNode) {
                    val = ((SingletonNode)val).getNode();
                }
                if (targetClass.isAssignableFrom(val.getClass())) {
                    return val;
                } else if (val instanceof EmptySequence) {
                    return null;
                } else {
                    throw new XPathException("Cannot convert value " + val.getClass() + " of type " +
                            Value.asValue(value).getItemType(context.getConfiguration().getTypeHierarchy()) +
                            " to class " + targetClass.getName());
                }
            }
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "(" + targetClass.getName() + ")" + var;
        }

    }

    public static class UnwrapExternalObject extends PJConverter {

        public static UnwrapExternalObject INSTANCE = new UnwrapExternalObject();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            Value val = Value.asValue(value).reduce();
            if (!(val instanceof ObjectValue)) {
                throw new XPathException("Expected external object of class " + targetClass +
                        ", got " + val.getClass());
            }
            Object obj = ((ObjectValue)val).getObject();
            if (!targetClass.isAssignableFrom(obj.getClass())) {
                throw new XPathException("External object has wrong class (is "
                        + obj.getClass() + ", expected " + targetClass);
            }
            return obj;
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "(" + targetClass.getName() + ")" +
                    compiler.cast(var, ObjectValue.class) + ".getObject()";
        }
    }

    public static class StringValueToString extends PJConverter {

        public static StringValueToString INSTANCE = new StringValueToString();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return value.getStringValue();
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, StringValue.class) + ".getStringValue()";
        }
    }

    public static class StringValueToChar extends PJConverter {

        public static StringValueToChar INSTANCE = new StringValueToChar();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            String str = value.getStringValue();
            if (str.length() == 1) {
                return new Character(str.charAt(0));
            } else {
                XPathException de = new XPathException("Cannot convert xs:string to Java char unless length is 1");
                de.setXPathContext(context);
                de.setErrorCode(SaxonErrorCode.SXJE0005);
                throw de;
            }
        }

       public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, StringValue.class) + ".getStringValue().charAt(0)";
        }
    }


    public static class BooleanValueToBoolean extends PJConverter {

        public static BooleanValueToBoolean INSTANCE = new BooleanValueToBoolean();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return Boolean.valueOf(((BooleanValue)value).getBooleanValue());
        }

       public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, BooleanValue.class) + ".getBooleanValue()";
        }
    }

    public static class IntegerValueToBigInteger extends PJConverter {

        public static IntegerValueToBigInteger INSTANCE = new IntegerValueToBigInteger();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return ((IntegerValue)value).asBigInteger();
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, IntegerValue.class) + ".asBigInteger()";
        }
    }

    public static class IntegerValueToLong extends PJConverter {

        public static IntegerValueToLong INSTANCE = new IntegerValueToLong();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return new Long(((IntegerValue)value).longValue());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, IntegerValue.class) + ".longValue()";
        }
    }

    public static class IntegerValueToInt extends PJConverter {

        public static IntegerValueToInt INSTANCE = new IntegerValueToInt();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return new Integer((int)((IntegerValue)value).longValue());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "(int)" + compiler.cast(var, IntegerValue.class) + ".longValue()";
        }
    }

     public static class IntegerValueToShort extends PJConverter {

        public static IntegerValueToShort INSTANCE = new IntegerValueToShort();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return new Short((short)((IntegerValue)value).longValue());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "(short)" + compiler.cast(var, IntegerValue.class) + ".longValue()";
        }
    }

    public static class IntegerValueToByte extends PJConverter {

        public static IntegerValueToByte INSTANCE = new IntegerValueToByte();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return new Byte((byte)((IntegerValue)value).longValue());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "(byte)" + compiler.cast(var, IntegerValue.class) + ".longValue()";
        }
    }

    public static class IntegerValueToChar extends PJConverter {

        public static IntegerValueToChar INSTANCE = new IntegerValueToChar();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return new Character((char)((IntegerValue)value).longValue());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "(char)" + compiler.cast(var, IntegerValue.class) + ".longValue()";
        }
    }

    public static class NumericValueToBigDecimal extends PJConverter {

        public static NumericValueToBigDecimal INSTANCE = new NumericValueToBigDecimal();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return ((NumericValue)value).getDecimalValue();
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, NumericValue.class) + ".getDecimalValue()";
        }
    }

    public static class NumericValueToDouble extends PJConverter {

        public static NumericValueToDouble INSTANCE = new NumericValueToDouble();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) {
            return new Double(((NumericValue)value).getDoubleValue());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, NumericValue.class) + ".getDoubleValue()";
        }
    }

    public static class NumericValueToFloat extends PJConverter {

        public static NumericValueToFloat INSTANCE = new NumericValueToFloat();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) {
            return new Float(((NumericValue)value).getFloatValue());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, NumericValue.class) + ".getFloatValue()";
        }
    }

    public static class AnyURIValueToURI extends PJConverter {

        public static AnyURIValueToURI INSTANCE = new AnyURIValueToURI();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            try {
                return new URI(value.getStringValue());
            } catch (URISyntaxException err) {
                throw new XPathException("The anyURI value '" + value + "' is not an acceptable Java URI");
            }
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "new java.net.URI(" + var + ")";
        }
    }

    public static class AnyURIValueToURL extends PJConverter {

        public static AnyURIValueToURL INSTANCE = new AnyURIValueToURL();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            try {
                return new URL(value.getStringValue());
            } catch (MalformedURLException err) {
                throw new XPathException("The anyURI value '" + value + "' is not an acceptable Java URL");
            }
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return "new java.net.URL(" + var + ".getStringValue())";
        }
    }

    public static class QualifiedNameValueToQName extends PJConverter {

        public static QualifiedNameValueToQName INSTANCE = new QualifiedNameValueToQName();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return ((QualifiedNameValue)value).makeQName(context.getConfiguration());
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, QualifiedNameValue.class) + ".makeQName(config)";
        }
    }

    public static class CalendarValueToDate extends PJConverter {

        public static CalendarValueToDate INSTANCE = new CalendarValueToDate();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return ((CalendarValue)value).getCalendar().getTime();
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, CalendarValue.class) + ".getCalendar().getTime()";
        }
    }

    public static class CalendarValueToCalendar extends PJConverter {

        public static CalendarValueToCalendar INSTANCE = new CalendarValueToCalendar();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            return ((CalendarValue)value).getCalendar();
        }

        public String compile(String var, Class targetClass, CodeGeneratorService compiler) {
            return compiler.cast(var, CalendarValue.class) + ".getCalendar()";
        }
    }


    /**
     * Converter for use when the source object is an atomic value, but nothing more is known
     * statically.
     */

    public static class Atomic extends PJConverter {

        public static Atomic INSTANCE = new Atomic();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
            // TODO: not really worth separating from General
            AtomicValue item = (AtomicValue)Value.asValue(value);
            if (item == null) {
                return null;
            }
            Configuration config = context.getConfiguration();
            PJConverter converter = allocate(
                    config, item.getItemType(config.getTypeHierarchy()), StaticProperty.EXACTLY_ONE, targetClass);
            return converter.convert(item, targetClass, context);
            //return ((AtomicValue)value).convertAtomicToJava(targetClass, context);
        }
        // TODO compile() method
    }

    /**
     * General-purpose converter when nothing more specific is available.
     * (Provided largely as a transition aid)
     */

    public static class General extends PJConverter {

        public static General INSTANCE = new General();

        public Object convert(ValueRepresentation value, Class targetClass, XPathContext context)
                throws XPathException {
            Value val = Value.asValue(value).reduce();
            Configuration config = context.getConfiguration();
            PJConverter converter = allocate(
                    config, val.getItemType(config.getTypeHierarchy()), val.getCardinality(), targetClass);
            if (converter instanceof General) {
                converter = Identity.INSTANCE;
//                throw new XPathException("Cannot convert value of type " +
//                                val.getItemType(config.getTypeHierarchy()) + " to class " + targetClass);
            }
            return converter.convert(val, targetClass, context);
            //return Value.asValue(value).convertToJava(targetClass, context);
        }
        // TODO: compile() method
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

