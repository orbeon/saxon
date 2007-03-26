package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.instruct.LocalParam;
import org.orbeon.saxon.instruct.UserFunctionParameter;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.SingletonNode;
import org.orbeon.saxon.value.Value;

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

    public VariableReference() {}

    /**
     * Constructor
     *
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
     * Create a clone copy of this VariableReference
     */

    public VariableReference copy() {
        VariableReference ref = new VariableReference();
        ref.binding = binding;
        ref.staticType = staticType;
        ref.constantValue = constantValue;
        ref.displayName = displayName;
        return ref;
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
        staticProperties = (properties & ~StaticProperty.CONTEXT_DOCUMENT_NODESET) |
                type.getCardinality() |
                getDependencies();
    }

    /**
     * Type-check the expression. At this stage details of the static type must be known.
     * If the variable has a compile-time value, this is substituted for the variable reference
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        if (constantValue != null) {
            binding = null;
            return constantValue;
        }
        if (staticType == null) {
            throw new IllegalStateException("Variable $" + displayName + " has not been fixed up");
        } else {
            return this;
        }
    }

    /**
     * Type-check the expression. At this stage details of the static type must be known.
     * If the variable has a compile-time value, this is substituted for the variable reference
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        if (constantValue != null) {
            binding = null;
            return constantValue;
        }

        return this;
    }

    /**
     * Fix up this variable reference to a Binding object, which enables the value of the variable
     * to be located at run-time.
     */

    public void fixup(Binding binding) {
        // System.err.println("Binding for " + this + " is " + binding.getVariableName());
        this.binding = binding;
        resetStaticProperties();
    }

    /**
     * Replace this VariableReference where appropriate by a more efficient implementation. This
     * can only be done after all slot numbers are allocated. The efficiency is gained by binding the
     * VariableReference directly to a local or global slot, rather than going via the Binding object
     */

    public void refineVariableReference() {
        if (binding instanceof Assignation ||
                binding instanceof LocalParam || binding instanceof UserFunctionParameter) {
            // A LocalVariableReference can be evaluated directly, without going via the Binding object.
            int slot = binding.getLocalSlotNumber();
            if (slot < 0) {
                // if slots haven't been allocated yet, we've come here too early.
                // See test group036 with -T option for an example.
                return;
            }
            LocalVariableReference ref = new LocalVariableReference(slot);
            ref.binding = binding;
            ref.staticType = staticType;
            ref.displayName = displayName;
            ref.setParentExpression(getParentExpression());
            ref.setLocationId(getLocationId());
//            if (getParentExpression() == null) {
//                System.err.println("Problem!");
//            }
            boolean found = getParentExpression().replaceSubExpression(this, ref);
            if (!found) {
                throw new IllegalStateException("Child expression not found in parent");
            }
        }
    }

    /**
     * Determine the data type of the expression, if possible
     *
     * @param th
     * @return the type of the variable, if this can be determined statically;
     *         otherwise Type.ITEM (meaning not known in advance)
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (staticType == null) {
            return AnyItemType.getInstance();
        } else {
            return staticType.getPrimaryType();
        }
    }

    /**
     * Get the static cardinality
     */

    public int computeCardinality() {
        if (staticType == null) {
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        } else {
            return staticType.getCardinality();
        }
    }

    /**
     * Determine the special properties of this expression
     *
     * @return {@link StaticProperty#NON_CREATIVE} (unless the variable is assignable using saxon:assign)
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (binding == null || !binding.isAssignable()) {
            // if the variable reference is assignable, we mustn't move it, or any expression that contains it,
            // out of a loop. The way to achieve this is to treat it as a "creative" expression, because the
            // optimizer recognizes such expressions and handles them with care...
            p |= StaticProperty.NON_CREATIVE;
        }
        return p;
    }

    /**
     * Test if this expression is the same as another expression.
     * (Note, we only compare expressions that
     * have the same static and dynamic context).
     */

    public boolean equals(Object other) {
        return (other instanceof VariableReference &&
                binding == ((VariableReference) other).binding &&
                binding != null);
    }

    /**
     * get HashCode for comparing two expressions
     */

    public int hashCode() {
        return binding == null ? 73619830 : binding.hashCode();
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
                //binding = null;
                offer.accepted = true;
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
     *
     * @param c the XPathContext which contains the relevant variable bindings
     * @return the value of the variable, if it is defined
     * @throws XPathException if the variable is undefined
     */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        try {
            ValueRepresentation actual = evaluateVariable(c);
            return Value.getIterator(actual);
        } catch (XPathException err) {
            if (err.getLocator() == null || err.getLocator().getLineNumber() == -1) {
                err.setLocator(this);
            }
            throw err;
        }
    }

    public Item evaluateItem(XPathContext c) throws XPathException {
        try {
            ValueRepresentation actual = evaluateVariable(c);
            if (actual instanceof Item) {
                return (Item) actual;
            }
            return Value.asItem(actual);
        } catch (XPathException err) {
            if (err.getLocator() == null || err.getLocator().getLineNumber() == -1) {
                err.setLocator(this);
            }
            throw err;
        }
    }

    public void process(XPathContext c) throws XPathException {
        try {
            ValueRepresentation actual = evaluateVariable(c);
            if (actual instanceof NodeInfo) {
                actual = new SingletonNode((NodeInfo) actual);
            }
            ((Value) actual).process(c);
        } catch (XPathException err) {
            if (err.getLocator() == null || err.getLocator().getLineNumber() == -1) {
                err.setLocator(this);
            }
            throw err;
        }
    }

    public ValueRepresentation evaluateVariable(XPathContext c) throws XPathException {
//        if (binding == null) {
//            throw new IllegalStateException("Variable $" + displayName + " has not been fixed up");
//        }
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

    public void display(int level, PrintStream out, Configuration config) {
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
