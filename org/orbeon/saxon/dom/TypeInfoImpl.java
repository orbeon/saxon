package net.sf.saxon.dom;

import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.AnyType;
import net.sf.saxon.Configuration;
import org.w3c.dom.TypeInfo;

/**
 * This class implements the DOM TypeInfo interface as a wrapper over the Saxon SchemaType
 * interface.
 */

public class TypeInfoImpl implements TypeInfo {

    private Configuration config;
    private SchemaType schemaType;

    /**
     * Construct a TypeInfo based on a SchemaType
     */

    public TypeInfoImpl(Configuration config, SchemaType type) {
        this.config = config;
        this.schemaType = type;
    }

    /**
     * Get the local name of the type (a system-allocated name if anonymous). Needed to implement the
     * DOM level 3 TypeInfo interface.
     */

    public String getTypeName() {
        return config.getNamePool().getLocalName(schemaType.getNameCode());
    }

    /**
     * Get the namespace name of the type (a system-allocated name if anonymous). Needed to implement the
     * DOM level 3 TypeInfo interface.
     */

    public String getTypeNamespace() {
        return config.getNamePool().getURI(schemaType.getNameCode());
    }

    /**
     * This method returns true if there is a derivation between the reference type definition, that is the TypeInfo
     * on which the method is being called, and the other type definition, that is the one passed as parameters.
     * This method implements the DOM Level 3 TypeInfo interface. It must be called only on a valid type.
     * @param typeNamespaceArg the namespace of the "other" type
     * @param typeNameArg the local name of the "other" type
     * @param derivationMethod the derivation method: zero or more of {@link SchemaType#DERIVATION_RESTRICTION},
     * {@link SchemaType#DERIVATION_EXTENSION}, {@link SchemaType#DERIVATION_LIST}, or {@link SchemaType#DERIVATION_UNION}.
     * Zero means derived by any possible route.
     */

    public boolean isDerivedFrom(String typeNamespaceArg,
                                 String typeNameArg,
                                 int derivationMethod) throws IllegalStateException {
        SchemaType base = schemaType.getBaseType();
        int fingerprint = config.getNamePool().allocate("", typeNamespaceArg, typeNameArg);
        if (derivationMethod==0 || (derivationMethod & schemaType.getDerivationMethod()) != 0) {
            if (base.getFingerprint() == fingerprint) {
                return true;
            } else if (base instanceof AnyType) {
                return false;
            } else {
                return new TypeInfoImpl(config, base).isDerivedFrom(typeNamespaceArg, typeNameArg, derivationMethod);
            }
        }
        return false;
        // TODO: if derivationMethod is RESTRICTION, this interpretation requires every step to be derived
        // by restriction. An alternative interpretation is that at least one step must be derived by restriction.
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//
