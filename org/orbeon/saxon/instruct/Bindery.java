package net.sf.saxon.instruct;
import net.sf.saxon.expr.TypeChecker;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.ErrorValue;
import net.sf.saxon.value.Value;


/**
* The Bindery class holds information about variables and their values. From Saxon 8.1, it is
* used only for global variables: local variables are now held in the XPathContext object.
*
* Variables are identified by a Binding object. Values will always be of class Value.
*/

public final class Bindery  {

    private ValueRepresentation[] globals;                        // values of global variables and parameters
    private boolean[] busy;                         // set to true while variable is being evaluated
    private GlobalParameterSet globalParameters;    // supplied global parameters
    private SlotManager globalVariableMap;          // contains the mapping of variable names to slot numbers

    /**
    * Define how many slots are needed for global variables
    */

    public void allocateGlobals(SlotManager map) {
        globalVariableMap = map;
        int n = map.getNumberOfVariables()+1;
        globals = new ValueRepresentation[n];
        busy = new boolean[n];
        for (int i=0; i<n; i++) {
            globals[i] = null;
            busy[i] = false;
        }
    }

    /**
    * Define global parameters
    * @param params The ParameterSet passed in by the user, eg. from the command line
    */

    public void defineGlobalParameters(GlobalParameterSet params) {
        globalParameters = params;
    }

    /**
    * Use global parameter. This is called when a global xsl:param element is processed.
    * If a parameter of the relevant name was supplied, it is bound to the xsl:param element.
    * Otherwise the method returns false, so the xsl:param default will be evaluated.
    * @param fingerprint The fingerprint of the parameter
    * @param binding The XSLParam element to bind its value to
    * @return true if a parameter of this name was supplied, false if not
    */

    public boolean useGlobalParameter(int fingerprint, GlobalParam binding, XPathContext context) throws XPathException {
        if (globalParameters==null) {
            return false;
        }
        Object obj = globalParameters.get(fingerprint);
        if (obj==null) {
            return false;
        }

        Value val;
        try {
            val = Value.convertJavaObjectToXPath(
                    obj, binding.getRequiredType(), context.getController().getConfiguration());
            if (val==null) {
                val = EmptySequence.getInstance();
            }
        } catch (XPathException err) {
            err.setLocator(binding);
            throw err;
        }

        ItemType reqItemType = binding.getRequiredType().getPrimaryType();
        if (val instanceof AtomicValue && reqItemType instanceof AtomicType) {
            // If the parameter is an atomic value, typically a string supplied on
            // the command line, we attempt to convert it to the required type. This
            // will not always succeed.
            val = ((AtomicValue)val).convert((AtomicType)reqItemType, null, true);
            if (val instanceof ErrorValue) {
                throw ((ErrorValue)val).getException();
            }
        } else {
            // For any other parameter value, we verify that if conforms to the
            // required type. This must be precise conformance, we don't attempt to
            // do atomization or to convert untypedAtomic values
            DynamicError err = TypeChecker.testConformance(val, binding.getRequiredType());
            if (err != null) {
                throw err;
            }
        }
        globals[binding.getSlotNumber()] = val;
        return true;
    }

    /**
    * Provide a value for a global variable
    * @param binding identifies the variable
    * @param value the value of the variable
    */

    public void defineGlobalVariable(GlobalVariable binding, ValueRepresentation value) {
        globals[binding.getSlotNumber()] = value;
    }

    /**
    * Set/Unset a flag to indicate that a particular global variable is currently being
    * evaluated.
    * @throws net.sf.saxon.trans.XPathException If an attempt is made to set the flag when it is already set, this means
    * the definition of the variable is circular.
    */

    public void setExecuting(GlobalVariable binding, boolean executing)
    throws XPathException {
        int slot = binding.getSlotNumber();
        if (executing) {
            if (busy[slot]) {
                throw new XPathException.Circularity("Circular definition");
            }
            // It would be better to detect circular references statically
            // at compile time. However, this is not always possible, because they
            // can arise via execution of templates or stylesheet functions.
            busy[slot]=true;
        } else {
            busy[slot]=false;
        }
    }

    /**
    * Get the value of a global variable
    * @param binding the Binding that establishes the unique instance of the variable
    * @return the Value of the variable if defined, null otherwise.
    */

    public ValueRepresentation getGlobalVariableValue(GlobalVariable binding) {
        return globals[binding.getSlotNumber()];
    }

     /**
    * Get the value of a global variable whose slot number is known
    * @param slot the slot number of the required variable
    * @return the Value of the variable if defined, null otherwise.
    */

    public ValueRepresentation getGlobalVariable(int slot) {
        return globals[slot];
    }

    /**
    * Assign a new value to a global variable. Supports saxon:assign.
    * @param binding identifies the local or global variable or parameter
    */

    public void assignGlobalVariable(GlobalVariable binding, ValueRepresentation value) {
        defineGlobalVariable(binding, value);
    }

    /**
     * Get the Global Variable Map, containing the mapping of variable names (fingerprints)
     * to slot numbers. This is provided for use by debuggers.
     */

    public SlotManager getGlobalVariableMap() {
        return globalVariableMap;
    }

    /**
     * Get all the global variables, as an array. This is provided for use by debuggers
     * that know the layout of the global variables within the array.
     */

    public ValueRepresentation[] getGlobalVariables() {
        return globals;
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
//
//
