package net.sf.saxon.instruct;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.Validation;
import net.sf.saxon.pattern.CombinedNodeTest;
import net.sf.saxon.pattern.ContentTypeTest;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.io.PrintStream;


/**
* An instruction that creates an element node whose name is known statically.
 * Used for literal results elements in XSLT, for direct element constructors
 * in XQuery, and for xsl:element in cases where the name and namespace are
 * known statically.
*/

public class FixedElement extends ElementCreator {

    private int nameCode;
    protected int[] namespaceCodes = null;
    private ItemType itemType;

    /**
     * Create an instruction that creates a new element node
     * @param nameCode Represents the name of the element node
     * @param namespaceCodes List of namespaces to be added to the element node.
     *                       May be null if none are required.
     * @param inheritNamespaces true if the children of this element are to inherit its namespaces
     * @param schemaType Type annotation for the new element node
     */
    public FixedElement(int nameCode,
                        int[] namespaceCodes,
                        boolean inheritNamespaces,
                        SchemaType schemaType,
                        int validation) {
        this.nameCode = nameCode;
        this.namespaceCodes = namespaceCodes;
        this.inheritNamespaces = inheritNamespaces;
        this.schemaType = schemaType;
        this.validation = validation;
    }

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
        details.setConstructType(Location.LITERAL_RESULT_ELEMENT);
        details.setObjectNameCode(nameCode);
        return details;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        setLazyConstruction(env.getConfiguration().isLazyConstructionMode());
        if (schemaType == null) {
            if (validation == Validation.STRICT) {
                SchemaDeclaration decl = env.getConfiguration().getElementDeclaration(nameCode & 0xfffff);
                if (decl == null) {
                    StaticError err = new StaticError("There is no global element declaration for " +
                            env.getNamePool().getDisplayName(nameCode) +
                            ", so strict validation will fail");
                    err.setIsTypeError(true);
                    err.setLocator(this);
                    throw err;
                }
                schemaType = decl.getType();
                itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT, schemaType, env.getConfiguration()));
            } else if (validation == Validation.LAX) {
                SchemaDeclaration decl = env.getConfiguration().getElementDeclaration(nameCode & 0xfffff);
                if (decl == null) {
                    env.issueWarning("There is no global element declaration for " +
                            env.getNamePool().getDisplayName(nameCode) +
                            ", so lax validation has no effect", this);
                    itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT,
                                BuiltInSchemaFactory.getSchemaType(StandardNames.XDT_UNTYPED),
                                env.getConfiguration()));
                } else {
                    schemaType = decl.getType();
                    itemType = new CombinedNodeTest(
                            new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                            Token.INTERSECT,
                            new ContentTypeTest(Type.ELEMENT, schemaType, env.getConfiguration()));
                }
            } else {
                // we know the result will be an untyped element
                itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT,
                                BuiltInSchemaFactory.getSchemaType(StandardNames.XDT_UNTYPED),
                                env.getConfiguration()));
            }
        } else {
            itemType = new CombinedNodeTest(
                    new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                    Token.INTERSECT,
                    new ContentTypeTest(Type.ELEMENT, schemaType, env.getConfiguration())
            );
        }
        return super.simplify(env);
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (schemaType != null) {
            try {
                schemaType.analyzeContentExpression(content, Type.ELEMENT, env);
            } catch (ValidationException e) {
                if (e.getLocator().getLineNumber() == 1) {
                    e.setLocator(this);
                }
                throw e;
            }
        }
        Expression res = super.analyze(env, contextItemType);
        verifyLazyConstruction();
        return res;
    }

    /**
     * Get the type of the item returned by this instruction
     * @return the item type
     */
    public ItemType getItemType() {
        if (itemType == null) {
            return super.getItemType();
        }
        return itemType;
    }

    /**
     * Callback from the superclass ElementCreator to get the nameCode
     * for the element name
     * @param context The evaluation context (not used)
     * @return the name code for the element name
     */

    public int getNameCode(XPathContext context) {
        return nameCode;
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (parentType instanceof SimpleType) {
            StaticError err = new StaticError("Element " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted here: the containing element is of simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else if (((ComplexType)parentType).isSimpleContent()) {
            StaticError err = new StaticError("Element " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted here: the containing element has a complex type with simple content");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        SchemaType type;
        try {
            type = ((ComplexType)parentType).getElementParticleType(nameCode & 0xfffff);
        } catch (SchemaException e) {
            throw new StaticError(e);
        }
        if (type == null) {
            if (parentType.allowsDerivation(SchemaType.DERIVATION_EXTENSION)) {
                // TODO: identify other cases where this can be a hard compile-time error
                env.issueWarning("Element " + env.getNamePool().getDisplayName(nameCode) +
                        " is not permitted in the content model of the complex type " +
                        parentType.getDescription() +
                        ". Validation will fail unless there is an extended type that permits this element", this);
                return;
            } else {
                StaticError err = new StaticError("Element " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted in the content model of the complex type " +
                    parentType.getDescription());
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
        }
        if (type instanceof AnyType) {
            return;
        }

        try {
            content.checkPermittedContents(type, env, true);
        } catch (XPathException e) {
            if (e.getLocator() == null || e.getLocator() == e) {
                e.setLocator(this);
            }
            throw e;
        }
    }

    /**
     * Callback from the superclass ElementCreator to output the namespace nodes
     * @param context The evaluation context (not used)
     * @param out The receiver to handle the output
     */

    protected void outputNamespaceNodes(XPathContext context, Receiver out)
    throws XPathException {
        if (namespaceCodes != null) {
            for (int i=0; i<namespaceCodes.length; i++) {
                out.namespace(namespaceCodes[i], 0);
            }
        }
    }

    /**
     * Callback to get a list of the intrinsic namespaces that need to be generated for the element.
     * The result is an array of namespace codes, the codes either occupy the whole array or are
     * terminated by a -1 entry. A result of null is equivalent to a zero-length array.
     */

    public int[] getActiveNamespaces() throws XPathException {
        return namespaceCodes;
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "element ");
        out.println(ExpressionTool.indent(level+1) + "name " +
                (pool==null ? nameCode+"" : pool.getDisplayName(nameCode)));
        out.println(ExpressionTool.indent(level+1) + "content");
        content.display(level+1, pool, out);
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
