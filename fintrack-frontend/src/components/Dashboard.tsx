import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import CreditCards from './CreditCards';
import Invoices from './Invoices';
import MyShares from './MyShares';
import InvoiceImport from './InvoiceImport';
import Layout from './layout/Layout';

const Dashboard: React.FC = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const location = useLocation();
  const [activeView, setActiveView] = useState<'main' | 'creditCards' | 'invoices' | 'importInvoices' | 'myShares'>('main');

  // Atualizar view baseado no estado da navegação
  useEffect(() => {
    if (location.state?.view) {
      setActiveView(location.state.view);
    }
  }, [location.state]);

  let title = t('dashboard.welcome') + (user?.name ? `, ${user.name}!` : '');
  if (activeView === 'creditCards') title = t('creditCards.title');
  if (activeView === 'invoices') title = t('invoices.title');
  if (activeView === 'importInvoices') title = t('invoiceImport.title');
  if (activeView === 'myShares') title = t('shares.title');

  return (
    <Layout title={title} userName={user?.name}>
      {activeView === 'main' && (
        <div className="dashboard-grid grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="dashboard-card bg-white rounded-lg shadow p-6 flex flex-col items-start">
            <h3 className="text-lg font-semibold mb-2">{t('dashboard.creditCards')}</h3>
            <p className="text-textSecondary mb-4">{t('dashboard.creditCardsDescription')}</p>
            <button 
              className="bg-primary text-white px-4 py-2 rounded hover:bg-primary/90 transition"
              onClick={() => setActiveView('creditCards')}
            >
              {t('dashboard.viewCreditCards')}
            </button>
          </div>
          <div className="dashboard-card bg-white rounded-lg shadow p-6 flex flex-col items-start">
            <h3 className="text-lg font-semibold mb-2">{t('dashboard.invoices')}</h3>
            <p className="text-textSecondary mb-4">{t('dashboard.invoicesDescription')}</p>
            <button 
              className="bg-primary text-white px-4 py-2 rounded hover:bg-primary/90 transition"
              onClick={() => setActiveView('invoices')}
            >
              {t('dashboard.viewInvoices')}
            </button>
          </div>
          <div className="dashboard-card bg-white rounded-lg shadow p-6 flex flex-col items-start">
            <h3 className="text-lg font-semibold mb-2">{t('dashboard.myShares')}</h3>
            <p className="text-textSecondary mb-4">{t('dashboard.mySharesDescription')}</p>
            <button 
              className="bg-primary text-white px-4 py-2 rounded hover:bg-primary/90 transition"
              onClick={() => setActiveView('myShares')}
            >
              {t('dashboard.viewShares')}
            </button>
          </div>
        </div>
      )}
      
      {activeView !== 'main' && (
        <div className="mb-6">
          <button 
            onClick={() => setActiveView('main')}
            className="flex items-center gap-2 text-primary hover:text-primary/80 transition mb-4"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            {t('common.back')}
          </button>
        </div>
      )}
      
      {activeView === 'creditCards' && <CreditCards />}
      {activeView === 'invoices' && <Invoices />}
      {activeView === 'importInvoices' && <InvoiceImport />}
      {activeView === 'myShares' && <MyShares />}
    </Layout>
  );
};

export default Dashboard; 