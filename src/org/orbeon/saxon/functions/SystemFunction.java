package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;


/**
* Abstract superclass for system-defined and user-defined functions
*/

public abstract class SystemFunction extends FunctionCall {

    /**
     * Make a system function call (one in the standard function namespace).
     * @param name The local name of the function.
     * @param arguments the arguments to the function call
     * @return a FunctionCall that implements this function, if it
     * exists, or null if the function is unknown.
     */

    public static FunctionCall makeSystemFunction(String name, Expression[] arguments) {
        StandardFunction.Entry entry = StandardFunction.getFunction(name, arguments.length);
        if (entry==null) {
            return null;
        }
        Class functionClass = entry.implementationClass;
        try {
            SystemFunction f = (SystemFunction)functionClass.newInstance();
            f.setDetails(entry);
            f.setFunctionName(new StructuredQName("", NamespaceConstant.FN, name));
            f.setArguments(arguments);
            return f;
        } catch (IllegalAccessException err) {
            return null;
        } catch (InstantiationException err) {
            return null;
        }
    }


    private StandardFunction.Entry details;
    protected int operation;

    /**
     * Set the details of this type of function
     * @param entry information giving details of the function signature
    */

    public void setDetails(StandardFunction.Entry entry) {
        details = entry;
        operation = details.opcode;
    }

    /**
     * Get the details of the function signature
     * @return information about the function signature
    */

    public StandardFunction.Entry getDetails() {
        return details;
    }

    /**
    * Method called during static type checking
    */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        checkArgumentCount(details.minArguments, details.maxArguments, visitor);
        for (int i=0; i<argument.length; i++) {
            checkArgument(i, visitor);
        }
    }

    /**
     * Perform static type checking on an argument to a function call, and add
     * type conversion logic where necessary.
     * @param arg argument number, zero-based
     * @param visitor an expression visitor
    */

    private void checkArgument(int arg, ExpressionVisitor visitor) throws XPathException {
        RoleLocator role = new RoleLocator(RoleLocator.FUNCTION,
                getFunctionName(), arg);
        //role.setSourceLocator(this);
        role.setErrorCode(getErrorCodeForTypeErrors());
        argument[arg] = TypeChecker.staticTypeCheck(
                                argument[arg],
                                getRequiredType(arg),
                                visitor.getStaticContext().isInBackwardsCompatibleMode(),
                                role, visitor);
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression sf = super.optimize(visitor, contextItemType);
        if (sf == this && argument.length <= details.resultIfEmpty.length) {
                    // the condition eliminates concat, which is a special case.
            for (int i=0; i<argument.length; i++) {
                if (Literal.isEmptySequence(argument[i]) && details.resultIfEmpty[i] != null) {
                    return Literal.makeLiteral(details.resultIfEmpty[i]);
                }
            }
        }
        return sf;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Expression[] a2 = new Expression[argument.length];
        for (int i=0; i<argument.length; i++) {
            a2[i] = argument[i].copy();
        }
        Expression e2 = SystemFunction.makeSystemFunction(details.name, a2);
        if (e2 == null) {
            throw new UnsupportedOperationException("copy");
        }
        return e2;
    }

    /**
     * Return the error code to be used for type errors. This is overridden for functions
     * such as exactly-one(), one-or-more(), ...
     * @return the error code to be used for type errors in the function call. Normally XPTY0004,
     * but different codes are used for functions such as exactly-one()
     */

    public String getErrorCodeForTypeErrors() {
        return "XPTY0004";
    }

    /**
     * Get the required type of the nth argument
     * @param arg the number of the argument whose type is requested, zero-based
     * @return the required type of the argument as defined in the function signature
    */

    protected SequenceType getRequiredType(int arg) {
        if (details == null) {
            return SequenceType.ANY_SEQUENCE;
        }
        return details.argumentTypes[arg];
        // this is overridden for concat()
    }

    /**
    * Determine the item type of the value returned by the function
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (details == null) {
            // probably an unresolved function call
            return AnyItemType.getInstance();
        }
        ItemType type = details.itemType;
        if (type == StandardFunction.SAME_AS_FIRST_ARGUMENT) {
            if (argument.length > 0) {
                return argument[0].getItemType(th);
            } else {
                return AnyItemType.getInstance();
                // if there is no first argument, an error will be reported
            }
        } else {
            return type;
        }
    }

    /**
    * Determine the cardinality of the function.
    */

    public int computeCardinality() {
        if (details==null) {
            //System.err.println("**** No details for " + getClass() + " at " + this);
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
        return details.cardinality;
    }

    /**
     * Determine the special properties of this expression. The general rule
     * is that a system function call is non-creative if its return type is
     * atomic, or if all its arguments are non-creative. This is overridden
     * for the generate-id() function, which is considered creative if
     * its operand is creative (because the result depends on the
     * identity of the operand)
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (details == null) {
            return p;
        }
        if (details.itemType.isAtomicType()) {
            return p | StaticProperty.NON_CREATIVE;
        }
        for (int i=0; i<argument.length; i++) {
            if ((argument[i].getSpecialProperties() & StaticProperty.NON_CREATIVE) == 0) {
                // the argument is creative
                return p;
            }
        }
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Set "." as the default value for the first and only argument. Called from subclasses.
    */

    protected final void useContextItemAsDefault() {
        if (argument.length==0) {
            argument = new Expression[1];
            argument[0] = new ContextItemExpression();
            ExpressionTool.copyLocationInfo(this, argument[0]);
            resetLocalStaticProperties();
        }
        // Note that the extra argument is added before type-checking takes place. The
        // type-checking will add any necessary checks to ensure that the context item
        // is a node, in cases where this is required.
    }

    /**
    * Add an implicit argument referring to the context document. Called by functions such as
    * id() and key() that take the context document as an implicit argument
     * @param pos the position of the argument whose default value is ".", zero-based
     * @param augmentedName the name to be used for the function call with its extra argument.
     * There are some cases where user function calls cannot supply the argument directly (notably
     * unparsed-entity-uri() and unparsed-entity-public-id()) and in these cases a synthesized
     * function name is used for the new function call.
    */

    protected final void addContextDocumentArgument(int pos, String augmentedName)
    throws XPathException {
        if (argument.length > pos) {
            return;
            // this can happen during optimization, if the extra argument is already present
        }
        if (argument.length != pos) {
            throw new XPathException("Too few arguments in call to " + augmentedName + "() function");
        }
        Expression[] newArgs = new Expression[pos+1];
        System.arraycopy(argument, 0, newArgs, 0, argument.length);
        RootExpression rootExpression = new RootExpression();
        ExpressionTool.copyLocationInfo(this, rootExpression);
        newArgs[pos] = rootExpression;
        argument = newArgs;
        setDetails(StandardFunction.getFunction(augmentedName, newArgs.length));
    }

    /**
     * Add a representation of a doc() call or similar function to a PathMap.
     * This is a convenience method called by the addToPathMap() methods for doc(), document(), collection()
     * and similar functions. These all create a new root expression in the path map.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodes the node in the PathMap representing the focus at the point where this expression
     *                    is called. Set to null if this expression appears at the top level.
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addDocToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodes) {
        argument[0].addToPathMap(pathMap, pathMapNodes);
        return new PathMap.PathMapNodeSet(pathMap.makeNewRoot(this));
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
