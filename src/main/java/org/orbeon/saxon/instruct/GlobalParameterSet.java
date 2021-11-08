package org.orbeon.saxon.instruct;

import org.orbeon.saxon.om.StructuredQName;

import java.util.HashMap;


/**
 * A GlobalParameterSet is a set of parameters supplied when invoking a stylesheet or query.
 * It is a collection of name-value pairs, the names being represented by StructuredQName objects.
 * The values are objects, as supplied by the caller: conversion of the object
 * to a required type takes place when the parameter is actually used.
 */

public class GlobalParameterSet
{
	private HashMap params = new HashMap(10);

    /**
     * Create an empty parameter set
     */

    public GlobalParameterSet() {}

    /**
     * Create a parameter set as a copy of an existing parameter set
     */

    public GlobalParameterSet(GlobalParameterSet existing) {
        params = new HashMap(existing.params);
    }

    /**
     * Add a parameter to the ParameterSet
     *
     * @param qName The fingerprint of the parameter name.
     * @param value The value of the parameter, or null if the parameter is to be removed
     */

    public void put (StructuredQName qName, Object value) {
        if (value == null) {
            params.remove(qName);
        } else {
            params.put(qName, value);
        }
    }

    /**
     * Get a parameter
     *
     * @param qName The parameter name.
     * @return The value of the parameter, or null if not defined
     */

    public Object get (StructuredQName qName) {
        return params.get(qName);
    }

    /**
     * Clear all values
     */

    public void clear() {
        params.clear();
    }

    /**
     * Get all the keys that have been allocated
     */

    public java.util.Collection getKeys() {
        return params.keySet();
    }

    /**
     * Get the number of entries in the result of getKeys() that are significant
     */

    public int getNumberOfKeys() {
        return params.size();
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