import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Calendar, CreditCard as CreditCardIcon, ShoppingCart, Receipt } from 'lucide-react';
import HelpTooltip from './common/HelpTooltip';
import { useAuth } from '../contexts/AuthContext';
import apiService from '../services/api';
import { DashboardOverviewResponse } from '../types/dashboard';
import { formatCurrency, getStatusColor, getStatusText, formatDate } from '../utils/invoiceUtils';
import DailyExpenseChart from './charts/DailyExpenseChart';
import CategoryRankingBars from './charts/CategoryRankingBars';
import CreditCards from './CreditCards';
import Invoices from './Invoices';
import MyShares from './MyShares';
import InvoiceImport from './InvoiceImport';
import ExpenseReport from './ExpenseReport';
import People from './People';
import Budgets from './Budgets';
import MonthComparison from './MonthComparison';
import CategoryManagement from './CategoryManagement';
import Layout from './layout/Layout';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState<string>('main');
  const [overview, setOverview] = useState<DashboardOverviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<string>(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });

  useEffect(() => {
    const path = location.pathname;
    if (path.includes('/credit-cards')) setActiveView('creditCards');
    else if (path.includes('/invoices')) setActiveView('invoices');
    else if (path.includes('/import-invoices')) setActiveView('importInvoices');
    else if (path.includes('/people')) setActiveView('people');
    else if (path.includes('/my-shares')) setActiveView('myShares');
    else if (path.includes('/expense-report')) setActiveView('expenseReport');
    else if (path.includes('/budgets')) setActiveView('budgets');
    else if (path.includes('/comparison')) setActiveView('comparison');
    else if (path.includes('/categories')) setActiveView('categories');
    else setActiveView('main');
  }, [location]);

  const loadOverview = useCallback(async () => {
    setError(null);
    try {
      setLoading(true);
      const data = await apiService.getDashboardOverview(selectedMonth);
      setOverview(data);
    } catch (err) {
      console.error('Error loading dashboard overview:', err);
      setError(t('dashboard.errorLoading'));
    } finally {
      setLoading(false);
    }
  }, [selectedMonth, t]);

  useEffect(() => {
    if (activeView === 'main') {
      loadOverview();
    }
  }, [activeView, loadOverview]);

  const title = useMemo(() => {
    let t_title = t('dashboard.welcome') + (user?.name ? `, ${user.name}!` : '');
    if (activeView === 'creditCards') t_title = t('creditCards.title');
    if (activeView === 'invoices') t_title = t('invoices.title');
    if (activeView === 'importInvoices') t_title = t('invoiceImport.title');
    if (activeView === 'people') t_title = t('people.title');
    if (activeView === 'myShares') t_title = t('shares.title');
    if (activeView === 'expenseReport') t_title = t('expenseReport.title');
    if (activeView === 'budgets') t_title = t('budgets.title');
    if (activeView === 'comparison') t_title = t('comparison.title');
    if (activeView === 'categories') t_title = t('categories.title');
    return t_title;
  }, [activeView, user?.name, t]);

  return (
    <Layout title={title}>
      {activeView === 'main' && (
        <div className="dashboard-main">
          {/* Month Selector */}
          <HelpTooltip textKey="help.dashboard.monthSelector" position="bottom">
            <div className="dashboard-month-selector">
              <label htmlFor="dashboard-month" className="dashboard-month-label">
                <Calendar size={18} />
                {t('expenseReport.month')}
              </label>
              <input
                id="dashboard-month"
                type="month"
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(e.target.value)}
                className="month-input"
              />
            </div>
          </HelpTooltip>

          {loading ? (
            <div className="loading-container">
              <div className="spinner" />
              <p>{t('common.loading')}</p>
            </div>
          ) : error ? (
            <div className="loading-container">
              <p className="error-message">{error}</p>
              <button onClick={loadOverview} className="retry-button">{t('expenseReport.retry')}</button>
            </div>
          ) : overview ? (
            <>
              {/* Summary Cards */}
              <HelpTooltip textKey="help.dashboard.totalExpenses" position="bottom">
              <div className="dashboard-stat-cards">
                <div
                  className="dashboard-stat-card stat-expenses dashboard-clickable"
                  onClick={() => navigate('/dashboard/expense-report')}
                  role="button"
                  tabIndex={0}
                >
                  <div className="stat-icon-wrapper stat-icon-expenses">
                    <ShoppingCart size={24} />
                  </div>
                  <div className="stat-content">
                    <span className="stat-label">{t('dashboard.totalExpenses')}</span>
                    <span className="stat-value">{formatCurrency(overview.totalExpenses)}</span>
                  </div>
                </div>

                <div
                  className="dashboard-stat-card stat-transactions dashboard-clickable"
                  onClick={() => navigate('/dashboard/invoices')}
                  role="button"
                  tabIndex={0}
                >
                  <div className="stat-icon-wrapper stat-icon-transactions">
                    <Receipt size={24} />
                  </div>
                  <div className="stat-content">
                    <span className="stat-label">{t('dashboard.transactions')}</span>
                    <span className="stat-value">{overview.totalTransactions}</span>
                  </div>
                </div>

                <div
                  className="dashboard-stat-card stat-cards dashboard-clickable"
                  onClick={() => navigate('/dashboard/credit-cards')}
                  role="button"
                  tabIndex={0}
                >
                  <div className="stat-icon-wrapper stat-icon-cards">
                    <CreditCardIcon size={24} />
                  </div>
                  <div className="stat-content">
                    <span className="stat-label">{t('dashboard.activeCards')}</span>
                    <span className="stat-value">{overview.creditCards.length}</span>
                  </div>
                </div>
              </div>
              </HelpTooltip>

              {/* Main Grid: Left (wide) + Right (narrow) */}
              <div className="dashboard-grid-layout">
                {/* Left Column */}
                <div className="dashboard-left-col">
                  {/* Credit Cards Table */}
                  {overview.creditCards.length > 0 && (
                    <HelpTooltip textKey="help.dashboard.creditCards" position="right">
                    <div className="dashboard-section-card">
                      <h3 className="dashboard-section-title">{t('dashboard.creditCardsOverview')}</h3>
                      <div className="dashboard-table-wrapper">
                        <table className="dashboard-table">
                          <thead>
                            <tr>
                              <th>{t('dashboard.card')}</th>
                              <th>{t('dashboard.dueDate')}</th>
                              <th>{t('dashboard.invoiceAmount')}</th>
                              <th>{t('common.status')}</th>
                            </tr>
                          </thead>
                          <tbody>
                            {overview.creditCards.map((card) => (
                              <tr
                                key={card.cardId}
                                className="dashboard-clickable-row"
                                onClick={() => navigate('/dashboard/invoices')}
                              >
                                <td>
                                  <div className="card-cell">
                                    <span className="card-cell-name">{card.cardName}</span>
                                    <span className="card-cell-digits">••{card.lastFourDigits}</span>
                                  </div>
                                </td>
                                <td>{card.dueDate ? formatDate(card.dueDate) : '-'}</td>
                                <td className="amount-cell">{formatCurrency(card.currentInvoiceAmount)}</td>
                                <td>
                                  <span className={`status-badge ${getStatusColor(card.invoiceStatus)}`}>
                                    {getStatusText(card.invoiceStatus, t)}
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                    </HelpTooltip>
                  )}

                  {/* Daily Expenses Chart */}
                  <HelpTooltip textKey="help.dashboard.dailyChart" position="right">
                  <div className="dashboard-section-card">
                    <h3 className="dashboard-section-title">{t('dashboard.dailyExpenses')}</h3>
                    <DailyExpenseChart data={overview.dailyExpenses} loading={false} />
                  </div>
                  </HelpTooltip>
                </div>

                {/* Right Column */}
                <div className="dashboard-right-col">
                  <HelpTooltip textKey="help.dashboard.categoryRanking" position="left">
                  <div
                    className="dashboard-section-card dashboard-clickable"
                    onClick={() => navigate('/dashboard/expense-report')}
                    role="button"
                    tabIndex={0}
                  >
                    <h3 className="dashboard-section-title">{t('dashboard.categoryRanking')}</h3>
                    <CategoryRankingBars data={overview.categoryRanking} loading={false} />
                  </div>
                  </HelpTooltip>
                </div>
              </div>
            </>
          ) : (
            <div className="empty-state">
              <p>{t('common.noData')}</p>
            </div>
          )}
        </div>
      )}

      {activeView === 'creditCards' && <CreditCards />}
      {activeView === 'invoices' && <Invoices />}
      {activeView === 'importInvoices' && <InvoiceImport />}
      {activeView === 'people' && <People />}
      {activeView === 'myShares' && <MyShares />}
      {activeView === 'expenseReport' && <ExpenseReport />}
      {activeView === 'budgets' && <Budgets />}
      {activeView === 'comparison' && <MonthComparison />}
      {activeView === 'categories' && <CategoryManagement />}
    </Layout>
  );
};

export default Dashboard;
