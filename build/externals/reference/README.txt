This directory contains the files needed to generate the Javascript specific
documentation for Helma.

The app directory contains code from the jsdoc-toolkit project. To find out 
more about jsdoc-toolkit or download the whole release, please visit the 
official project homepage:

 http://code.google.com/p/jsdoc-toolkit/

You may use the following command to build the documentation manually from 
the main helma directory:

 java -Djsdoc.dir=work/reference -jar lib/rhino.jar work/reference/app/run.js \
   -t=work/reference/templates -d=docs/framework -r=3 \
   work/reference/coreEnvironment/ work/reference/coreExtensions/ modules/
