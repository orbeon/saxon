package net.sf.saxon.type;
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

    private static HashMap lookup = new HashMap(100);

    private BuiltInSchemaFactory() {
    }

    static {

        final String XS = NamespaceConstant.SCHEMA;
        final String XDT = NamespaceConstant.XDT;

        AnySimpleType anySimpleType = AnySimpleType.getInstance();
        lookup.put(new Integer(StandardNames.XS_ANY_SIMPLE_TYPE), anySimpleType);

        BuiltInAtomicType anyAtomicType =
                makeAtomicType(XDT, "anyAtomicType", anySimpleType);
        BuiltInAtomicType numeric =
                makeAtomicType(XDT, "numeric", anyAtomicType);
        BuiltInAtomicType string =
                makeAtomicType(XS, "string", anyAtomicType);
        BuiltInAtomicType xsboolean =
                makeAtomicType(XS, "boolean", anyAtomicType);
        BuiltInAtomicType duration =
                makeAtomicType(XS, "duration", anyAtomicType);
        BuiltInAtomicType dateTime =
                makeAtomicType(XS, "dateTime", anyAtomicType);
        BuiltInAtomicType date =
                makeAtomicType(XS, "date", anyAtomicType);
        BuiltInAtomicType time =
                makeAtomicType(XS, "time", anyAtomicType);
        BuiltInAtomicType gYearMonth =
                makeAtomicType(XS, "gYearMonth", anyAtomicType);
        BuiltInAtomicType gMonth =
                makeAtomicType(XS, "gMonth", anyAtomicType);
        BuiltInAtomicType gMonthDay =
                makeAtomicType(XS, "gMonthDay", anyAtomicType);
        BuiltInAtomicType gYear =
                makeAtomicType(XS, "gYear", anyAtomicType);
        BuiltInAtomicType gDay =
                makeAtomicType(XS, "gDay", anyAtomicType);
        BuiltInAtomicType hexBinary =
                makeAtomicType(XS, "hexBinary", anyAtomicType);
        BuiltInAtomicType base64Binary =
                makeAtomicType(XS, "base64Binary", anyAtomicType);
        BuiltInAtomicType anyURI =
                makeAtomicType(XS, "anyURI", anyAtomicType);
        BuiltInAtomicType qName =
                makeAtomicType(XS, "QName", anyAtomicType);
        BuiltInAtomicType notation =
                makeAtomicType(XS, "NOTATION", anyAtomicType);

        BuiltInAtomicType untypedAtomic =
                makeAtomicType(XDT, "untypedAtomic", anyAtomicType);

        //SimpleType javaObject =
        //        makeSimpleType(NamespaceConstant.JAVA_TYPE, "java.lang.Object", anyAtomicType, Type.OBJECT);

        BuiltInAtomicType decimal =
                makeAtomicType(XS, "decimal", numeric);
        BuiltInAtomicType xsfloat =
                makeAtomicType(XS, "float", numeric);
        BuiltInAtomicType xsdouble =
                makeAtomicType(XS, "double", numeric);

        BuiltInAtomicType xsinteger =
                makeAtomicType(XS, "integer", decimal);

        BuiltInAtomicType nonPositiveInteger =
                makeAtomicType(XS, "nonPositiveInteger", xsinteger);
        BuiltInAtomicType negativeInteger =
                makeAtomicType(XS, "negativeInteger", nonPositiveInteger);
        BuiltInAtomicType xslong =
                makeAtomicType(XS, "long", xsinteger);
        BuiltInAtomicType xsint =
                makeAtomicType(XS, "int", xslong);
        BuiltInAtomicType xsshort =
                makeAtomicType(XS, "short", xsint);
        BuiltInAtomicType xsbyte =
                makeAtomicType(XS, "byte", xsshort);
        BuiltInAtomicType nonNegativeInteger =
                makeAtomicType(XS, "nonNegativeInteger", xsinteger);
        BuiltInAtomicType positiveInteger =
                makeAtomicType(XS, "positiveInteger", nonNegativeInteger);
        BuiltInAtomicType unsignedLong =
                makeAtomicType(XS, "unsignedLong", nonNegativeInteger);
        BuiltInAtomicType unsignedInt =
                makeAtomicType(XS, "unsignedInt", unsignedLong);
        BuiltInAtomicType unsignedShort =
                makeAtomicType(XS, "unsignedShort", unsignedInt);
        BuiltInAtomicType unsignedByte =
                makeAtomicType(XS, "unsignedByte", unsignedShort);

        BuiltInAtomicType ymd =
                makeAtomicType(XDT, "yearMonthDuration", duration);
        BuiltInAtomicType dtd =
                makeAtomicType(XDT, "dayTimeDuration", duration);

        BuiltInAtomicType normalizedString =
                makeAtomicType(XS, "normalizedString", string);
        BuiltInAtomicType token =
                makeAtomicType(XS, "token", normalizedString);
        BuiltInAtomicType language =
                makeAtomicType(XS, "language", token);
        BuiltInAtomicType name =
                makeAtomicType(XS, "Name", token);
        BuiltInAtomicType nmtoken =
                makeAtomicType(XS, "NMTOKEN", token);
        BuiltInAtomicType ncname =
                makeAtomicType(XS, "NCName", name);
        BuiltInAtomicType id =
                makeAtomicType(XS, "ID", ncname);
        BuiltInAtomicType idref =
                makeAtomicType(XS, "IDREF", ncname);
        BuiltInAtomicType entity =
                makeAtomicType(XS, "ENTITY", ncname);

        makeListType(XS, "NMTOKENS");
        makeListType(XS, "IDREFS");
        makeListType(XS, "ENTITIES");

        lookup.put(new Integer(StandardNames.XS_ANY_TYPE), AnyType.getInstance());
        lookup.put(new Integer(StandardNames.XDT_UNTYPED), Untyped.getInstance());
    }

    private static BuiltInAtomicType makeAtomicType(String namespace,
                                             String lname,
                                             SimpleType baseType) {
        BuiltInAtomicType t = new BuiltInAtomicType(StandardNames.getFingerprint(namespace, lname));
        t.setBaseTypeFingerprint(baseType.getFingerprint());
        lookup.put(new Integer(t.getFingerprint()), t);
        return t;
    }

    private static BuiltInListType makeListType(String namespace,
                                           String lname) {
        BuiltInListType t = new BuiltInListType(StandardNames.getFingerprint(namespace, lname));
        lookup.put(new Integer(t.getFingerprint()), t);
        return t;
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
