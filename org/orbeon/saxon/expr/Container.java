package net.sf.saxon.expr;

import net.sf.saxon.instruct.Executable;

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
