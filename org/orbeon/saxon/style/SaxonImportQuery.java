package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.functions.ExecutableFunctionLibrary;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.xpath.XPathException;

import javax.xml.transform.TransformerConfigurationException;
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
     * @throws TransformerConfigurationException
     */

    public void importModule() throws TransformerConfigurationException {
        prepareAttributes();
        loadLibraryModule();
    }

    public void prepareAttributes() throws TransformerConfigurationException {

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

    public void validate() throws TransformerConfigurationException {
        checkEmpty();
        checkTopLevel(null);
    }

    private void loadLibraryModule() throws TransformerConfigurationException {

        if (href==null && moduleURI==null) {
            // error already reported
            return;
        }

        try {
            XSLStylesheet top = getPrincipalStylesheet();
            getExecutable().setFunctionLibrary(new ExecutableFunctionLibrary(getConfiguration()));
                        // this is not actually used, but is needed to keep the XQuery processor happy
            StaticQueryContext importedModule = loadModule(moduleURI, href);

            // Do the importing

            short ns = importedModule.getModuleNamespaceCode();
            NamePool pool = getTargetNamePool();
            Iterator it = importedModule.getFunctionDefinitions();
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
     * Load another query module
     */

    private StaticQueryContext loadModule(String namespaceURI, String locationURI)
    throws XPathException {
        return StaticQueryContext.loadQueryModule(
                getConfiguration(),
                getExecutable(),
                getBaseURI(),
                namespaceURI,
                locationURI,
                null
        );
    }

//        Configuration config = getPreparedStylesheet().getConfiguration();
//        StaticQueryContext mod = config.getQueryLibraryModule(namespaceURI);
//        if (mod != null) {
//            return mod;
//        }
//
//        if (locationURI == null) {
//            throw new XPathException.Static(
//                    "saxon:import-query must either specify a known namespace or a location");
//        }
//        // Resolve relative URI
//
//        URL absoluteURL;
//        String baseURI = getBaseURI();
//        if (baseURI==null) {    // no base URI available
//            try {
//                // the href might be an absolute URL
//                absoluteURL = new URL(locationURI);
//            } catch (MalformedURLException err) {
//                // it isn't
//                throw new XPathException.Static("Cannot resolve absolute URI", err);
//            }
//        } else {
//            try {
//                absoluteURL = new URL(new URL(baseURI), locationURI);
//            } catch (MalformedURLException err) {
//                throw new XPathException.Static("Cannot resolve relative URI", err);
//            }
//        }
//        try {
//            InputStream is = absoluteURL.openStream();
//            BufferedReader reader = new BufferedReader(
//                                        new InputStreamReader(is));
//
//            StringBuffer sb = new StringBuffer();
//            char[] buffer = new char[2048];
//            int actual=0;
//            while (true) {
//                actual = reader.read(buffer, 0, 2048);
//                if (actual<0) break;
//                sb.append(buffer, 0, actual);
//            }
//            StaticQueryContext module = new StaticQueryContext(config);
//            module.setBaseURI(absoluteURL.toString());
//            module.setExecutable(getExecutable());
//            QueryParser qp = new QueryParser();
//            qp.parseLibraryModule(sb.toString(), module);
//            if (module.getModuleNamespace() == null) {
//                throw new XPathException.Static(
//                        "Imported module must be a library module");
//            }
//            if (!module.getModuleNamespace().equals(namespaceURI)) {
//                throw new XPathException.Static(
//                        "Imported module's namespace does not match requested namespace");
//            }
//            config.addQueryLibraryModule(module);
//            return module;
//        } catch (java.io.IOException ioErr) {
//            throw new DynamicError(ioErr);
//        }
//    }

    public Expression compile(Executable exec) throws TransformerConfigurationException {
        return null;
        // no action. The node will never be compiled, because it replaces itself
        // by the contents of the included file.
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
