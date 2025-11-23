package com.iso8583.simulator.core.packager;

import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOStringFieldPackager;

public class ISOFixedFieldPreserveSpaces extends ISOStringFieldPackager {
    @Override
    public int unpack(ISOComponent c, byte[] b, int offset) throws ISOException {
        try {
            // Leer EXACTAMENTE la cantidad de bytes especificada
            // y convertirlos a string preservando espacios
            byte[] fieldBytes = new byte[getLength()];
            System.arraycopy(b, offset, fieldBytes, 0, getLength());
            String value = new String(fieldBytes, "ISO8859-1");

            // Forzar la preservación de espacios
            c.setValue(value);
            return offset + getLength();
        } catch (Exception e) {
            throw new ISOException(e);
        }
    }

    @Override
    public byte[] pack(ISOComponent c) throws ISOException {
        try {
            String s = (String) c.getValue();
            // Asegurar que tenga la longitud correcta con espacios
            if (s.length() < getLength()) {
                s = String.format("%-" + getLength() + "s", s);
            }
            return s.getBytes("ISO8859-1");
        } catch (Exception e) {
            throw new ISOException(e);
        }
    }
}