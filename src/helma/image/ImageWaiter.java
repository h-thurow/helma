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

import java.awt.Image;
import java.awt.image.ImageObserver;

/**
 * The ImageWaiter will only be used like this:
 * image = ImageWaiter.waitForImage(image);
 */
public class ImageWaiter implements ImageObserver {
    Image image;
    int width;
    int height;
    boolean waiting;
    boolean firstFrameLoaded;

    private ImageWaiter(Image image) {
        this.image = image;
        this.waiting = true;
        this.firstFrameLoaded = false;
    }
        
    public static Image waitForImage(Image image) {
        ImageWaiter waiter = new ImageWaiter(image);
        try {
            waiter.waitForImage();
        } finally {
            waiter.done();
        }
        return waiter.width == -1 || waiter.height == -1 ? null : image;
    }

    private synchronized void waitForImage() {
        this.width = this.image.getWidth(this);
        this.height = this.image.getHeight(this);

        if (this.width == -1 || this.height == -1) {
            try {
                wait(45000);
            } catch (InterruptedException x) {
                this.waiting = false;
                return;
            } finally {
                this.waiting = false;
            }
        }

        // if width and height haven't been set, throw tantrum
        if (this.width == -1 || this.height == -1) {
            throw new RuntimeException(Messages.getString("ImageWaiter.0")); //$NON-NLS-1$
        }
    }

    private synchronized void done() {
        this.waiting = false;
        notifyAll();
    }

    public synchronized boolean imageUpdate(Image img, int infoflags, int x,
        int y, int w, int h) {
        // check if there was an error
        if (!this.waiting || (infoflags & ERROR) > 0 || (infoflags & ABORT) > 0) {
            // we either timed out or there was an error.
            notifyAll();

            return false;
        }

        if ((infoflags & WIDTH) > 0 || (infoflags & HEIGHT) > 0) {
            if ((infoflags & WIDTH) > 0) {
                this.width = w;
            }

            if ((infoflags & HEIGHT) > 0) {
                this.height = h;
            }

            if (this.width > -1 && h > -1 && this.firstFrameLoaded) {
                notifyAll();

                return false;
            }
        }

        if ((infoflags & ALLBITS) > 0 || (infoflags & FRAMEBITS) > 0) {
            this.firstFrameLoaded = true;
            notifyAll();

            return false;
        }

        return true;
    }
}