import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../contexts/AuthContext';
import CreditCards from './CreditCards';
import Invoices from './Invoices';
import MyShares from './MyShares';
import InvoiceImport from './InvoiceImport';
import ExpenseReport from './ExpenseReport';
import People from './People';
import Layout from './layout/Layout';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const location = useLocation();
  const [activeView, setActiveView] = useState<'main' | 'creditCards' | 'invoices' | 'importInvoices' | 'people' | 'myShares' | 'expenseReport'>('main');

  // Atualizar view baseado no estado da navegação
  useEffect(() => {
    const path = location.pathname;
    if (path.includes('/credit-cards')) setActiveView('creditCards');
    else if (path.includes('/invoices')) setActiveView('invoices');
    else if (path.includes('/import-invoices')) setActiveView('importInvoices');
    else if (path.includes('/people')) setActiveView('people');
    else if (path.includes('/my-shares')) setActiveView('myShares');
    else if (path.includes('/expense-report')) setActiveView('expenseReport');
    else setActiveView('main');
  }, [location]);

  let title = t('dashboard.welcome') + (user?.name ? `, ${user.name}!` : '');
  if (activeView === 'creditCards') title = t('creditCards.title');
  if (activeView === 'invoices') title = t('invoices.title');
  if (activeView === 'importInvoices') title = t('invoiceImport.title');
  if (activeView === 'people') title = t('people.title');
  if (activeView === 'myShares') title = t('shares.title');
  if (activeView === 'expenseReport') title = t('expenseReport.title');

  return (
    <Layout title={title}>
      {/* Main Dashboard View */}
      {activeView === 'main' && (
        <div className="dashboard-main">
          <div className="welcome-section">
            <h1>{t('dashboard.welcome')}{user?.name ? `, ${user.name}!` : ''}</h1>
            <p>{t('dashboard.description')}</p>
          </div>
          
          <div className="quick-actions">
            <h2>{t('dashboard.quickActions')}</h2>
            <div className="actions-grid">
              <div className="action-card" onClick={() => setActiveView('creditCards')}>
                <div className="action-icon">💳</div>
                <h3>{t('creditCards.title')}</h3>
                <p>{t('dashboard.manageCreditCards')}</p>
              </div>
              
              <div className="action-card" onClick={() => setActiveView('invoices')}>
                <div className="action-icon">📄</div>
                <h3>{t('invoices.title')}</h3>
                <p>{t('dashboard.manageInvoices')}</p>
              </div>
              
              <div className="action-card" onClick={() => setActiveView('importInvoices')}>
                <div className="action-icon">📤</div>
                <h3>{t('invoiceImport.title')}</h3>
                <p>{t('dashboard.importInvoices')}</p>
              </div>
              
              <div className="action-card" onClick={() => setActiveView('myShares')}>
                <div className="action-icon">👥</div>
                <h3>{t('shares.title')}</h3>
                <p>{t('dashboard.manageShares')}</p>
              </div>
            </div>
          </div>
        </div>
      )}
      
      {activeView === 'creditCards' && <CreditCards />}
      {activeView === 'invoices' && <Invoices />}
      {activeView === 'importInvoices' && <InvoiceImport />}
      {activeView === 'people' && <People />}
      {activeView === 'myShares' && <MyShares />}
      {activeView === 'expenseReport' && <ExpenseReport />}
    </Layout>
  );
};

export default Dashboard; 