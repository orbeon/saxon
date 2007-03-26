package org.orbeon.saxon.expr;

import org.orbeon.saxon.instruct.UserFunction;
import org.orbeon.saxon.instruct.UserFunctionParameter;
import org.orbeon.saxon.instruct.SimpleContentConstructor;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the defining occurrence of a variable declared for local use
 * within an expression, for example the $x in "for $x in ...". This object is used
 * only at compile-time. In XQuery (but not in XSLT) this class is also used to represent
 * the formal arguments of a function.
 */

public class RangeVariableDeclaration implements VariableDeclaration {

    private int nameCode;
    private SequenceType requiredType;
    private String variableName;
    private List references = new ArrayList(5);

    /**
     * Set the name of the variable, as a namepool name code
     * @param nameCode
     */

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    /**
     * Get the name of the variable, as a namepool name code
     * @return the nameCode
     */

    public int getNameCode() {
        return nameCode;
    }

    /**
     * Get the required type (declared type) of the variable
     * @return the required type
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * Set the required type (declared type) of the variable
     * @param requiredType the required type
     */
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

    public void setReferenceList(List references) {
        this.references = references;
    }

    public List getReferenceList() {
        return references;
    }

    /**
     * Determine whether all references to this variable are using the value as a string
     */

    public boolean allReferencesAreStrings() {
        for (int i=references.size()-1; i>=0; i--) {
            BindingReference bref = (BindingReference)references.get(i);
            if (bref instanceof VariableReference) {
                VariableReference ref = (VariableReference)bref;
                Container parent = ref.getParentExpression();
                if (parent instanceof SimpleContentConstructor || parent instanceof Atomizer) {
                    // OK, it's a string context
                } else {
                    return false;
                }

            } else {
                // it must be saxon:assign
                return false;
            }
        }
        return true;
    }

    /**
     * Determine how often the range variable is referenced. This is the number of times
     * it is referenced at run-time: so a reference in a loop counts as "many".
     * @param binding the variable binding
     * @param env
     * @return the number of references. The only interesting values are 0, 1, and "many" (represented
     * by any value >1).
     */
    public int getReferenceCount(Binding binding, StaticContext env) {
        return getReferenceCount(references, binding, env, true);
    }

    /**
     * Determine how often a variable is referenced. This is the number of times
     * it is referenced at run-time: so a reference in a loop counts as "many". This code
     * currently handles local variables (Let expressions) and function parameters. It is
     * not currently used for XSLT template parameters. It's not the end of the world if
     * the answer is wrong (unless it's wrongly given as zero), but if wrongly returned as
     * 1 then the variable will be repeatedly evaluated.
     * @param references a list of references to a variable binding: each item in this list
     * must be a VariableReference object
     * @param binding the variable binding
     * @param removeUnbound
     * @return the number of references. The interesting values are 0, 1,  "many" (represented
     * by any value >1), and the special value FILTERED, which indicates that there are
     * multiple references and one or more of them is of the form $x[....] indicating that an
     * index might be useful.
     */

    public static int getReferenceCount(List references, Binding binding, StaticContext env, boolean removeUnbound) {
        if (removeUnbound) {
            // remove any references to the variable that have been inlined
            for (int i=references.size()-1; i>=0; i--) {
                if (((VariableReference)references.get(i)).getBinding() == null) {
                    references.remove(i);
                }
            }
        }
        int rcount = 0;
        for (int i=references.size()-1; i>=0; i--) {
            BindingReference bref = (BindingReference)references.get(i);
            if (bref instanceof VariableReference) {
                VariableReference ref = (VariableReference)bref;
                Expression child = ref;
                Container parent = ref.getParentExpression();
                boolean filtered = (parent instanceof FilterExpression && child == ((FilterExpression)parent).getBaseExpression());
                int count = 0;
                while (true) {

                    if (parent instanceof ComputedExpression) {
                        // If the variable reference occurs in a subexpression that is evaluated repeatedly, for example
                        // in the predicate of a filter expression, then return 10, meaning "multiple references".
                        if (parent == binding) {
                            rcount += 1;
                            break;
                        } else if (filtered && parent instanceof FilterExpression && child == ((FilterExpression)parent).getBaseExpression()) {
                            return FILTERED;    // no need to look any further
                        } else if (ExpressionTool.isRepeatedSubexpression((ComputedExpression)parent, child, env)) {
                            rcount += 10;
                            break;
                        } else  {
                            child = (ComputedExpression)parent;
                            parent = child.getParentExpression();
                            if (parent == null) {
                                // can happen during a tree rewrite: play safe
                                rcount += 10;
                                break;
                            }
                            if (count++ > 10000) {
                                throw new IllegalStateException("The expression tree appears to contain a cycle");
                            }
                        }
                    } else if (parent instanceof UserFunction) {
                        UserFunctionParameter[] params = ((UserFunction)parent).getParameterDefinitions();
                        for (int p=0; p<params.length; p++) {
                            if (params[p] == binding) {
                                int refs = params[p].getReferenceCount();
                                rcount += refs;
                            }
                        }
                        rcount += 10;
                        break;
                    } else {
                        // we should have found the binding by now, but we haven't - so just skip the optimization
                        rcount += 10;
                        break;
                    }
                }
                //rcount += 10;
            } else {
                // it must be a saxon:assign
                rcount++;
            }
        }
        return rcount;
    }

    public static final int FILTERED = 10000;

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
                                      Value constantValue, int properties, StaticContext env) {
        for (Iterator iter=references.iterator(); iter.hasNext();) {
            BindingReference ref = (BindingReference)iter.next();
            if (ref instanceof VariableReference) {
                final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
                ItemType oldItemType = ((VariableReference)ref).getItemType(th);
                ItemType newItemType = oldItemType;
                if (env.getConfiguration().getTypeHierarchy().isSubType(type, oldItemType)) {
                    newItemType = type;
                }
                int newcard = cardinality & ((VariableReference)ref).getCardinality();
                if (newcard==0) {
                    // this will probably lead to a type error later
                    newcard = ((VariableReference)ref).getCardinality();
                }
                SequenceType seqType = SequenceType.makeSequenceType(newItemType, newcard);

                ref.setStaticType(seqType, constantValue, properties);
                ((VariableReference)ref).resetStaticProperties();
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