package org.orbeon.saxon.instruct;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pattern.CombinedNodeTest;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.Configuration;

import java.io.PrintStream;
import java.util.Iterator;


/**
* An instruction that creates an element node whose name is known statically.
 * Used for literal results elements in XSLT, for direct element constructors
 * in XQuery, and for xsl:element in cases where the name and namespace are
 * known statically.
*/

public class FixedElement extends ElementCreator {

    // TODO: create a separate class for the case where schema validation is involved

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
        setSchemaType(schemaType);
        this.validation = validation;
        this.validating = schemaType != null || validation != Validation.PRESERVE;
    }

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
        details.setConstructType(Location.LITERAL_RESULT_ELEMENT);
        details.setObjectNameCode(nameCode);
        return details;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        final Configuration config = env.getConfiguration();
        setLazyConstruction(config.isLazyConstructionMode());
        validating &= config.isSchemaAware(Configuration.XML_SCHEMA);
        itemType = computeFixedElementItemType(this, env, validation, getSchemaType(), nameCode, content);
        return super.simplify(env);
    }

    private ItemType computeFixedElementItemType(FixedElement instr, StaticContext env,
                                            int validation, SchemaType schemaType,
                                            int nameCode, Expression content) throws XPathException {
        final Configuration config = env.getConfiguration();
        ItemType itemType;
        if (schemaType == null) {
            if (validation == Validation.STRICT) {
                SchemaDeclaration decl = config.getElementDeclaration(nameCode & 0xfffff);
                if (decl == null) {
                    StaticError err = new StaticError("There is no global element declaration for " +
                            env.getNamePool().getDisplayName(nameCode) +
                            ", so strict validation will fail");
                    err.setErrorCode(instr.isXSLT() ? "XTTE1512" : "XQDY0027");
                    err.setIsTypeError(true);
                    err.setLocator(instr);
                    throw err;
                }
                schemaType = decl.getType();
                instr.setSchemaType(schemaType);
                itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT, schemaType, config));
                try {
                    schemaType.analyzeContentExpression(content, Type.ELEMENT, env);
                } catch (XPathException e) {
                    e.setErrorCode(instr.isXSLT() ? "XTTE1510" : "XQDY0027");
                    e.setLocator(instr);
                    throw e;
                }
                SchemaType xsiType = instr.getXSIType(env);
                if (xsiType != null) {
                    xsiType.analyzeContentExpression(content, Type.ELEMENT, env);
                    try {
                        config.checkTypeDerivationIsOK(xsiType, schemaType, 0);
                    } catch (SchemaException e) {
                        throw new StaticError("The specified xsi:type " + xsiType.getDescription() +
                                " is not validly derived from the required type " + schemaType.getDescription());
                    }
                }
            } else if (validation == Validation.LAX) {
                SchemaDeclaration decl = config.getElementDeclaration(nameCode & 0xfffff);
                if (decl == null) {
                    env.issueWarning("There is no global element declaration for " +
                            env.getNamePool().getDisplayName(nameCode) +
                            ", so lax validation has no effect", instr);
                    itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT,
                                BuiltInSchemaFactory.getSchemaType(StandardNames.XDT_UNTYPED),
                                config));
                } else {
                    schemaType = decl.getType();
                    instr.setSchemaType(schemaType);
                    itemType = new CombinedNodeTest(
                            new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                            Token.INTERSECT,
                            new ContentTypeTest(Type.ELEMENT, instr.getSchemaType(), config));
                    try {
                        schemaType.analyzeContentExpression(content, Type.ELEMENT, env);
                    } catch (XPathException e) {
                        e.setErrorCode(instr.isXSLT() ? "XTTE1515" : "XQDY0027");
                        e.setLocator(instr);
                        throw e;
                    }
                }
            } else if (validation == Validation.PRESERVE) {
                // we know the result will be an element of type xs:anyType
                itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT,
                                BuiltInSchemaFactory.getSchemaType(StandardNames.XS_ANY_TYPE),
                                config));
            } else {
                // we know the result will be an untyped element
                itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT,
                                BuiltInSchemaFactory.getSchemaType(StandardNames.XDT_UNTYPED),
                                config));
            }
        } else {
            itemType = new CombinedNodeTest(
                    new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                    Token.INTERSECT,
                    new ContentTypeTest(Type.ELEMENT, schemaType, config)
            );
            try {
                schemaType.analyzeContentExpression(content, Type.ELEMENT, env);
            } catch (XPathException e) {
                e.setErrorCode(instr.isXSLT() ? "XTTE1540" : "XQDY0027");
                e.setLocator(instr);
                throw e;
            }
        }
        return itemType;
    }

    /**
     * Get the type of the item returned by this instruction
     * @return the item type
     * @param th
     */
    public ItemType getItemType(TypeHierarchy th) {
        if (itemType == null) {
            return super.getItemType(th);
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

    public String getNewBaseURI(XPathContext context) {
        return getBaseURI();
    }

    /**
     * Determine whether the element constructor creates a fixed xsi:type attribute, and if so, return the
     * relevant type.
     * @return the type denoted by the constructor's xsi:type attribute if there is one.
     * Return null if there is no xsi:type attribute, or if the value of the xsi:type
     * attribute is a type that is not statically known (this is allowed)
     * @throws XPathException if there is an xsi:type attribute and its value is not a QName.
     */

    private SchemaType getXSIType(StaticContext env) throws XPathException {
        if (content instanceof FixedAttribute) {
            return testForXSIType((FixedAttribute)content, env);
        } else if (content instanceof Block) {
            Iterator iter = content.iterateSubExpressions();
            while (iter.hasNext()) {
                Expression exp = (Expression)iter.next();
                if (exp instanceof FixedAttribute) {
                    SchemaType type = testForXSIType((FixedAttribute)exp, env);
                    if (type != null) {
                        return type;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private SchemaType testForXSIType(FixedAttribute fat, StaticContext env) throws XPathException {
        int att = fat.evaluateNameCode(null) & NamePool.FP_MASK;
        if (att == StandardNames.XSI_TYPE) {
            Expression attValue = fat.getSelect();
            if (attValue instanceof StringValue) {
                try {
                    NamePool pool = env.getNamePool();
                    String[] parts = env.getConfiguration().getNameChecker().getQNameParts(
                            ((StringValue)attValue).getStringValue());
                    // The only namespace bindings we can trust are those declared on this element
                    // TODO: we could also trust those on enclosing LREs in the same function/template,
                    int uriCode = -1;
                    for (int n=0; n<namespaceCodes.length; n++) {
                        String prefix = pool.getPrefixFromNamespaceCode(namespaceCodes[n]);
                        if (prefix.equals(parts[0])) {
                            uriCode = namespaceCodes[n] & 0xffff;
                            break;
                        }
                    }
                    if (uriCode == -1) {
                        return null;
                    }
                    String uri = pool.getURIFromURICode((short)uriCode);
                    int typefp = pool.allocate(parts[0], uri, parts[1]) & NamePool.FP_MASK;
                    SchemaType type = env.getConfiguration().getSchemaType(typefp);
                    return type;
                } catch (QNameException e) {
                    throw new StaticError(e.getMessage());
                }
            }
        }
        return null;
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
            StaticError err = new StaticError("Element " + env.getNamePool().getDisplayName(nameCode) +
                " is not permitted in the content model of the complex type " +
                parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            err.setErrorCode(isXSLT() ? "XTTE1510" : "XQDY0027");
            throw err;
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

    public int[] getActiveNamespaces() {
        return namespaceCodes;
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "element ");
        out.println(ExpressionTool.indent(level+1) + "name " +
                (config==null ? nameCode+"" : config.getNamePool().getDisplayName(nameCode)));
        out.println(ExpressionTool.indent(level+1) + "content");
        content.display(level+1, out, config);
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
