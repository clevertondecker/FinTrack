import React from 'react';
import { useTranslation } from 'react-i18next';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { ExpenseByCategoryResponse } from '../../types/expenseReport';
import { formatCurrency } from '../../utils/invoiceUtils';
import './CategoryDonutChart.css';

interface CategoryDonutChartProps {
  data: ExpenseByCategoryResponse[];
  totalAmount: number;
}

const CategoryDonutChart: React.FC<CategoryDonutChartProps> = ({ data, totalAmount }) => {
  const { t } = useTranslation();

  if (data.length === 0) {
    return (
      <div className="chart-empty-state">
        <p>{t('expenseReport.charts.noChartData')}</p>
      </div>
    );
  }

  const chartData = data.map(item => ({
    name: item.category?.name || t('expenseReport.uncategorized'),
    value: item.totalAmount,
    color: item.category?.color || '#CCCCCC',
  }));

  const renderCustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const entry = payload[0];
      const percentage = totalAmount > 0
        ? ((entry.value / totalAmount) * 100).toFixed(1)
        : '0';
      return (
        <div className="donut-tooltip">
          <div className="donut-tooltip-color" style={{ backgroundColor: entry.payload.color }} />
          <div>
            <p className="donut-tooltip-name">{entry.name}</p>
            <p className="donut-tooltip-value">{formatCurrency(entry.value)}</p>
            <p className="donut-tooltip-percent">
              {t('expenseReport.charts.percentOfTotal', { percent: percentage })}
            </p>
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="donut-chart-wrapper">
      <div className="donut-chart-container">
        <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={70}
              outerRadius={110}
              paddingAngle={2}
              dataKey="value"
              stroke="none"
            >
              {chartData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={renderCustomTooltip} />
          </PieChart>
        </ResponsiveContainer>
        <div className="donut-center-label">
          <span className="donut-center-amount">{formatCurrency(totalAmount)}</span>
          <span className="donut-center-text">{t('expenseReport.totalAmount')}</span>
        </div>
      </div>
      <div className="donut-legend">
        {chartData.map((entry, index) => (
          <div key={index} className="donut-legend-item">
            <span className="donut-legend-dot" style={{ backgroundColor: entry.color }} />
            <span className="donut-legend-name">{entry.name}</span>
            <span className="donut-legend-value">{formatCurrency(entry.value)}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default CategoryDonutChart;
