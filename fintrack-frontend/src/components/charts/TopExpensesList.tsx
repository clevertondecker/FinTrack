import React from 'react';
import { useTranslation } from 'react-i18next';
import { TopExpenseItem } from '../../types/expenseReport';
import { formatCurrency, formatDate } from '../../utils/invoiceUtils';
import './TopExpensesList.css';

interface TopExpensesListProps {
  expenses: TopExpenseItem[];
  loading: boolean;
}

const TopExpensesList: React.FC<TopExpensesListProps> = ({ expenses, loading }) => {
  const { t } = useTranslation();

  if (loading) {
    return (
      <div className="chart-loading">
        <div className="chart-spinner" />
      </div>
    );
  }

  if (expenses.length === 0) {
    return (
      <div className="chart-empty-state">
        <p>{t('expenseReport.charts.noChartData')}</p>
      </div>
    );
  }

  const maxAmount = expenses[0]?.amount || 1;

  return (
    <div className="top-expenses-wrapper">
      {expenses.map((expense) => {
        const barWidth = maxAmount > 0 ? (expense.amount / maxAmount) * 100 : 0;
        return (
          <div key={expense.itemId} className="top-expense-item">
            <div className="top-expense-rank">#{expense.rank}</div>
            <div className="top-expense-content">
              <div className="top-expense-header">
                <span className="top-expense-description">{expense.description}</span>
                <span className="top-expense-amount">{formatCurrency(expense.amount)}</span>
              </div>
              <div className="top-expense-bar-track">
                <div
                  className="top-expense-bar-fill"
                  style={{
                    width: `${barWidth}%`,
                    backgroundColor: expense.category?.color || '#CCCCCC',
                  }}
                />
              </div>
              <div className="top-expense-meta">
                <span className="top-expense-category">
                  <span
                    className="top-expense-category-dot"
                    style={{ backgroundColor: expense.category?.color || '#CCCCCC' }}
                  />
                  {expense.category?.name || t('expenseReport.uncategorized')}
                </span>
                <span className="top-expense-date">{formatDate(expense.purchaseDate)}</span>
                <span className="top-expense-percent">
                  {t('expenseReport.charts.percentOfTotal', {
                    percent: expense.percentageOfTotal.toFixed(1),
                  })}
                </span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default TopExpensesList;
