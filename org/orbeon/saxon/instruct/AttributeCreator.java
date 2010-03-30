package org.orbeon.saxon.instruct;

import org.orbeon.saxon.type.SimpleType;
import org.orbeon.saxon.event.ReceiverOptions;

/**
 * Abstract class for fixed and computed attribute constructor expressions
 */

public abstract class AttributeCreator extends SimpleNodeConstructor {

    private SimpleType schemaType;
    private int annotation;
    private int validationAction;
    private int options;

    /**
     * Set the required schema type of the attribute
     * @param type the required schema type, if validation against a specific type is required
     */

    public void setSchemaType(SimpleType type) {
        schemaType = type;
    }

    /**
     * Return the required schema type of the attribute
     * @return if validation against a schema type was requested, return the schema type (always a simple type).
     * Otherwise, if validation against a specific type was not requested, return null
     */

    public SimpleType getSchemaType() {
        return schemaType;
    }

    /**
     * Set the validation action required
     * @param action the validation action required, for example strict or lax
     */

    public void setValidationAction(int action) {
        validationAction = action;
    }

    /**
     * Get the validation action requested
     * @return the validation action, for example strict or lax
     */

    public int getValidationAction() {
        return validationAction;
    }

    /**
     * Set the options to be used on the attribute event
     * @param options
     */

    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Indicate that two attributes with the same name are not acceptable.
     * (This option is set in XQuery, but not in XSLT)
     */

    public void setRejectDuplicates() {
        options |= ReceiverOptions.REJECT_DUPLICATES;
    }

    /**
     * Indicate that the attribute value contains no special characters that
     * might need escaping
     */

    public void setNoSpecialChars() {
        options |= ReceiverOptions.NO_SPECIAL_CHARS;
    }    

    /**
     * Get the options to be used on the attribute event
     * @return the option flags to be used
     */

    public int getOptions() {
        return options;
    }

    /**
     * Set the type annotation fingerprint to be used on the attribute event
     * @param type the fingerprint of the type annotation to be used
     */

    public void setAnnotation(int type) {
        annotation = type;
    }

    /**
     * Get the type annotation fingerprint to be used on the attribute event
     * @return the fingerprint of the type annotation to be used
     */

    public int getAnnotation() {
        return annotation;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



