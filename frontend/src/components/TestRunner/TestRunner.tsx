import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Alert, AlertDescription } from '../ui/alert';
import {
  Play,
  Pause,
  Square,
  FileText,
  CheckCircle,
  XCircle,
  Clock,
  Activity,
  Upload
} from 'lucide-react';

interface TestResult {
  id: string;
  name: string;
  status: 'pending' | 'running' | 'passed' | 'failed' | 'skipped';
  duration?: number;
  message?: string;
  timestamp: Date;
}

export const TestRunner: React.FC = () => {
  const [isRunning, setIsRunning] = useState(false);
  const [selectedTests, setSelectedTests] = useState<string[]>([]);
  const [testResults, setTestResults] = useState<TestResult[]>([]);

  // Mock test scenarios
  const testScenarios = [
    {
      id: 'test-1',
      name: 'Solicitud Financiera 0200 - Aprobada',
      description: 'Prueba una transacción financiera exitosa',
      enabled: true,
      messageType: 'FINANCIAL_REQUEST_0200'
    },
    {
      id: 'test-2',
      name: 'Solicitud Financiera 0200 - Rechazada',
      description: 'Prueba una transacción rechazada por fondos insuficientes',
      enabled: true,
      messageType: 'FINANCIAL_REQUEST_0200'
    },
    {
      id: 'test-3',
      name: 'Reverso 0400 - Exitoso',
      description: 'Prueba un reverso de transacción',
      enabled: true,
      messageType: 'REVERSAL_REQUEST_0400'
    },
    {
      id: 'test-4',
      name: 'Network Management 0800',
      description: 'Prueba de conectividad de red',
      enabled: true,
      messageType: 'NETWORK_REQUEST_0800'
    },
    {
      id: 'test-5',
      name: 'Timeout Test',
      description: 'Prueba de timeout de conexión',
      enabled: false,
      messageType: 'FINANCIAL_REQUEST_0200'
    }
  ];

  const runTests = async () => {
    setIsRunning(true);
    setTestResults([]);

    const testsToRun = testScenarios.filter(test =>
      selectedTests.includes(test.id) && test.enabled
    );

    for (const test of testsToRun) {
      // Simular ejecución de prueba
      const startTime = Date.now();

      setTestResults(prev => [...prev, {
        id: test.id,
        name: test.name,
        status: 'running',
        timestamp: new Date()
      }]);

      // Simular delay de ejecución
      await new Promise(resolve => setTimeout(resolve, 2000 + Math.random() * 3000));

      const duration = Date.now() - startTime;
      const success = Math.random() > 0.2; // 80% de éxito

      setTestResults(prev => prev.map(result =>
        result.id === test.id
          ? {
              ...result,
              status: success ? 'passed' : 'failed',
              duration,
              message: success
                ? 'Prueba ejecutada exitosamente'
                : 'Error: Respuesta inesperada del autorizador'
            }
          : result
      ));
    }

    setIsRunning(false);
  };

  const stopTests = () => {
    setIsRunning(false);
    setTestResults(prev => prev.map(result =>
      result.status === 'running'
        ? { ...result, status: 'skipped', message: 'Prueba cancelada por el usuario' }
        : result
    ));
  };

  const toggleTestSelection = (testId: string) => {
    setSelectedTests(prev =>
      prev.includes(testId)
        ? prev.filter(id => id !== testId)
        : [...prev, testId]
    );
  };

  const selectAllTests = () => {
    const enabledTests = testScenarios.filter(test => test.enabled).map(test => test.id);
    setSelectedTests(enabledTests);
  };

  const clearSelection = () => {
    setSelectedTests([]);
  };

  const getStatusIcon = (status: TestResult['status']) => {
    switch (status) {
      case 'running':
        return <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600" />;
      case 'passed':
        return <CheckCircle className="w-4 h-4 text-green-600" />;
      case 'failed':
        return <XCircle className="w-4 h-4 text-red-600" />;
      case 'skipped':
        return <Clock className="w-4 h-4 text-gray-400" />;
      default:
        return <Clock className="w-4 h-4 text-gray-400" />;
    }
  };

  const getStatusBadge = (status: TestResult['status']) => {
    const variants = {
      running: 'default',
      passed: 'default',
      failed: 'destructive',
      skipped: 'secondary',
      pending: 'secondary'
    } as const;

    const labels = {
      running: 'Ejecutando',
      passed: 'Exitoso',
      failed: 'Fallido',
      skipped: 'Cancelado',
      pending: 'Pendiente'
    };

    return (
      <Badge variant={variants[status]}>
        {labels[status]}
      </Badge>
    );
  };

  const getExecutionSummary = () => {
    const total = testResults.length;
    const passed = testResults.filter(r => r.status === 'passed').length;
    const failed = testResults.filter(r => r.status === 'failed').length;
    const skipped = testResults.filter(r => r.status === 'skipped').length;

    return { total, passed, failed, skipped };
  };

  const summary = getExecutionSummary();

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Ejecutor de Pruebas</h1>
        <div className="flex space-x-2">
          <Button variant="outline" size="sm">
            <Upload className="w-4 h-4 mr-2" />
            Cargar CSV
          </Button>
          <Button variant="outline" size="sm">
            <FileText className="w-4 h-4 mr-2" />
            Exportar Resultados
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Test Selection */}
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <span>Escenarios de Prueba</span>
              <div className="flex space-x-2">
                <Button variant="outline" size="sm" onClick={selectAllTests}>
                  Todos
                </Button>
                <Button variant="outline" size="sm" onClick={clearSelection}>
                  Ninguno
                </Button>
              </div>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {testScenarios.map(test => (
                <div
                  key={test.id}
                  className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                    selectedTests.includes(test.id)
                      ? 'border-blue-500 bg-blue-50'
                      : 'border-gray-200 hover:border-gray-300'
                  } ${!test.enabled ? 'opacity-50' : ''}`}
                  onClick={() => test.enabled && toggleTestSelection(test.id)}
                >
                  <div className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      checked={selectedTests.includes(test.id)}
                      disabled={!test.enabled}
                      readOnly
                      className="rounded"
                    />
                    <div className="flex-1">
                      <p className="font-medium text-sm">{test.name}</p>
                      <p className="text-xs text-gray-500">{test.description}</p>
                      <Badge variant="outline" className="mt-1 text-xs">
                        {test.messageType}
                      </Badge>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Control Buttons */}
            <div className="mt-6 space-y-2">
              {!isRunning ? (
                <Button
                  onClick={runTests}
                  disabled={selectedTests.length === 0}
                  className="w-full"
                >
                  <Play className="w-4 h-4 mr-2" />
                  Ejecutar Pruebas ({selectedTests.length})
                </Button>
              ) : (
                <Button
                  onClick={stopTests}
                  variant="destructive"
                  className="w-full"
                >
                  <Square className="w-4 h-4 mr-2" />
                  Detener Ejecución
                </Button>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Test Results */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <span>Resultados de Pruebas</span>
              {isRunning && (
                <div className="flex items-center space-x-2">
                  <Activity className="w-4 h-4 animate-pulse" />
                  <span className="text-sm">Ejecutando...</span>
                </div>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {/* Summary */}
            {testResults.length > 0 && (
              <div className="grid grid-cols-4 gap-4 mb-6">
                <div className="text-center">
                  <p className="text-2xl font-bold">{summary.total}</p>
                  <p className="text-sm text-gray-600">Total</p>
                </div>
                <div className="text-center">
                  <p className="text-2xl font-bold text-green-600">{summary.passed}</p>
                  <p className="text-sm text-gray-600">Exitosos</p>
                </div>
                <div className="text-center">
                  <p className="text-2xl font-bold text-red-600">{summary.failed}</p>
                  <p className="text-sm text-gray-600">Fallidos</p>
                </div>
                <div className="text-center">
                  <p className="text-2xl font-bold text-gray-400">{summary.skipped}</p>
                  <p className="text-sm text-gray-600">Cancelados</p>
                </div>
              </div>
            )}

            {/* Results List */}
            <div className="space-y-3">
              {testResults.length === 0 ? (
                <div className="text-center text-gray-500 py-8">
                  <FileText className="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p>Seleccione pruebas y haga clic en "Ejecutar" para ver los resultados</p>
                </div>
              ) : (
                testResults.map(result => (
                  <div key={result.id} className="border rounded-lg p-4">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        {getStatusIcon(result.status)}
                        <div>
                          <p className="font-medium">{result.name}</p>
                          <p className="text-sm text-gray-500">
                            {result.timestamp.toLocaleTimeString()}
                            {result.duration && ` • ${result.duration}ms`}
                          </p>
                        </div>
                      </div>
                      {getStatusBadge(result.status)}
                    </div>
                    {result.message && (
                      <Alert className="mt-3" variant={result.status === 'failed' ? 'destructive' : 'default'}>
                        <AlertDescription>{result.message}</AlertDescription>
                      </Alert>
                    )}
                  </div>
                ))
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};