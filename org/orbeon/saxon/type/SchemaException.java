
package net.sf.saxon.type;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;

/**
 * An exception that identifies an error in reading, parsing, or
 * validating a schema.
 */

public class SchemaException extends TransformerConfigurationException {

    /**
     * Creates a new XMLException with no message
     * or nested Exception.
     */

    public SchemaException() {
        super();
    }

    public SchemaException(String message, SourceLocator locator) {
        super(message, locator);
    }

    /**
     * Creates a new XMLException with the given message.
     *
     * @param message the message for this Exception
     */

    public SchemaException(String message) {
        super(message);
    }

    /**
     * Creates a new XMLException with the given nested
     * exception.
     *
     * @param exception the nested exception
     */

    public SchemaException(Exception exception) {
        super(exception);
    }

    /**
     * Creates a new XMLException with the given message
     * and nested exception.
     *
     * @param message the detail message for this exception
     * @param exception the nested exception
     */

    public SchemaException(String message, Exception exception) {
        super(message, exception);
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//