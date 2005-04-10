package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.trans.XPathException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A FunctionLibraryList is a list of FunctionLibraries. It is also a FunctionLibrary in its own right.
 * When required, it searches the list of FunctionLibraries to find the required function.
 */
public class FunctionLibraryList implements FunctionLibrary {

    public List libraryList = new ArrayList(8);

    /**
     * Add a new FunctionLibrary to the list of FunctionLibraries in this FunctionLibraryList. Note
     * that libraries are searched in the order they are added to the list.
     * @param lib A function library to be added to the list of function libraries to be searched.
     */

    public void addFunctionLibrary(FunctionLibrary lib) {
        libraryList.add(lib);
    }

    /**
     * Test whether an extension function with a given name and arity is available. This supports
     * the function-available() function in XSLT. This method may be called either at compile time
     * or at run time.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param arity The number of arguments. This is set to -1 in the case of the single-argument
     * function-available() function; in this case the method should return true if there is some
     * matching extension function, regardless of its arity.
     */

    public boolean isAvailable(int fingerprint, String uri, String local, int arity) {
        for (Iterator it=libraryList.iterator(); it.hasNext();) {
            FunctionLibrary lib = (FunctionLibrary)it.next();
            if (lib.isAvailable(fingerprint, uri, local, arity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Bind an extension function, given the URI and local parts of the function name,
     * and the list of expressions supplied as arguments. This method is called at compile
     * time.
     * @param uri  The URI of the function name
     * @param local  The local part of the function name
     * @param staticArgs  The expressions supplied statically in arguments to the function call.
     * The length of this array represents the arity of the function. The intention is
     * that the static type of the arguments (obtainable via getItemType() and getCardinality() may
     * be used as part of the binding algorithm. In some cases it may be possible for the function
     * to be pre-evaluated at compile time, for example if these expressions are all constant values.
     * @return An object representing the extension function to be called, if one is found;
     * null if no extension function was found matching the required name and arity.
     * @throws net.sf.saxon.trans.XPathException if a function is found with the required name and arity, but
     * the implementation of the function cannot be loaded or used; or if an error occurs
     * while searching for the function.
     */

    public Expression bind(int nameCode, String uri, String local, Expression[] staticArgs)
            throws XPathException {
        for (Iterator it=libraryList.iterator(); it.hasNext();) {
            FunctionLibrary lib = (FunctionLibrary)it.next();
            Expression func = lib.bind(nameCode, uri, local, staticArgs);
            if (func != null) {
                return func;
            }
        }
        return null;
    }

    /**
     * Get the list of contained FunctionLibraries. This method allows the caller to modify
     * the library list, for example by adding a new FunctionLibrary at a chosen position,
     * by removing a library from the list, or by changing the order of libraries in the list.
     * Note that such changes may violate rules in the
     * language specifications, or assumptions made within the product.
     * @return a list whose members are of class FunctionLibrary
     */

    public List getLibraryList() {
        return libraryList;
    }

    /**
     * This method creates a copy of a FunctionLibrary: if the original FunctionLibrary allows
     * new functions to be added, then additions to this copy will not affect the original, or
     * vice versa.
     *
     * @return a copy of this function library. This must be an instance of the original class.
     */

    public FunctionLibrary copy() {
        FunctionLibraryList fll = new FunctionLibraryList();
        fll.libraryList = new ArrayList(libraryList.size());
        for (int i=0; i<libraryList.size(); i++) {
            fll.libraryList.add(((FunctionLibrary)libraryList.get(i)).copy());
        }
        return fll;
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