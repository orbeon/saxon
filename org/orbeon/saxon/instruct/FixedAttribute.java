package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.Err;
import net.sf.saxon.event.NoOpenStartTagException;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.Orphan;
import net.sf.saxon.om.Validation;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;

import java.io.PrintStream;

/**
 * An instruction derived from an xsl:attribute element in stylesheet, or from
 * an attribute constructor in XQuery. This version deals only with attributes
 * whose name is known at compile time. It is also used for attributes of
 * literal result elements. The value of the attribute is in general computed
 * at run-time.
*/

public final class FixedAttribute extends SimpleNodeConstructor {

    private int nameCode;
    private SimpleType schemaType;
    private int annotation;
    private int options;
    private int validationAction;

    /**
    * Construct an Attribute instruction
    * @param nameCode Represents the attribute name
    * @param annotation Integer code identifying the type named in the <code>type</code> attribute
    * of the instruction - zero if the attribute was not present
    */

    public FixedAttribute (  int nameCode,
                             int validationAction,
                             SimpleType schemaType,
                             int annotation ) {
        this.nameCode = nameCode;
        this.schemaType = schemaType;
        this.annotation = annotation;
        this.validationAction = validationAction;
        this.options = 0;
    }

    /**
     * Get the name of this instruction (return 'xsl:attribute')
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_ATTRIBUTE;
    }
    /**
     * Indicate that two attributes with the same name are not acceptable.
     * (This option is set in XQuery, but not in XSLT)
     */

    public void setRejectDuplicates() {
        this.options |= ReceiverOptions.REJECT_DUPLICATES;
    }

    /**
     * Indicate that the attribute value contains no special characters that
     * might need escaping
     */

    public void setNoSpecialChars() {
        this.options |= ReceiverOptions.NO_SPECIAL_CHARS;
    }

    /**
     * Set the expression defining the value of the attribute. If this is a constant, and if
     * validation against a schema type was requested, the validation is done immediately.
     * @param select The expression defining the content of the attribute
     * @throws StaticError if the expression is a constant, and validation is requested, and
     * the constant doesn't match the required type.
     */
    public void setSelect(Expression select) throws StaticError {
        super.setSelect(select);

        // Attempt early validation if possible
        if (select instanceof AtomicValue && schemaType != null && !schemaType.isNamespaceSensitive()) {
            CharSequence value = ((AtomicValue)select).getStringValueCS();
            XPathException err = schemaType.validateContent(value, DummyNamespaceResolver.getInstance());
            if (err != null) {
                throw new StaticError("Attribute value " + Err.wrap(value, Err.VALUE) +
                                               " does not the match the required type " +
                                               schemaType.getDescription() + ". " +
                                               err.getMessage());
            }
        }
    }

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
        details.setConstructType(Location.LITERAL_RESULT_ATTRIBUTE);
        details.setObjectNameCode(nameCode);
        return details;
    }

    public ItemType getItemType() {
        return NodeKindTest.ATTRIBUTE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }


    public void typeCheck(StaticContext env, ItemType contextItemType) {
    }

    protected int evaluateNameCode(XPathContext context)  {
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
             StaticError err = new StaticError("Attribute " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted in the content model of the simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        SchemaType type;
        try {
            type = ((ComplexType)parentType).getAttributeUseType(nameCode & 0xfffff);
        } catch (SchemaException e) {
            throw new StaticError(e);
        }
        if (type == null) {
            StaticError err = new StaticError("Attribute " + env.getNamePool().getDisplayName(nameCode) +
                    " is not permitted in the content model of the complex type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        if (type instanceof AnyType) {
            return;
        }

        try {
            select.checkPermittedContents(type, env, true);
        } catch (XPathException e) {
            if (e.getLocator() == null || e.getLocator() == e) {
                e.setLocator(this);
            }
            throw e;
        }
    }

    /**
    * Process this instruction
    * @param context the dynamic context of the transformation
    * @return a TailCall to be executed by the caller, always null for this instruction
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException
    {
        Controller controller = context.getController();
        SequenceReceiver out = context.getReceiver();
        int opt = options;
        int ann = annotation;

    	// we may need to change the namespace prefix if the one we chose is
    	// already in use with a different namespace URI: this is done behind the scenes
    	// by the Outputter

        String value = expandChildren(context).toString();
        if (schemaType != null) {
            // test whether the value actually conforms to the given type
            XPathException err = schemaType.validateContent(value, DummyNamespaceResolver.getInstance());
            if (err != null) {
                ValidationException verr = new ValidationException(
                        "Attribute value " + Err.wrap(value, Err.VALUE) +
                        " does not the match the required type " +
                        schemaType.getDescription() + ". " + err.getMessage());
                verr.setLocator(this);
                throw verr;
            }
            if (schemaType.isNamespaceSensitive()) {
                opt |= ReceiverOptions.NEEDS_PREFIX_CHECK;
            }
        } else if (validationAction==Validation.STRICT ||
                validationAction==Validation.LAX) {
            long res;
            try {
                res = controller.getConfiguration().validateAttribute(
                        nameCode, value, validationAction);
            } catch (ValidationException e) {
                e.setLocator(this);
                throw e;
            }
            ann = (int)(res & 0xffffffff);
            opt |= (int)(res >> 32);

        }
        try {
            out.attribute(nameCode, ann, value, locationId, opt);
        } catch (NoOpenStartTagException err) {
            err.setXPathContext(context);
            err.setLocator(getSourceLocator());
            context.getController().recoverableError(err);
        } catch (XPathException err) {
            throw dynamicError(this, err, context);
        }

        return null;
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        Orphan o = (Orphan)super.evaluateItem(context);
        if (schemaType != null) {
            XPathException err = schemaType.validateContent(
                    o.getStringValueCS(), DummyNamespaceResolver.getInstance());
            if (err != null) {
                throw new ValidationException("Attribute value " + Err.wrap(o.getStringValueCS(), Err.VALUE) +
                                           " does not the match the required type " +
                                           schemaType.getDescription() + ". " +
                                           err.getMessage());
            }
            o.setTypeAnnotation(schemaType.getFingerprint());
            if (schemaType.isNamespaceSensitive()) {
                throw new DynamicError("Cannot validate a parentless attribute whose content is namespace-sensitive");
            }
        } else if (validationAction==Validation.STRICT ||
                validationAction==Validation.LAX) {
            long res = context.getController().getConfiguration().validateAttribute(nameCode,
                                                                         o.getStringValueCS(),
                                                                         validationAction);
            int ann = (int)(res & 0xffffffff);
            int opt = (int)(res >> 32);
            if (opt != 0) {
                throw new DynamicError("Cannot validate a parentless attribute whose content is namespace-sensitive");
            }
            o.setTypeAnnotation(ann);
        }

        return o;
    }

     /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "attribute ");
        out.println(ExpressionTool.indent(level+1) + "name " +
                (pool==null ? nameCode+"" : pool.getDisplayName(nameCode)));
        super.display(level+1, pool, out);
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
