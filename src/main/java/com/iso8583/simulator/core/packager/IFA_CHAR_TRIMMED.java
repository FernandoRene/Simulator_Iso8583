package com.iso8583.simulator.core.packager;

import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFieldPackager;

import java.io.IOException;
import java.io.InputStream;

/**
 * Custom ISOFieldPackager para campos de longitud fija con manejo de espacios
 *
 * Este packager extiende el comportamiento de IF_CHAR para:
 * 1. PACK: Agregar padding de espacios al final si el valor es más corto
 * 2. UNPACK: Leer exactamente N caracteres y hacer trim() al final
 *
 * Soluciona el problema de desfase cuando campos tienen espacios al final
 */
public class IFA_CHAR_TRIMMED extends ISOFieldPackager {

    public IFA_CHAR_TRIMMED() {
        super();
    }

    public IFA_CHAR_TRIMMED(int len, String description) {
        super(len, description);
    }

    /**
     * Pack: Agrega padding de espacios al final hasta completar la longitud fija
     */
    @Override
    public byte[] pack(ISOComponent c) throws ISOException {
        try {
            String value = (String) c.getValue();
            if (value == null) {
                value = "";
            }

            int len = getLength();

            // Si el valor es más largo que length, truncar
            if (value.length() > len) {
                value = value.substring(0, len);
            }

            // Padding con espacios al final
            StringBuilder sb = new StringBuilder(value);
            while (sb.length() < len) {
                sb.append(' ');
            }

            // Usar encoding ISO-8859-1 (estándar para ISO8583)
            return sb.toString().getBytes("ISO-8859-1");
        } catch (Exception e) {
            throw new ISOException("Error packing field " + c.getKey(), e);
        }
    }

    /**
     * Unpack: Lee exactamente N caracteres y hace trim() al final
     * IMPORTANTE: Siempre lee los N caracteres completos del stream para evitar desfases
     */
    @Override
    public int unpack(ISOComponent c, byte[] b, int offset) throws ISOException {
        try {
            int len = getLength();

            // CRÍTICO: Siempre leer exactamente 'length' bytes
            if (offset + len > b.length) {
                throw new ISOException(
                        String.format("Field %s: Insufficient data. Need %d bytes, but only %d available at offset %d",
                                c.getKey(), len, b.length - offset, offset)
                );
            }

            // Leer exactamente 'length' caracteres usando encoding ISO-8859-1
            String value = new String(b, offset, len, "ISO-8859-1");

            // Hacer trim() para remover espacios al final
            value = value.trim();

            // Set el valor trimmed
            c.setValue(value);

            // Retornar cuántos bytes se consumieron (SIEMPRE 'length', no value.length())
            return len;

        } catch (Exception e) {
            throw new ISOException("Error unpacking field " + c.getKey(), e);
        }
    }

    /**
     * Unpack desde InputStream
     */
    @Override
    public void unpack(ISOComponent c, InputStream in) throws IOException, ISOException {
        try {
            int len = getLength();

            // Leer exactamente 'length' bytes del stream
            byte[] buffer = new byte[len];
            int bytesRead = in.read(buffer);

            if (bytesRead < len) {
                throw new ISOException(
                        String.format("Field %s: Expected %d bytes, got %d",
                                c.getKey(), len, bytesRead)
                );
            }

            // Convertir a String y hacer trim usando encoding ISO-8859-1
            String value = new String(buffer, "ISO-8859-1").trim();
            c.setValue(value);

        } catch (Exception e) {
            throw new ISOException("Error unpacking field " + c.getKey() + " from stream", e);
        }
    }

    @Override
    public int getMaxPackedLength() {
        return getLength();
    }
}