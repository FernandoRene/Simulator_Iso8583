export interface SwitchConfiguration {
  host: string;
  port: number;
  timeout: number;
  connectionPool: {
    initialSize: number;
    maxSize: number;
    maxIdleTime: number;
  };
  retry: {
    maxAttempts: number;
    delay: number;
  };
  ssl?: {
    enabled: boolean;
    keyStore?: string;
    keyStorePassword?: string;
    trustStore?: string;
    trustStorePassword?: string;
  };
}

export interface FieldGenerationConfig {
  autoGenerateMissing: boolean;
  traceNumber: {
    start: number;
    max: number;
    current?: number;
  };
  datetime: {
    autoGenerate: boolean;
    timezone: string;
    format?: string;
  };
  pan: {
    prefix?: string;
    length?: number;
    generateCheckDigit?: boolean;
  };
  amounts: {
    defaultCurrency?: string;
    precision?: number;
  };
}

export interface MonitoringConfiguration {
  enabled: boolean;
  metricsInterval: number;
  performanceTracking: boolean;
  logLevel: 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';
  alerting?: {
    enabled: boolean;
    thresholds: {
      responseTime: number;
      errorRate: number;
      connectionFailures: number;
    };
  };
}

export interface PackagerConfiguration {
  name: string;
  type: 'XML' | 'JSON' | 'CUSTOM';
  path: string;
  settings?: Record<string, any>;
}

export interface ApplicationConfiguration {
  switch: SwitchConfiguration;
  fieldGeneration: FieldGenerationConfig;
  monitoring: MonitoringConfiguration;
  packager: PackagerConfiguration;
  security?: {
    authentication: boolean;
    encryption: boolean;
    roles: string[];
  };
}