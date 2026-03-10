import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import { ExpenseByCardResponse } from '../../types/expenseReport';
import { formatCurrency } from '../../utils/invoiceUtils';
import './ExpenseByCardChart.css';

interface ExpenseByCardChartProps {
  data: ExpenseByCardResponse[];
  loading: boolean;
}

const CARD_COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'];

const ExpenseByCardChart: React.FC<ExpenseByCardChartProps> = ({ data, loading }) => {
  const { t } = useTranslation();
  const chartData = useMemo(() => data.map((card, i) => ({
    name: `${card.cardName} ••${card.lastFourDigits}`,
    amount: card.totalAmount,
    percentage: card.percentage,
    color: CARD_COLORS[i % CARD_COLORS.length],
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

  const renderTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const item = payload[0].payload;
      return (
        <div className="card-chart-tooltip">
          <p className="card-chart-tooltip-name">{item.name}</p>
          <p className="card-chart-tooltip-value">{formatCurrency(item.amount)}</p>
          <p className="card-chart-tooltip-pct">{item.percentage.toFixed(1)}%</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="card-chart-wrapper">
      <ResponsiveContainer width="100%" height={Math.max(200, data.length * 60)}>
        <BarChart data={chartData} layout="vertical" margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" horizontal={false} />
          <XAxis
            type="number"
            tick={{ fontSize: 11, fill: '#737373' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
            tickFormatter={(v) => formatCurrency(v)}
          />
          <YAxis
            type="category"
            dataKey="name"
            width={140}
            tick={{ fontSize: 12, fill: '#404040' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
          />
          <Tooltip content={renderTooltip} />
          <Bar dataKey="amount" radius={[0, 4, 4, 0]} barSize={28}>
            {chartData.map((entry, i) => (
              <Cell key={i} fill={entry.color} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default ExpenseByCardChart;
