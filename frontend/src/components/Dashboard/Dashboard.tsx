import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { AlertCircle, CheckCircle, Activity, Send, Settings } from 'lucide-react';
import { ApiService } from '../../services/ApiService';
import { MetricsPanel } from './MetricsPanel';

interface ConnectionStatus {
  connected: boolean;
  host: string;
  port: number;
  lastChecked: string;
  error?: string;
}

interface SimulatorStats {
  totalMessagesSent: number;
  successfulResponses: number;
  failedResponses: number;
  averageResponseTime: number;
  connectionStatus: ConnectionStatus;
}

export const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<SimulatorStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [testingConnection, setTestingConnection] = useState(false);

  useEffect(() => {
    loadStats();
    const interval = setInterval(loadStats, 5000); // Actualizar cada 5 segundos
    return () => clearInterval(interval);
  }, []);

  const loadStats = async () => {
    try {
      const response = await ApiService.getSimulatorStats();
      setStats(response);
    } catch (error) {
      console.error('Error loading stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const testConnection = async () => {
    setTestingConnection(true);
    try {
      const result = await ApiService.testConnection();
      console.log('Connection test result:', result);
      // Actualizar stats después del test
      await loadStats();
    } catch (error) {
      console.error('Error testing connection:', error);
    } finally {
      setTestingConnection(false);
    }
  };

  const getConnectionStatusBadge = (connected: boolean) => {
    return connected ? (
      <Badge variant="default" className="bg-green-100 text-green-800">
        <CheckCircle className="w-3 h-3 mr-1" />
        Conectado
      </Badge>
    ) : (
      <Badge variant="destructive" className="bg-red-100 text-red-800">
        <AlertCircle className="w-3 h-3 mr-1" />
        Desconectado
      </Badge>
    );
  };

  const calculateSuccessRate = () => {
    if (!stats || stats.totalMessagesSent === 0) return 0;
    return Math.round((stats.successfulResponses / stats.totalMessagesSent) * 100);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">ISO8583 Simulator Dashboard</h1>
        <div className="flex space-x-2">
          <Button
            onClick={testConnection}
            disabled={testingConnection}
            variant="outline"
            size="sm"
          >
            {testingConnection ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600 mr-2"></div>
            ) : (
              <Activity className="w-4 h-4 mr-2" />
            )}
            Test Conexión
          </Button>
          <Button variant="outline" size="sm">
            <Settings className="w-4 h-4 mr-2" />
            Configuración
          </Button>
        </div>
      </div>

      {/* Connection Status Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between">
            <span>Estado de Conexión</span>
            {stats && getConnectionStatusBadge(stats.connectionStatus.connected)}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {stats && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <p className="text-sm text-gray-600">Host</p>
                <p className="font-semibold">{stats.connectionStatus.host}</p>
              </div>
              <div>
                <p className="text-sm text-gray-600">Puerto</p>
                <p className="font-semibold">{stats.connectionStatus.port}</p>
              </div>
              <div>
                <p className="text-sm text-gray-600">Última verificación</p>
                <p className="font-semibold text-sm">
                  {new Date(stats.connectionStatus.lastChecked).toLocaleString()}
                </p>
              </div>
              {stats.connectionStatus.error && (
                <div className="col-span-full">
                  <p className="text-sm text-red-600 bg-red-50 p-2 rounded">
                    {stats.connectionStatus.error}
                  </p>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Stats Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Mensajes</CardTitle>
            <Send className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalMessagesSent || 0}</div>
            <p className="text-xs text-muted-foreground">
              Mensajes enviados en total
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Exitosos</CardTitle>
            <CheckCircle className="h-4 w-4 text-green-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              {stats?.successfulResponses || 0}
            </div>
            <p className="text-xs text-muted-foreground">
              Respuestas exitosas
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Fallidos</CardTitle>
            <AlertCircle className="h-4 w-4 text-red-600" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">
              {stats?.failedResponses || 0}
            </div>
            <p className="text-xs text-muted-foreground">
              Respuestas fallidas
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Tasa de Éxito</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{calculateSuccessRate()}%</div>
            <p className="text-xs text-muted-foreground">
              Promedio: {stats?.averageResponseTime || 0}ms
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Metrics Panel */}
      <MetricsPanel stats={stats} />

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Acciones Rápidas</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Button
              className="h-16 flex flex-col items-center justify-center space-y-2"
              onClick={() => window.location.href = '/message-editor'}
            >
              <Send className="w-6 h-6" />
              <span>Enviar Mensaje</span>
            </Button>

            <Button
              variant="outline"
              className="h-16 flex flex-col items-center justify-center space-y-2"
              onClick={() => window.location.href = '/test-runner'}
            >
              <Activity className="w-6 h-6" />
              <span>Ejecutar Pruebas</span>
            </Button>

            <Button
              variant="outline"
              className="h-16 flex flex-col items-center justify-center space-y-2"
              onClick={() => window.location.href = '/configuration'}
            >
              <Settings className="w-6 h-6" />
              <span>Configuración</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};