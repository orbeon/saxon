package net.sf.saxon.trans;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Template;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.UnionPattern;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
  * <B>RuleManager</B> maintains a set of template rules, one set for each mode
  * @version 10 December 1999: carved out of the old Controller class
  * @author Michael H. Kay
  */

public class RuleManager implements Serializable {

    private Mode defaultMode;           // node handlers with default mode
    private HashMap modes;              // tables of node handlers for non-default modes
    private Mode omniMode = null;       // node handlers that specify mode="all"

    /**
    * create a RuleManager and initialise variables.
    */

    public RuleManager() {
        resetHandlers();
    }

    /**
    * Set up a new table of handlers.
    */

    public void resetHandlers() {
        defaultMode = new Mode(Mode.DEFAULT_MODE);
        modes = new HashMap(5);
    }

    /**
    * Get the Mode object for a named mode. If there is not one already registered.
    * a new Mode is created.
    * @param modeNameCode The name code of the mode. Supply Mode.DEFAULT_MODE to get the default
    * mode or Mode.ALL_MODES to get the Mode object containing "mode=all" rules
     * @return the Mode with this name
    */

    public Mode getMode(int modeNameCode) {
        if (modeNameCode==Mode.DEFAULT_MODE) {
            return defaultMode;
        }
        if (modeNameCode==Mode.ALL_MODES) {
            if (omniMode==null) {
                omniMode = new Mode(Mode.NAMED_MODE);
            }
            return omniMode;
        }
        Integer modekey = new Integer(modeNameCode & 0xfffff);
        Mode m = (Mode)modes.get(modekey);
        if (m==null) {
            m = new Mode(omniMode);
            modes.put(modekey, m);
            // when creating a specific mode, copy all the rules currently held
            // in the omniMode, as these apply to all modes
        }
        return m;
    }

    /**
      * Register a handler for a particular pattern. The priority of the rule
      * is the default priority for the pattern, which depends on the syntax of
      * the pattern suppllied.
      * @param pattern A match pattern
      * @param eh The ElementHandler to be used
      * @param mode The processing mode
      * @param precedence The import precedence (use 0 by default)
      */

    public void setHandler(Pattern pattern, Template eh, Mode mode, int precedence) {
        // for a union pattern, register the parts separately (each with its own priority)
        if (pattern instanceof UnionPattern) {
            UnionPattern up = (UnionPattern)pattern;
            Pattern p1 = up.getLHS();
            Pattern p2 = up.getRHS();
            setHandler(p1, eh, mode, precedence);
            setHandler(p2, eh, mode, precedence);
            return;
        }

        double priority = pattern.getDefaultPriority();
        setHandler(pattern, eh, mode, precedence, priority);
    }


    /**
      * Register a handler for a particular pattern.
      * @param pattern Must be a valid Pattern.
      * @param eh The Template to be used
      * @param mode The processing mode to which this element handler applies
      * @param precedence The import precedence of this rule
      * @param priority The priority of the rule: if an element matches several patterns, the
      * one with highest priority is used
      * @see Pattern
      */

    public void setHandler(Pattern pattern, Template eh,
                 Mode mode, int precedence, double priority) {

        // for a union pattern, register the parts separately
        if (pattern instanceof UnionPattern) {
            UnionPattern up = (UnionPattern)pattern;
            Pattern p1 = up.getLHS();
            Pattern p2 = up.getRHS();
            setHandler(p1, eh, mode, precedence, priority);
            setHandler(p2, eh, mode, precedence, priority);
            return;
        }
        mode.addRule(pattern, eh, precedence, priority);

        // if adding a rule to the omniMode (mode='all') add it to all
        // the other modes as well
        if (mode==omniMode) {
            defaultMode.addRule(pattern, eh, precedence, priority);
            Iterator iter = modes.values().iterator();
            while (iter.hasNext()) {
                Mode m = (Mode)iter.next();
                m.addRule(pattern, eh, precedence, priority);
            }
        }
    }

     /**
      * Find the template rule registered for a particular node in a specific mode.
      * @param node The NodeInfo for the relevant node
      * @param mode The processing mode
      * @param c The controller for this transformation
      * @return The template rule that will process this node
      * Returns null if there is no specific handler registered.
      */

    public Template getTemplateRule (NodeInfo node, Mode mode, XPathContext c) throws XPathException {

        if (mode==null) {
            mode = defaultMode;
        }

        return (Template)mode.getRule(node, c);
    }

    /**
     * Get a template rule whose import precedence is in a particular range. This is used to support
     * the xsl:apply-imports function
     * @param node The node to be matched
     * @param mode The mode for which a rule is required
     * @param min  The minimum import precedence that the rule must have
     * @param max  The maximum import precedence that the rule must have
     * @param c    The Controller for the transformation
     * @return     The template rule to be invoked
     * @throws XPathException
     */

    public Template getTemplateRule (NodeInfo node, Mode mode, int min, int max, XPathContext c)
    throws XPathException {
        if (mode==null) {
            mode = defaultMode;
        }
        return (Template)mode.getRule(node, min, max, c);
    }

    /**
     * Get the next-match handler after the current one
     * @param node  The node to be matched
     * @param mode  The processing mode
     * @param currentHandler The current template rule
     * @param c     The dynamic context for the transformation
     * @return      The template rule to be executed
     * @throws XPathException
     */

    public Template getNextMatchHandler(NodeInfo node, Mode mode, Template currentHandler, XPathContext c)
    throws XPathException {
        if (mode==null) {
            mode = defaultMode;
        }
        return (Template)mode.getNextMatchRule(node, currentHandler, c);
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
