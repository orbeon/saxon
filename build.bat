
@setlocal enabledelayedexpansion

@set this_fqn=%~fs0

@for %%i in ( %this_fqn% ) do set bld_dir=%%~dp%i

@mkdir %bld_dir%\classes

@dir /s /b /a %bld_dir%\org\*.java | findstr /v org.orbeon.saxon.jdom | findstr /v org.orbeon.saxon.xom | findstr /v org.orbeon.saxon.dom | findstr /v org.orbeon.saxon.xpath > %bld_dir%\files.txt

@javac -source 1.4 -target 1.4 -classpath %CP% -g -d %bld_dir%\classes @%bld_dir%\files.txt && (
  @jar cf0 saxon-8_4_orbeon.jar  META-INF\MANIFEST.MF META-INF\services\javax.xml.transform.TransformerFactory META-INF\services\javax.xml.xpath.XPathFactory -C classes org
)



