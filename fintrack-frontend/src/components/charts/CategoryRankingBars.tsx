import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { CategoryRanking } from '../../types/dashboard';
import { formatCurrency } from '../../utils/invoiceUtils';
import './CategoryRankingBars.css';

interface CategoryRankingBarsProps {
  data: CategoryRanking[];
  loading: boolean;
}

const CategoryRankingBars: React.FC<CategoryRankingBarsProps> = ({ data, loading }) => {
  const { t } = useTranslation();
  const maxPercentage = useMemo(() => data.length > 0 ? Math.max(...data.map(d => d.percentage)) : 1, [data]);

  if (loading) {
    return (
      <div className="chart-loading">
        <div className="chart-spinner" />
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <div className="chart-empty-state">
        <p>{t('expenseReport.charts.noChartData')}</p>
      </div>
    );
  }

  return (
    <div className="ranking-bars-container">
      {data.map((item, index) => (
        <div key={item.categoryId ?? 'uncategorized'} className="ranking-bar-row">
          <div className="ranking-bar-header">
            <div className="ranking-bar-label">
              <span className="ranking-position">#{index + 1}</span>
              <span
                className="ranking-color-dot"
                style={{ backgroundColor: item.color || '#a3a3a3' }}
              />
              <span className="ranking-name">{item.categoryName || t('expenseReport.uncategorized')}</span>
            </div>
            <div className="ranking-bar-values">
              <span className="ranking-percentage">{item.percentage.toFixed(1)}%</span>
              <span className="ranking-amount">{formatCurrency(item.amount)}</span>
            </div>
          </div>
          <div className="ranking-bar-track">
            <div
              className="ranking-bar-fill"
              style={{
                width: `${(item.percentage / maxPercentage) * 100}%`,
                backgroundColor: item.color || '#a3a3a3',
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
};

export default CategoryRankingBars;
