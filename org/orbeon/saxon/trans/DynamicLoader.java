package org.orbeon.saxon.trans;

/**
 *
 */
public class DynamicLoader {

    private ClassLoader classLoader;

    /**
     * Set a ClassLoader to be used when loading external classes. Examples of classes that are
     * loaded include SAX parsers, localization modules for formatting numbers and dates,
     * extension functions, external object models. In an environment such as Eclipse that uses
     * its own ClassLoader, this ClassLoader should be nominated to ensure that any class loaded
     * by Saxon is identical to a class of the same name loaded by the external environment.
     * <p>
     * This method is intended for external use by advanced users, but should be regarded as
     * experimental.
     * @param loader the ClassLoader to be used in this configuration
     */

    public void setClassLoader(ClassLoader loader) {
        classLoader = loader;
    }

    /**
     * Get the ClassLoader supplied using the method {@link #setClassLoader}.
     * If none has been supplied, return null.
     * <p>
     * This method is intended for external use by advanced users, but should be regarded as
     * experimental.
     * @return the ClassLoader used in this configuration
     */

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Load a class using the class name provided.
     * Note that the method does not check that the object is of the right class.
     * <p>
     * This method is intended for internal use only.
     *
     * @param className A string containing the name of the
     *   class, for example "com.microstar.sax.LarkDriver"
     * @param tracing true if diagnostic tracing is required
     * @param classLoader The ClassLoader to be used to load the class. If this is null, then
     * the classLoader used will be the first one available of: the classLoader registered
     * with the Configuration using {@link #setClassLoader}; the context class loader for
     * the current thread; or failing that, the class loader invoked implicitly by a call
     * of Class.forName() (which is the ClassLoader that was used to load the Configuration
     * object itself).
     * @return an instance of the class named, or null if it is not
     * loadable.
     * @throws XPathException if the class cannot be loaded.
     *
    */

    public Class getClass(String className, boolean tracing, ClassLoader classLoader) throws XPathException {
        if (tracing) {
            System.err.println("Loading " + className);
        }

        try {
            ClassLoader loader = classLoader;
            if (loader == null) {
                loader = this.classLoader;
            }
            if (loader == null) {
                loader = Thread.currentThread().getContextClassLoader();
            }
            if (loader != null) {
                try {
                    return loader.loadClass(className);
                } catch (Exception ex) {
                    return Class.forName(className);
                }
            } else {
                return Class.forName(className);
            }
        }
        catch (Exception e) {
            if (tracing) {
                // The exception is often masked, especially when calling extension
                // functions
                System.err.println("No Java class " + className + " could be loaded");
            }
            throw new XPathException("Failed to load " + className, e );
        }

    }

  /**
    * Instantiate a class using the class name provided.
    * Note that the method does not check that the object is of the right class.
   * <p>
   * This method is intended for internal use only.
   *
    * @param className A string containing the name of the
    *   class, for example "com.microstar.sax.LarkDriver"
    * @param classLoader The ClassLoader to be used to load the class. If this is null, then
     * the classLoader used will be the first one available of: the classLoader registered
     * with the Configuration using {@link #setClassLoader}; the context class loader for
     * the current thread; or failing that, the class loader invoked implicitly by a call
     * of Class.forName() (which is the ClassLoader that was used to load the Configuration
     * object itself).
    * @return an instance of the class named, or null if it is not
    * loadable.
    * @throws XPathException if the class cannot be loaded.
    *
    */

    public Object getInstance(String className, ClassLoader classLoader) throws XPathException {
        Class theclass = getClass(className, false, classLoader);
        try {
            return theclass.newInstance();
        } catch (Exception err) {
            throw new XPathException("Failed to instantiate class " + className, err);
        }
    }

    /**
      * Instantiate a class using the class name provided, with the option of tracing
      * Note that the method does not check that the object is of the right class.
      * <p>
      * This method is intended for internal use only.
      *
      * @param className A string containing the name of the
      *   class, for example "com.microstar.sax.LarkDriver"
      * @param tracing true if attempts to load classes are to be traced to the console
      * @param classLoader The ClassLoader to be used to load the class. If this is null, then
      * the classLoader used will be the first one available of: the classLoader registered
      * with the Configuration using {@link #setClassLoader}; the context class loader for
      * the current thread; or failing that, the class loader invoked implicitly by a call
      * of Class.forName() (which is the ClassLoader that was used to load the Configuration
      * object itself).
      * @return an instance of the class named, or null if it is not
      * loadable.
      * @throws XPathException if the class cannot be loaded.
      *
      */

      public Object getInstance(String className, boolean tracing, ClassLoader classLoader) throws XPathException {
          Class theclass = getClass(className, tracing, classLoader);
          try {
              return theclass.newInstance();
          } catch (Exception err) {
              throw new XPathException("Failed to instantiate class " + className, err);
          }
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

