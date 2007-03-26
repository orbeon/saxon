package org.orbeon.saxon.trans;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.InstructionDetails;
import org.orbeon.saxon.instruct.Procedure;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.InstructionInfoProvider;

import java.io.Serializable;
import java.util.Comparator;

/**
  * Corresponds to a single xsl:key declaration.<P>
  * @author Michael H. Kay
  */

public class KeyDefinition extends Procedure implements Serializable, InstructionInfoProvider {

    private Pattern match;          // the match pattern
    private Comparator collation;     // the collating sequence, when type=string
    private String collationName;   // the collation URI
    private boolean backwardsCompatible = false;
    private boolean strictComparison = false;

    /**
    * Constructor to create a key definition
     * @param match the pattern in the xsl:key match attribute
     * @param use the expression in the xsl:key use attribute, or the expression that results from compiling
     * the xsl:key contained instructions
     * @param collationName the name of the collation being used
     * @param collation the actual collation. This must be one that supports generation of collation keys.
    */

    public KeyDefinition(Pattern match, Expression use, String collationName, Comparator collation) {
        setHostLanguage(Configuration.XSLT);
        this.match = match;
        setBody(use);
        this.collation = collation;
        this.collationName = collationName;
    }

    /**
     * Set backwards compatibility mode. The key definition is backwards compatible if ANY of the xsl:key
     * declarations has version="1.0" in scope.
     */

    public void setBackwardsCompatible(boolean bc) {
        backwardsCompatible = bc;
    }

    /**
     * Test backwards compatibility mode
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }

    /**
     * Set whether strict comparison is needed. Strict comparison treats non-comparable values as an
     * error rather than a no-match. This is used for internal keys that support value comparisons in
     * Saxon-SA, it is not used for user-defined XSLT keys.
     */

    public void setStrictComparison(boolean strict) {
        strictComparison = strict;
    }

    /**
     * Get whether strict comparison is needed.
     */

    public boolean isStrictComparison() {
        return strictComparison;
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

    public Comparator getCollation() {
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
