package org.orbeon.saxon.style;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;

import javax.xml.transform.TransformerException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

/**
* A saxon:script element in the stylesheet.
*/

public class SaxonScript extends StyleElement {

    private Class javaClass = null;
    private String implementsURI = null;
    private String language = null;

    public void prepareAttributes() throws XPathException {

	    String languageAtt = null;
	    String implementsAtt = null;
	    String srcAtt = null;
	    String archiveAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.LANGUAGE) {
        		languageAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.IMPLEMENTS_PREFIX) {
        		implementsAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.SRC) {
        		srcAtt = atts.getValue(a).trim();
        	} else if (f==StandardNames.ARCHIVE) {
        		archiveAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (implementsAtt==null) {
            reportAbsence("implements-prefix");
            return;
        }
        implementsURI = getURIForPrefix(implementsAtt, false);
        if (implementsURI == null) {
            undeclaredNamespaceError(implementsAtt, SaxonErrorCode.SXXF0002);
            return;
        }

        if (languageAtt==null) {
            reportAbsence("language");
            return;
        }
        language = languageAtt;

        if (language.equals("java")) {
            if (srcAtt==null) {
                compileError("For java, the src attribute is mandatory");
                return;
            }
            if (!srcAtt.startsWith("java:")) {
                compileError("The src attribute must be a URI of the form java:full.class.Name");
                return;
            }
            String className = srcAtt.substring(5);

            if (archiveAtt==null) {
                try {
                    javaClass = getConfiguration().getClass(className, false, null);
                } catch (TransformerException err) {
                    compileError(err);
                    return;
                }
            } else {
                URL base;
                try {
                    base = new URL(getBaseURI());
                } catch (MalformedURLException err) {
                    compileError("Invalid base URI " + getBaseURI());
                    return;
                }
                StringTokenizer st = new StringTokenizer(archiveAtt);
                int count = 0;
                while (st.hasMoreTokens()) {
                    count++;
                    st.nextToken();
                }
                URL[] urls = new URL[count];
                count = 0;
                st = new StringTokenizer(archiveAtt);
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    try {
                        urls[count++] = new URL(base, s);
                    } catch (MalformedURLException err) {
                        compileError("Invalid URL " + s);
                        return;
                    }
                }
                try {
                    javaClass = new URLClassLoader(urls).loadClass(className);
                } catch (java.lang.ClassNotFoundException err) {
                    compileError("Cannot find class " + className + " in the specified archive"
                                    + (count>1 ? "s" : ""));
                } catch (java.lang.NoClassDefFoundError err2) {
                    compileError("Cannot use the archive attribute with this Java VM");
                }
            }
        } else {
            // language != java
            compileError("The only language supported for Saxon extension functions is 'java'");
        }
        getPrincipalStylesheet().declareJavaClass(implementsURI, javaClass);
    }

    public void validate() throws XPathException {
        checkTopLevel(null);
    }

    public Expression compile(Executable exec) throws XPathException {
        return null;
    }


    /**
    * Get the Java class, if this saxon:script element matches the specified URI.
    * Otherwise return null
    */

//    private Class getJavaClass(String uri) {
//        if (language==null) {
//            // allow for forwards references, but don't bother reporting
//            // any errors; that will happen when the element is processed
//            // in its own right.
//            try {
//                prepareAttributes();
//            } catch (TransformerConfigurationException e) {
//                return null;
//            }
//        }
//        if (language.equals("java") && implementsURI.equals(uri)) {
//            return javaClass;
//        } else {
//            return null;
//        }
//    }

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

