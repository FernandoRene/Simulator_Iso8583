export interface MessageRequest {
  messageType: string;
  fields: Record<string, string>;
  mockResponse?: boolean;
  timeout?: number;
}

export interface MessageResponse {
  success: boolean;
  errorMessage?: string;
  requestMti?: string;
  responseMti?: string;
  requestFields?: Record<string, string>;
  responseFields?: Record<string, string>;
  responseCode?: string;
  responseTime?: number;
  timestamp: string | Date;
}

export interface MessageTemplate {
  messageType: string;
  fields: Record<string, string>;
  description?: string;
}

export interface MessageType {
  value: string;
  label: string;
  description?: string;
  requiredFields?: string[];
  optionalFields?: string[];
}

export interface ISO8583Field {
  number: string;
  name: string;
  description: string;
  type: 'numeric' | 'alphanumeric' | 'binary' | 'bitmap';
  length?: number;
  format?: string;
  required?: boolean;
  example?: string;
}

export interface MessageValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

export interface MessageHistory {
  id: string;
  timestamp: Date;
  messageType: string;
  request: MessageRequest;
  response?: MessageResponse;
  duration?: number;
  status: 'pending' | 'success' | 'error';
}