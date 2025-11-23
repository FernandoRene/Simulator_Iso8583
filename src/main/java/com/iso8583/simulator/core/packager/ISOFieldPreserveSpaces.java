package com.iso8583.simulator.core.packager;

import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOStringFieldPackager;

public class ISOFieldPreserveSpaces extends ISOStringFieldPackager {
    @Override
    public byte[] pack(ISOComponent c) throws ISOException {
        try {
            String s = (String) c.getValue();
            return s.getBytes("ISO8859-1");
        } catch (Exception e) {
            throw new ISOException(e);
        }
    }

    @Override
    public int unpack(ISOComponent c, byte[] b, int offset) throws ISOException {
        try {
            String s = new String(b, offset, getLength(), "ISO8859-1");
            c.setValue(s);
            return offset + getLength();
        } catch (Exception e) {
            throw new ISOException(e);
        }
    }
}