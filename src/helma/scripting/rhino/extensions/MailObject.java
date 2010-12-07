/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.scripting.rhino.extensions;

import helma.util.*;
import org.mozilla.javascript.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import javax.activation.*;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 * A JavaScript wrapper around a JavaMail message class to send
 * mail via SMTP from Helma
 */
public class MailObject extends ScriptableObject {

    private static final long serialVersionUID = -4834981850233741039L;

    public static final int OK = 0;
    public static final int SUBJECT = 10;
    public static final int TEXT = 11;
    public static final int MIMEPART = 12;
    public static final int TO = 20;
    public static final int CC = 21;
    public static final int BCC = 22;
    public static final int FROM = 23;
    public static final int REPLYTO = 24;
    public static final int SEND = 30;

    MimeMessage message;
    Multipart multipart;
    String multipartType = "mixed"; //$NON-NLS-1$
    StringBuffer buffer;
    int status;

    // these are only set on the prototype object
    Session session = null;
    Properties props = null;
    String host = null;

    /**
     * Creates a new Mail object.
     */
    MailObject(Session session) {
        this.status = OK;
        this.message = new MimeMessage(session);
    }


    /**
     * Creates a new MailObject prototype.
     *
     * @param mprops the Mail properties
     */
    MailObject(Properties mprops) {
        this.status = OK;
        this.props = mprops;
    }

    /**
     *  Overrides abstract method in ScriptableObject
     */
    @Override
    public String getClassName() {
        return "Mail"; //$NON-NLS-1$
    }

    /**
     * Get the cached JavaMail session. This is similar to Session.getDefaultSession(),
     * except that we check if the properties have changed.
     */
    protected Session getSession() {
        if (this.props == null) {
            throw new NullPointerException(Messages.getString("MailObject.0")); //$NON-NLS-1$
        }

        // set the mail encoding system property if it isn't set. Necessary
        // on Macs, where we otherwise get charset=MacLatin
        // http://java.sun.com/products/javamail/javadocs/overview-summary.html
        System.setProperty("mail.mime.charset", //$NON-NLS-1$
                           this.props.getProperty("mail.charset", "ISO-8859-15"));  //$NON-NLS-1$//$NON-NLS-2$

        // get the host property - first try "mail.host", then "smtp" property
        String newHost = this.props.getProperty("mail.host"); //$NON-NLS-1$
        if (newHost == null) {
            newHost = this.props.getProperty("smtp"); //$NON-NLS-1$
        }

        // has the host changed?
        boolean hostChanged = (this.host == null && newHost != null) ||
                              (this.host != null && !this.host.equals(newHost));

        if (this.session == null || hostChanged) {
            this.host = newHost;

            // create properties and for the Session. Only set mail host if it is
            // explicitly set, otherwise we'll go with the system default.
            Properties sessionProps = new Properties();
            if (this.host != null) {
                sessionProps.put("mail.smtp.host", this.host); //$NON-NLS-1$
            }

            this.session = Session.getInstance(sessionProps);
        }

        return this.session;
    }

    /**
     * JavaScript constructor, called by the Rhino runtime.
     */
    public static MailObject mailObjCtor(Context cx, Object[] args,
                Function ctorObj, boolean inNewExpr) {
        MailObject proto = (MailObject) ctorObj.get("prototype", ctorObj); //$NON-NLS-1$
        return new MailObject(proto.getSession());
    }

    /**
     * Initialize Mail extension for the given scope, called by RhinoCore.
     */
    public static void init(Scriptable scope, Properties props) {
        Method[] methods = MailObject.class.getDeclaredMethods();
        MailObject proto = new MailObject(props);
        proto.setPrototype(getObjectPrototype(scope));
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("mailObjCtor".equals(methods[i].getName())) { //$NON-NLS-1$
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("Mail", ctorMember, scope); //$NON-NLS-1$
        ctor.addAsConstructor(scope, proto);
        ctor.put("props", ctor, props); //$NON-NLS-1$
        String[] mailFuncs = {
                "addBCC", "addCC", "addPart", "addText", "addTo",   //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$ //$NON-NLS-5$
                "send", "setFrom", "setSubject", "setText", "setTo", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                "setReplyTo", "setMultipartType", "getMultipartType" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        try {
            proto.defineFunctionProperties(mailFuncs, MailObject.class, 0);
            proto.defineProperty("status", MailObject.class, 0); //$NON-NLS-1$
        } catch (Exception ignore) {
            System.err.println (Messages.getString("MailObject.1")+ignore); //$NON-NLS-1$
        }
    }


    /**
     *  Set the error status of this message
     *
     * @param status the new error status
     */
    protected void setStatus(int status) {
        // Only register the first error that occurrs
        if (this.status == 0) {
            this.status = status;
        }
    }

    /**
     *  Returns the error status of this message.
     *
     * @return the error status of this message
     */
    public int getStatus() {
        return this.status;
    }


    /**
     *  Add some text to a plain text message.
     */
    public void addText(String text) {
        if (text != null) {
            if (this.buffer == null) {
                this.buffer = new StringBuffer();
            }
            this.buffer.append(text);
        }
    }


    /**
     *  Set the text to a plain text message, clearing any previous text.
     */
    public void setText(String text) {
        if (text != null) {
            this.buffer = new StringBuffer(text);
        }
    }

    /**
     * Returns the MIME multipart message subtype. The default value is
  	 * "mixed" for messages of type multipart/mixed. A common value
  	 * is "alternative" for the multipart/alternative MIME type.
     * @return the MIME subtype such as "mixed" or "alternative"
     */
    public String getMultipartType() {
        return this.multipartType;
    }

    /**
     * Sets the MIME multipart message subtype. The default value is
  	 * "mixed" for messages of type multipart/mixed. A common value
  	 * is "alternative" for the multipart/alternative MIME type.
     * @param subtype the MIME subtype such as "mixed" or "alternative".
     */
    public void setMultipartType(String subtype) {
        this.multipartType = subtype;
    }

    /**
     *  Add a MIME message part to a multipart message
     *
     * @param obj the MIME part object. Supported classes are java.lang.String,
     *            java.io.File and helma.util.MimePart.
     * @param filename optional file name for the mime part
     */
    public void addPart(Object obj, Object filename) {
        try {
            if (obj == null || obj == Undefined.instance) {
                throw new IOException(Messages.getString("MailObject.2")); //$NON-NLS-1$
            }

            if (this.multipart == null) {
                this.multipart = new MimeMultipart(this.multipartType);
            }

            MimeBodyPart part = new MimeBodyPart();

            // if param is wrapped JavaObject unwrap.
            if (obj instanceof Wrapper) {
                obj = ((Wrapper) obj).unwrap();
            }

            if (obj instanceof String) {
                part.setContent(obj.toString(), "text/plain"); //$NON-NLS-1$
            } else if (obj instanceof File) {
                FileDataSource source = new FileDataSource((File) obj);

                part.setDataHandler(new DataHandler(source));
            } else if (obj instanceof MimePart) {
                MimePartDataSource source = new MimePartDataSource((MimePart) obj);

                part.setDataHandler(new DataHandler(source));
            }

            // check if an explicit file name was given for this part
            if (filename != null && filename != Undefined.instance) {
                try {
                    part.setFileName(filename.toString());
                } catch (Exception x) {}
            } else if (obj instanceof File) {
                try {
                    part.setFileName(((File) obj).getName());
                } catch (Exception x) {}
            }

            this.multipart.addBodyPart(part);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.3")+mx); //$NON-NLS-1$
            setStatus(MIMEPART);
        }

    }

    /**
     *  Set the subject of this message
     *
     * @param subject the message subject
     */
    public void setSubject(Object subject) {
            if (subject == null || subject == Undefined.instance) {
                return;
            }

        try {
            this.message.setSubject(MimeUtility.encodeWord(subject.toString()));
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.4")+mx); //$NON-NLS-1$
            setStatus(SUBJECT);
        }
    }

    /**
     * Set the Reply-to address for this message
     *
     * @param addstr the email address to set in the Reply-to header
     */
    public void setReplyTo(String addstr) {
        try {
            if (addstr.indexOf("@") < 0) { //$NON-NLS-1$
                throw new AddressException();
            }

            Address[] replyTo = new Address[1];

            replyTo[0] = new InternetAddress(addstr);
            this.message.setReplyTo(replyTo);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.5")+mx); //$NON-NLS-1$
            setStatus(REPLYTO);
        }
    }

    /**
     * Set the From address for this message
     *
     * @param addstr the email address to set in the From header
     * @param name the name this address belongs to
     */
    public void setFrom(String addstr, Object name) {
        try {
            if (addstr.indexOf("@") < 0) { //$NON-NLS-1$
                throw new AddressException();
            }

            Address address = null;

            if (name != null && name != Undefined.instance) {
                address = new InternetAddress(addstr,
                                          MimeUtility.encodeWord(name.toString()));
            } else {
                address = new InternetAddress(addstr);
            }

            this.message.setFrom(address);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.6")+mx); //$NON-NLS-1$
            setStatus(FROM);
        }
    }


    /**
     * Set the To address for this message
     *
     * @param addstr the email address to set in the To header
     * @param name the name this address belongs to
     */
    public void setTo(String addstr, Object name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.TO);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.7")+mx); //$NON-NLS-1$
            setStatus(TO);
        }

    }


    /**
     * Add a To address for this message
     *
     * @param addstr the email address to set in the To header
     * @param name the name this address belongs to
     */
    public void addTo(String addstr, Object name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.TO);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.8")+mx); //$NON-NLS-1$
            setStatus(TO);
        }

    }

    /**
     * ADd a CC address for this message
     *
     * @param addstr the email address to set in the CC header
     * @param name the name this address belongs to
     */
    public void addCC(String addstr, Object name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.CC);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.9")+mx); //$NON-NLS-1$
            setStatus(CC);
        }
    }

    /**
     *  Add a BCC address for this message
     *
     * @param addstr the email address to set in the BCC header
     * @param name the name this address belongs to
     */
    public void addBCC(String addstr, Object name) {
        try {
            addRecipient(addstr, name, Message.RecipientType.BCC);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.10")+mx); //$NON-NLS-1$
            setStatus(BCC);
        }
    }


    /**
     * Add a recipient for this message
     *
     * @param addstr the email address
     * @param name the name this address belongs to
     * @param type the type of the recipient such as To, CC, BCC
     *
     * @throws Exception ...
     * @throws AddressException ...
     */
    private void addRecipient(String addstr,
                              Object name,
                              Message.RecipientType type) throws Exception {
        if (addstr.indexOf("@") < 0) { //$NON-NLS-1$
            throw new AddressException();
        }

        Address address = null;

        if (name != null && name != Undefined.instance) {
            address = new InternetAddress(addstr,
                                          MimeUtility.encodeWord(name.toString()));
        } else {
            address = new InternetAddress(addstr);
        }

        this.message.addRecipient(type, address);
    }


    /**
     *  Send the message.
     */
    public void send() {
        // only send message if everything's ok
        if (this.status != OK) {
            System.err.println(Messages.getString("MailObject.11")+this.status); //$NON-NLS-1$
        }
        try {
            if (this.buffer != null) {
                // if we also have a multipart body, add
                // plain string as first part to it.
                if (this.multipart != null) {
                    MimeBodyPart part = new MimeBodyPart();

                    part.setContent(this.buffer.toString(), "text/plain"); //$NON-NLS-1$
                    this.multipart.addBodyPart(part, 0);
                    this.message.setContent(this.multipart);
                } else {
                    this.message.setText(this.buffer.toString());
                }
            } else if (this.multipart != null) {
                this.message.setContent(this.multipart);
            } else {
                this.message.setText(""); //$NON-NLS-1$
            }

            Transport.send(this.message);
        } catch (Exception mx) {
            System.err.println(Messages.getString("MailObject.12")+mx); //$NON-NLS-1$
            setStatus(SEND);
        }
    }
}
