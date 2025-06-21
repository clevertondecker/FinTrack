import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import CreditCards from './CreditCards';
import Invoices from './Invoices';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const [activeView, setActiveView] = useState<'main' | 'creditCards' | 'invoices'>('main');

  const handleLogout = () => {
    logout();
  };

  const handleViewChange = (view: 'main' | 'creditCards' | 'invoices') => {
    setActiveView(view);
  };

  if (activeView === 'creditCards') {
    return (
      <div className="dashboard-container">
        <header className="dashboard-header">
          <div className="header-content">
            <button 
              onClick={() => setActiveView('main')} 
              className="back-button"
            >
              ← Back to Dashboard
            </button>
            <div className="user-info">
              <span>Welcome, {user?.name}!</span>
              <button onClick={handleLogout} className="logout-button">
                Logout
              </button>
            </div>
          </div>
        </header>
        <CreditCards />
      </div>
    );
  }

  if (activeView === 'invoices') {
    return (
      <div className="dashboard-container">
        <header className="dashboard-header">
          <div className="header-content">
            <button 
              onClick={() => setActiveView('main')} 
              className="back-button"
            >
              ← Back to Dashboard
            </button>
            <div className="user-info">
              <span>Welcome, {user?.name}!</span>
              <button onClick={handleLogout} className="logout-button">
                Logout
              </button>
            </div>
          </div>
        </header>
        <Invoices />
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>FinTrack Dashboard</h1>
          <div className="user-info">
            <span>Welcome, {user?.name}!</span>
            <button onClick={handleLogout} className="logout-button">
              Logout
            </button>
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="dashboard-grid">
          <div className="dashboard-card">
            <h3>Credit Cards</h3>
            <p>Manage your credit cards and view transactions</p>
            <button 
              className="card-button"
              onClick={() => handleViewChange('creditCards')}
            >
              View Cards
            </button>
          </div>

          <div className="dashboard-card">
            <h3>Invoices</h3>
            <p>Track your monthly invoices and payments</p>
            <button 
              className="card-button"
              onClick={() => handleViewChange('invoices')}
            >
              View Invoices
            </button>
          </div>

          <div className="dashboard-card">
            <h3>Banks</h3>
            <p>Manage your bank accounts and connections</p>
            <button className="card-button">View Banks</button>
          </div>

          <div className="dashboard-card">
            <h3>Reports</h3>
            <p>Generate financial reports and analytics</p>
            <button className="card-button">View Reports</button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard; 