import { MessageRequest, MessageResponse } from '../types/Message';

const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';

class ApiServiceClass {
  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${BASE_URL}${endpoint}`;

    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }

      return response.text() as unknown as T;
    } catch (error) {
      console.error(`API Error (${endpoint}):`, error);
      throw error;
    }
  }

  // Simulator endpoints
  async sendMessage(request: MessageRequest): Promise<MessageResponse> {
    return this.request<MessageResponse>('/simulator/send', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async generateMockMessage(request: MessageRequest): Promise<MessageResponse> {
    return this.request<MessageResponse>('/simulator/mock', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  async getConnectionStatus(): Promise<any> {
    return this.request('/simulator/connection/status');
  }

  async testConnection(): Promise<any> {
    return this.request('/simulator/connection/test', {
      method: 'POST',
    });
  }

  async getSimulatorStats(): Promise<any> {
    return this.request('/simulator/stats');
  }

  async getMessageTypes(): Promise<any> {
    return this.request('/simulator/message-types');
  }

  async getMessageTemplate(messageType: string): Promise<any> {
    return this.request(`/simulator/message-template/${messageType}`);
  }

  async getHealth(): Promise<any> {
    return this.request('/simulator/health');
  }

  // Configuration endpoints
  async getSwitchConfiguration(): Promise<any> {
    return this.request('/config/switch');
  }

  async updateSwitchConfiguration(config: any): Promise<any> {
    return this.request('/config/switch', {
      method: 'PUT',
      body: JSON.stringify(config),
    });
  }

  async getFieldGenerationConfig(): Promise<any> {
    return this.request('/config/field-generation');
  }

  async updateFieldGenerationConfig(config: any): Promise<any> {
    return this.request('/config/field-generation', {
      method: 'PUT',
      body: JSON.stringify(config),
    });
  }

  // Test endpoints
  async runTestScenario(scenario: any): Promise<any> {
    return this.request('/test/scenario', {
      method: 'POST',
      body: JSON.stringify(scenario),
    });
  }

  async getTestResults(): Promise<any> {
    return this.request('/test/results');
  }

  async loadTestData(csvData: string): Promise<any> {
    return this.request('/test/load-data', {
      method: 'POST',
      headers: {
        'Content-Type': 'text/csv',
      },
      body: csvData,
    });
  }
}

export const ApiService = new ApiServiceClass();