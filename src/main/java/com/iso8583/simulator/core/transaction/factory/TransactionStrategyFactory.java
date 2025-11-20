package com.iso8583.simulator.core.transaction.factory;

import com.iso8583.simulator.core.transaction.strategy.TransactionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Factory para gestionar las estrategias de transacciones
 * Utiliza Spring para inyección automática de todas las implementaciones
 */
@Service
public class TransactionStrategyFactory {

    private final Map<String, TransactionStrategy> strategies;

    @Autowired
    public TransactionStrategyFactory(List<TransactionStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        TransactionStrategy::getTransactionType,
                        strategy -> strategy
                ));
    }

    /**
     * Obtiene la estrategia para un tipo de transacción específico
     */
    public TransactionStrategy getStrategy(String transactionType) {
        TransactionStrategy strategy = strategies.get(transactionType.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Tipo de transacción no soportado: " + transactionType);
        }
        return strategy;
    }

    /**
     * Lista todos los tipos de transacciones soportados
     */
    public List<String> getSupportedTransactionTypes() {
        return new ArrayList<>(strategies.keySet());
    }

    /**
     * Busca estrategia por processing code
     */
    public TransactionStrategy getStrategyByProcessingCode(String processingCode) {
        return strategies.values().stream()
                .filter(strategy -> Arrays.asList(strategy.getProcessingCodes()).contains(processingCode))
                .findFirst()
                .orElse(null);
    }

    /**
     * Verifica si un tipo de transacción está soportado
     */
    public boolean isSupported(String transactionType) {
        return strategies.containsKey(transactionType.toUpperCase());
    }

    /**
     * Obtiene información detallada de todas las estrategias
     */
    public Map<String, TransactionStrategy> getAllStrategies() {
        return strategies;
    }
}