package org.orbeon.saxon.expr;

import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.event.LocationProvider;

import javax.xml.transform.SourceLocator;

/**
 * A Container is something that can act as the parent of an expression. It is either an
 * expression that can have subexpressions (which rules out Values), or an object such as a function,
 * a template, or an attribute set that is not itself an expression but that can contain expressions
 */

public interface Container extends SourceLocator {

    /**
     * Get the Executable (representing a complete stylesheet or query) of which this Container forms part
     */

    public Executable getExecutable();

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider();

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link org.orbeon.saxon.Configuration#XSLT} or {@link org.orbeon.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage();

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression was found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement);

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
// Contributor(s): Michael Kay
//
