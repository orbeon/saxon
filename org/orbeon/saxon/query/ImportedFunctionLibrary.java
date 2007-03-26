package org.orbeon.saxon.query;

import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceType;

import java.util.HashSet;
import java.util.Iterator;

/**
 * This implementation of FunctionLibrary contains all the functions imported into a Query Module.
 * It is implemented as a view of the "global" XQueryFunctionLibrary for the whole query, selecting
 * only those functions that are in an imported namespace.
 */

public class ImportedFunctionLibrary implements FunctionLibrary, XQueryFunctionBinder {

    StaticQueryContext importingModule;
    XQueryFunctionLibrary baseLibrary;
    HashSet namespaces = new HashSet(5);

    public ImportedFunctionLibrary(StaticQueryContext importingModule, XQueryFunctionLibrary baseLibrary) {
        this.importingModule = importingModule;
        this.baseLibrary = baseLibrary;
    }

    public void addImportedNamespace(String namespace) {
        namespaces.add(namespace);
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     *
     * @param nameCode   The namepool nameCode of the function name. The uri and local name are also
     *                   supplied (redundantly) to avoid fetching them from the name pool.
     * @param uri        The URI of the function name
     * @param local      The local part of the function name
     * @param staticArgs The expressions supplied statically in arguments to the function call.
     *                   The length of this array represents the arity of the function. The intention is
     *                   that the static type of the arguments (obtainable via getItemType() and getCardinality()) may
     *                   be used as part of the binding algorithm. In some cases it may be possible for the function
     *                   to be pre-evaluated at compile time, for example if these expressions are all constant values.
     *                   <p/>
     *                   The conventions of the XPath language demand that the results of a function depend only on the
     *                   values of the expressions supplied as arguments, and not on the form of those expressions. For
     *                   example, the result of f(4) is expected to be the same as f(2+2). The actual expression is supplied
     *                   here to enable the binding mechanism to select the most efficient possible implementation (including
     *                   compile-time pre-evaluation where appropriate).
     * @return An object representing the function to be called, if one is found;
     *         null if no function was found matching the required name and arity.
     * @throws org.orbeon.saxon.trans.XPathException
     *          if a function is found with the required name and arity, but
     *          the implementation of the function cannot be loaded or used; or if an error occurs
     *          while searching for the function.
     */

    public Expression bind(int nameCode, String uri, String local, Expression[] staticArgs) throws XPathException {
        if (namespaces.contains(uri)) {
            Expression call = baseLibrary.bind(nameCode, uri, local, staticArgs);
            if (call != null) {
                // Check that the result type and all the argument types are in the static context of the
                // calling module
                XQueryFunction def = baseLibrary.getDeclaration(nameCode, uri, local, staticArgs);
                importingModule.checkImportedType(def.getResultType(), def);
                for (int i=0; i<def.getNumberOfArguments(); i++) {
                    SequenceType argType = def.getArgumentTypes()[i];
                    importingModule.checkImportedType(argType, def);
                }
            }
            return call;
        } else {
            return null;
        }
    }

    /**
     * Get the function declaration corresponding to a given function name and arity
     * @return the XQueryFunction if there is one, or null if not.
     */

    public XQueryFunction getDeclaration(int nameCode, String uri, String local, Expression[] staticArgs) {
        if (namespaces.contains(uri)) {
            return baseLibrary.getDeclaration(nameCode, uri, local, staticArgs);
        } else {
            return null;
        }
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        ImportedFunctionLibrary lib = new ImportedFunctionLibrary(importingModule, baseLibrary);
        Iterator iter = namespaces.iterator();
        while (iter.hasNext()) {
            String ns = (String)iter.next();
            lib.addImportedNamespace(ns);
        }
        return lib;
    }

    public void setImportingModule(StaticQueryContext importingModule) {
        this.importingModule = importingModule;
    }

    /**
     * Test whether an extension function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time. If the function library is to be used only in an XQuery or free-standing XPath
     * environment, this method may throw an UnsupportedOperationException.
     *
     * @param fingerprint The namepool fingerprint of the function name. This must match the
     *                    uri and localName; the information is provided redundantly to avoid repeated lookups in the name pool.
     * @param uri         The URI of the function name
     * @param local       The local part of the function name
     * @param arity       The number of arguments. This is set to -1 in the case of the single-argument
     *                    function-available() function; in this case the method should return true if there is some
     *                    matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
        if (namespaces.contains(uri)) {
            return baseLibrary.isAvailable(fingerprint, uri, local, arity);
        } else {
            return false;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
