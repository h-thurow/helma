/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2007 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Image.js,v $
 * $Author: zumbrunn $
 * $Revision: 9139 $
 * $Date: 2008-07-01 20:47:55 +0200 (Die, 01. Jul 2008) $
 */

/**
 * @fileoverview Default properties and methods of  
 * the Image prototype.
 */


/**
 * Helma's built-in image object allows you to read, manipulate, and save images.
 * <br /><br />
 * An image object is created using the Image() constructor.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/image.gif");</pre>
 * 
 * @param {String} img as String of a URL, or a java.io.InputStream object
 * @constructor
 */
function Image(img){}


/**
 * Cuts out (crops) a rectanglular area of an image.
 * <br /><br />
 * The dimensions of the area are calculated from the 
 * xNumber- and yNumber-offsets (ie. from the top left 
 * corner of the image as upper left reference point) 
 * to xNumber + widthNumber and yNumber + heightNumber 
 * as lower right reference point.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.gif");
 * res.write('&lt:img src="/images/original.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/hopcropanim.gif" />
 * &nbsp;
 * img.crop(58, 9, 48, 21);
 * img.saveAs("/www/images/processed.gif");
 * res.write('&lt;img src="/images/processed.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/crop.gif" /></pre>
 * 
 * @param {Number} x as Number, offset from the top left corner of the image
 * @param {Number} y as Number, offset from the lower right corner of image
 * @param {Number} width as Number, the width of the image
 * @param {Number} height as Number, the height of the image
 */
Image.prototype.crop = function(x, y, width, height) {};


/**
 * Disposes an Image object that is no longer needed. 
 * <br /><br />
 * If an instance of java.awt.Graphics has been allocated 
 * for the Image, the dispose() method is called on it to 
 * free its resources.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.jpg");
 * // do something useful or funny with the image
 * img.dispose();</pre>
 */
Image.prototype.dispose = function() {};


/**
 * Draws a line onto an image.
 * <br /><br />
 * The line starts at the reference point defined by the offsets
 * x1Number and y1Number from the top left corner of the image 
 * and ends at the reference point defined by the offsets x2Number
 * and y2Number.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.gif");
 * res.write('&lt;img src="/images/original.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/helmalogo.gif" />
 * &nbsp;
 * img.setColor(204, 0, 0);
 * img.drawLine(58, 26, 100, 26);
 * img.saveAs("/www/images/processed.gif");
 * res.write('&lt;img src="/images/processed.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/drawline.gif" />
 * 
 * @param {Number} xStart as Number, horizontal reference point from top left corner of the image
 * @param {Number} yStart as Number, vertical reference point from top left corner of the image
 * @param {Number} xEnd as Number, horizontal reference point where the line ends in the image
 * @param {Number} yEnd as Number, vertical reference point where the line ends in the image
 */
Image.prototype.drawLine = function(xStart, yStart, xEnd, yEnd) {};


/**
 * Draws a rectangle onto an image.
 * <br /><br />
 * The rectangle's dimensions are calculated from the 
 * xNumber- and yNumber-offset (ie. from the top left 
 * corner of the image as upper left reference point) 
 * to xNumber + widthNumber and yNumber + heightNumber 
 * as lower right reference point.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/originalgif");
 * res.write('&lt;img src="/images/original.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/helmalogo.gif" />
 * &nbsp;
 * img.setColor(204, 0, 0);
 * img.drawRect(57, 8, 46, 20);
 * img.saveAs("/www/images/processed.gif");
 * res.write('&lt;img src="/images/processed.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/drawrect.gif" />
 * 
 * @param {Number} x as Number, as upper left reference point. 
 * @param {Number} y as Number, as the lower right reference point upperleft
 * @param {Number} width as Number, width of the crop
 * @param {Number} height as Number, height of the crop
 */
Image.prototype.drawRect = function(x, y, width, height) {};


/**
 * Draws text onto an image.
 * <br /><br />
 * The string will be drawn starting at the xNumber- 
 * and yNumber-offset from the top left corner of the image.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.gif");
 * res.write('&lt;img src="/images/original.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/helmalogo.gif' />
 * &nbsp;
 * img.setColor(204, 0, 0);
 * img.drawString("rocks!", 82, 35);
 * img.saveAs("/www/images/processed.gif");
 * res.write('&lt;img src="/images/processed.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/drawstring.gif" />
 * 
 * @param {String} textToDraw as String, the string to be drawn on the image.
 * @param {Number} x as Number, horizontal offset from the top left corner of the image.
 * @param {Number} y as Number, vertical offset from the top left corner of the image.
 */
Image.prototye.drawString = function(textToDraw, x, y) {};


/**
 * Draws a filled rectangle onto an image.
 * <br /><br />
 * The rectangle's dimensions are calculated from the 
 * xNumber- and yNumber-offset (ie. from the top left 
 * corner of the image as upper left reference point) 
 * to xNumber + widthNumber and yNumber + heightNumber 
 * as lower right reference point.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.gif");
 * res.write('&lt;img src="/images/original.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/helmalogo.gif" />
 * &nbsp;
 * img.setColor(204, 0, 0);
 * img.fillRect(58, 27, 43, 29);
 * img.saveAs("/www/images/processed.gif");
 * res.write('&lt;img src="/images/processed.gif" /&gt;');
 * &nbsp;
 * </pre><img src="http://helma.org/static/images/helma/fillrect.gif" />
 * 
 * @param {Number} x as Number, from the top left corner of the image.
 * @param {Number} y as Number, from the lower reference point.
 * @param {Number} width as Number, width of the rectangle
 * @param {Number} height as Number, height of the rectangle
 */
Image.prototype.fillRect = function(x, y, width, height) {};


/**
 * Returns the height of an image measured in pixels.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/hop.gif");
 * res.write('&lt;img src="/images/hop.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/helmalogo.gif" />
 * &nbsp;
 * var height = img.getHeight();
 * res.write(height);
 * <i>35</i></pre>
 * 
 * @return Number
 * @type Number
 */
Image.prototype.getHeight = function() {};


/**
 * Returns an ImageInfo object for an image.
 * <br /><br />
 * This function allows to retrieve image properties 
 * such as width, height and MIME type without the need 
 * to fully decode the image.
 * <br /><br />
 * Example:
 * &nbsp;
 * <pre>var info = Image.getInfo("http://helma.org/images/hop.gif");
 * if (info) {
 * &nbsp;&nbsp;res.writeln('width: ' + info.width);
 * &nbsp;&nbsp;res.writeln('height: ' + info.height);
 * }
 * &nbsp;
 * <i>width: 174
 * height: 35</i>
 * <br /><br />
 * Note that in contrast to the other Image function, this is called on the 
 * constructor rather than on a decoded Image object. This is because the 
 * purpose of this function is to avoid the expensive image decoding 
 * process if we're just interested in some of the image's properties.
 * <br /><br />
 * The function returns an instance of Marco Schmidt's ImageInfo object 
 * if the image could be read, or null if the image couldn't be read. 
 * See the ImageInfo API documentation for the full set of methods. 
 * (Remember that you can getters as properties in Rhino as shown in the example above.)
 * 
 * @param {String} pathOfImage as String, it can be a path, url, byteArray or inputstream.
 * @return ImageInfo
 * @type ImageInfo
 */
Image.prototype.getInfo = function(pathOfImage) {};


/**
 * Returns information about the pixel at x, y. 
 * <br /><br />
 * If the image is indexed, it returns the palette index, otherwise 
 * the rgb code of the color is returned.
 * 
 * @param {Number} x the x coordinate of the pixel
 * @param {Number} y the y coordinate of the pixel
 * @return the pixel at x, y
 */
Image.prototype.getPixel(x,y) {};


/**
 * Retrieves a Java-compatible image object from a Helma image object.
 * <br /><br />
 * To use some of the image filters provided by the JIMI Java image 
 * processing package com.sun.jimi.core.filters, a specific Java 
 * class of image objects is needed.
 * <br /><br />
 * Helma wraps the sun.awt.image objects into a custom class. 
 * To use Helma image objects with JIMI's filters these have to be 
 * "unwrapped" using the getSource() function.
 * <br /><br />
 * <b>The following filter functions have been successfully applied the way as described in the examples below:</b>
 * <pre>&nbsp;&nbsp;* Rotate(degreeNumber)
 * &nbsp;&nbsp;* Gray()
 * &nbsp;&nbsp;* Flip(typeNumber)
 * &nbsp;&nbsp;* Oil(intensityNumber)
 * &nbsp;&nbsp;* Invert()
 * &nbsp;&nbsp;* Smooth(intensityNumber)
 * &nbsp;&nbsp;* Shear(degreeNumber)
 * &nbsp;&nbsp;* Edges()
 * &nbsp;&nbsp;* Shrink(multiplyNumber)
 * &nbsp;&nbsp;* Enlarge(divisionNumber)</pre>
 * <br /><br />
 * Please take into account that the quality might suffer depending on the type 
 * and amount of filters applied to the image.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/static/original.jpg");
 * var filters = Packages.com.sun.jimi.core.filters;
 * var rotator = new filters.Rotate(45);
 * var processed = new Image(img, rotator);
 * processed.saveAs("/path/to/static/processed.jpg");
 * res.write('&lt;img src="/static/processed.jpg" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/rotated45.jpg" />
 * &nbsp;
 * var oil = new filters.Oil(img.getSource(), 3);
 * var processed = new Image(img, oil);
 * processed.saveAs("/path/to/static/processed.jpg");
 * res.write('&lt;img src="/static/processed.jpg" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/oilfiltered.jpg" />
 * 
 * @return Packages.com.sun.awt.image
 * @type awt.image
 * @see Packages.com.sun.awt.image
 */
Image.prototype.getSource = function() {};


/**
 * Returns the width of an image measured in pixels.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/hop.gif");
 * res.write('&lt;img src="/images/hop.gif" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/helmalogo.gif" />
 * &nbsp;
 * var width = img.getWidth();
 * res.write(width);
 * <i>174</i></pre>
 * 
 * @return Number
 * @type Number
 */
Image.prototype.getWidth = function() {};


/**
 * Reduces the number of available colors (color depth) in an image.
 * <br /><br />
 * Note: GIF images need a color depth of 256 colors maximum. Use this function with caution, generally.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.jpg");
 * res.write('&lt;img src="/images/original.jpg" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/hawatosch.jpg" />
 * &nbsp;
 * img.reduceColors(8);
 * img.saveAs("/www/images/processed.jpg");
 * res.write('&lt;img src="/images/processed.jpg" /&gt;');</pre>
 * <img src="http://helma.org/static/images/helma/reducedcolors.jpg" />
 * 
 * @param {Number} colorDepth as Number, value of color depth in an image.
 * @param {Number} dither as Boolean, optional parameter to enable dithering
 * @param {Number} alphaToBitmask as Boolean, optional parameter to use the alpha channel to create a bitmask
 */
Image.prototype.reduceColors = function(colorDepth, dither, alphaToBitmask) {};


/**
 * Sets the height and width of an image to new values.
 * <br /><br />
 * The widthNumber and heightNumber arguments need to be integers, 
 * so be careful to round the new values eventually.
 * <br /><br />
 * In case of a GIF image always reduce the image to 256 colors after resizing 
 * by using the reduceColors() function.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.jpg");
 * res.write('&ltimg src="/images/original.jpg" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/hawatosch.jpg" />
 * &nbsp;
 * var factor = 0.66;
 * var wd = Math.round(img.getWidth() * factor);
 * var ht = Math.round(img.getHeight() * factor);
 * img.resize(wd, ht);
 * img.saveAs("/www/images/processed.jpg");
 * &nbsp;
 * <i>res.write('&lt;img src="/images/processed.jpg" /&gt;');</i></pre>
 * <img src="http://helma.org/static/images/helma/hawatosch.jpg" />
 * 
 * @param {Number} width as Number, new width of the image.
 * @param {Number} height as Number, new height of the image.
 * @see Image.resizeFast
 */
Image.prototype.resize = function(width, heigth) {};


/**
 * Resizes the image, using a fast and cheap algorithm.
 * 
 * @param {Number} width as Number, new width of the image.
 * @param {Number} height as Number, new height of the image.
 * @see Image.resize
 */
Image.prototype.resizeFast = function(width, heigth) {};


/**
 * Sets the color for current drawing actions.
 * <br /><br />
 * The colorNumber argument is represented by a 24-bit integer (0-16777215), 
 * the redNumber, greenNumber and blueNumber arguments build an RGB 
 * tuple with each element ranging from 0 to 255.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.jpg");
 * res.write('&lt;img src="/images/original.jpg" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/hawatosch.jpg" />
 * &nbsp;
 * img.setColor(16777215);
 * img.fillRect(80, 50, 30, 30);
 * img.setColor(255, 255, 0);
 * img.fillRect(65, 15, 30, 30);
 * img.saveAs("/www/images/processed.jpg");
 * res.write('&lt;img src="/images/processed.jpg" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/setcolor.jpg" />
 * 
 * @param {Number} red as Number, RGB value ranging from 0 to 255 or if it the only parameter then it can be a 24 bit Integer
 * @param {Number} green as Number, RGB value ranging from 0 to 255
 * @param {Number} blue as Number, RGB value ranging from 0 to 255.
 */
Image.prototype.setColor = function(red, green, blue) {};


/**
 * Determines the typeface font to be used in current drawing actions.
 * <br /><br />
 * Using this function sets the font to be used in subsequent calls of the drawString() function.
 * <br /><br />
 * The nameString argument specifies the font face as string (e.g. "sansserif", "dialog").
 * <br /><br />
 * The integer styleNumber sets the font to normal (0), bold (1), italic (2) or bold and italic (3).
 * <br /><br />
 * The sizeNumber, an integer as well, refers to the font size in pixels.
 * <br /><br />
 * Please take a look at the description about adding fonts to the Java runtime 
 * for a detailed look onto Java's font mechanics.
 * <br /><br />
 * Example:
 * <pre>var img = new Image("http://helma.org/images/original.jpg");
 * res.write('&lt;img src="/images/original.jpg" /&gt;');
 * &nbsp;
 * <img src="http://helma.org/static/images/helma/hawatosch.jpg" />
 * &nbsp;
 * img.setColor(255, 255, 255);
 * img.setFont("serif", 0, 12);
 * img.drawString("I shot the serif.", 50, 15);
 * img.setFont("sansserif", 1, 14);
 * img.drawString("But I didn't shoot", 10, 100);
 * img.setFont("monospaced", 2, 16);
 * img.drawString("the monotype.", 15, 115);
 * img.saveAs("/www/images/processed.png");
 * res.write('&lt;img src="/images/processed.png" /&gt;');</pre>
 * <img src="http://helma.org/static/images/helma/setfont.jpg" />
 * 
 * @param {String} name as String, specifies the font face (e.g. "sansserif").
 * @param {Number} style as Number, the styleNumber normal (0), bold (1), italic (2) or bold and italic (3).
 * @param {Number} size as Number, refers to font size in pixels
 */
Image.prototype.setFont = function(name, style, size) {};


/**
 * Sets the palette index of the transparent color for Images with an IndexColorModel.
 * 
 * This can be used together with an Image's getPixel method.
 * 
 * @param {Number} index as Number, the index position specifying the transparent color.
 * @see Image.getPixel
 */
Image.prototype.setTransparentPixel = function(index) {};
