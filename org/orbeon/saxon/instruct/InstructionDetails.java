package net.sf.saxon.instruct;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trace.Location;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
* Details about an instruction, used when reporting errors and when tracing
*/

public final class InstructionDetails implements InstructionInfo, InstructionInfoProvider, Serializable {

    private int constructType = Location.UNCLASSIFIED;
    private String systemId = null;
    private int lineNumber = -1;
    private int columnNumber = -1;
    private int objectNameCode = -1;
    private NamespaceResolver namespaceResolver;
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
     * Set the namespace context for the instruction being traced. This is needed if the
     * tracelistener wants to evaluate XPath expressions in the context of the current instruction
     */

    public void setNamespaceResolver(NamespaceResolver resolver) {
        namespaceResolver = resolver;
    }

    /**
     * Get the namespace resolver to supply the namespace context of the instruction
     * that is being traced
     */

    public NamespaceResolver getNamespaceResolver() {
        return namespaceResolver;
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
    * Get the name of the instruction
    * @return the name of the instruction. This is a conventional name, it has the form
    * of a QName but the prefix chosen is arbitrary
    */

    public int getInstructionFingerprint() {
        return (constructType < 1024 ? constructType : -1);
    }

    /**
     * Set a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public void setObjectNameCode(int nameCode) {
        objectNameCode = nameCode;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public int getObjectNameCode() {
        return objectNameCode;
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

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
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
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
