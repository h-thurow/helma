/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 */

package helma.image;

import helma.main.Server;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory class for generating Image objects from various sources.
 *  
 */
public abstract class AbstractImageGenerator {
    protected static AbstractImageGenerator generator = null;

    /**
     * Returns an AbstractImageGenerator singleton, creating it if necessary. If the JIMI
     * package is installed, an instance of {@link helma.image.jimi.JimiGenerator JimiGenerator}
     * will be returned. Otherwise, if the javax.imageio package is available,
     * an instance of {@link helma.image.imageio.ImageIOGenerator ImageIOGenerator}
     * is returned. Additionally, the class of the AbstractImageGenerator implementation
     * to be used can be set using the <code>imageGenerator</code> property in either
     * the app.properties or server.properties file.
     *
     * @return a new AbstractImageGenerator instance
     */
    public static AbstractImageGenerator getInstance() {
        if (generator == null) {
            // first see wether an image wrapper class was specified in
            // server.properties:
            String className = null;
            if (Server.getServer() != null) {
                className = Server.getServer().getProperty("imageGenerator"); //$NON-NLS-1$
            }

            Class generatorClass = null;
            if (className == null) {
                // if no class is defined, try the default ones:
                try {
                    // start with ImageIO
                    Class.forName("javax.imageio.ImageIO"); //$NON-NLS-1$
                    // if we're still here, ImageIOWrapper can be used
                    className = "helma.image.imageio.ImageIOGenerator"; //$NON-NLS-1$
                } catch (ClassNotFoundException e1) {
                    try {
                        // use Jimi as a fallback scenaio
                        Class.forName("com.sun.jimi.core.Jimi"); //$NON-NLS-1$
                        // if we're still here, JimiWrapper can be used
                        className = "helma.image.jimi.JimiGenerator"; //$NON-NLS-1$
                    } catch (ClassNotFoundException e2) {
                        throw new RuntimeException(Messages.getString("AbstractImageGenerator.0")); //$NON-NLS-1$
                    }
                }
            }
            // now let's get the generator class and create an instance:
            try {
                generatorClass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                    Messages.getString("AbstractImageGenerator.1") + className); //$NON-NLS-1$
            }
            try {
                generator = (AbstractImageGenerator)generatorClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                    Messages.getString("AbstractImageGenerator.2") //$NON-NLS-1$
                        + className);
            }
        }
        return generator;
    }

    /**
     * @param w ...
     * @param h ...
     * 
     * @return ...
     */
    public ImageWrapper createImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        return new ImageWrapper(img, w, h, this);
    }

    /**
     * @param src ...
     * 
     * @return ...
     * @throws IOException
     */
    public ImageWrapper createImage(byte[] src) throws IOException {
        Image img = read(src);
        return img != null ? new ImageWrapper(img, this) : null;
    }
    
    /**
     * @param filenamne ...
     * 
     * @return ...
     * @throws IOException
     */
    public ImageWrapper createImage(String filenamne)
        throws IOException {
        Image img = read(filenamne);
        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param url ...
     * 
     * @return ...
     * @throws MalformedURLException
     * @throws IOException
     */
    public ImageWrapper createImage(URL url)
        throws MalformedURLException, IOException {
        Image img = read(url);
        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param input ...
     * @return ...
     * @throws IOException
     */
    public ImageWrapper createImage(InputStream input)
        throws IOException {
        Image img = read(input);
        return img != null ? new ImageWrapper(img, this) : null;
    }


    /**
     * @param iw ...
     * @param filter ...
     * 
     * @return ...
     */
    public ImageWrapper createImage(ImageWrapper iw, ImageFilter filter) {
        // use the ImagFilterOp wrapper for ImageFilters that works directly
        // on BufferedImages. The filtering is much faster like that.
        // Attention: needs testing with all the filters!
        
        return createImage(iw, new ImageFilterOp(filter));
//        Image img = ImageWaiter.waitForImage(
//            Toolkit.getDefaultToolkit().createImage(
//                new FilteredImageSource(iw.getSource(), filter)));
//        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param iw ...
     * @param imageOp ...
     * 
     * @return ...
     */
    public ImageWrapper createImage(ImageWrapper iw, BufferedImageOp imageOp) {
        Image img = imageOp.filter(iw.getBufferedImage(), null);
        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param filename the filename of the image to create
     *
     * @return the newly created image
     * @throws IOException
     */
    public Image read(String filename) throws IOException {
        return ImageIO.read(new File(filename));
    }

    /**
     * @param url the URL of the image to create
     *
     * @return the newly created image
     * @throws IOException
     */
    public Image read(URL url) throws IOException {
        return ImageIO.read(url);
    }

    /**
     * @param src the data of the image to create
     *
     * @return the newly created image
     * @throws IOException
     */
    public Image read(byte[] src) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(src));
    }

    /**
     * @param input the data of the image to create
     *
     * @return the newly created image
     * @throws IOException
     */
    public Image read(InputStream input) throws IOException {
        return ImageIO.read(input);
    }

    /**
     * Saves the image. Image format is deduced from filename.
     * 
     * @param wrapper
     * @param filename
     * @param quality
     * @param alpha
     * @throws IOException
     */
    public abstract void write(ImageWrapper wrapper, String filename,
        float quality, boolean alpha) throws IOException;

    /**
     * Saves the image. Image format is deduced from the dataSource.
     * 
     * @param wrapper
     * @param out
     * @param quality
     * @param alpha
     * @throws IOException
     */
    public abstract void write(ImageWrapper wrapper, OutputStream out, String type,
        float quality, boolean alpha) throws IOException;
}