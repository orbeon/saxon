package net.sf.saxon.trans;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.InstructionDetails;
import net.sf.saxon.instruct.Procedure;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.InstructionInfoProvider;

import java.io.Serializable;
import java.text.Collator;

/**
  * Corresponds to a single key definition.<P>
  * @author Michael H. Kay
  */

public class KeyDefinition extends Procedure implements Serializable, InstructionInfoProvider {

    private Pattern match;          // the match pattern
    private Collator collation;     // the collating sequence, when type=string
    private String collationName;   // the collation URI

    /**
    * Constructor to create a key definition
    */

    public KeyDefinition(Pattern match, Expression use, String collationName, Collator collation) {
        this.match = match;
        setBody(use);
        this.collation = collation;
        this.collationName = collationName;
    }

    /**
     * Set the map of local variables needed while evaluating the "use" expression
     */

    public void setStackFrameMap(SlotManager map) {
        if (map != null && map.getNumberOfVariables() > 0) {
            super.setStackFrameMap(map);
        }
    }

    /**
     * Set the system Id and line number of the source xsl:key definition
     */

    public void setLocation(String systemId, int lineNumber) {
        setSystemId(systemId);
        setLineNumber(lineNumber);
    }

    /**
    * Get the match pattern for the key definition
     * @return the pattern specified in the "match" attribute of the xsl:key declaration
    */

    public Pattern getMatch() {
        return match;
    }

    /**
    * Get the use expression for the key definition
     * @return the expression specified in the "use" attribute of the xsl:key declaration
    */

    public Expression getUse() {
        return getBody();
    }

    /**
    * Get the collation name for this key definition.
    * @return the collation name (the collation URI)
    */

    public String getCollationName() {
        return collationName;
    }

    /**
    * Get the collation.
     * @return the collation
    */

    public Collator getCollation() {
        return collation;
    }

    /**
     * Get the InstructionInfo details about the construct. This information isn't used for tracing,
     * but it is available when inspecting the context stack.
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(StandardNames.XSL_KEY);
        details.setSystemId(getSystemId());
        details.setLineNumber(getLineNumber());
        details.setProperty("key", this);
        return details;
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
// The Original Code is: all this file 
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//
