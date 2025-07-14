package com.iso8583.simulator.core.transaction.strategy;

import com.iso8583.simulator.core.transaction.model.TransactionRequest;
import com.iso8583.simulator.core.transaction.model.TransactionResponse;
import com.iso8583.simulator.core.transaction.model.ValidationResult;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * Interfaz base para estrategias de transacciones ISO 8583
 * Implementa el Strategy Pattern para diferentes tipos de transacciones
 */
public interface TransactionStrategy {

    /**
     * Tipo de transacción que maneja esta estrategia
     */
    String getTransactionType();

    /**
     * Processing code(s) que maneja esta estrategia
     */
    String[] getProcessingCodes();

    /**
     * Construye el mensaje ISO 8583 para este tipo de transacción
     */
    ISOMsg buildMessage(TransactionRequest request) throws ISOException;

    /**
     * Valida el request (solo formato, NO negocio)
     */
    ValidationResult validateRequest(TransactionRequest request);

    /**
     * Procesa la respuesta recibida del core
     */
    TransactionResponse processResponse(ISOMsg request, ISOMsg response);

    /**
     * Indica si esta transacción requiere PIN
     */
    boolean requiresPIN();

    /**
     * Campos obligatorios para este tipo de transacción
     */
    String[] getRequiredFields();
}