package net.sf.saxon.expr;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.SingletonNode;

import java.io.PrintStream;

/**
* Variable reference: a reference to a variable. This may be an XSLT-defined variable, a range
* variable defined within the XPath expression, or a variable defined in some other static context.
*/

public class VariableReference extends ComputedExpression implements BindingReference {

    Binding binding = null;     // This will be null until fixup() is called; it will also be null
                                // if the variable reference has been inlined
    SequenceType staticType = null;
    Value constantValue = null;
    transient String displayName = null;

    /**
    * Constructor
    * @param declaration the variable declaration to which this variable refers
    */

    public VariableReference(VariableDeclaration declaration) {

        // Register this variable reference with the variable declaration. When the variable declaration
        // is compiled, the declaration will call the fixup() method of the variable reference. Note
        // that the object does not retain a pointer to the variable declaration, which would cause the
        // stylesheet to be locked in memory.

        // System.err.println("Register reference " + this + " with declaration " + declaration + " name=" + declaration.getVariableName());
        declaration.registerReference(this);
        displayName = declaration.getVariableName();
    }

    /**
    * Simplify the expression. Does nothing.
    */

    public Expression simplify(StaticContext env) {
        return this;
    }

    /**
    * Set static type. This is a callback from the variable declaration object. As well
    * as supplying the static type, it may also supply a compile-time value for the variable.
    * As well as the type information, other static properties of the value are supplied:
    * for example, whether the value is an ordered node-set.
    */

    public void setStaticType(SequenceType type, Value value, int properties) {
        // System.err.println(this + " Set static type = " + type);
        staticType = type;
        constantValue = value;
            // Although the variable may be a context document node-set at the point it is defined,
            // the context at the point of use may be different, so this property cannot be transferred.
        staticProperties = (properties &~StaticProperty.CONTEXT_DOCUMENT_NODESET) |
                type.getCardinality() |
                getDependencies();
    }

    /**
    * Type-check the expression. At this stage details of the static type must be known.
    * If the variable has a compile-time value, this is substituted for the variable reference
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (constantValue != null) {
            binding = null;
            return constantValue;
        }
        if (staticType==null) {
            throw new IllegalStateException("Variable $" + displayName + " has not been fixed up");
        } else {
            return this;
        }
    }

    /**
    * Fix up this variable reference to a Binding object, which enables the value of the variable
    * to be located at run-time.
    */

    public void fixup(Binding binding) {
        // System.err.println("Binding for " + this + " is " + binding.getVariableName());
        this.binding = binding;
    }

    /**
    * Determine the data type of the expression, if possible
    * @return the type of the variable, if this can be determined statically;
    * otherwise Type.ITEM (meaning not known in advance)
    */

    public ItemType getItemType() {
        if (staticType==null) {
            return AnyItemType.getInstance();
        } else {
            return staticType.getPrimaryType();
        }
    }

    /**
    * Get the static cardinality
    */

    public int computeCardinality() {
        if (staticType==null) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return staticType.getCardinality();
        }
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Test if this expression is the same as another expression.
    * (Note, we only compare expressions that
    * have the same static and dynamic context).
    */

    public boolean equals(Object other) {
        return (other instanceof VariableReference &&
                binding == ((VariableReference)other).binding &&
                binding != null);
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        return binding.hashCode();
    }


    public int getIntrinsicDependencies() {
        if (binding == null || !binding.isGlobal()) {
            return StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
        } else {
            return 0;
        }
    }

    /**
    * Promote this expression if possible
    */

    public Expression promote(PromotionOffer offer) throws XPathException {
        if (offer.action == PromotionOffer.INLINE_VARIABLE_REFERENCES) {
            Expression exp = offer.accept(this);
            if (exp != null) {
                // Replace the variable reference with the given expression.
                binding = null;
                return exp;
            }
        }
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both all three methods
     * natively.
     */

    public int getImplementationMethod() {
        return EVALUATE_METHOD | ITERATE_METHOD | PROCESS_METHOD;
    }

    /**
    * Get the value of this variable in a given context.
    * @param c the XPathContext which contains the relevant variable bindings
    * @return the value of the variable, if it is defined
    * @throws XPathException if the variable is undefined
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        ValueRepresentation actual = evaluateVariable(c);
        return Value.getIterator(actual);
    }

    public Item evaluateItem(XPathContext c) throws XPathException {
        ValueRepresentation actual = evaluateVariable(c);
        if (actual instanceof Item) {
            return (Item)actual;
        }
        return Value.asItem((Value)actual, c);
    }

    public void process(XPathContext c) throws XPathException {
        ValueRepresentation actual = evaluateVariable(c);
        if (actual instanceof NodeInfo) {
            actual = new SingletonNode((NodeInfo)actual);
        }
        ((Value)actual).process(c);
    }

    public ValueRepresentation evaluateVariable(XPathContext c) throws XPathException {

        if (binding==null) {
            // System.err.println("No binding for " + this);
            throw new IllegalStateException("Variable $" + displayName + " has not been fixed up");
        }

        return binding.evaluateVariable(c);
    }

    /**
    * Get the object bound to the variable
    */

    public Binding getBinding() {
        return binding;
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        if (displayName != null) {
            out.println(ExpressionTool.indent(level) + '$' + displayName);
        } else {
            out.println(ExpressionTool.indent(level) + "$(unbound variable)");
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
