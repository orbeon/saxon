package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.NoNodeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.value.Value;

/**
* A SingletonAtomizer combines the functions of an Atomizer and a CardinalityChecker: it is used to
 * atomize a sequence of nodes, checking that the result of the atomization contains zero or one atomic
 * values. Note that the input may be a sequence of nodes or atomic values, even though the result must
 * contain at most one atomic value.
*/

public final class SingletonAtomizer extends UnaryExpression {

    private boolean allowEmpty;
    private RoleLocator role;

    /**
    * Constructor
     * @param sequence the sequence to be atomized
     * @param allowEmpty true if the result sequence is allowed to be empty.
    */

    public SingletonAtomizer(Expression sequence, RoleLocator role, boolean allowEmpty) {
        super(sequence);
        this.allowEmpty = allowEmpty;
        this.role = role;
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (operand instanceof AtomicValue) {
            return operand;
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);
        resetStaticProperties();
        if (operand instanceof EmptySequence) {
            if(!allowEmpty) {
                typeError("An empty sequence is not allowed as the " +
                             role.getMessage(), role.getErrorCode(), null);
            }
            ComputedExpression.setParentExpression(operand, getParentExpression());
            return operand;
        }
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        if (operand.getItemType(th).isAtomicType()) {
            ComputedExpression.setParentExpression(operand, getParentExpression());
            return operand;
        }
        return this;
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
     * Get the RoleLocator (used to construct error messages)
     */

    public RoleLocator getRole() {
        return role;
    }

    /**
    * Evaluate as an Item. This should only be called if the Atomizer has cardinality zero-or-one,
    * which will only be the case if the underlying expression has cardinality zero-or-one.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        int found = 0;
        Item result = null;
        SequenceIterator iter = operand.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof AtomicValue) {
                if (found++ > 0) {
                    typeError(
                        "A sequence of more than one item is not allowed as the " +
                        role.getMessage(), role.getErrorCode(), context);
                }
                result = item;
            } else {
                Value value = ((NodeInfo)item).atomize();
                found += value.getLength();
                if (found > 1) {
                     typeError(
                        "A sequence of more than one item is not allowed as the " +
                        role.getMessage(), role.getErrorCode(), context);
                }
                result = value.itemAt(0);
            }
        }
        if (found == 0 && !allowEmpty) {
            typeError("An empty sequence is not allowed as the " +
                             role.getMessage(), role.getErrorCode(), null);
        }
        return result;
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER. For this class, the
     * result is always an atomic type, but it might be more specific.
     * @param th
     */

	public ItemType getItemType(TypeHierarchy th) {
        boolean isSchemaAware = true;
        Executable exec = getExecutable();
        if (exec != null && !exec.getConfiguration().isSchemaAware(Configuration.XML_SCHEMA)) {
            isSchemaAware = false;
        }
        ItemType in = operand.getItemType(th);
        if (in.isAtomicType()) {
            return in;
        }
        if (in instanceof NodeTest) {

            if (in instanceof NoNodeTest) {
                return in;
            }

            int kinds = ((NodeTest)in).getNodeKindMask();
            if (!isSchemaAware) {
                // Some node-kinds always have a typed value that's a string

                if ((kinds | STRING_KINDS) == STRING_KINDS) {
                    return Type.STRING_TYPE;
                }
                // Some node-kinds are always untyped atomic; some are untypedAtomic provided that the configuration
                // is untyped

                if ((kinds | UNTYPED_IF_UNTYPED_KINDS) == UNTYPED_IF_UNTYPED_KINDS) {
                    return Type.UNTYPED_ATOMIC_TYPE;
                }
            } else {
                if ((kinds | UNTYPED_KINDS) == UNTYPED_KINDS) {
                    return Type.UNTYPED_ATOMIC_TYPE;
                }
            }

            return in.getAtomizedItemType();

//            SchemaType schemaType = ((NodeTest)in).getContentType();
//            if (schemaType instanceof SimpleType) {
//                return ((SimpleType)schemaType).getCommonAtomicType();
//            } else if (((ComplexType)schemaType).isSimpleContent()) {
//                return ((ComplexType)schemaType).getSimpleContentType().getCommonAtomicType();
//            } else if (schemaType instanceof AnyType) {
//                // AnyType includes AnySimpleType as a subtype, so the atomized value can be any atomic type
//                // including untypedAtomic
//                return Type.ANY_ATOMIC_TYPE;
//            } else {
//                // if a complex type with complex content (other than AnyType) can be atomized at all,
//                // then it will return untypedAtomic values
//                return Type.UNTYPED_ATOMIC_TYPE;
//            }
        }
	    return Type.ANY_ATOMIC_TYPE;
	}

    /**
     * Node kinds whose typed value is always a string
     */
    private static final int STRING_KINDS =
            (1<<Type.NAMESPACE) | (1<<Type.COMMENT) | (1<<Type.PROCESSING_INSTRUCTION);

    /**
     * Node kinds whose typed value is always untypedAtomic
     */

    private static final int UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.DOCUMENT);

    /**
     * Node kinds whose typed value is untypedAtomic if the configuration is untyped
     */

    private static final int UNTYPED_IF_UNTYPED_KINDS =
            (1<<Type.TEXT) | (1<<Type.ELEMENT) | (1<<Type.DOCUMENT) | (1<<Type.ATTRIBUTE);

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        if (allowEmpty) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
	}

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        return "atomize singleton";
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
// Contributor(s): none.
//
