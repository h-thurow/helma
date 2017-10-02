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

/*
 * The imageio integration is inspired by the package org.freehep.graphicsio.gif
 */

package helma.image.imageio.gif;

import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.*;

import helma.image.*;

public class GIFImageWriter extends ImageWriter {
    GIFEncoder encoder;

    public GIFImageWriter(GIFImageWriterSpi originatingProvider) {
        super(originatingProvider);
        this.encoder = new GIFEncoder();
    }

    @Override
    public void write(IIOMetadata streamMetadata, IIOImage image,
        ImageWriteParam param) throws IOException {
        if (image == null)
            throw new IllegalArgumentException(Messages.getString("GIFImageWriter.0")); //$NON-NLS-1$

        if (image.hasRaster())
            throw new UnsupportedOperationException(Messages.getString("GIFImageWriter.1")); //$NON-NLS-1$

        Object output = getOutput();
        if (output == null)
            throw new IllegalStateException(Messages.getString("GIFImageWriter.2")); //$NON-NLS-1$

        if (param == null)
            param = getDefaultWriteParam();

        RenderedImage ri = image.getRenderedImage();
        if (!(ri instanceof BufferedImage))
            throw new IOException(Messages.getString("GIFImageWriter.3")); //$NON-NLS-1$
        if (!(output instanceof DataOutput))
            throw new IOException(Messages.getString("GIFImageWriter.4")); //$NON-NLS-1$
        this.encoder.encode((BufferedImage) ri, (DataOutput) output,
            param.getProgressiveMode() != ImageWriteParam.MODE_DISABLED, null);
    }

    @Override
    public IIOMetadata convertStreamMetadata(IIOMetadata inData,
        ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata convertImageMetadata(IIOMetadata inData,
        ImageTypeSpecifier imageType, ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
        ImageWriteParam param) {
        return null;
    }

    @Override
    public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
        return null;
    }

    @Override
    public ImageWriteParam getDefaultWriteParam() {
        return new GIFImageWriteParam(getLocale());
    }
}