import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';
import { ExpenseByRecurrenceResponse } from '../../types/expenseReport';
import { formatCurrency } from '../../utils/invoiceUtils';
import './RecurrenceDonutChart.css';

interface RecurrenceDonutChartProps {
  data: ExpenseByRecurrenceResponse[];
  loading: boolean;
}

const RECURRENCE_COLORS: Record<string, string> = {
  INSTALLMENT: '#6366f1',
  SINGLE: '#10b981',
};

const RecurrenceDonutChart: React.FC<RecurrenceDonutChartProps> = ({ data, loading }) => {
  const { t } = useTranslation();
  const total = useMemo(() => data.reduce((sum, d) => sum + d.amount, 0), [data]);

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

  const renderTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const item = payload[0].payload;
      return (
        <div className="recurrence-tooltip">
          <p className="recurrence-tooltip-name">
            {item.type === 'INSTALLMENT' ? t('expenseReport.installment') : t('expenseReport.singlePurchase')}
          </p>
          <p className="recurrence-tooltip-value">{formatCurrency(item.amount)}</p>
          <p className="recurrence-tooltip-pct">{item.percentage.toFixed(1)}%</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="recurrence-chart-wrapper">
      <div className="recurrence-chart-donut">
        <ResponsiveContainer width="100%" height={220}>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={90}
              dataKey="amount"
              stroke="none"
            >
              {data.map((entry) => (
                <Cell
                  key={entry.type}
                  fill={RECURRENCE_COLORS[entry.type] || '#a3a3a3'}
                />
              ))}
            </Pie>
            <Tooltip content={renderTooltip} />
          </PieChart>
        </ResponsiveContainer>
        <div className="recurrence-center-label">
          <span className="recurrence-center-value">{formatCurrency(total)}</span>
          <span className="recurrence-center-text">{t('common.total')}</span>
        </div>
      </div>
      <div className="recurrence-legend">
        {data.map((entry) => (
          <div key={entry.type} className="recurrence-legend-item">
            <span
              className="recurrence-legend-dot"
              style={{ backgroundColor: RECURRENCE_COLORS[entry.type] || '#a3a3a3' }}
            />
            <span className="recurrence-legend-name">
              {entry.type === 'INSTALLMENT' ? t('expenseReport.installment') : t('expenseReport.singlePurchase')}
            </span>
            <span className="recurrence-legend-value">{entry.percentage.toFixed(1)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default RecurrenceDonutChart;
