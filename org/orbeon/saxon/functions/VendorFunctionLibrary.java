package org.orbeon.saxon.functions;

import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;

import java.util.HashMap;

/**
 * The VendorFunctionLibrary represents specially-recognized functions in the Saxon namespace. It doesn't
 * handle Saxon extension functions that are implemented as normal extension functions, which are bound using
 * the {@link org.orbeon.saxon.functions.JavaExtensionLibrary}.
 */

public class VendorFunctionLibrary implements FunctionLibrary {

    private HashMap functionTable;

    /**
     * Create the Vendor Function Library for Saxon
     */

    public VendorFunctionLibrary() {
        init();
    }

    /**
     * Register an extension function in the table of function details.
     * @param name the function name
     * @param implementationClass the class used to implement the function
     * @param opcode identifies the function when a single class implements several functions
     * @param minArguments the minimum number of arguments required
     * @param maxArguments the maximum number of arguments allowed
     * @param itemType the item type of the result of the function
     * @param cardinality the cardinality of the result of the function
     * @return the entry describing the function. The entry is incomplete, it does not yet contain information
     * about the function arguments.
    */

    protected StandardFunction.Entry register( String name,
                                 Class implementationClass,
                                 int opcode,
                                 int minArguments,
                                 int maxArguments,
                                 ItemType itemType,
                                 int cardinality ) {
        StandardFunction.Entry e = StandardFunction.makeEntry(
                name, implementationClass, opcode, minArguments, maxArguments, itemType, cardinality);
        functionTable.put(name, e);
        return e;
    }

    protected void init() {
        functionTable = new HashMap(30);
        StandardFunction.Entry e;
        e = register("evaluate", Evaluate.class, Evaluate.EVALUATE, 1, 10, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            StandardFunction.arg(e, 0, BuiltInAtomicType.STRING, StaticProperty.EXACTLY_ONE, null);

        e = register("evaluate-node", Evaluate.class, Evaluate.EVALUATE_NODE, 1, 1, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            StandardFunction.arg(e, 0, Type.NODE_TYPE, StaticProperty.EXACTLY_ONE, null);

        e = register("eval", Evaluate.class, Evaluate.EVAL, 1, 10, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE);
            StandardFunction.arg(e, 0, BuiltInAtomicType.ANY_ATOMIC, StaticProperty.EXACTLY_ONE, null);

        e = register("expression", Evaluate.class, Evaluate.EXPRESSION, 1, 2, BuiltInAtomicType.ANY_ATOMIC, StaticProperty.EXACTLY_ONE);
            StandardFunction.arg(e, 0, BuiltInAtomicType.STRING, StaticProperty.EXACTLY_ONE, null);
            StandardFunction.arg(e, 1, NodeKindTest.ELEMENT, StaticProperty.EXACTLY_ONE, null);

        e = register("is-whole-number", IsWholeNumber.class, 1, 1, 1, BuiltInAtomicType.BOOLEAN, StaticProperty.EXACTLY_ONE);
            StandardFunction.arg(e, 0, BuiltInAtomicType.NUMERIC, StaticProperty.ALLOWS_ZERO_OR_ONE, null);

        e = register("item-at", ItemAt.class, 1, 2, 2, StandardFunction.SAME_AS_FIRST_ARGUMENT, StaticProperty.ALLOWS_ZERO_OR_ONE);
            StandardFunction.arg(e, 0, Type.ITEM_TYPE, StaticProperty.ALLOWS_ZERO_OR_MORE, null);
            StandardFunction.arg(e, 1, BuiltInAtomicType.NUMERIC, StaticProperty.ALLOWS_ZERO_OR_ONE, null);

        e = register("parse", Parse.class, 0, 1, 1, NodeKindTest.DOCUMENT, StaticProperty.EXACTLY_ONE);
            StandardFunction.arg(e, 0, BuiltInAtomicType.STRING, StaticProperty.EXACTLY_ONE, null);

        e = register("serialize", Serialize.class, 0, 2, 2, BuiltInAtomicType.STRING, StaticProperty.EXACTLY_ONE);
            StandardFunction.arg(e, 0, Type.NODE_TYPE, StaticProperty.ALLOWS_ZERO_OR_ONE, null);
            StandardFunction.arg(e, 1, Type.ITEM_TYPE, StaticProperty.EXACTLY_ONE, null);

    }

    /**
     * Test whether a Saxon function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param functionName the name of the function
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     */

    public boolean isAvailable(StructuredQName functionName, int arity) {
        if (functionName.getNamespaceURI().equals(NamespaceConstant.SAXON)) {
            StandardFunction.Entry entry = (StandardFunction.Entry)functionTable.get(functionName.getLocalName());
            return entry != null && (arity == -1 ||
                    (arity >= entry.minArguments && arity <= entry.maxArguments));
        } else {
            return false;
        }
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param functionName the name of the function
     * @param staticArgs  The expressions supplied statically in the function call. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm.
     * @param env
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws org.orbeon.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function; or if this function library "owns" the namespace containing
     * the function call, but no function was found.
     */

    public Expression bind(StructuredQName functionName, Expression[] staticArgs, StaticContext env)
            throws XPathException {
        String uri = functionName.getNamespaceURI();
        String local = functionName.getLocalName();
        if (uri.equals(NamespaceConstant.SAXON)) {
            StandardFunction.Entry entry = (StandardFunction.Entry)functionTable.get(local);
            if (entry == null) {
                return null;
            }
            Class functionClass = entry.implementationClass;
            SystemFunction f;
            try {
                f = (SystemFunction)functionClass.newInstance();
            } catch (Exception err) {
                throw new AssertionError("Failed to load Saxon extension function: " + err.getMessage());
            }
            f.setDetails(entry);
            f.setFunctionName(functionName);
            f.setArguments(staticArgs);
            checkArgumentCount(staticArgs.length, entry.minArguments, entry.maxArguments, local);
            return f;
        } else {
            return null;
        }
    }

    /**
     * Make a Saxon function with a given name
     * @param localName the local name of the function
     * @param env the static context
     * @param arguments the arguments of the function
     * @return an exprssion representing a call on the given function
     */

    public Expression makeSaxonFunction(String localName, StaticContext env, Expression[] arguments)
    throws XPathException {
        String uri = NamespaceConstant.SAXON;
        StructuredQName functionName = new StructuredQName("saxon", uri, localName);
        return bind(functionName, arguments, env);
    }

    /**
    * Check number of arguments. <BR>
    * A convenience routine for use in subclasses.
    * @param numArgs the actual number of arguments
    * @param min the minimum number of arguments allowed
    * @param max the maximum number of arguments allowed
    * @param local the local name of the function, used for diagnostics
    * @return the actual number of arguments
    * @throws org.orbeon.saxon.trans.XPathException if the number of arguments is out of range
    */

    private int checkArgumentCount(int numArgs, int min, int max, String local) throws XPathException {
        if (min==max && numArgs != min) {
            throw new XPathException("Function " + Err.wrap("saxon:"+local, Err.FUNCTION) + " must have "
                    + min + pluralArguments(min));
        }
        if (numArgs < min) {
            throw new XPathException("Function " + Err.wrap("saxon:"+local, Err.FUNCTION) + " must have at least "
                    + min + pluralArguments(min));
        }
        if (numArgs > max) {
            throw new XPathException("Function " + Err.wrap("saxon:"+local, Err.FUNCTION) + " must have no more than "
                    + max + pluralArguments(max));
        }
        return numArgs;
    }

    /**
    * Utility routine used in constructing error messages
     * @param num a number
     * @return the string " argument" or " arguments" if num is plural
    */

    public static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        return this;
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