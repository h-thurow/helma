/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2010-2017 Daniel Ruthardt. All rights reserved.
 */

package helma.scripting.quercus;

import helma.framework.ResponseTrans;

import java.io.IOException;
import java.util.Arrays;

import com.caucho.vfs.StreamImpl;

/**
 * This is a helper class wich ensures to have the full functionality of the PHP scripting language 
 * regarding output handling.
 * 
 * The QuercusEngine exposes the ResponseTransmitter to the PHP context, but PHP scripts are uses to produce
 * output in a more direct way (e.g. by calling the echo function). This class catches output produced in a 
 * direct wy and forwards it to the ResponseTransmitter.
 */
public class ResponseStream extends StreamImpl {

    /**
     * The response transactor to write to.
     */
    private ResponseTrans _response;

    /**
     * Default constructor.
     * 
     * @param response
     *  The response transactor to write to.
     */
    public ResponseStream(final ResponseTrans response) {
        // set the response transactor
        this._response = response;
    }

    /*
     * (non-Javadoc)
     * @see com.caucho.vfs.StreamImpl#close()
     */
    @Override
    public void close() throws IOException {
        // flush the response transactor
        this._response.flush();
        // do what would have been done anyways
        super.close();
    }

    /*
     * (non-Javadoc)
     * @see com.caucho.vfs.StreamImpl#closeWrite()
     */
    @Override
    public void closeWrite() throws IOException {
        // flush the response transactor
        this._response.flush();
        // do what would have been done anyways
        super.closeWrite();
    }

    /*
     * (non-Javadoc)
     * @see com.caucho.vfs.StreamImpl#flush()
     */
    @Override
    public void flush() throws IOException {
        // flush the response transactor
        this._response.flush();
        // do what would have been done anyways
        super.flush();
    }

    /*
     * (non-Javadoc)
     * @see com.caucho.vfs.StreamImpl#flushBuffer()
     */
    @Override
    public void flushBuffer() throws IOException {
        this._response.flush();
        super.flushBuffer();
    }

    /**
     * Replaced the current response transactor with the given one.
     * 
     * @param response
     *  The response transactor to write to.
     * @return
     *  No return value.
     */
    protected void setResponse(final ResponseTrans response) {
        // check if a new response transactor is provided
        if (response != null) {
            // replace the existing response transactor with the new one
            this._response = response;
        }
    }

    /*
     * (non-Javadoc)
     * @see com.caucho.vfs.StreamImpl#write(byte[], int, int, boolean)
     */
    @Override
    public void write(final byte[] buffer, final int offset, final int length, final boolean isEnd) {
        // get the binary data to write to the response transactor
        final byte[] usedBuffer = Arrays.copyOfRange(buffer, offset, length);
        // write the binary data to the response transactor
        this._response.writeBinary(usedBuffer);

        // check if we are done writing binary data to the response transactor
        if (isEnd) {
            // flush the response transactor
            this._response.flush();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.caucho.vfs.StreamImpl#write(byte[], int, int, byte[], int, int,
     * boolean)
     */
    @Override
    public boolean write(final byte[] buffer1, final int offset1, final int length1, final byte[] buffer2, 
            final int offset2, final int length2, final boolean isEnd) {
        // write the binary data to the response transactor
        write(buffer1, offset1, length1, isEnd);
        // write the binary data to the response transactor
        write(buffer2, offset2, length2, isEnd);

        // TODO check if it really is a good idea to always return true
        // we are done writing binary data to the response transactor
        return true;
    }

}