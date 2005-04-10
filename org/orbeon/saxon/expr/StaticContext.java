package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.instruct.LocationMap;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.SourceLocator;
import java.util.Comparator;

/**
* A StaticContext contains the information needed while an expression or pattern
* is being parsed. The information is also sometimes needed at run-time.
*/

public interface StaticContext {

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration();

    /**
     * Get the location map. This is a mapping from short location ids held with each expression or
     * subexpression, to a fully-resolved location in a source stylesheet or query.
     */

    public LocationMap getLocationMap();

    /**
    * Issue a compile-time warning
    */

    public void issueWarning(String s, SourceLocator locator);

    /**
    * Get the System ID of the container of the expression. This is the containing
    * entity (file) and is therefore useful for diagnostics. Use getBaseURI() to get
    * the base URI, which may be different.
    */

    public String getSystemId();

    /**
    * Get the line number of the expression within its containing entity
    * Returns -1 if no line number is available
    */

    public int getLineNumber();

    /**
    * Get the Base URI of the stylesheet element, for resolving any relative URI's used
    * in the expression.
    * Used by the document(), doc(), resolve-uri(), and base-uri() functions.
    * May return null if the base URI is not known.
    */

    public String getBaseURI();

    /**
    * Get the URI for a namespace prefix. The default namespace is NOT used
    * when the prefix is empty.
    * @param prefix The prefix
    * @throws XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException;

    /**
    * Get the NamePool used for compiling expressions
    */

    public NamePool getNamePool();

    /**
    * Bind a variable used in this element to the XSLVariable element in which it is declared
    */

    public VariableDeclaration bindVariable(int fingerprint) throws StaticError;

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary();

    /**
    * Get a named collation.
    * @param name The name of the required collation. Supply null to get the default collation.
    * @return the collation; or null if the required collation is not found.
    */

    public Comparator getCollation(String name) throws XPathException;

    /**
    * Get the name of the default collation.
    * @return the name of the default collation; or the name of the codepoint collation
    * if no default collation has been defined
    */

    public String getDefaultCollationName();

    /**
    * Get the default XPath namespace, as a namespace code that can be looked up in the NamePool
    */

    public short getDefaultElementNamespace();

    /**
     * Get the default function namespace
     */

    public String getDefaultFunctionNamespace();

    /**
    * Determine whether Backwards Compatible Mode is used
    */

    public boolean isInBackwardsCompatibleMode();

    /**
     * Determine whether a Schema for a given target namespace has been imported. Note that the
     * in-scope element declarations, attribute declarations and schema types are the types registered
     * with the (schema-aware) configuration, provided that their namespace URI is registered
     * in the static context as being an imported schema namespace. (A consequence of this is that
     * within a Configuration, there can only be one schema for any given namespace, including the
     * null namespace).
     */

    public boolean isImportedSchema(String namespace);

    /**
     * Determine whether a built-in type is available in this context. This method caters for differences
     * between host languages as to which set of types are built in.
     * @param type the supposedly built-in type. This will always be a type in the
     * XS or XDT namespace.
     * @return true if this type can be used in this static context
     */

    public boolean isAllowedBuiltInType(AtomicType type);

    /**
     * Get a namespace resolver to resolve the namespaces declared in this static context.
     * @return a namespace resolver.
     */

    NamespaceResolver getNamespaceResolver();

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
