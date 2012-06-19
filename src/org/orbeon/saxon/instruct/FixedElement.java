package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pattern.CombinedNodeTest;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.xml.sax.Locator;

import java.util.Iterator;


/**
* An instruction that creates an element node whose name is known statically.
 * Used for literal results elements in XSLT, for direct element constructors
 * in XQuery, and for xsl:element in cases where the name and namespace are
 * known statically.
*/

public class FixedElement extends ElementCreator {

    // TODO: create a separate class for the case where schema validation is involved

    // TODO: if the sequence of child elements is statically known (e.g. if the content is a Block consisting
    // entirely of FixedElement instructions) then validate against the schema content model at compile time.

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
     * @param validation Validation mode to be applied, for example STRICT, LAX, SKIP
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
        preservingTypes = schemaType == null && validation == Validation.PRESERVE;
    }

//    public InstructionInfo getInstructionInfo() {
//        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
//        details.setConstructType(Location.LITERAL_RESULT_ELEMENT);
//        details.setObjectNameCode(nameCode);
//        return details;
//    }

    /**
     * Simplify an expression. This performs any context-independent rewriting
     * @param visitor the expression visitor
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        final Configuration config = visitor.getConfiguration();
        setLazyConstruction(config.isLazyConstructionMode());
        preservingTypes |= !config.isSchemaAware(Configuration.XML_SCHEMA);
        int val = validation;
        SchemaType type = getSchemaType();
        itemType = computeFixedElementItemType(this, visitor.getStaticContext(),
                val, type, nameCode, content);
        return super.simplify(visitor);
    }


    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        // Remove any unnecessary creation of namespace nodes by child literal result elements.
        // Specifically, if this instruction creates a namespace node, then a child literal result element
        // doesn't need to create the same namespace if all the following conditions are true:
        // (a) the child element is in the same namespace as its parent, and
        // (b) this element doesn't specify xsl:inherit-namespaces="no"
        // (c) the child element is incapable of creating attributes in a non-null namespace

        if (!inheritNamespaces) {
            return this;
        }
        if (namespaceCodes == null || namespaceCodes.length == 0) {
            return this;
        }
        NamePool pool = visitor.getExecutable().getConfiguration().getNamePool();
        int uriCode = getURICode(pool);
        if (content instanceof FixedElement) {
            if (((FixedElement)content).getURICode(pool) == uriCode) {
                ((FixedElement)content).removeRedundantNamespaces(visitor, namespaceCodes);
            }
            return this;
        }
        if (content instanceof Block) {
            Iterator iter = content.iterateSubExpressions();
            while (iter.hasNext()) {
                Expression exp = (Expression)iter.next();
                if (exp instanceof FixedElement && ((FixedElement)exp).getURICode(pool) == uriCode) {
                    ((FixedElement)exp).removeRedundantNamespaces(visitor, namespaceCodes);
                }
            }
        }
        return this;
    }

    /**
     * Remove namespaces that are not required for this element because they are output on
     * the parent element
     * @param visitor the expression visitor
     * @param parentNamespaces the namespaces that are output by the parent element
     */

    private void removeRedundantNamespaces(ExpressionVisitor visitor, int[] parentNamespaces) {
        // It's only safe to remove any namespaces if the element is incapable of creating any attribute nodes
        // in a non-null namespace
        // This is because namespaces created on this element take precedence over namespaces created by namespace
        // fixup based on the prefix used in the attribute name (see atrs24)
        if (namespaceCodes == null || namespaceCodes.length == 0) {
            return;
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        ItemType contentType = content.getItemType(th);
        boolean ok = th.relationship(contentType, NodeKindTest.ATTRIBUTE) == TypeHierarchy.DISJOINT;
        if (!ok) {
            // if the content might include attributes, discount any that are known to be in the null namespace
            if (content instanceof Block) {
                ok = true;
                Iterator iter = content.iterateSubExpressions();
                while (iter.hasNext()) {
                    Expression exp = (Expression)iter.next();
                    if (exp instanceof FixedAttribute) {
                        int attNameCode = ((FixedAttribute)exp).getAttributeNameCode();
                        if (NamePool.getPrefixIndex(attNameCode) != 0) {
                            ok = false;
                            break;
                        }
                    } else {
                        ItemType childType = exp.getItemType(th);
                        if (th.relationship(childType, NodeKindTest.ATTRIBUTE) != TypeHierarchy.DISJOINT) {
                            ok = false;
                            break;
                        }
                    }
                }
            }
        }
        if (ok) {
            int removed = 0;
            for (int i=0; i<namespaceCodes.length; i++) {
                for (int j=0; j<parentNamespaces.length; j++) {
                    if (namespaceCodes[i] == parentNamespaces[j]) {
                        namespaceCodes[i] = -1;
                        removed++;
                        break;
                    }
                }
            }
            if (removed > 0) {
                if (removed == namespaceCodes.length) {
                    namespaceCodes = null;
                } else {
                    int[] ns2 = new int[namespaceCodes.length - removed];
                    int j=0;
                    for (int i=0; i<namespaceCodes.length; i++) {
                        if (namespaceCodes[i] != -1) {
                            ns2[j++] = namespaceCodes[i];
                        }
                    }
                    namespaceCodes = ns2;
                }
            }
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        FixedElement fe = new FixedElement(nameCode, namespaceCodes, inheritNamespaces, getSchemaType(), validation);
        fe.setContentExpression(content.copy());
        fe.setBaseURI(getBaseURI());
        return fe;
    }

    /**
     * Get the URI code representing the namespace URI of the element being constructed
     * @param pool the NamePool
     * @return the URI code
     */

    public short getURICode(NamePool pool) {
        return pool.getURICode(nameCode);
    }

    /**
     * Determine the item type of an element being constructed
     * @param instr the FixedElement instruction
     * @param env the static context
     * @param validation the schema validation mode
     * @param schemaType the schema type for validation
     * @param nameCode the name of the element
     * @param content the expression that computes the content of the element
     * @return the item type
     * @throws XPathException
     */

    private ItemType computeFixedElementItemType(FixedElement instr, StaticContext env,
                                            int validation, SchemaType schemaType,
                                            int nameCode, Expression content) throws XPathException {
        final Configuration config = env.getConfiguration();
        ItemType itemType;
        boolean typeBasedValidation = (schemaType != null);
        if (schemaType == null) {
            if (validation == Validation.STRICT) {
                SchemaDeclaration decl = config.getElementDeclaration(nameCode & 0xfffff);
                if (decl == null) {
                    XPathException err = new XPathException("There is no global element declaration for " +
                            env.getNamePool().getDisplayName(nameCode) +
                            ", so strict validation will fail");
                    err.setErrorCode(instr.isXSLT() ? "XTTE1512" : "XQDY0027");
                    err.setIsTypeError(true);
                    err.setLocator(instr);
                    throw err;
                }
                if (decl.isAbstract()) {
                    XPathException err = new XPathException("The element declaration for " +
                            env.getNamePool().getDisplayName(nameCode) +
                            " is abstract, so strict validation will fail");
                    err.setErrorCode(instr.isXSLT() ? "XTTE1512" : "XQDY0027");
                    err.setIsTypeError(true);
                    err.setLocator(instr);
                    throw err;
                }
                schemaType = decl.getType();
                instr.setSchemaType(schemaType);
                    // TODO: this causes validation against the type, rather than the declaration:
                    // are identity constraints being tested on the top-level element?
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
                        ValidationException ve = new ValidationException("The specified xsi:type " + xsiType.getDescription() +
                                " is not validly derived from the required type " + schemaType.getDescription());
                        ve.setConstraintReference(1, "cvc-elt", "4.3");
                        ve.setErrorCode(instr.isXSLT() ? "XTTE1515" : "XQDY0027");
                        ve.setLocator((Locator)instr);
                        throw ve;
                    }
                }
            } else if (validation == Validation.LAX) {
                SchemaDeclaration decl = config.getElementDeclaration(nameCode & 0xfffff);
                if (decl == null) {
                    env.issueWarning("There is no global element declaration for " +
                            env.getNamePool().getDisplayName(nameCode), instr);
                    itemType = new NameTest(Type.ELEMENT, nameCode, env.getNamePool());
//                    itemType = new CombinedNodeTest(
//                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
//                        Token.INTERSECT,
//                        new ContentTypeTest(Type.ELEMENT, Untyped.getInstance(), config));
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
                        new ContentTypeTest(Type.ELEMENT, AnyType.getInstance(), config));
            } else {
                // we know the result will be an untyped element
                itemType = new CombinedNodeTest(
                        new NameTest(Type.ELEMENT, nameCode, env.getNamePool()),
                        Token.INTERSECT,
                        new ContentTypeTest(Type.ELEMENT, Untyped.getInstance(), config));
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
     * @param th The type hierarchy cache
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
     * @param env the static context
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
        int att = fat.getAttributeNameCode() & NamePool.FP_MASK;
        if (att == StandardNames.XSI_TYPE) {
            Expression attValue = fat.getSelect();
            if (attValue instanceof StringLiteral) {
                try {
                    NamePool pool = env.getNamePool();
                    String[] parts = env.getConfiguration().getNameChecker().getQNameParts(
                            ((StringLiteral)attValue).getStringValue());
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
                    return env.getConfiguration().getSchemaType(typefp);
                } catch (QNameException e) {
                    throw new XPathException(e.getMessage());
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
            XPathException err = new XPathException("Element " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted here: the containing element is of simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else if (((ComplexType)parentType).isSimpleContent()) {
            XPathException err = new XPathException("Element " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted here: the containing element has a complex type with simple content");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        SchemaType type;
        try {
            type = ((ComplexType)parentType).getElementParticleType(nameCode & 0xfffff, true);
        } catch (SchemaException e) {
            throw new XPathException(e);
        }
        if (type == null) {
            XPathException err = new XPathException("Element " + env.getNamePool().getDisplayName(nameCode) +
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
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("directElement");
        out.emitAttribute("name", out.getNamePool().getDisplayName(nameCode));
        out.emitAttribute("validation", Validation.toString(validation));
        if (getSchemaType() != null) {
            out.emitAttribute("type", getSchemaType().getDescription());
        }
        content.explain(out);
        out.endElement();
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
