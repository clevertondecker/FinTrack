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
import { MonthlyExpenseData } from '../../types/expenseReport';
import { formatCurrency, formatMonthLabel } from '../../utils/invoiceUtils';
import './MonthlyEvolutionChart.css';

interface MonthlyEvolutionChartProps {
  data: MonthlyExpenseData[];
  loading: boolean;
}

const MonthlyEvolutionChart: React.FC<MonthlyEvolutionChartProps> = ({ data, loading }) => {
  const { t, i18n } = useTranslation();

  // Collect unique categories across all months and build stacked data
  const { chartData, categoryKeys } = useMemo(() => {
    if (data.length === 0) return { chartData: [], categoryKeys: [] };

    // Collect all unique categories with their colors
    const categoryMap = new Map<string, string>();
    data.forEach(month => {
      month.categories.forEach(cat => {
        if (!categoryMap.has(cat.categoryName)) {
          categoryMap.set(cat.categoryName, cat.categoryColor);
        }
      });
    });

    // Sort categories by total amount across all months (descending)
    const categoryTotals = new Map<string, number>();
    data.forEach(month => {
      month.categories.forEach(cat => {
        categoryTotals.set(
          cat.categoryName,
          (categoryTotals.get(cat.categoryName) || 0) + cat.totalAmount
        );
      });
    });

    const sortedCategories = Array.from(categoryMap.entries())
      .sort((a, b) => (categoryTotals.get(b[0]) || 0) - (categoryTotals.get(a[0]) || 0));

    const keys = sortedCategories.map(([name, color]) => ({ name, color }));

    const rows = data.map(month => {
      const row: Record<string, any> = {
        month: month.month,
        label: formatMonthLabel(month.month, i18n.language),
        total: month.totalAmount,
      };
      keys.forEach(({ name }) => {
        const cat = month.categories.find(c => c.categoryName === name);
        row[name] = cat?.totalAmount || 0;
      });
      return row;
    });

    return { chartData: rows, categoryKeys: keys };
  }, [data, i18n.language]); // eslint-disable-line react-hooks/exhaustive-deps

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

  const renderCustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const total = payload.reduce((sum: number, entry: any) => sum + (entry.value || 0), 0);
      return (
        <div className="evolution-tooltip">
          <p className="evolution-tooltip-month">{label}</p>
          <p className="evolution-tooltip-total">{formatCurrency(total)}</p>
          <div className="evolution-tooltip-categories">
            {payload
              .filter((entry: any) => entry.value > 0)
              .sort((a: any, b: any) => b.value - a.value)
              .map((entry: any, index: number) => (
                <div key={index} className="evolution-tooltip-row">
                  <span className="evolution-tooltip-dot" style={{ backgroundColor: entry.fill }} />
                  <span className="evolution-tooltip-name">{entry.dataKey}</span>
                  <span className="evolution-tooltip-value">{formatCurrency(entry.value)}</span>
                </div>
              ))}
          </div>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="evolution-chart-wrapper">
      <ResponsiveContainer width="100%" height={350}>
        <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 12, fill: '#737373' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
          />
          <YAxis
            tick={{ fontSize: 12, fill: '#737373' }}
            axisLine={{ stroke: '#e5e5e5' }}
            tickLine={false}
            tickFormatter={(value) => formatCurrency(value)}
          />
          <Tooltip content={renderCustomTooltip} />
          {categoryKeys.map(({ name, color }) => (
            <Area
              key={name}
              type="monotone"
              dataKey={name}
              stackId="1"
              stroke={color}
              fill={color}
              fillOpacity={0.6}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
      <div className="evolution-legend">
        {categoryKeys.map(({ name, color }) => (
          <div key={name} className="evolution-legend-item">
            <span className="evolution-legend-dot" style={{ backgroundColor: color }} />
            <span className="evolution-legend-name">{name}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default MonthlyEvolutionChart;
