import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { MonthlyExpenseData } from '../../types/expenseReport';
import { formatCurrency, formatMonthLabel } from '../../utils/invoiceUtils';
import './MonthComparisonChart.css';

interface MonthComparisonChartProps {
  currentMonth: MonthlyExpenseData | null;
  previousMonth: MonthlyExpenseData | null;
  loading: boolean;
}

const MonthComparisonChart: React.FC<MonthComparisonChartProps> = ({
  currentMonth,
  previousMonth,
  loading,
}) => {
  const { t, i18n } = useTranslation();

  const chartData = useMemo(() => {
    if (!currentMonth && !previousMonth) return [];

    // Collect all unique category names
    const categoryNames = new Set<string>();
    currentMonth?.categories.forEach(c => categoryNames.add(c.categoryName));
    previousMonth?.categories.forEach(c => categoryNames.add(c.categoryName));

    return Array.from(categoryNames).map(name => {
      const prev = previousMonth?.categories.find(c => c.categoryName === name);
      const curr = currentMonth?.categories.find(c => c.categoryName === name);
      return {
        category: name,
        [t('expenseReport.charts.previousMonth')]: prev?.totalAmount || 0,
        [t('expenseReport.charts.currentMonth')]: curr?.totalAmount || 0,
        color: curr?.categoryColor || prev?.categoryColor || '#CCCCCC',
      };
    }).sort((a, b) => {
      const aTotal = (a[t('expenseReport.charts.currentMonth')] as number) +
        (a[t('expenseReport.charts.previousMonth')] as number);
      const bTotal = (b[t('expenseReport.charts.currentMonth')] as number) +
        (b[t('expenseReport.charts.previousMonth')] as number);
      return bTotal - aTotal;
    });
  }, [currentMonth, previousMonth, t]);

  if (loading) {
    return (
      <div className="chart-loading">
        <div className="chart-spinner" />
      </div>
    );
  }

  if (chartData.length === 0) {
    return (
      <div className="chart-empty-state">
        <p>{t('expenseReport.charts.noChartData')}</p>
      </div>
    );
  }

  const prevLabel = previousMonth
    ? formatMonthLabel(previousMonth.month, i18n.language)
    : t('expenseReport.charts.previousMonth');
  const currLabel = currentMonth
    ? formatMonthLabel(currentMonth.month, i18n.language)
    : t('expenseReport.charts.currentMonth');

  const renderCustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="comparison-tooltip">
          <p className="comparison-tooltip-label">{label}</p>
          {payload.map((entry: any, index: number) => (
            <div key={index} className="comparison-tooltip-row">
              <span className="comparison-tooltip-dot" style={{ backgroundColor: entry.fill }} />
              <span className="comparison-tooltip-name">{entry.name}:</span>
              <span className="comparison-tooltip-value">{formatCurrency(entry.value)}</span>
            </div>
          ))}
        </div>
      );
    }
    return null;
  };

  const totalDiff = (currentMonth?.totalAmount || 0) - (previousMonth?.totalAmount || 0);
  const diffPercent = previousMonth && previousMonth.totalAmount > 0
    ? ((totalDiff / previousMonth.totalAmount) * 100).toFixed(1)
    : null;

  return (
    <div className="comparison-chart-wrapper">
      <div className="comparison-summary">
        <div className="comparison-month-box">
          <span className="comparison-month-label">{prevLabel}</span>
          <span className="comparison-month-value">{formatCurrency(previousMonth?.totalAmount || 0)}</span>
        </div>
        <div className={`comparison-diff ${totalDiff > 0 ? 'diff-increase' : totalDiff < 0 ? 'diff-decrease' : ''}`}>
          {totalDiff > 0 ? '+' : ''}{formatCurrency(totalDiff)}
          {diffPercent && <span className="diff-percent"> ({totalDiff > 0 ? '+' : ''}{diffPercent}%)</span>}
        </div>
        <div className="comparison-month-box">
          <span className="comparison-month-label">{currLabel}</span>
          <span className="comparison-month-value">{formatCurrency(currentMonth?.totalAmount || 0)}</span>
        </div>
      </div>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis
            dataKey="category"
            tick={{ fontSize: 12, fill: '#737373' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
            interval={0}
            angle={-25}
            textAnchor="end"
            height={60}
          />
          <YAxis
            tick={{ fontSize: 12, fill: '#737373' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
            tickFormatter={(value) => formatCurrency(value)}
          />
          <Tooltip content={renderCustomTooltip} />
          <Legend />
          <Bar
            dataKey={t('expenseReport.charts.previousMonth')}
            fill="#94a3b8"
            radius={[4, 4, 0, 0]}
            maxBarSize={40}
          />
          <Bar
            dataKey={t('expenseReport.charts.currentMonth')}
            fill="#6366f1"
            radius={[4, 4, 0, 0]}
            maxBarSize={40}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};

export default MonthComparisonChart;
