import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronDown, ChevronUp, Calendar, Filter } from 'lucide-react';
import apiService from '../services/api';
import { ExpenseReportResponse } from '../types/expenseReport';
import { Category } from '../types/invoice';
import { formatCurrency, formatDate } from '../utils/invoiceUtils';
import './ExpenseReport.css';

const ExpenseReport: React.FC = () => {
  const { t, i18n } = useTranslation();
  const [report, setReport] = useState<ExpenseReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<string>(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [selectedCategoryId, setSelectedCategoryId] = useState<number | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [expandedCategories, setExpandedCategories] = useState<Set<number>>(new Set());
  const [showCategoryFilter, setShowCategoryFilter] = useState(false);

  useEffect(() => {
    loadCategories();
  }, []);

  useEffect(() => {
    loadExpenseReport();
  }, [selectedMonth, selectedCategoryId]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadCategories = async () => {
    try {
      const response = await apiService.getCategories();
      const uniqueByName = new Map<string, Category>();
      response.categories.forEach(category => {
        if (!uniqueByName.has(category.name)) {
          uniqueByName.set(category.name, category);
        }
      });
      setCategories(Array.from(uniqueByName.values()));
    } catch (err) {
      console.error('Error loading categories:', err);
    }
  };

  const loadExpenseReport = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiService.getExpensesByCategory(
        selectedMonth,
        selectedCategoryId || undefined
      );
      setReport(response);
    } catch (err: any) {
      console.error('Error loading expense report:', err);
      setError(err.response?.data?.message || t('expenseReport.errorLoadingReport'));
    } finally {
      setLoading(false);
    }
  };

  const handleMonthChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSelectedMonth(e.target.value);
  };

  const handleCategoryFilterChange = (categoryId: number | null) => {
    setSelectedCategoryId(categoryId);
    setShowCategoryFilter(false);
  };

  const toggleCategoryExpansion = (categoryId: number | null) => {
    // Use a string key to handle null categories (uncategorized)
    const key = categoryId !== null ? categoryId : -1;
    const newExpanded = new Set(expandedCategories);
    if (newExpanded.has(key)) {
      newExpanded.delete(key);
    } else {
      newExpanded.add(key);
    }
    setExpandedCategories(newExpanded);
  };

  const getMonthDisplay = (monthString: string) => {
    const [year, month] = monthString.split('-');
    const date = new Date(parseInt(year), parseInt(month) - 1);
    // Use the current i18n language for date formatting
    const locale = i18n.language === 'pt' ? 'pt-BR' : 'en-US';
    return date.toLocaleDateString(locale, { month: 'long', year: 'numeric' });
  };

  const calculatePercentage = (amount: number, total: number) => {
    if (total === 0) return 0;
    return (amount / total) * 100;
  };

  const getTotalTransactions = () => {
    if (!report) return 0;
    return report.expensesByCategory.reduce((sum, cat) => sum + cat.transactionCount, 0);
  };

  if (loading) {
    return (
      <div className="expense-report-container">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>{t('expenseReport.loading')}</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="expense-report-container">
        <div className="error-container">
          <p className="error-message">{error}</p>
          <button onClick={loadExpenseReport} className="retry-button">
            {t('expenseReport.retry')}
          </button>
        </div>
      </div>
    );
  }

  if (!report) {
    return (
      <div className="expense-report-container">
        <div className="empty-state">
          <p>{t('expenseReport.noData')}</p>
        </div>
      </div>
    );
  }

  const totalAmount = report.totalAmount;
  const totalCategories = report.expensesByCategory.length;
  const totalTransactions = getTotalTransactions();

  return (
    <div className="expense-report-container">
      {/* Header with Filters */}
      <div className="report-header">
        <h1 className="report-title">{t('expenseReport.title')}</h1>
        <div className="filters-container">
          <div className="filter-group">
            <label htmlFor="month-selector" className="filter-label">
              <Calendar size={18} />
              {t('expenseReport.month')}
            </label>
            <input
              id="month-selector"
              type="month"
              value={selectedMonth}
              onChange={handleMonthChange}
              className="month-input"
            />
          </div>
          
          <div className="filter-group">
            <label className="filter-label">
              <Filter size={18} />
              {t('expenseReport.category')}
            </label>
            <div className="category-filter-wrapper">
              <button
                onClick={() => setShowCategoryFilter(!showCategoryFilter)}
                className="category-filter-button"
              >
                {selectedCategoryId
                  ? categories.find(c => c.id === selectedCategoryId)?.name || t('expenseReport.allCategories')
                  : t('expenseReport.allCategories')}
              </button>
              {showCategoryFilter && (
                <div className="category-filter-dropdown">
                  <button
                    onClick={() => handleCategoryFilterChange(null)}
                    className={`filter-option ${selectedCategoryId === null ? 'active' : ''}`}
                  >
                    {t('expenseReport.allCategories')}
                  </button>
                  {categories.map(category => (
                    <button
                      key={category.id}
                      onClick={() => handleCategoryFilterChange(category.id || 0)}
                      className={`filter-option ${selectedCategoryId === category.id ? 'active' : ''}`}
                    >
                      {category.name}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="summary-cards">
        <div className="summary-card total-card">
          <div className="card-icon">💰</div>
          <div className="card-content">
            <h3 className="card-label">{t('expenseReport.totalAmount')}</h3>
            <p className="card-value">{formatCurrency(totalAmount)}</p>
          </div>
        </div>

        <div className="summary-card categories-card">
          <div className="card-icon">📊</div>
          <div className="card-content">
            <h3 className="card-label">{t('expenseReport.totalCategories')}</h3>
            <p className="card-value">{totalCategories}</p>
          </div>
        </div>

        <div className="summary-card transactions-card">
          <div className="card-icon">📝</div>
          <div className="card-content">
            <h3 className="card-label">{t('expenseReport.totalTransactions')}</h3>
            <p className="card-value">{totalTransactions}</p>
          </div>
        </div>
      </div>

      {/* Month Display */}
      <div className="month-display">
        <h2>{getMonthDisplay(selectedMonth)}</h2>
      </div>

      {/* Categories List */}
      {report.expensesByCategory.length === 0 ? (
        <div className="empty-state">
          <p>{t('expenseReport.noExpensesForMonth')}</p>
        </div>
      ) : (
        <div className="categories-list">
          {report.expensesByCategory.map((categoryExpense) => {
            const categoryId = categoryExpense.category?.id || null;
            const expansionKey = categoryId !== null ? categoryId : -1;
            const isExpanded = expandedCategories.has(expansionKey);
            const percentage = calculatePercentage(categoryExpense.totalAmount, totalAmount);

            return (
              <div
                key={categoryId || 'uncategorized'}
                className="category-card"
                style={{
                  borderLeftColor: categoryExpense.category?.color || '#CCCCCC'
                }}
              >
                <div
                  className="category-card-header"
                  onClick={() => toggleCategoryExpansion(categoryId)}
                >
                  <div className="category-info">
                    <div
                      className="category-color-indicator"
                      style={{ backgroundColor: categoryExpense.category?.color || '#CCCCCC' }}
                    />
                    <div className="category-details">
                      <h3 className="category-name">{categoryExpense.category?.name || 'Sem categoria'}</h3>
                      <div className="category-meta">
                        <span className="category-percentage">
                          {percentage.toFixed(1)}% {t('expenseReport.ofTotal')}
                        </span>
                        <span className="category-separator">•</span>
                        <span className="category-transactions">
                          {categoryExpense.transactionCount} {t('expenseReport.transactions')}
                        </span>
                      </div>
                    </div>
                  </div>
                  <div className="category-amount-section">
                    <p className="category-amount">{formatCurrency(categoryExpense.totalAmount)}</p>
                    <button className="expand-button">
                      {isExpanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                    </button>
                  </div>
                </div>

                {isExpanded && (
                  <div className="category-details-expanded">
                    {categoryExpense.details && categoryExpense.details.length > 0 ? (
                      <div className="expense-details-list">
                        {categoryExpense.details.map((detail, index) => (
                          <div key={detail.shareId || detail.itemId || index} className="expense-detail-item">
                            <div className="detail-main">
                              <div className="detail-description">
                                <p className="detail-item-name">{detail.itemDescription}</p>
                                <p className="detail-date">{formatDate(detail.purchaseDate)}</p>
                              </div>
                              <div className="detail-amount">
                                <p className="detail-amount-value">{formatCurrency(detail.amount)}</p>
                              </div>
                            </div>
                            {detail.shareId && (
                              <div className="detail-meta">
                                <span className="detail-badge shared-badge">
                                  {t('expenseReport.shared')}
                                </span>
                              </div>
                            )}
                            {!detail.shareId && (
                              <div className="detail-meta">
                                <span className="detail-badge unshared-badge">
                                  {t('expenseReport.unshared')}
                                </span>
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="details-placeholder">
                        {t('expenseReport.noDetailsAvailable')}
                      </p>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default ExpenseReport;

