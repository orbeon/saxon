package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.CastExpression;
import net.sf.saxon.expr.ErrorExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;

/**
 * The ConstructorFunctionLibrary represents the collection of constructor functions for atomic types. These
 * are provided for the built-in types such as xs:integer and xs:date, and also for user-defined atomic types.
 */

public class ConstructorFunctionLibrary implements FunctionLibrary {

    private Configuration config;
    
    /**
     * Create a SystemFunctionLibrary
     * @param config the Configuration
     */ 
    
    public ConstructorFunctionLibrary(Configuration config) {
        this.config = config;
    }
    
    /**
     * Test whether a system function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
        if (arity != 1 && arity != -1) {
            return false;
        }
        if (uri.equals(NamespaceConstant.SCHEMA) || uri.equals(NamespaceConstant.SCHEMA_DATATYPES)
                || uri.equals(NamespaceConstant.XDT)) {

            AtomicType type = (AtomicType)Type.getBuiltInItemType(uri, local);
            return type != null;
        }

        SchemaType st = config.getSchemaType(fingerprint);
        return (st != null && st instanceof AtomicType);
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param uri  The URI of the function name
     * @param localName  The local part of the function name
     * @param arguments  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found. 
     */

    public Expression bind(int nameCode, String uri, String localName, Expression[] arguments)
            throws XPathException {
         if (uri.equals(NamespaceConstant.SCHEMA) || uri.equals(NamespaceConstant.SCHEMA_DATATYPES)
                || uri.equals(NamespaceConstant.XDT)) {
            // it's a constructor function: treat it as shorthand for a cast expression
            if (arguments.length != 1) {
                throw new StaticError("A constructor function must have exactly one argument");
            }
            AtomicType type = (AtomicType)Type.getBuiltInItemType(uri, localName);
            if (type==null) {
                return new ErrorExpression(
                            new DynamicError("Unknown constructor function: " + localName));
            }
            return new CastExpression(arguments[0], type, true);
        }

        // Now see if it's a constructor function for a user-defined type

        if (arguments.length == 1) {
            SchemaType st = config.getSchemaType(nameCode & 0xfffff);
            if (st != null && st instanceof AtomicType) {
                return new CastExpression(arguments[0], (AtomicType)st, true);
            }
        }

        return null;
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