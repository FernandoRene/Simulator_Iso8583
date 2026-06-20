package com.iso8583.simulator.core.cardvault.model;

/**
 * Modo de operación para carga masiva del Card Vault.
 *
 * REPLACE: reemplaza por completo el contenido actual del vault.
 * MERGE:   inserta/actualiza por PAN; lo que no esté en el archivo cargado
 *          se conserva tal cual.
 */
public enum BulkMode {
    REPLACE,
    MERGE
}
