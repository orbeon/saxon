package net.sf.saxon.query;

import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.om.NamePool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to hold compile-time information about an XQuery global variable
 * or parameter
 */

public class GlobalVariableDefinition implements VariableDeclaration {

    private List references = new ArrayList();
    private SequenceType requiredType;
    private Expression value;
    private int nameCode;
    private boolean isParameter;
    private String variableName;
    private int lineNumber;


    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setValueExpression(Expression val) {
        this.value = val;
    }

    public void setIsParameter(boolean b) {
        isParameter = b;
    }

    public void registerReference(BindingReference ref) {
        references.add(ref);
    }

    public int getNameCode() {
        return nameCode;
    }

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
            RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, variableName, 0);
            Expression value2 = TypeChecker.staticTypeCheck(
                                    value.simplify(env).analyze(env, Type.ITEM_TYPE),
                                    requiredType,
                                    false,
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
                requiredType = new SequenceType(itemType, cardinality);
                var.setRequiredType(requiredType);
            } catch (Exception err) {
                // exceptions can happen because references to variables and functions are still unbound
            }
        }
        //var.setGlobal(true);
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