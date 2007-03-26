package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.functions.ExecutableFunctionLibrary;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.query.QueryReader;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.query.XQueryFunction;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.query.ModuleURIResolver;

import javax.xml.transform.stream.StreamSource;
import java.util.Iterator;


/**
* The class implements a saxon:import-query declaration in a stylesheet. This
 * declaration imports an XQuery library module and adds the functions defined
 * in that module to the static context, making them available for calling from
 * XPath expressions in the stylesheet.
*/

public class SaxonImportQuery extends StyleElement {

    private String href;
    private String moduleURI;

    /**
     * The importModule() method is called very early, before preparing the attributes,
     * to make sure that all functions in the imported modules are available in the static
     * context.
     * @throws XPathException
     */

    public void importModule() throws XPathException {
        prepareAttributes();
        loadLibraryModule();
    }

    public void prepareAttributes() throws XPathException {

        // Avoid reporting errors twice
        if (href!=null || moduleURI!=null) {
            return;
        }

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.HREF) {
        		href = atts.getValue(a).trim();
        	} else if (f==StandardNames.NAMESPACE) {
        		moduleURI = atts.getValue(a).trim();
            } else {
        		checkUnknownAttribute(nc);
                moduleURI="";   // for error recovery path
        	}
        }

        if (href==null && moduleURI==null) {
            compileError("At least one of href or namespace must be specified");
            moduleURI="";   // for error recovery path
        }
    }

    public void validate() throws XPathException {
        checkEmpty();
        checkTopLevel(null);
    }

    private void loadLibraryModule() throws XPathException {

        if (href==null && moduleURI==null) {
            // error already reported
            return;
        }

        try {
            XSLStylesheet top = getPrincipalStylesheet();
            getExecutable().setFunctionLibrary(new ExecutableFunctionLibrary(getConfiguration()));
                        // this is not actually used, but is needed to keep the XQuery processor happy
            StaticQueryContext importedModule = loadModule();

            // Do the importing

            short ns = importedModule.getModuleNamespaceCode();
            NamePool pool = getTargetNamePool();
            Iterator it = importedModule.getGlobalFunctionLibrary().getFunctionDefinitions();
            while (it.hasNext()) {
                XQueryFunction def = (XQueryFunction)it.next();
                // don't import functions transitively
                if (pool.getURICode(def.getFunctionFingerprint()) == ns) {
                    top.declareXQueryFunction(def);
                }
                // Note, we are not importing global variables at present
            }
        } catch (XPathException err) {
            compileError(err);
        }
    }

    /**
     * Load a query module
     */

    private StaticQueryContext loadModule() throws XPathException {
        // Call the module URI resolver to find the module or modules

        ModuleURIResolver resolver = getConfiguration().getModuleURIResolver();
        if (resolver == null) {
            resolver = getConfiguration().getStandardModuleURIResolver();
        }

        String[] hints = {href};
        StreamSource[] sources;
        try {
            sources = resolver.resolve(moduleURI, getBaseURI(), hints);
            if (sources == null) {
                resolver = getConfiguration().getStandardModuleURIResolver();
                sources = resolver.resolve(moduleURI, getBaseURI(), hints);
            }
        } catch (XPathException e) {
            throw StaticError.makeStaticError(e);
        }

        if (sources.length != 1) {
            StaticError err = new StaticError("Query module resolver must return a single module");
            throw err;
        }


        StreamSource ss = sources[0];
        String baseURI = ss.getSystemId();
        if (baseURI == null) {
            ss.setSystemId(hints[0]);
        }
        String queryText = QueryReader.readSourceQuery(ss, getConfiguration().getNameChecker());
        StaticQueryContext sqc = StaticQueryContext.makeStaticQueryContext(
                baseURI, getExecutable(), null, queryText, moduleURI);
        getExecutable().fixupQueryModules(sqc);
        return sqc;


    }


    public Expression compile(Executable exec) throws XPathException {
        exec.setReasonUnableToCompile("Cannot compile a stylesheet that imports an XQuery library module");
        return null;
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
