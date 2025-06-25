import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import CreditCards from './CreditCards';
import Invoices from './Invoices';
import MyShares from './MyShares';
import LanguageSelector from './LanguageSelector';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const [activeView, setActiveView] = useState<'main' | 'creditCards' | 'invoices' | 'myShares'>('main');

  const handleLogout = () => {
    logout();
  };

  const handleViewChange = (view: 'main' | 'creditCards' | 'invoices' | 'myShares') => {
    setActiveView(view);
  };

  if (activeView === 'creditCards') {
    return (
      <div className="dashboard-container">
        <header className="dashboard-header">
          <div className="header-content">
            <button 
              onClick={() => handleViewChange('main')}
              className="back-button"
            >
              ← {t('common.back')}
            </button>
            <h1>{t('creditCards.title')}</h1>
            <div className="header-actions">
              <LanguageSelector />
              <button onClick={handleLogout} className="logout-button">
                {t('auth.logout')}
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
              onClick={() => handleViewChange('main')}
              className="back-button"
            >
              ← {t('common.back')}
            </button>
            <h1>{t('invoices.title')}</h1>
            <div className="header-actions">
              <LanguageSelector />
              <button onClick={handleLogout} className="logout-button">
                {t('auth.logout')}
              </button>
            </div>
          </div>
        </header>
        <Invoices />
      </div>
    );
  }

  if (activeView === 'myShares') {
    return (
      <div className="dashboard-container">
        <header className="dashboard-header">
          <div className="header-content">
            <button 
              onClick={() => handleViewChange('main')}
              className="back-button"
            >
              ← {t('common.back')}
            </button>
            <h1>{t('shares.title')}</h1>
            <div className="header-actions">
              <LanguageSelector />
              <button onClick={handleLogout} className="logout-button">
                {t('auth.logout')}
              </button>
            </div>
          </div>
        </header>
        <MyShares />
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>{t('dashboard.welcome')}, {user?.name}!</h1>
          <div className="header-actions">
            <LanguageSelector />
            <button onClick={handleLogout} className="logout-button">
              {t('auth.logout')}
            </button>
          </div>
        </div>
      </header>

      <main className="dashboard-main">
        <div className="dashboard-grid">
          <div className="dashboard-card">
            <h3>{t('dashboard.creditCards')}</h3>
            <p>{t('dashboard.creditCardsDescription')}</p>
            <button 
              className="card-button"
              onClick={() => handleViewChange('creditCards')}
            >
              {t('dashboard.viewCreditCards')}
            </button>
          </div>

          <div className="dashboard-card">
            <h3>{t('dashboard.invoices')}</h3>
            <p>{t('dashboard.invoicesDescription')}</p>
            <button 
              className="card-button"
              onClick={() => handleViewChange('invoices')}
            >
              {t('dashboard.viewInvoices')}
            </button>
          </div>

          <div className="dashboard-card">
            <h3>{t('dashboard.myShares')}</h3>
            <p>{t('dashboard.mySharesDescription')}</p>
            <button 
              className="card-button"
              onClick={() => handleViewChange('myShares')}
            >
              {t('dashboard.viewShares')}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Dashboard; 