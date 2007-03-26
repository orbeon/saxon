package org.orbeon.saxon.query;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.GeneralVariable;
import org.orbeon.saxon.instruct.GlobalParam;
import org.orbeon.saxon.instruct.GlobalVariable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.Configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to hold compile-time information about an XQuery global variable
 * or parameter
 */

public class GlobalVariableDefinition implements VariableDeclaration, Declaration {

    protected List references = new ArrayList(10);
    private SequenceType requiredType;
    private Expression value;
    private int nameCode;
    private boolean isParameter;
    private String variableName;
    private String systemId;        // identifies the module where the variable declaration occurred
    private int lineNumber;         // identifies the line number of the variable declaration
    private GlobalVariable compiledVar;

    /**
     * Set the required type of the variable
     * @param type the declared type, from the "as" clause if present
     */

    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    /**
     * Get the required type of the variable
     * @return the declared type, from the "as" clause if present
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * Set the variable name
     * @param nameCode the variable name, expressed as a NamePool name code
     */
    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Get the variable name
     * @return the variable name, expressed as a NamePool name code
     */
    public int getNameCode() {
        return nameCode;
    }
    /**
     * Set the line number where the variable declaration appears in the source
     * @param lineNumber the line number
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Get the line number where the declaration appears
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get column number
     * @return -1 always
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get public identifier
     * @return null always
     */

    public String getPublicId() {
        return null;
    }

    /**
     * Set the system ID of the module where the variable declaration appears
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system ID of the module containing the variable declaration
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the name of the variable
     * @return the variable name, as a lexical QName
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Set the variable name
     * @param variableName the variable name, as a lexical QName
     */
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    /**
     * Set the expression used to define the value of the variable
     * @param val the initializing expression
     */
    public void setValueExpression(Expression val) {
        this.value = val;
    }

    /**
     * Indicate whether this global variable is a "parameter" (an external variable, in XQuery terminology)
     * @param b true if this variable is external
     */
    public void setIsParameter(boolean b) {
        isParameter = b;
    }

    /**
     * Register a variable reference that refers to this global variable
     * @param ref the variable reference
     */
    public void registerReference(BindingReference ref) {
        references.add(ref);
    }

    /**
     * Iterate over the references to this variable
     */

    public Iterator iterateReferences() {
        return references.iterator();
    }


    /**
     * Create a compiled representation of this global variable
     * @param env the static context for the query module
     * @param slot the slot number allocated to this variable
     * @return the compiled representation
     * @throws XPathException if compile-time errors are found.
     */

    public GlobalVariable compile(StaticQueryContext env, int slot) throws XPathException {

        GlobalVariable var;
        if (isParameter) {
            var = new GlobalParam();
            var.setExecutable(env.getExecutable());
            var.setRequiredParam(value==null);
        } else {
            var = new GlobalVariable();
            var.setExecutable(env.getExecutable());
        }

        var.setHostLanguage(Configuration.XQUERY);
        var.setSelectExpression(value);
        var.setNameCode(nameCode);
        var.setRequiredType(requiredType);
        var.setVariableName(variableName);
        var.setSlotNumber(slot);

        int loc = var.getExecutable().getLocationMap().allocateLocationId(systemId, lineNumber);
        var.setLocationId(loc);

        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            BindingReference binding = (BindingReference)iter.next();
            binding.setStaticType(requiredType, null, 0);
            binding.fixup(var);
        }
        env.getExecutable().registerGlobalVariable(var);
        int referenceCount = RangeVariableDeclaration.getReferenceCount(references, var, env, true);
        if (referenceCount < 10) {
            // allow for the fact that the references may be in functions that are executed repeatedly
            referenceCount = 10;
        }
        var.setReferenceCount(referenceCount);
        compiledVar = var;
        return var;
    }

    /**
     * Type check the compiled representation of this global variable
     * @param env the static context for the query module
     * @throws XPathException if compile-time errors are found.
     */

    public static void typeCheck(StaticQueryContext env, GeneralVariable var) throws XPathException {

        Expression value = var.getSelectExpression();
        if (value != null) {
            ComputedExpression.setParentExpression(value, var);
            RoleLocator role = new RoleLocator(
                    RoleLocator.VARIABLE, env.getNamePool().getDisplayName(var.getNameCode()), 0, null);
            Expression value2 = TypeChecker.strictTypeCheck(
                                    value.simplify(env).typeCheck(env, Type.ITEM_TYPE),
                                    var.getRequiredType(),
                                    role, env);
            value2 = value2.optimize(env.getConfiguration().getOptimizer(), env, Type.ITEM_TYPE);
            var.setSelectExpression(value2);
            ComputedExpression.setParentExpression(value2, var);
            // the value expression may declare local variables
            SlotManager map = env.getConfiguration().makeSlotManager();
            int slots = ExpressionTool.allocateSlots(value2, 0, map);
            if (slots > 0) {
                ((GlobalVariable)var).setContainsLocals(map);
            }
        }

        if (var.getRequiredType() == SequenceType.ANY_SEQUENCE && !(var instanceof GlobalParam)) {
            // no type was declared; try to deduce a type from the value
            try {
                final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
                final ItemType itemType = value.getItemType(th);
                final int cardinality = value.getCardinality();
                var.setRequiredType(SequenceType.makeSequenceType(itemType, cardinality));
            } catch (Exception err) {
                // exceptions can happen because references to variables and functions are still unbound
            }
        }
    }

    /**
     * Get the compiled variable if the definition has been compiled
     */

    public GlobalVariable getCompiledVariable() {
        return compiledVar;
    }


    /**
     * Produce diagnostic output showing the compiled and optimized expression tree for a function
     * @param config the configuration to be used
     */
    public void explain(Configuration config) {
        NamePool pool = config.getNamePool();
        System.err.println("declare variable " + pool.getDisplayName(nameCode) + " := ");
        if (value != null) {
            value.display(4, System.err, config);
        }
        System.err.println(";");
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
// Contributor(s): none
//