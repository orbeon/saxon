package net.sf.saxon.trace;

import net.sf.saxon.event.SaxonLocator;
import net.sf.saxon.om.NamespaceResolver;

import java.util.Iterator;


/**
* Information about an instruction in the stylesheet, made
* available at run-time to a TraceListener
*/

public interface InstructionInfo extends SaxonLocator {

    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link net.sf.saxon.style.StandardNames}: all less than 1024)
     * or it will be a constant in class {@link Location}.
     */

    public int getConstructType();

    /**
     * Get the name of the instruction. This is applicable only when the construct type
     * is Location.INSTRUCTION. This returns the same value as getConstructType if the construct is
     * a standard XSLT instruction (values < 1024) or -1 otherwise.
     * @deprecated since Saxon 8.1 - use getConstructType
    */

    public int getInstructionFingerprint();

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public int getObjectNameCode();

    /**
    * Get the system identifier (URI) of the source stylesheet or query module containing
    * the instruction. This will generally be an absolute URI. If the system
    * identifier is not known, the method may return null. In some cases, for example
    * where XML external entities are used, the correct system identifier is not
    * always retained.
    */

    public String getSystemId();

    /**
    * Get the line number of the instruction in the source stylesheet module.
    * If this is not known, or if the instruction is an artificial one that does
    * not relate to anything in the source code, the value returned may be -1.
    */

    public int getLineNumber();

    /**
     * Get the namespace context of the instruction. This will not always be available, in which
     * case the method returns null.
     */

    public NamespaceResolver getNamespaceResolver();

    /**
     * Get the value of a particular property of the instruction. Properties
     * of XSLT instructions are generally known by the name of the stylesheet attribute
     * that defines them.
     * @param name The name of the required property
     * @return  The value of the requested property, or null if the property is not available
     */

    public Object getProperty(String name);

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property. The iterator may return properties whose
     * value is null.
     */

    public Iterator getProperties();

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
