import React from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts';
import { Activity, Clock, Zap, TrendingUp } from 'lucide-react';

interface MetricsPanelProps {
  stats: any;
}

export const MetricsPanel: React.FC<MetricsPanelProps> = ({ stats }) => {
  // Mock data para el gráfico - en producción vendría del backend
  const responseTimeData = [
    { time: '10:00', responseTime: 45, throughput: 120 },
    { time: '10:05', responseTime: 52, throughput: 135 },
    { time: '10:10', responseTime: 38, throughput: 110 },
    { time: '10:15', responseTime: 49, throughput: 145 },
    { time: '10:20', responseTime: 41, throughput: 128 },
    { time: '10:25', responseTime: 55, throughput: 155 },
    { time: '10:30', responseTime: 43, throughput: 132 },
  ];

  const successRateData = [
    { time: '10:00', success: 98, failed: 2 },
    { time: '10:05', success: 97, failed: 3 },
    { time: '10:10', success: 99, failed: 1 },
    { time: '10:15', success: 96, failed: 4 },
    { time: '10:20', success: 98, failed: 2 },
    { time: '10:25', success: 99, failed: 1 },
    { time: '10:30', success: 97, failed: 3 },
  ];

  const formatResponseTime = (value: number) => `${value}ms`;
  const formatThroughput = (value: number) => `${value}/min`;

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Response Time Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <Clock className="w-5 h-5 mr-2" />
            Tiempo de Respuesta
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={200}>
            <LineChart data={responseTimeData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis tickFormatter={formatResponseTime} />
              <Tooltip
                formatter={(value: number) => [formatResponseTime(value), 'Tiempo']}
                labelFormatter={(label) => `Hora: ${label}`}
              />
              <Line
                type="monotone"
                dataKey="responseTime"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={{ fill: '#3b82f6', strokeWidth: 2 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* Throughput Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <Zap className="w-5 h-5 mr-2" />
            Throughput
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={responseTimeData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis tickFormatter={formatThroughput} />
              <Tooltip
                formatter={(value: number) => [formatThroughput(value), 'Throughput']}
                labelFormatter={(label) => `Hora: ${label}`}
              />
              <Area
                type="monotone"
                dataKey="throughput"
                stroke="#10b981"
                fill="#10b981"
                fillOpacity={0.3}
              />
            </AreaChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* Success Rate Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <TrendingUp className="w-5 h-5 mr-2" />
            Tasa de Éxito vs Fallos
          </CardTitle>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={200}>
            <AreaChart data={successRateData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="time" />
              <YAxis domain={[0, 100]} />
              <Tooltip
                formatter={(value: number, name: string) => [
                  `${value}%`,
                  name === 'success' ? 'Éxito' : 'Fallos'
                ]}
                labelFormatter={(label) => `Hora: ${label}`}
              />
              <Area
                type="monotone"
                dataKey="success"
                stackId="1"
                stroke="#10b981"
                fill="#10b981"
              />
              <Area
                type="monotone"
                dataKey="failed"
                stackId="1"
                stroke="#ef4444"
                fill="#ef4444"
              />
            </AreaChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* Performance Summary */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <Activity className="w-5 h-5 mr-2" />
            Resumen de Performance
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Tiempo Promedio</span>
              <span className="font-semibold">{stats?.averageResponseTime || 0}ms</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Pico Máximo</span>
              <span className="font-semibold">55ms</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Tiempo Mínimo</span>
              <span className="font-semibold">38ms</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Uptime</span>
              <span className="font-semibold text-green-600">99.8%</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Mensajes/Hora</span>
              <span className="font-semibold">8,250</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};