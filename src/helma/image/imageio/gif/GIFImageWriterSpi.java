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

/*
 * The imageio integration is inspired by the package org.freehep.graphicsio.gif
 */

package helma.image.imageio.gif;

import java.util.*;
import javax.imageio.*;
import javax.imageio.spi.*;

public class GIFImageWriterSpi extends ImageWriterSpi {
	
    public GIFImageWriterSpi() {
        super(
            "Helma Object Publisher, http://helma.org/", //$NON-NLS-1$
            "1.0", //$NON-NLS-1$
            new String[] {"gif", "GIF"}, //$NON-NLS-1$ //$NON-NLS-2$
            new String[] {"gif", "GIF"},  //$NON-NLS-1$//$NON-NLS-2$
            new String[] {"image/gif", "image/x-gif"},  //$NON-NLS-1$//$NON-NLS-2$
            "helma.image.imageio.gif.GIFImageWriter", //$NON-NLS-1$
            STANDARD_OUTPUT_TYPE,
            null,
            false, null, null, null, null,
            false, null, null, null, null
        );
    }

    @Override
    public String getDescription(Locale locale) {
        return "Graphics Interchange Format"; //$NON-NLS-1$
    }

    @Override
    public ImageWriter createWriterInstance(Object extension) {
        return new GIFImageWriter(this);
    }

    @Override
    public boolean canEncodeImage(ImageTypeSpecifier type) {
        // FIXME handle # colors
        return true;
    }
}