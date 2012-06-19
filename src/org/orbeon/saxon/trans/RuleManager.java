package org.orbeon.saxon.trans;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.Template;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.pattern.UnionPattern;
import org.orbeon.saxon.trace.ExpressionPresenter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

/**
  * <B>RuleManager</B> maintains a set of template rules, one set for each mode
  * @version 10 December 1999: carved out of the old Controller class
  * @author Michael H. Kay
  */

public final class RuleManager implements Serializable {

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
        defaultMode = new Mode(Mode.DEFAULT_MODE, Mode.DEFAULT_MODE_NAME);
        modes = new HashMap(5);
    }

    /**
     * Get the mode object for the default (unnamed) mode
     */

    public Mode getDefaultMode() {
        return defaultMode;
    }

    /**
     * Get the Mode object for a named mode. If there is not one already registered.
     * a new Mode is created.
     * @param modeName The name of the mode. Supply null to get the default
     * mode or Mode.ALL_MODES to get the Mode object containing "mode=all" rules
     * @param createIfAbsent if true, then if the mode does not already exist it will be created.
     * If false, then if the mode does not already exist the method returns null.
     * @return the Mode with this name
     */

    public Mode getMode(StructuredQName modeName, boolean createIfAbsent) {
        if (modeName == null || modeName.equals(Mode.DEFAULT_MODE_NAME)) {
            return defaultMode;
        }
        if (modeName.equals(Mode.ALL_MODES)) {
            if (omniMode==null) {
                omniMode = new Mode(Mode.NAMED_MODE, modeName);
            }
            return omniMode;
        }
        //Integer modekey = new Integer(modeNameCode & 0xfffff);
        Mode m = (Mode)modes.get(modeName);
        if (m == null && createIfAbsent) {
            m = new Mode(omniMode, modeName);
            modes.put(modeName, m);
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
      * Register a template for a particular pattern.
      * @param pattern Must be a valid Pattern.
      * @param eh The Template to be used
      * @param mode The processing mode to which this template applies
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
        mode.addRule(pattern, eh, precedence, priority, true);

        // if adding a rule to the omniMode (mode='all') add it to all
        // the other modes as well
        if (mode==omniMode) {
            defaultMode.addRule(pattern, eh, precedence, priority, false);
            Iterator iter = modes.values().iterator();
            while (iter.hasNext()) {
                Mode m = (Mode)iter.next();
                m.addRule(pattern, eh, precedence, priority, false);
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

    public Rule getTemplateRule (NodeInfo node, Mode mode, XPathContext c) throws XPathException {

        if (mode==null) {
            mode = defaultMode;
        }

        return mode.getRule(node, c);
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

    public Rule getTemplateRule (NodeInfo node, Mode mode, int min, int max, XPathContext c)
    throws XPathException {
        if (mode==null) {
            mode = defaultMode;
        }
        return mode.getRule(node, min, max, c);
    }

    /**
     * Get the next-match handler after the current one
     * @param node  The node to be matched
     * @param mode  The processing mode
     * @param currentRule The current template rule
     * @param c     The dynamic context for the transformation
     * @return      The template rule to be executed
     * @throws XPathException
     */

    public Rule getNextMatchHandler(NodeInfo node, Mode mode, Rule currentRule, XPathContext c)
    throws XPathException {
        if (mode==null) {
            mode = defaultMode;
        }
        return mode.getNextMatchRule(node, currentRule, c);
    }

    /**
     * Explain (that is, output the expression tree) all template rules
     */

    public void explainTemplateRules(ExpressionPresenter presenter) {
        presenter.startElement("templateRules");
        defaultMode.explainTemplateRules(presenter);
        Iterator iter = modes.values().iterator();
        while (iter.hasNext()) {
            Mode mode = (Mode)iter.next();
            int s = presenter.startElement("mode");
            if (!mode.isDefaultMode()) {
                presenter.emitAttribute("name", mode.getModeName().getDisplayName());
            }
            mode.explainTemplateRules(presenter);
            int e = presenter.endElement();
            if (s != e) {
                throw new IllegalStateException("tree unbalanced");
            }
        }

        presenter.endElement();
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
