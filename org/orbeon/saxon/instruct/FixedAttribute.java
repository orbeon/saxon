package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.event.NoOpenStartTagException;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.Orphan;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SimpleType;
import org.orbeon.saxon.type.ValidationException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.StaticError;
import org.orbeon.saxon.xpath.XPathException;

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
        if (select instanceof AtomicValue && schemaType != null && !schemaType.isNamespaceSensitive()) {
            String value = ((AtomicValue)select).getStringValue();
            try {

                schemaType.validateContent(value, DummyNamespaceResolver.getInstance());
            } catch (ValidationException err) {
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
            try {
                schemaType.validateContent(value, DummyNamespaceResolver.getInstance());
                if (schemaType.isNamespaceSensitive()) {
                    opt |= ReceiverOptions.NEEDS_PREFIX_CHECK;
                }
            } catch (ValidationException err) {
                throw new ValidationException("Attribute value " + Err.wrap(value, Err.VALUE) +
                                               " does not the match the required type " +
                                               schemaType.getDescription() + ". " +
                                               err.getMessage());
            }
        } else if (validationAction==Validation.STRICT ||
                validationAction==Validation.LAX) {
            long res = controller.getConfiguration().validateAttribute(nameCode,
                                                                         value,
                                                                         validationAction);
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
            try {
                schemaType.validateContent(o.getStringValue(), DummyNamespaceResolver.getInstance());
                o.setTypeAnnotation(schemaType.getFingerprint());
                if (schemaType.isNamespaceSensitive()) {
                    throw new DynamicError("Cannot validate a parentless attribute whose content is namespace-sensitive");
                }
            } catch (ValidationException err) {
                throw new ValidationException("Attribute value " + Err.wrap(o.getStringValue(), Err.VALUE) +
                                               " does not the match the required type " +
                                               schemaType.getDescription() + ". " +
                                               err.getMessage());
            }
        } else if (validationAction==Validation.STRICT ||
                validationAction==Validation.LAX) {
            long res = context.getController().getConfiguration().validateAttribute(nameCode,
                                                                         o.getStringValue(),
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
