package net.sf.saxon.type;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.style.StandardNames;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This class is used to construct Schema objects containing all the built-in types:
 * that is, the types defined in the "xs" and "xdt" namespaces.
 */

public abstract class BuiltInSchemaFactory implements Serializable {

    /**
    * Define the type hierarchy
    */

    private static HashMap lookup = new HashMap();

    static {

        final String XS = NamespaceConstant.SCHEMA;
        final String XDT = NamespaceConstant.XDT;

        SimpleType anySimpleType =
                makeSimpleType(XS, "anySimpleType", null, StandardNames.XS_ANY_SIMPLE_TYPE);
        try {
            anySimpleType.setBaseType(AnyType.getInstance());
        } catch (SchemaException err) {
            throw new AssertionError(err);
        }

        SimpleType anyAtomicType =
                makeSimpleType(XDT, "anyAtomicType", anySimpleType, Type.ATOMIC);
        SimpleType numeric =
                makeSimpleType(XDT, "numeric", anyAtomicType, Type.NUMBER);
        SimpleType string =
                makeSimpleType(XS, "string", anyAtomicType, Type.STRING);
        SimpleType xsboolean =
                makeSimpleType(XS, "boolean", anyAtomicType, Type.BOOLEAN);
        SimpleType duration =
                makeSimpleType(XS, "duration", anyAtomicType, Type.DURATION);
        SimpleType dateTime =
                makeSimpleType(XS, "dateTime", anyAtomicType, Type.DATE_TIME);
        SimpleType date =
                makeSimpleType(XS, "date", anyAtomicType, Type.DATE);
        SimpleType time =
                makeSimpleType(XS, "time", anyAtomicType, Type.TIME);
        SimpleType gYearMonth =
                makeSimpleType(XS, "gYearMonth", anyAtomicType, Type.G_YEAR_MONTH);
        SimpleType gMonth =
                makeSimpleType(XS, "gMonth", anyAtomicType, Type.G_MONTH);
        SimpleType gMonthDay =
                makeSimpleType(XS, "gMonthDay", anyAtomicType, Type.G_MONTH_DAY);
        SimpleType gYear =
                makeSimpleType(XS, "gYear", anyAtomicType, Type.G_YEAR);
        SimpleType gDay =
                makeSimpleType(XS, "gDay", anyAtomicType, Type.G_DAY);
        SimpleType hexBinary =
                makeSimpleType(XS, "hexBinary", anyAtomicType, Type.HEX_BINARY);
        SimpleType base64Binary =
                makeSimpleType(XS, "base64Binary", anyAtomicType, Type.BASE64_BINARY);
        SimpleType anyURI =
                makeSimpleType(XS, "anyURI", anyAtomicType, Type.ANY_URI);
        SimpleType qName =
                makeSimpleType(XS, "QName", anyAtomicType, Type.QNAME);
        SimpleType notation =
                makeSimpleType(XS, "NOTATION", anyAtomicType, Type.NOTATION);

        SimpleType untypedAtomic =
                makeSimpleType(XDT, "untypedAtomic", anyAtomicType, Type.UNTYPED_ATOMIC);

        //SimpleType javaObject =
        //        makeSimpleType(NamespaceConstant.JAVA_TYPE, "java.lang.Object", anyAtomicType, Type.OBJECT);

        SimpleType decimal =
                makeSimpleType(XS, "decimal", numeric, Type.DECIMAL);
        SimpleType xsfloat =
                makeSimpleType(XS, "float", numeric, Type.FLOAT);
        SimpleType xsdouble =
                makeSimpleType(XS, "double", numeric, Type.DOUBLE);

        SimpleType xsinteger =
                makeSimpleType(XS, "integer", decimal, Type.INTEGER);

        SimpleType nonPositiveInteger =
                makeSimpleType(XS, "nonPositiveInteger", xsinteger, Type.NON_POSITIVE_INTEGER);
        SimpleType negativeInteger =
                makeSimpleType(XS, "negativeInteger", nonPositiveInteger, Type.NEGATIVE_INTEGER);
        SimpleType xslong =
                makeSimpleType(XS, "long", xsinteger, Type.LONG);
        SimpleType xsint =
                makeSimpleType(XS, "int", xslong, Type.INT);
        SimpleType xsshort =
                makeSimpleType(XS, "short", xsint, Type.SHORT);
        SimpleType xsbyte =
                makeSimpleType(XS, "byte", xsshort, Type.BYTE);
        SimpleType nonNegativeInteger =
                makeSimpleType(XS, "nonNegativeInteger", xsinteger, Type.NON_NEGATIVE_INTEGER);
        SimpleType positiveInteger =
                makeSimpleType(XS, "positiveInteger", nonNegativeInteger, Type.POSITIVE_INTEGER);
        SimpleType unsignedLong =
                makeSimpleType(XS, "unsignedLong", nonNegativeInteger, Type.UNSIGNED_LONG);
        SimpleType unsignedInt =
                makeSimpleType(XS, "unsignedInt", unsignedLong, Type.UNSIGNED_INT);
        SimpleType unsignedShort =
                makeSimpleType(XS, "unsignedShort", unsignedInt, Type.UNSIGNED_SHORT);
        SimpleType unsignedByte =
                makeSimpleType(XS, "unsignedByte", unsignedShort, Type.UNSIGNED_BYTE);

        SimpleType ymd =
                makeSimpleType(XDT, "yearMonthDuration", duration, Type.YEAR_MONTH_DURATION);
        SimpleType dtd =
                makeSimpleType(XDT, "dayTimeDuration", duration, Type.DAY_TIME_DURATION);

        SimpleType normalizedString =
                makeSimpleType(XS, "normalizedString", string, Type.NORMALIZED_STRING);
        SimpleType token =
                makeSimpleType(XS, "token", normalizedString, Type.TOKEN);
        SimpleType language =
                makeSimpleType(XS, "language", token, Type.LANGUAGE);
        SimpleType name =
                makeSimpleType(XS, "Name", token, Type.NAME);
        SimpleType nmtoken =
                makeSimpleType(XS, "NMTOKEN", token, Type.NMTOKEN);
        SimpleType ncname =
                makeSimpleType(XS, "NCName", name, Type.NCNAME);
        SimpleType id =
                makeSimpleType(XS, "ID", ncname, Type.ID);
        SimpleType idref =
                makeSimpleType(XS, "IDREF", ncname, Type.IDREF);
        SimpleType entity =
                makeSimpleType(XS, "ENTITY", ncname, Type.ENTITY);

        makeListType(XS, "NMTOKENS", nmtoken);
        makeListType(XS, "IDREFS", nmtoken);
        makeListType(XS, "ENTITIES", nmtoken);

        lookup.put(new Integer(StandardNames.XS_ANY_TYPE), AnyType.getInstance());
        lookup.put(new Integer(StandardNames.XDT_UNTYPED), Untyped.getInstance());
    }

    private static SimpleType makeSimpleType(String namespace,
                                             String lname,
                                             SimpleType baseType,
                                             int code) {
        try {
            AtomicType t = new AtomicType();
            t.setIsBuiltIn(true);
            t.setBaseType(baseType);
            t.setDerivationMethodName("restriction");
            t.setFingerprint(StandardNames.getFingerprint(namespace, lname));
            t.setLocalName(lname);
            t.setNamePool(NamePool.getDefaultNamePool());
            lookup.put(new Integer(t.getFingerprint()), t);
            return t;
        } catch (SchemaException err) {
            throw new AssertionError("No exception should be thrown here. " + err.getMessage());
        }
    }

    private static SimpleType makeListType(String namespace,
                                           String lname,
                                           SimpleType itemType) {
        try {

            ListType t = new ListType(NamePool.getDefaultNamePool());
            t.setItemType(itemType);
            t.setBaseType(getSchemaType(StandardNames.XS_ANY_SIMPLE_TYPE));
            t.setDerivationMethodName("list");
            t.setFingerprint(StandardNames.getFingerprint(namespace, lname));
            t.setLocalName(lname);
            lookup.put(new Integer(t.getFingerprint()), t);
            return t;
        } catch (SchemaException err) {
            throw new AssertionError("No exception should be thrown here. " + err.getMessage());
        }
    }

    public static SchemaType getSchemaType(int fingerprint) {
        return (SchemaType)lookup.get(new Integer(fingerprint));
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
