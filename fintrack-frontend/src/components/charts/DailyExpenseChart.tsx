import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { DailyExpense } from '../../types/dashboard';
import { formatCurrency } from '../../utils/invoiceUtils';
import './DailyExpenseChart.css';

interface DailyExpenseChartProps {
  data: DailyExpense[];
  loading: boolean;
}

const DailyExpenseChart: React.FC<DailyExpenseChartProps> = ({ data, loading }) => {
  const { t } = useTranslation();
  const chartData = useMemo(() => data.map(d => ({
    ...d,
    label: new Date(d.date + 'T00:00:00').getDate().toString(),
  })), [data]);

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

  const renderTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const item = payload[0];
      return (
        <div className="daily-tooltip">
          <p className="daily-tooltip-date">{t('dashboard.day')} {label}</p>
          <p className="daily-tooltip-value">{formatCurrency(item.value)}</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="daily-chart-wrapper">
      <ResponsiveContainer width="100%" height={280}>
        <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 5 }}>
          <defs>
            <linearGradient id="dailyGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#6366f1" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 11, fill: '#737373' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
          />
          <YAxis
            tick={{ fontSize: 11, fill: '#737373' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
            tickFormatter={(v) => formatCurrency(v)}
          />
          <Tooltip content={renderTooltip} />
          <Area
            type="monotone"
            dataKey="amount"
            stroke="#6366f1"
            strokeWidth={2}
            fill="url(#dailyGradient)"
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

export default DailyExpenseChart;
