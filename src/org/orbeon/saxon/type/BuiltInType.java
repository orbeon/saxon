package org.orbeon.saxon.type;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.sort.IntHashMap;

import java.io.Serializable;

/**
 * This non-instantiable class acts as a register of Schema objects containing all the built-in types:
 * that is, the types defined in the "xs" namespace.
 *
 * <p>Previously called BuiltInSchemaFactory; but its original function has largely been moved to the two
 * classes {@link BuiltInAtomicType} and {@link BuiltInListType}
 */

public abstract class BuiltInType implements Serializable {

    /**
    * Table of all built in types
    */

    private static IntHashMap lookup = new IntHashMap(100);

    /**
     * Class is never instantiated
     */

    private BuiltInType() {
    }

    static {
        register(StandardNames.XS_ANY_SIMPLE_TYPE, AnySimpleType.getInstance());
        register(StandardNames.XS_ANY_TYPE, AnyType.getInstance());
        register(StandardNames.XS_UNTYPED, Untyped.getInstance());
    }

    /**
     * Get the schema type with a given fingerprint
     * @param fingerprint the fingerprint representing the name of the required type
     * @return the SchemaType object representing the given type, if known, otherwise null
     */

    public static SchemaType getSchemaType(int fingerprint) {
        SchemaType st = (SchemaType)lookup.get(fingerprint);
        if (st == null) {
            // this means the method has been called before doing the static initialization of BuiltInAtomicType
            // or BuiltInListType. So force it now
            if (BuiltInAtomicType.DOUBLE == null || BuiltInListType.NMTOKENS == null) {
                // no action, except to force the initialization to run
            }
            st = (SchemaType)lookup.get(fingerprint);
        }
        return st;                  
    }

    /**
     * Method for internal use to register a built in type with this class
     * @param fingerprint the fingerprint of the type name
     * @param type the SchemaType representing the built in type
     */

    static void register(int fingerprint, SchemaType type) {
        lookup.put(fingerprint, type);
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
