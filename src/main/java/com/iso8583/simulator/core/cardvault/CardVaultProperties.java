package com.iso8583.simulator.core.cardvault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del Card Vault.
 *
 * Propiedades (prefijo "card-vault" en application.yml):
 *
 *   card-vault:
 *     storage-path: ./data/card-vault.json     # archivo externo, editable y exportable
 *     seed-path: classpath:card-vault/seed-card-vault.json   # opcional, solo si storage-path no existe aún
 */
@Component
@ConfigurationProperties(prefix = "card-vault")
public class CardVaultProperties {

    /**
     * Ruta del archivo externo donde se persiste el Card Vault.
     * Debe ser una ruta de filesystem (no dentro del jar), para que pueda
     * leerse/editarse/exportarse y sobrevivir a reinicios.
     */
    private String storagePath = "./data/card-vault.json";

    /**
     * Ruta opcional de una "semilla" inicial (classpath:... o ruta de archivo).
     * Solo se usa si storagePath aún no existe (primer arranque).
     * Si no se configura, el vault arranca vacío.
     */
    private String seedPath;

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getSeedPath() {
        return seedPath;
    }

    public void setSeedPath(String seedPath) {
        this.seedPath = seedPath;
    }
}
