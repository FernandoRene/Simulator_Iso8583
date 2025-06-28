import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Textarea } from '../ui/textarea';
import { Badge } from '../ui/badge';
import { Alert, AlertDescription } from '../ui/alert';
import { Send, FileText, AlertCircle, CheckCircle, Copy, Download } from 'lucide-react';
import { ApiService } from '../../services/ApiService';
import { MessageRequest, MessageResponse } from '../../types/Message';

const MESSAGE_TYPES = [
  { value: 'FINANCIAL_REQUEST_0200', label: '0200 - Solicitud Financiera' },
  { value: 'REVERSAL_REQUEST_0400', label: '0400 - Solicitud de Reverso' },
  { value: 'NETWORK_REQUEST_0800', label: '0800 - Solicitud de Red' }
];

const COMMON_FIELDS = {
  '2': 'PAN (Primary Account Number)',
  '3': 'Processing Code',
  '4': 'Transaction Amount',
  '7': 'Transmission Date/Time',
  '11': 'System Trace Audit Number',
  '12': 'Local Transaction Time',
  '13': 'Local Transaction Date',
  '32': 'Acquiring Institution ID',
  '37': 'Retrieval Reference Number',
  '41': 'Terminal ID',
  '42': 'Merchant ID',
  '39': 'Response Code',
  '90': 'Original Data Elements'
};

export const MessageEditor: React.FC = () => {
  const [messageType, setMessageType] = useState<string>('');
  const [fields, setFields] = useState<Record<string, string>>({});
  const [mockMode, setMockMode] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [response, setResponse] = useState<MessageResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (messageType) {
      loadTemplate(messageType);
    }
  }, [messageType]);

  const loadTemplate = async (msgType: string) => {
    try {
      const template = await ApiService.getMessageTemplate(msgType);
      setFields(template.fields || {});
    } catch (error) {
      console.error('Error loading template:', error);
    }
  };

  const updateField = (fieldNumber: string, value: string) => {
    setFields(prev => ({
      ...prev,
      [fieldNumber]: value
    }));
  };

  const addField = () => {
    const fieldNumber = prompt('Ingrese el número del campo (ej: 48):');
    if (fieldNumber && !fields[fieldNumber]) {
      updateField(fieldNumber, '');
    }
  };

  const removeField = (fieldNumber: string) => {
    setFields(prev => {
      const newFields = { ...prev };
      delete newFields[fieldNumber];
      return newFields;
    });
  };

  const sendMessage = async () => {
    if (!messageType) {
      setError('Seleccione un tipo de mensaje');
      return;
    }

    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const request: MessageRequest = {
        messageType,
        fields,
        mockResponse: mockMode,
        timeout: 30000
      };

      const result = await ApiService.sendMessage(request);
      setResponse(result);
    } catch (error: any) {
      setError(error.message || 'Error enviando mensaje');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  const exportMessage = () => {
    const data = {
      messageType,
      fields,
      response: response ? {
        success: response.success,
        responseCode: response.responseCode,
        responseTime: response.responseTime,
        timestamp: response.timestamp
      } : null
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `iso8583-message-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-gray-900">Editor de Mensajes ISO8583</h1>
        <div className="flex space-x-2">
          <Button variant="outline" onClick={exportMessage} disabled={!response}>
            <Download className="w-4 h-4 mr-2" />
            Exportar
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Message Builder */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <FileText className="w-5 h-5 mr-2" />
              Construcción del Mensaje
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Message Type Selection */}
            <div>
              <Label htmlFor="messageType">Tipo de Mensaje</Label>
              <Select value={messageType} onValueChange={setMessageType}>
                <SelectTrigger>
                  <SelectValue placeholder="Seleccione el tipo de mensaje" />
                </SelectTrigger>
                <SelectContent>
                  {MESSAGE_TYPES.map(type => (
                    <SelectItem key={type.value} value={type.value}>
                      {type.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Mock Mode Toggle */}
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="mockMode"
                checked={mockMode}
                onChange={(e) => setMockMode(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="mockMode">Modo Mock (sin enviar al autorizador)</Label>
            </div>

            {/* Fields Editor */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <Label>Campos del Mensaje</Label>
                <Button variant="outline" size="sm" onClick={addField}>
                  + Agregar Campo
                </Button>
              </div>

              <div className="space-y-2 max-h-64 overflow-y-auto">
                {Object.entries(fields).map(([fieldNumber, value]) => (
                  <div key={fieldNumber} className="flex items-center space-x-2">
                    <div className="w-12">
                      <Badge variant="outline">{fieldNumber}</Badge>
                    </div>
                    <div className="flex-1">
                      <Input
                        placeholder={COMMON_FIELDS[fieldNumber] || `Campo ${fieldNumber}`}
                        value={value}
                        onChange={(e) => updateField(fieldNumber, e.target.value)}
                      />
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => removeField(fieldNumber)}
                    >
                      ×
                    </Button>
                  </div>
                ))}
              </div>
            </div>

            {/* Send Button */}
            <Button
              onClick={sendMessage}
              disabled={loading || !messageType}
              className="w-full"
            >
              {loading ? (
                <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
              ) : (
                <Send className="w-4 h-4 mr-2" />
              )}
              {mockMode ? 'Generar Mock' : 'Enviar Mensaje'}
            </Button>

            {/* Error Display */}
            {error && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
          </CardContent>
        </Card>

        {/* Response Display */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <CheckCircle className="w-5 h-5 mr-2" />
              Respuesta del Mensaje
            </CardTitle>
          </CardHeader>
          <CardContent>
            {response ? (
              <div className="space-y-4">
                {/* Status */}
                <div className="flex items-center justify-between">
                  <span className="font-medium">Estado:</span>
                  <Badge variant={response.success ? "default" : "destructive"}>
                    {response.success ? 'Exitoso' : 'Fallido'}
                  </Badge>
                </div>

                {/* Response Time */}
                {response.responseTime && (
                  <div className="flex items-center justify-between">
                    <span className="font-medium">Tiempo de Respuesta:</span>
                    <span>{response.responseTime}ms</span>
                  </div>
                )}

                {/* Response Code */}
                {response.responseCode && (
                  <div className="flex items-center justify-between">
                    <span className="font-medium">Código de Respuesta:</span>
                    <Badge variant="outline">{response.responseCode}</Badge>
                  </div>
                )}

                {/* MTI */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <span className="font-medium text-sm">Request MTI:</span>
                    <p className="text-sm text-gray-600">{response.requestMti}</p>
                  </div>
                  <div>
                    <span className="font-medium text-sm">Response MTI:</span>
                    <p className="text-sm text-gray-600">{response.responseMti}</p>
                  </div>
                </div>

                {/* Response Fields */}
                {response.responseFields && (
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-medium">Campos de Respuesta:</span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => copyToClipboard(JSON.stringify(response.responseFields, null, 2))}
                      >
                        <Copy className="w-3 h-3 mr-1" />
                        Copiar
                      </Button>
                    </div>
                    <Textarea
                      value={JSON.stringify(response.responseFields, null, 2)}
                      readOnly
                      className="font-mono text-xs"
                      rows={8}
                    />
                  </div>
                )}

                {/* Error Message */}
                {response.errorMessage && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>{response.errorMessage}</AlertDescription>
                  </Alert>
                )}

                {/* Timestamp */}
                <div className="text-xs text-gray-500">
                  {new Date(response.timestamp).toLocaleString()}
                </div>
              </div>
            ) : (
              <div className="text-center text-gray-500 py-8">
                <FileText className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>Envíe un mensaje para ver la respuesta aquí</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};