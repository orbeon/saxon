package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.om.NamespaceResolver;

import java.util.HashMap;
import java.util.Iterator;

/**
 * A subclass of TraceWrapper used to trace expressions in XPath and XQuery. Unlike
 * the TraceInstruction class, this class contains all information needed for tracing,
 * rather than referencing a separate InstructionDetails object.
 */

// This class is used for tracing
// within XPath/XQuery expressions. The TraceExpression itself holds all the information
// about the source needing to be traced (the InstructionInfo).

public class TraceExpression extends TraceWrapper implements InstructionInfo {

    private int lineNumber = -1;
    private int columnNumber = -1;
    private String systemId = null;
    private int objectNameCode = -1;
    private int constructType;
    private NamespaceResolver namespaceResolver = null;
    private HashMap properties = new HashMap(10);

    /**
     * Create a trace expression that traces execution of a given child expression
     * @param child the expression to be traced. This will be available to the TraceListener
     * as the value of the "expression" property of the InstructionInfo.
     */
    public TraceExpression(Expression child) {
        this.child = child;
        setProperty("expression", child);
    }

    /**
     * Set the line number of the expression being traced
     * @param line
     */
    public void setLineNumber(int line) {
        lineNumber = line;
    }

    /**
     * Set the column number of the expression being traced
     * @param column
     */
    public void setColumnNumber(int column) {
        columnNumber = column;
    }

    /**
     * Set the type of construct. This will generally be a constant
     * in class {@link org.orbeon.saxon.trace.Location}
     */

    public void setConstructType(int type) {
        constructType = type;
    }

    /**
     * Get the construct type. This will generally be a constant
     * in class {@link org.orbeon.saxon.trace.Location}
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
     * @deprecated since Saxon 8.1: use {@link #getConstructType} instead
     */

    public int getInstructionFingerprint() {
        return -1;
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
     * Set a named property of the instruction/expression
     */

    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    /**
     * Get a named property of the instruction/expression
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
    * Get the column number identifying the position of the instruction. This method
    * is provided to satisfy the SourceLocator interface. However, the column number is
    * not maintained by Saxon, and the method always returns -1
    * @return -1
    */

    public int getColumnNumber() {
        return columnNumber;
    }

     /**
     * Get the InstructionInfo details about the construct. This is to satisfy the InstructionInfoProvider
     * interface.
     */

    public InstructionInfo getInstructionInfo() {
        return this;
    }

    /**
     * Get the system identifier (that is the base URI) of the static context of the expression being
     * traced. This returns the same result as getSystemId(), it is provided to satisfy the
     * {@link org.orbeon.saxon.event.LocationProvider} interface.
     * @param locationId not used
     * @return the URI of the module containing the expression
     */
    public String getSystemId(int locationId) {
        return getSystemId();
    }
     /**
     * Get the line number of the expression being
     * traced. This returns the same result as getLineNumber(), it is provided to satisfy the
     * {@link org.orbeon.saxon.event.LocationProvider} interface.
     * @param locationId not used
     * @return the line number of the expression within its module
     */

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

