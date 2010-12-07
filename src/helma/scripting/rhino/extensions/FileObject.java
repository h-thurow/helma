// FileIO.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package helma.scripting.rhino.extensions;


import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.EOFException;
import java.io.IOException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
  * An EcmaScript FileIO 'File' object
  */
public class FileObject extends ScriptableObject {
    private static final long serialVersionUID = -9098307162306984764L;

    File file = null;
    Object readerWriter = null;
    boolean atEOF = false;
    String lastLine = null;
    Throwable lastError = null;

    protected FileObject() {
    }

    protected FileObject(String fileName) {
        // always convert to absolute file straight away, since
        // relative file name handling is pretty broken in java.io.File
        this.file = new File(fileName).getAbsoluteFile();
    }

    protected FileObject(String pathName, String fileName) {
        // always convert to absolute file straight away, since
        // relative file name handling is pretty broken in java.io.File
        this.file = new File(pathName, fileName).getAbsoluteFile();
    }

    public static FileObject fileObjCtor(Context cx, Object[] args,
                Function ctorObj, boolean inNewExpr) {
        if (args.length == 0 || args[0] == Undefined.instance) {
            throw new IllegalArgumentException(Messages.getString("FileObject.0")); //$NON-NLS-1$
        }
        if (args.length < 2 || args[1] == Undefined.instance) {
            return new FileObject(args[0].toString());
        }
        return new FileObject(args[0].toString(), args[1].toString());
    }

    public static void init(Scriptable scope) {
        Method[] methods = FileObject.class.getDeclaredMethods();
        ScriptableObject proto = new FileObject();
        proto.setPrototype(getObjectPrototype(scope));
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("fileObjCtor".equals(methods[i].getName())) { //$NON-NLS-1$
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("File", ctorMember, scope); //$NON-NLS-1$
        ctor.addAsConstructor(scope, proto);
        String[] fileFuncs = {
                                "toString", //$NON-NLS-1$
                                "getName", //$NON-NLS-1$
                                "getParent", //$NON-NLS-1$
                                "isAbsolute", //$NON-NLS-1$
                                "write", //$NON-NLS-1$
                                "remove", //$NON-NLS-1$
                                "list", //$NON-NLS-1$
                                "flush", //$NON-NLS-1$
                                "writeln", //$NON-NLS-1$
                                "close", //$NON-NLS-1$
                                "getPath", //$NON-NLS-1$
                                "open", //$NON-NLS-1$
                                "error", //$NON-NLS-1$
                                "canRead", //$NON-NLS-1$
                                "canWrite", //$NON-NLS-1$
                                "exists", //$NON-NLS-1$
                                "getAbsolutePath", //$NON-NLS-1$
                                "getLength", //$NON-NLS-1$
                                "isDirectory", //$NON-NLS-1$
                                "isFile", //$NON-NLS-1$
                                "lastModified", //$NON-NLS-1$
                                "mkdir", //$NON-NLS-1$
                                "renameTo", //$NON-NLS-1$
                                "eof", //$NON-NLS-1$
                                "isOpened", //$NON-NLS-1$
                                "readln", //$NON-NLS-1$
                                "clearError", //$NON-NLS-1$
                                "readAll" //$NON-NLS-1$
                               };
        try {
            proto.defineFunctionProperties(fileFuncs, FileObject.class, 0);
        } catch (Exception ignore) {
            System.err.println (Messages.getString("FileObject.1")+ignore); //$NON-NLS-1$
        }
    }

    @Override
    public String getClassName() {
        return "File"; //$NON-NLS-1$
    }

    @Override
    public String toString() {
         if (this.file==null) return "<null>"; //$NON-NLS-1$
         return this.file.toString();
    }

    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" + //$NON-NLS-1$ //$NON-NLS-2$
            ((this.file == null) ? "null" : this.file.toString()) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected void setError(Throwable e) {
        this.lastError = e;
    }

    public boolean exists() {
        if (this.file == null) return false;
        return this.file.exists();
    }

    public boolean open() {
        if (this.readerWriter != null) {
            setError(new IllegalStateException(Messages.getString("FileObject.2"))); //$NON-NLS-1$
            return false;
        }
        if (this.file == null) {
            setError(new IllegalArgumentException(Messages.getString("FileObject.3"))); //$NON-NLS-1$
            return false;
        }

        // We assume that the BufferedReader and PrintWriter creation
        // cannot fail except if the FileReader/FileWriter fails.
        // Otherwise we have an open file until the reader/writer
        // get garbage collected.
        try{
           if (this.file.exists()) {
               this.readerWriter = new BufferedReader(new FileReader(this.file));
           } else {
               this.readerWriter = new PrintWriter(new FileWriter(this.file));
           }
           return true;
       } catch (IOException e) {
           setError(e);
           return false;
       }
    }

    public boolean isOpened() {
       return (this.readerWriter != null);
    }

    public boolean close() {
       if (this.readerWriter == null)
                       return false;
       try {
          if (this.readerWriter instanceof Reader) {
              ((Reader) this.readerWriter).close();
          } else {
              ((Writer) this.readerWriter).close();
          }
          this.readerWriter = null;
          return true;
       } catch (IOException e) {
           setError(e);
           this.readerWriter = null;
           return false;
       }
    }
   
    public boolean write(Object what) {
        if (this.readerWriter == null) {
            setError(new IllegalStateException(Messages.getString("FileObject.4"))); //$NON-NLS-1$
            return false;
        }
        if (! (this.readerWriter instanceof PrintWriter)) {
            setError(new IllegalStateException(Messages.getString("FileObject.5"))); //$NON-NLS-1$
            return false;
        }
        PrintWriter writer = (PrintWriter) this.readerWriter;
        if (what != null) {
            writer.print(what.toString());
        }
        // writer.println();
        return true;
    }

    public boolean writeln(Object what) {
        if (this.readerWriter == null) {
            setError(new IllegalStateException(Messages.getString("FileObject.6"))); //$NON-NLS-1$
            return false;
        }
        if (! (this.readerWriter instanceof PrintWriter)) {
            setError(new IllegalStateException(Messages.getString("FileObject.7"))); //$NON-NLS-1$
            return false;
        }
        PrintWriter writer = (PrintWriter) this.readerWriter;
        if (what != null) {
            writer.print(what.toString());
        }
        writer.println();
        return true;
    }

    public String readln() {
        if (this.readerWriter == null) {
            setError(new IllegalStateException(Messages.getString("FileObject.8"))); //$NON-NLS-1$
            return null;
        }
        if (! (this.readerWriter instanceof BufferedReader)) {
            setError(new IllegalStateException(Messages.getString("FileObject.9"))); //$NON-NLS-1$
            return null;
        }
        if (this.atEOF) {
            setError(new EOFException());
            return null;
        }
        if (this.lastLine!=null) {
            String line = this.lastLine;
            this.lastLine = null;
            return line;
        }
        BufferedReader reader = (BufferedReader) this.readerWriter;
        // Here lastLine is null, return a new line
        try {
          String line = reader.readLine();
          if (line == null) {
              this.atEOF = true;
              setError(new EOFException());
          }
          return line;
        } catch (IOException e) {
          setError(e);
          return null;
        }
    }

    public boolean eof() {
        if (this.readerWriter == null) {
            setError(new IllegalStateException(Messages.getString("FileObject.10"))); //$NON-NLS-1$
            return true;
        }
        if (! (this.readerWriter instanceof BufferedReader)) {
            setError(new IllegalStateException(Messages.getString("FileObject.11"))); //$NON-NLS-1$
            return true;
        }
        if (this.atEOF) return true;
        if (this.lastLine!=null) return false;
        BufferedReader reader = (BufferedReader) this.readerWriter;
        try {
          this.lastLine = reader.readLine();
          if (this.lastLine == null) this.atEOF = true;
          return this.atEOF;
        } catch (IOException e) {
          setError(e);
          return true;
        }
    }

    public boolean isFile() {
        if (this.file == null) {
            setError(new IllegalArgumentException(Messages.getString("FileObject.12"))); //$NON-NLS-1$
            return false;
        }
        return this.file.isFile();
    }

    public boolean isDirectory() {
        if (this.file == null) {
            setError(new IllegalArgumentException(Messages.getString("FileObject.13"))); //$NON-NLS-1$
            return false;
        }
        return this.file.isDirectory();
    }

    public boolean flush() {
        if (this.readerWriter == null) {
            setError(new IllegalStateException(Messages.getString("FileObject.14"))); //$NON-NLS-1$
            return false;
        }
        if (this.readerWriter instanceof Writer) {
              try {
                  ((Writer) this.readerWriter).flush();
             } catch (IOException e) {
                 setError(e);
                 return false;
             }
        } else {
              setError(new IllegalStateException(Messages.getString("FileObject.15"))); //$NON-NLS-1$
              return false; // not supported by reader
        }
        return true;
    }
   
   
    public double getLength() {
       if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.16"))); //$NON-NLS-1$
           return -1;
       }
       return this.file.length();
    }
  
    public double lastModified() {
       if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.17"))); //$NON-NLS-1$
           return 0L;
       }
       return this.file.lastModified();
    }
  
    public String error() {
      if (this.lastError == null) {
          return ""; //$NON-NLS-1$
      }
    String exceptionName = this.lastError.getClass().getName();
      int l = exceptionName.lastIndexOf("."); //$NON-NLS-1$
      if (l>0) exceptionName = exceptionName.substring(l+1);
      return exceptionName +": " + this.lastError.getMessage(); //$NON-NLS-1$
    }
   
    public void clearError() {
        this.lastError = null;
    }
   
    public boolean remove() {
       if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.18"))); //$NON-NLS-1$
           return false;
       }
       if (this.readerWriter != null) {
           setError(new IllegalStateException(Messages.getString("FileObject.19"))); //$NON-NLS-1$
           return false;
       }
       return this.file.delete();
    }
   
    public boolean renameTo(FileObject toFile) {
       if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.20"))); //$NON-NLS-1$
           return false;
       }
       if (toFile.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.21"))); //$NON-NLS-1$
           return false;
       }
       if (this.readerWriter != null) {
           setError(new IllegalStateException(Messages.getString("FileObject.22"))); //$NON-NLS-1$
           return false;
       }
       if (toFile.readerWriter!=null) {
           setError(new IllegalStateException(Messages.getString("FileObject.23"))); //$NON-NLS-1$
           return false;
       }
       return this.file.renameTo(toFile.file);
    }
   
    public boolean canRead() {
        if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.24"))); //$NON-NLS-1$
           return false;
        }
        return this.file.canRead();
    }
    
    public boolean canWrite() {
        if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.25"))); //$NON-NLS-1$
           return false;
        }
        return this.file.canWrite();
    }
    
    public String getParent() {
        if (this.file == null) {
            setError(new IllegalArgumentException(Messages.getString("FileObject.26"))); //$NON-NLS-1$
            return ""; //$NON-NLS-1$
        }
        String parent = this.file.getParent();
        return (parent==null ? "" : parent); //$NON-NLS-1$
    }
    
    public String getName() {
        if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.27"))); //$NON-NLS-1$
           return ""; //$NON-NLS-1$
        }
        String name = this.file.getName();
        return (name==null ? "" : name); //$NON-NLS-1$
    }
    
    public String getPath() {
        if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.28"))); //$NON-NLS-1$
           return ""; //$NON-NLS-1$
        }
        String path = this.file.getPath();
        return (path==null ? "" : path); //$NON-NLS-1$
    }
    
    public String getAbsolutePath() {
        if (this.file == null) {
           setError(new IllegalArgumentException(Messages.getString("FileObject.29"))); //$NON-NLS-1$
           return ""; //$NON-NLS-1$
        }
        String absolutPath = this.file.getAbsolutePath();
        return (absolutPath==null ? "" : absolutPath); //$NON-NLS-1$
    }
    
    public boolean isAbsolute() {
        if (this.file == null) return false;
        return this.file.isAbsolute();
    }
    
    public boolean mkdir() {
        if (this.file == null) return false;
        if(this.readerWriter != null) return false;
        return this.file.mkdirs();   // Using multi directory version
    }
    
    public Object list() {
        if (this.file == null) return null;
        if(this.readerWriter != null) return null;
        if (!this.file.isDirectory()) return null;
        return this.file.list();   
    }
    
    
    public String readAll() {
        // Open the file for readAll
        if (this.readerWriter != null) {
            setError(new IllegalStateException(Messages.getString("FileObject.30"))); //$NON-NLS-1$
            return null;
        }
        if (this.file == null) {
            setError(new IllegalArgumentException(Messages.getString("FileObject.31"))); //$NON-NLS-1$
            return null;
        }
        try{ 
           if (this.file.exists()) {
               this.readerWriter = new BufferedReader(new FileReader(this.file));
           } else {
               setError(new IllegalStateException(Messages.getString("FileObject.32"))); //$NON-NLS-1$
               return null;
           }
           if(!this.file.isFile()) {
               setError(new IllegalStateException(Messages.getString("FileObject.33"))); //$NON-NLS-1$
               return null;
           }

           // read content line by line to setup properl eol
           StringBuffer buffer = new StringBuffer((int) (this.file.length()*1.10));
           BufferedReader reader = (BufferedReader) this.readerWriter;
           while (true) {
              String line = reader.readLine();
              if (line == null) {
                  break;
              }
              buffer.append(line);
              buffer.append("\n");  // EcmaScript EOL //$NON-NLS-1$
           }

           
           // Close the file
           ((Reader) this.readerWriter).close();
           this.readerWriter = null;
           return buffer.toString();
       } catch (IOException e) {
           this.readerWriter = null;
           setError(e);
           return null;
       }
    }

    protected File getFile() {
        return this.file;
    }
  
} //class FileObject

