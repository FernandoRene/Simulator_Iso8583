package com.iso8583.simulator.core.packager;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFieldPackager;
import org.jpos.iso.ISOComponent;

/**
 * FieldPackager personalizado que preserva espacios en campos de longitud fija
 */
public class IFA_FIXED_SPACES extends ISOFieldPackager {
    private int length;

    public IFA_FIXED_SPACES() {
        super();
    }

    public IFA_FIXED_SPACES(int length) {
        super();
        this.length = length;
    }

    @Override
    public int unpack(ISOComponent c, byte[] b, int offset) throws ISOException {
        try {
            if (b.length - offset < length) {
                throw new ISOException("Not enough data to unpack field. Required: " + length +
                        ", Available: " + (b.length - offset));
            }

            byte[] fieldData = new byte[length];
            System.arraycopy(b, offset, fieldData, 0, length);
            String value = new String(fieldData, "ISO8859-1"); // PRESERVA ESPACIOS

            c.setValue(value);
            return length; // Retorna la cantidad de bytes consumidos

        } catch (Exception e) {
            throw new ISOException("Error unpacking fixed field with spaces: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] pack(ISOComponent c) throws ISOException {
        try {
            String value = (String) c.getValue();
            if (value == null) {
                value = "";
            }

            // Asegurar longitud fija rellenando con espacios a la derecha
            if (value.length() > length) {
                value = value.substring(0, length);
            } else if (value.length() < length) {
                value = String.format("%-" + length + "s", value); // Rellenar con espacios a la derecha
            }

            return value.getBytes("ISO8859-1");
        } catch (Exception e) {
            throw new ISOException("Error packing fixed field with spaces: " + e.getMessage(), e);
        }
    }

    @Override
    public int getMaxPackedLength() {
        return length;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public void setLength(int length) {
        this.length = length;
    }

    @Override
    public String getDescription() {
        return "Fixed length field preserving spaces (" + length + ")";
    }
}