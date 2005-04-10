package net.sf.saxon.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.ContentTypeTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * An instruction representing an xsl:element element in an XSLT stylesheet,
 * or a computed element constructor in XQuery. (In both cases, if the element name
 * is expressed as a compile-time expression, then a FixedElement instruction
 * is used instead.)
 * @see FixedElement
 */

public class ComputedElement extends ElementCreator {

    private Expression elementName;
    private Expression namespace = null;
    private NamespaceResolver nsContext;
    private boolean allowNameAsQName;
    private ItemType itemType;

    /**
     * Create an instruction that creates a new element node
     *
     * @param elementName      Expression that evaluates to produce the name of the
     *                         element node as a lexical QName
     * @param namespace        Expression that evaluates to produce the namespace URI of
     *                         the element node. Set to null if the namespace is to be deduced from the prefix
     *                         of the elementName.
     * @param nsContext        Saved copy of the static namespace context for the instruction.
     *                         Can be set to null if namespace is supplied.
     * @param schemaType       The required schema type for the content
     * @param allowQName
     */
    public ComputedElement(Expression elementName,
                           Expression namespace,
                           NamespaceResolver nsContext,
                           SchemaType schemaType,
                           int validation,
                           boolean inheritNamespaces,
                           boolean allowQName) {
        this.elementName = elementName;
        this.namespace = namespace;
        this.nsContext = nsContext;
        this.schemaType = schemaType;
        this.validation = validation;
        this.inheritNamespaces = inheritNamespaces;
        this.allowNameAsQName = allowQName;
    }

    public Expression simplify(StaticContext env) throws XPathException {
        elementName = elementName.simplify(env);
        if (namespace != null) {
            namespace = namespace.simplify(env);
        }
        Configuration config = env.getConfiguration();
        setLazyConstruction(config.isLazyConstructionMode());

        if (schemaType != null) {
            itemType = new ContentTypeTest(Type.ELEMENT, schemaType, config);
        } else if (validation == Validation.STRIP || !config.isSchemaAware(Configuration.XML_SCHEMA)) {
            itemType = new ContentTypeTest(Type.ELEMENT,
                    BuiltInSchemaFactory.getSchemaType(StandardNames.XDT_UNTYPED),
                    config);
        } else {
            // paradoxically, we know less about the type if validation="strict" is specified!
            // We know that it won't be untyped, but we have no way of representing that.
            itemType = NodeKindTest.ELEMENT;
        }
        return super.simplify(env);
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        elementName = elementName.analyze(env, contextItemType);

        RoleLocator role = new RoleLocator(RoleLocator.INSTRUCTION, "element/name", 0, null);
        role.setSourceLocator(this);

        if (allowNameAsQName) {
            // Can only happen in XQuery
            elementName = TypeChecker.staticTypeCheck(elementName, SequenceType.SINGLE_ATOMIC, false, role, env);
        } else {
            elementName = TypeChecker.staticTypeCheck(elementName, SequenceType.SINGLE_STRING, false, role, env);
        }
        if (namespace != null) {
            namespace = namespace.analyze(env, contextItemType);

            role = new RoleLocator(RoleLocator.INSTRUCTION, "element/namespace", 0, null);
            role.setSourceLocator(this);
            namespace = TypeChecker.staticTypeCheck(namespace, SequenceType.SINGLE_STRING, false, role, env);
        }
        Expression res = super.analyze(env, contextItemType);
        verifyLazyConstruction();
        return res;
    }

    /**
     * Get the item type of the value returned by this instruction
     *
     * @return the item type
     */

    public ItemType getItemType() {
        if (itemType == null) {
            return super.getItemType();
        }
        return itemType;
    }

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(8);
        list.add(content);
        list.add(elementName);
        if (namespace != null) {
            list.add(namespace);
        }
        return list.iterator();
    }

    /**
     * Offer promotion for subexpressions. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws net.sf.saxon.trans.XPathException if any error is detected
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        elementName = elementName.promote(offer);
        if (namespace != null) {
            namespace = namespace.promote(offer);
        }
        super.promoteInst(offer);
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
            StaticError err = new StaticError(
                    "Elements are not permitted here: the containing element has the simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else if (((ComplexType)parentType).isSimpleContent()) {
            StaticError err = new StaticError(
                    "Elements are not permitted here: the containing element has a complex type with simple content");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        // NOTE: we could in principle check that if all the elements permitted in the content of the parentType
        // themselves have a simple type (not uncommon, perhaps) then this element must not have element content.
    }


    /**
     * Callback from the superclass ElementCreator to get the nameCode
     * for the element name
     *
     * @param context The evaluation context (not used)
     * @return the name code for the element name
     */

    public int getNameCode(XPathContext context)
            throws XPathException, XPathException {

        Controller controller = context.getController();
        NamePool pool = controller.getNamePool();

        String prefix;
        String localName;
        String uri;

        // name needs to be evaluated at run-time
        Item nameValue = elementName.evaluateItem(context);
        if (nameValue instanceof StringValue) {
            // this will always be the case in XSLT
            CharSequence rawName = nameValue.getStringValueCS();
            try {
                String[] parts = Name.getQNameParts(rawName);
                prefix = parts[0];
                localName = parts[1];
            } catch (QNameException err) {
                DynamicError err1 = new DynamicError("Invalid element name. " + err.getMessage(), this);
                err1.setErrorCode((isXSLT(context) ? "XTDE0820" : "XQDY0074"));
                err1.setXPathContext(context);
                throw dynamicError(this, err1, context);
                //return -1;
            }
        } else if (nameValue instanceof QNameValue && allowNameAsQName) {
            // this is allowed in XQuery
            localName = ((QNameValue)nameValue).getLocalName();
            uri = ((QNameValue)nameValue).getNamespaceURI();
            namespace = new StringValue(uri);
            // we need to allocate a prefix. Any one will do; if it's a duplicate,
            // a different one will be substituted
            prefix = pool.suggestPrefixForURI(uri);
            if (prefix == null) {
                prefix = "nsq0";
            }
        } else {
            DynamicError err = new DynamicError("Computed element name has incorrect type");
            err.setErrorCode((isXSLT(context) ? "XTDE0820" : "XPTY0004"));
            err.setIsTypeError(true);
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (namespace == null) {
            uri = nsContext.getURIForPrefix(prefix, true);
            if (uri == null) {
                DynamicError err = new DynamicError("Undeclared prefix in element name: " + prefix, this);
                err.setErrorCode((isXSLT(context) ? "XTDE0830" : "XQDY0074"));
                err.setXPathContext(context);
                throw dynamicError(this, err, context);
            }

        } else {
            uri = namespace.evaluateAsString(context);
            if (uri.equals("")) {
                // there is a special rule for this case in the specification;
                // we force the element to go in the null namespace
                prefix = "";
            }
            if (prefix.equals("xmlns")) {
                // this isn't a legal prefix so we mustn't use it
                prefix = "x-xmlns";
            }
        }

        return pool.allocate(prefix, uri, localName);

    }

    /**
     * Callback to output namespace nodes for the new element.
     *
     * @param context The execution context
     * @param out     the Receiver where the namespace nodes are to be written
     * @throws XPathException
     */
    protected void outputNamespaceNodes(XPathContext context, Receiver out)
            throws XPathException {
        // do nothing
    }


    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_ELEMENT;
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "element ");
        out.println(ExpressionTool.indent(level + 1) + "name");
        elementName.display(level + 2, pool, out);
        out.println(ExpressionTool.indent(level + 1) + "content");
        content.display(level + 1, pool, out);
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
