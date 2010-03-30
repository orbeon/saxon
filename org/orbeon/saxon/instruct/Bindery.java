package org.orbeon.saxon.instruct;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.pattern.NameTest;

import java.net.URI;
import java.net.URISyntaxException;


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
     * @param map the SlotManager that keeps track of slot allocation for global variables.
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
     * @param qName The name of the parameter
     * @param slot The slot number allocated to the parameter
     * @param requiredType The declared type of the parameter
     * @param context the XPath dynamic evaluation context
     * @return true if a parameter of this name was supplied, false if not
     */

    public boolean useGlobalParameter(StructuredQName qName, int slot, SequenceType requiredType, XPathContext context)
            throws XPathException {
        if (globals[slot] != null) {
            return true;
        }

        if (globalParameters==null) {
            return false;
        }
        Object obj = globalParameters.get(qName);
        if (obj==null) {
            return false;
        }

        // If the supplied value is a document node, and the document node has a systemID that is an absolute
        // URI, and the absolute URI does not already exist in the document pool, then register it in the document
        // pool, so that the document-uri() function will find it there, and so that a call on doc() will not
        // reload it.

        if (obj instanceof DocumentInfo) {
            String systemId = ((DocumentInfo)obj).getSystemId();
            try {
                if (systemId != null && new URI(systemId).isAbsolute()) {
                    DocumentPool pool = context.getController().getDocumentPool();
                    if (pool.find(systemId) == null) {
                        pool.add(((DocumentInfo)obj), systemId);
                    }
                }
            } catch (URISyntaxException err) {
                // ignore it
            }
        }   

        JPConverter converter = JPConverter.allocate(obj.getClass(), context.getConfiguration());
        ValueRepresentation val = converter.convert(obj, context);
        //val = Value.convertJavaObjectToXPath(obj, requiredType, context);
        if (val==null) {
            val = EmptySequence.getInstance();
        }

        val = applyFunctionConversionRules(val, requiredType, context);

//        ItemType reqItemType = requiredType.getPrimaryType();
//        if (val instanceof AtomicValue && reqItemType.isAtomicType()) {
//            // If the parameter is an atomic value, typically a string supplied on
//            // the command line, we attempt to convert it to the required type. This
//            // will not always succeed.
//            val = ((AtomicValue)val).convert((AtomicType)reqItemType, true, context).asAtomic();
//        } else {
//            // For any other parameter value, we verify that if conforms to the
//            // required type. This must be precise conformance, we don't attempt to
//            // do atomization or to convert untypedAtomic values
//            XPathException err = TypeChecker.testConformance(val, requiredType, context);
//            if (err != null) {
//                throw err;
//            }
//        }
        globals[slot] = val;
        return true;
    }

    /**
     * Apply the function conversion rules
     * @param value a value to be converted
     * @param requiredType the required type
     * @param context the conversion context
     * @return the converted value
     * @throws XPathException if the value cannot be converted to the required type
     */

    public static Value applyFunctionConversionRules(
            ValueRepresentation value, SequenceType requiredType, final XPathContext context)
            throws XPathException {
        final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        final ItemType requiredItemType = requiredType.getPrimaryType();
        ItemType suppliedItemType = (value instanceof NodeInfo
                ? new NameTest(((NodeInfo)value))
                : ((Value)value).getItemType(th));

        SequenceIterator iterator = Value.asIterator(value);

        if (requiredItemType.isAtomicType()) {

            // step 1: apply atomization if necessary

            if (!suppliedItemType.isAtomicType()) {
                iterator = Atomizer.getAtomizingIterator(iterator);
                suppliedItemType = suppliedItemType.getAtomizedItemType();
            }

            // step 2: convert untyped atomic values to target item type

            if (th.relationship(suppliedItemType, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
                ItemMappingFunction converter = new ItemMappingFunction() {
                    public Item map(Item item) throws XPathException {
                        if (item instanceof UntypedAtomicValue) {
                            ConversionResult val = ((UntypedAtomicValue)item).convert(
                                    (AtomicType)requiredItemType, true, context);
                            if (val instanceof ValidationFailure) {
                                ValidationFailure vex = (ValidationFailure)val;
                                throw vex.makeException();
                            }
                            return (AtomicValue)val;
                        } else {
                            return item;
                        }
                    }
                };
                iterator = new ItemMappingIterator(iterator, converter);
            }

            // step 3: apply numeric promotion

            if (th.isSubType(requiredItemType, BuiltInAtomicType.NUMERIC)) {
                ItemMappingFunction promoter = new ItemMappingFunction() {
                    public Item map(Item item) throws XPathException {
                        if (!(item instanceof NumericValue || item instanceof UntypedAtomicValue)) {
                            throw new XPathException(
                                    "Cannot promote non-numeric value to " + requiredItemType.toString(), "XPTY0004", context);
                        }
                        return ((AtomicValue)item).convert((AtomicType)requiredItemType, true, context).asAtomic();
                    }
                };
                iterator = new ItemMappingIterator(iterator, promoter);
            }

            // step 4: apply URI-to-string promotion

            if (requiredItemType.equals(BuiltInAtomicType.STRING) &&
                    th.relationship(suppliedItemType, BuiltInAtomicType.ANY_URI) != TypeHierarchy.DISJOINT) {
                ItemMappingFunction promoter = new ItemMappingFunction() {
                    public Item map(Item item) throws XPathException {
                        if (item instanceof AnyURIValue) {
                            return new StringValue(item.getStringValueCS());
                        } else {
                            return item;
                        }
                    }
                };
                iterator = new ItemMappingIterator(iterator, promoter);
            }
        }

        Value result = Value.asValue(SequenceExtent.makeSequenceExtent(iterator));
        XPathException err = TypeChecker.testConformance(result, requiredType, context);
        if (err != null) {
            throw err;
        }
        return result;
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
     * @param binding the global variable in question
     * @param executing true when we start evaluating the variable, false when evaluation has finished
     * @throws org.orbeon.saxon.trans.XPathException If an attempt is made to set the flag when it is already set, this means
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
    * Set the value of a global variable whose slot number is known
    * @param slot the slot number of the required variable
    * @param value the Value of the variable if defined, null otherwise.
    */

    public void setGlobalVariable(int slot, ValueRepresentation value) {
        globals[slot] = value;
    }

    /**
     * Assign a new value to a global variable. Supports saxon:assign.
     * @param binding identifies the global variable or parameter
     * @param value the value to be assigned to the variable
    */

    public void assignGlobalVariable(GlobalVariable binding, ValueRepresentation value) {
        defineGlobalVariable(binding, value);
    }

    /**
     * Get the Global Variable Map, containing the mapping of variable names (fingerprints)
     * to slot numbers. This is provided for use by debuggers.
     * @return the SlotManager containing information about the assignment of slot numbers
     * to global variables and parameters
     */

    public SlotManager getGlobalVariableMap() {
        return globalVariableMap;
    }

    /**
     * Get all the global variables, as an array. This is provided for use by debuggers
     * that know the layout of the global variables within the array.
     * @return the array of global varaibles.
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
