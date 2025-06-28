import React, { useState } from 'react';
import { Dashboard } from './components/Dashboard/Dashboard';
import { MessageEditor } from './components/MessageEditor/MessageEditor';
import { TestRunner } from './components/TestRunner/TestRunner';
import { Configuration } from './components/Configuration/Configuration';
import {
  LayoutDashboard,
  MessageSquare,
  TestTube,
  Settings,
  Menu,
  X
} from 'lucide-react';
import './App.css';

type ActiveTab = 'dashboard' | 'message-editor' | 'test-runner' | 'configuration';

const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ActiveTab>('dashboard');
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const navigation = [
    {
      id: 'dashboard' as ActiveTab,
      name: 'Dashboard',
      icon: LayoutDashboard,
      description: 'Información general del simulador'
    },
    {
      id: 'message-editor' as ActiveTab,
      name: 'Editor de Mensajes',
      icon: MessageSquare,
      description: 'Crear y enviar mensajes ISO8583'
    },
    {
      id: 'test-runner' as ActiveTab,
      name: 'Ejecutor de Pruebas',
      icon: TestTube,
      description: 'Ejecutar pruebas automatizadas'
    },
    {
      id: 'configuration' as ActiveTab,
      name: 'Configuración',
      icon: Settings,
      description: 'Configurar el simulador'
    }
  ];

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <Dashboard />;
      case 'message-editor':
        return <MessageEditor />;
      case 'test-runner':
        return <TestRunner />;
      case 'configuration':
        return <Configuration />;
      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <div className={`
        fixed inset-y-0 left-0 z-50 w-64 bg-white shadow-lg transform transition-transform duration-300 ease-in-out
        lg:translate-x-0 lg:static lg:inset-0
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
        <div className="flex items-center justify-between h-16 px-6 border-b border-gray-200">
          <h1 className="text-xl font-bold text-gray-900">ISO8583 Simulator</h1>
          <button
            onClick={() => setSidebarOpen(false)}
            className="lg:hidden p-1 rounded-md text-gray-400 hover:text-gray-500"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        <nav className="mt-8">
          <div className="px-3 space-y-1">
            {navigation.map((item) => {
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setActiveTab(item.id);
                    setSidebarOpen(false);
                  }}
                  className={`
                    group flex items-center px-3 py-2 text-sm font-medium rounded-md w-full text-left transition-colors duration-200
                    ${isActive
                      ? 'bg-blue-100 text-blue-700 border-r-2 border-blue-500'
                      : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
                    }
                  `}
                >
                  <item.icon
                    className={`
                      mr-3 flex-shrink-0 h-5 w-5
                      ${isActive ? 'text-blue-600' : 'text-gray-400 group-hover:text-gray-500'}
                    `}
                  />
                  <div className="flex flex-col">
                    <span>{item.name}</span>
                    <span className="text-xs text-gray-500">{item.description}</span>
                  </div>
                </button>
              );
            })}
          </div>
        </nav>

        {/* Footer */}
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-200">
          <div className="text-xs text-gray-500 text-center">
            <p>ISO8583 Simulator v1.0.0</p>
            <p className="mt-1">Connected to: 172.16.1.211:5105</p>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="flex-1 lg:ml-0">
        {/* Mobile header */}
        <div className="lg:hidden flex items-center justify-between h-16 px-4 bg-white shadow-sm border-b border-gray-200">
          <button
            onClick={() => setSidebarOpen(true)}
            className="p-2 rounded-md text-gray-400 hover:text-gray-500"
          >
            <Menu className="h-6 w-6" />
          </button>
          <h1 className="text-lg font-semibold text-gray-900">
            {navigation.find(item => item.id === activeTab)?.name}
          </h1>
          <div className="w-10" /> {/* Spacer */}
        </div>

        {/* Page content */}
        <main className="flex-1">
          {renderContent()}
        </main>
      </div>

      {/* Sidebar overlay for mobile */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-gray-600 bg-opacity-75 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}
    </div>
  );
};

export default App;