package net.sf.saxon.instruct;
import net.sf.saxon.trans.DynamicError;

/**
* An exception thrown by xsl:message terminate="yes".
*/

public class TerminationException extends DynamicError {

    /**
    * Construct a TerminationException
    * @param message the text of the message to be output
    */
	
	public TerminationException(String message) {
		super(message);
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
