package com.iso8583.simulator.core.packager;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Validación del nuevo packager EBCDIC (iso87ebcdic.xml).
 *
 * No requiere contexto de Spring ni conexión real -- solo carga el XML con
 * jPOS y verifica que codifica los bytes tal como especifica el manual
 * "ITM Host-to-Host Specifications v4.5".
 */
class EbcdicPackagerTest {

    @Test
    void packagerCargaSinErrores() throws Exception {
        GenericPackager packager = new GenericPackager("jar:packagers/iso87ebcdic.xml");
        assertNotNull(packager);
    }

    @Test
    void pinBlockSeCodificaIgualQueEnElManualItm() throws Exception {
        // Ejemplo textual del manual (sección "Transmission of Binary Data
        // Elements"): PIN block 4ABF12C3D567980E debe viajar como estos 16
        // caracteres EBCDIC.
        GenericPackager packager = new GenericPackager("jar:packagers/iso87ebcdic.xml");

        ISOMsg msg = new ISOMsg();
        msg.set(52, ISOUtil.hex2byte("4ABF12C3D567980E"));

        byte[] campoEmpacado = packager.getFieldPackager(52).pack(msg.getComponent(52));
        String hexResultante = ISOUtil.hexString(campoEmpacado);

        assertEquals("F4C1C2C6F1F2C3F3C4F5F6F7F9F8F0C5", hexResultante);
    }

    @Test
    void bitmapSeCodificaIgualQueEnElManualItm() throws Exception {
        // Mismo ejemplo del manual (sección "Bitmap"): bitmap en hexadecimal
        // 7ABA04010EE0C000 debe viajar como estos 16 caracteres EBCDIC.
        GenericPackager packager = new GenericPackager("jar:packagers/iso87ebcdic.xml");

        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI("0400");
        // Bits usados en el ejemplo del manual: 2,3,4,5,7,9,11,12,13,15,22,32,37,38,39,41,42,43,49,50
        int[] bitsDeEjemplo = {2, 3, 4, 5, 7, 9, 11, 12, 13, 15, 22, 32, 37, 38, 39, 41, 42, 43, 49, 50};
        for (int bit : bitsDeEjemplo) {
            msg.set(bit, "0");
        }

        byte[] mensajeEmpacado = msg.pack();
        // El bitmap son los primeros 16 bytes luego del MTI (4 bytes)
        byte[] bitmapEmpacado = new byte[16];
        System.arraycopy(mensajeEmpacado, 4, bitmapEmpacado, 0, 16);

        assertEquals("F7C1C2C1F0F4F0F1F0C5C5F0C3F0F0F0", ISOUtil.hexString(bitmapEmpacado));
    }
}