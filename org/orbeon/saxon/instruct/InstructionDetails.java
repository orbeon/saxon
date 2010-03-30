package org.orbeon.saxon.instruct;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
* Details about an instruction, used when reporting errors and when tracing
*/

public final class InstructionDetails implements InstructionInfo, Serializable {

    private int constructType = Location.UNCLASSIFIED;
    private String systemId = null;
    private int lineNumber = -1;
    private int columnNumber = -1;
    private StructuredQName objectName;
    private HashMap properties = new HashMap(5);

    public InstructionDetails() {}

    /**
     * Set the type of construct
     */

    public void setConstructType(int type) {
        constructType = type;
    }

    /**
     * Get the construct type
     */
    public int getConstructType() {
        return constructType;
    }

    /**
    * Set the URI of the module containing the instruction
    * @param systemId the module's URI
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Get the URI of the module containing the instruction
    * @return the module's URI
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Set the line number of the instruction within the module
    * @param lineNumber the line number
    */

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
    * Get the line number of the instruction within its module
    * @return the line number
    */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Set a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public void setObjectName(StructuredQName qName) {
        objectName = qName;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public StructuredQName getObjectName() {
        if (objectName != null) {
            return objectName;
        } else {
            return null;
        }
    }

    /**
     * Set a named property of the instruction
     */

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Get a named property of the instruction
     */

    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property.
     */

    public Iterator getProperties() {
        return properties.keySet().iterator();
    }

    /**
    * Get the public ID of the module containing the instruction. This method
    * is provided to satisfy the SourceLocator interface. However, the public ID is
    * not maintained by Saxon, and the method always returns null
    * @return null
    */

    public String getPublicId() {
        return null;
    }

    /**
     * Set the column number
     */

    public void setColumnNumber(int column) {
        columnNumber = column;
    }

    /**
    * Get the column number identifying the position of the instruction.
    * @return -1 if column number is not known
    */

    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Get a description of the instruction
     */

//    public String getDescription(NamePool pool) {
//        switch (constructType) {
//            case Location.INSTRUCTION:
//                return pool.getDisplayName(instructionNameCode);
//            case Location.LITERAL_RESULT_ELEMENT:
//                return "element constructor <" + pool.getDisplayName(objectNameCode) + ">";
//            case Location.LITERAL_RESULT_ATTRIBUTE:
//                return "attribute constructor " + pool.getDisplayName(objectNameCode) + "=\"{...}\"";
//            default:
//                return "" + constructType;
//        }
//    }

    /**
     * Get the InstructionInfo details about the construct. This is to satisfy the InstructionInfoProvider
     * interface.
     */

    public InstructionInfo getInstructionInfo() {
        return this;
    }

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
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
// Contributor(s):
//
