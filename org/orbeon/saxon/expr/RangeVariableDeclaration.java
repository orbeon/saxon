package org.orbeon.saxon.expr;

import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the defining occurrence of a variable declared for local use
 * within an expression, for example the $x in "for $x in ...". This object is used
 * only at compile-time.
 */

public class RangeVariableDeclaration implements VariableDeclaration {

    private int nameCode;
    private SequenceType requiredType;
    private String variableName;
    private List references = new ArrayList();


    public void setNameCode(int fingerprint) {
        this.nameCode = fingerprint;
    }

    public int getNameCode() {
        return nameCode;
    }

    public SequenceType getRequiredType() {
        return requiredType;
    }

    public void setRequiredType(SequenceType requiredType) {
        this.requiredType = requiredType;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    public void registerReference(BindingReference ref) {
        references.add(ref);
    }

    public void fixupReferences(Binding binding) {
        for (Iterator iter=references.iterator(); iter.hasNext();) {
            BindingReference ref = (BindingReference)iter.next();
            ref.setStaticType(requiredType, null, 0);
                   // we supply the properties of the expression (3rd argument) later
                   // in the call of refineTypeInformation
            ref.fixup(binding);
        }
    }

    public void refineTypeInformation(ItemType type, int cardinality,
                                      Value constantValue, int properties) {
        for (Iterator iter=references.iterator(); iter.hasNext();) {
            BindingReference ref = (BindingReference)iter.next();
            if (ref instanceof VariableReference) {
                ItemType oldItemType = ((VariableReference)ref).getItemType();
                ItemType newItemType = oldItemType;
                if (Type.isSubType(type, oldItemType)) {
                    newItemType = type;
                }
                int newcard = cardinality & ((VariableReference)ref).getCardinality();
                if (newcard==0) {
                    // this will probably lead to a type error later
                    newcard = ((VariableReference)ref).getCardinality();
                }
                SequenceType seqType =
                        new SequenceType(newItemType, newcard);

                ref.setStaticType(seqType, constantValue, properties);
            }
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