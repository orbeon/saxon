package net.sf.saxon.query;

import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.GeneralVariable;
import net.sf.saxon.instruct.GlobalParam;
import net.sf.saxon.instruct.GlobalVariable;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to hold compile-time information about an XQuery global variable
 * or parameter
 */

public class GlobalVariableDefinition implements VariableDeclaration {

    private List references = new ArrayList(10);
    private SequenceType requiredType;
    private Expression value;
    private int nameCode;
    private boolean isParameter;
    private String variableName;
    private int lineNumber;

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
     * Create a compiled representation of this global variable
     * @param env the static context for the query module
     * @param slot the slot number allocated to this variable
     * @return the compiled representation
     * @throws XPathException if compile-time errors are found.
     */

    public GeneralVariable compile(StaticQueryContext env, int slot) throws XPathException {

        GeneralVariable var;
        if (isParameter) {
            var = new GlobalParam();
            ((GlobalParam)var).setExecutable(env.getExecutable());
            var.setRequiredParam(value==null);
        } else {
            var = new GlobalVariable();
            ((GlobalVariable)var).setExecutable(env.getExecutable());
        }
        if (value != null) {
            RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, variableName, 0, null);
            Expression value2 = TypeChecker.strictTypeCheck(
                                    value.simplify(env).analyze(env, Type.ITEM_TYPE),
                                    requiredType,
                                    role, env);
            var.setSelectExpression(value2);
            if (value2 instanceof ComputedExpression) {
                ((ComputedExpression)value2).setParentExpression(var);
            }
            // the value expression may declare local variables
            SlotManager map = env.getConfiguration().makeSlotManager();
            int slots = ExpressionTool.allocateSlots(value2, 0, map);
            if (slots > 0) {
                //env.allocateLocalSlots(slots);
                ((GlobalVariable)var).setContainsLocals(map);
            }
        }
        var.setNameCode(nameCode);
        var.setRequiredType(requiredType);
        if (requiredType == SequenceType.ANY_SEQUENCE && !isParameter) {
            // no type was declared; try to deduce a type from the value
            try {
                ItemType itemType = value.getItemType();
                int cardinality = value.getCardinality();
                requiredType = SequenceType.makeSequenceType(itemType, cardinality);
                var.setRequiredType(requiredType);
            } catch (Exception err) {
                // exceptions can happen because references to variables and functions are still unbound
            }
        }
        var.setVariableName(variableName);
        var.setSlotNumber(slot);

        int loc = var.getExecutable().getLocationMap().allocateLocationId(env.getSystemId(), lineNumber);
        var.setLocationId(loc);

        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            BindingReference binding = (BindingReference)iter.next();
            binding.setStaticType(requiredType, null, 0);
            binding.fixup(var);
        }

        return var;
    }

    /**
     * Produce diagnostic output showing the compiled and optimized expression tree for a function
     * @param pool the namepool to be used
     */
    public void explain(NamePool pool) {
        System.err.println("declare variable " + pool.getDisplayName(nameCode) + " := ");
        value.display(4, pool, System.err);
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