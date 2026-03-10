import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar, TrendingUp, TrendingDown, Minus, ArrowRight } from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
} from 'recharts';
import apiService from '../services/api';
import { PeriodComparisonResponse } from '../types/expenseReport';
import { formatCurrency } from '../utils/invoiceUtils';
import './MonthComparison.css';

const MonthComparison: React.FC = () => {
  const { t } = useTranslation();
  const [data, setData] = useState<PeriodComparisonResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showTotal, setShowTotal] = useState(false);

  const now = new Date();
  const currentYM = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const prevDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const prevYM = `${prevDate.getFullYear()}-${String(prevDate.getMonth() + 1).padStart(2, '0')}`;

  const [monthA, setMonthA] = useState(currentYM);
  const [monthB, setMonthB] = useState(prevYM);

  const loadComparison = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiService.compareExpenses(monthA, monthB, showTotal);
      setData(result);
    } catch {
      setError(t('comparison.errorLoading'));
    } finally {
      setLoading(false);
    }
  }, [monthA, monthB, showTotal, t]);

  useEffect(() => {
    loadComparison();
  }, [loadComparison]);

  const chartData = useMemo(() => {
    if (!data) return [];
    return data.categoryComparisons
      .filter(c => c.currentAmount > 0 || c.comparisonAmount > 0)
      .sort((a, b) => Math.abs(b.differenceAmount) - Math.abs(a.differenceAmount))
      .map(c => ({
        name: c.category.name,
        color: c.category.color,
        current: c.currentAmount,
        comparison: c.comparisonAmount,
        diff: c.differenceAmount,
        diffPct: c.differencePercentage,
      }));
  }, [data]);

  const formatMonthDisplay = (ym: string) => {
    const [y, m] = ym.split('-');
    const date = new Date(parseInt(y), parseInt(m) - 1);
    return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short' });
  };

  const renderDiffIcon = (value: number) => {
    if (value > 0) return <TrendingUp size={16} />;
    if (value < 0) return <TrendingDown size={16} />;
    return <Minus size={16} />;
  };

  const diffClass = (value: number) => {
    if (value > 0) return 'diff-up';
    if (value < 0) return 'diff-down';
    return 'diff-neutral';
  };

  const renderChartTooltip = ({ active, payload, label }: any) => {
    if (!active || !payload?.length) return null;
    return (
      <div className="comparison-tooltip">
        <p className="comparison-tooltip-label">{label}</p>
        {payload.map((p: any) => (
          <p key={p.dataKey} style={{ color: p.fill }}>
            {p.name}: {formatCurrency(p.value)}
          </p>
        ))}
      </div>
    );
  };

  if (loading) {
    return (
      <div className="comparison-container">
        <div className="loading-container">
          <div className="spinner" />
          <p>{t('common.loading')}</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="comparison-container">
        <div className="loading-container">
          <p className="error-message">{error}</p>
          <button onClick={loadComparison} className="retry-button">{t('expenseReport.retry')}</button>
        </div>
      </div>
    );
  }

  return (
    <div className="comparison-container">
      {/* Controls */}
      <div className="comparison-controls">
        <div className="comparison-months-row">
          <div className="comparison-month-group">
            <label>{t('comparison.monthA')}</label>
            <div className="comparison-month-input">
              <Calendar size={16} />
              <input type="month" value={monthA} onChange={e => setMonthA(e.target.value)} />
            </div>
          </div>
          <ArrowRight size={20} className="comparison-arrow" />
          <div className="comparison-month-group">
            <label>{t('comparison.monthB')}</label>
            <div className="comparison-month-input">
              <Calendar size={16} />
              <input type="month" value={monthB} onChange={e => setMonthB(e.target.value)} />
            </div>
          </div>
        </div>
        <div className="comparison-toggle">
          <label className="toggle-label">
            <input type="checkbox" checked={showTotal} onChange={e => setShowTotal(e.target.checked)} />
            <span>{t('expenseReport.showingTotal')}</span>
          </label>
        </div>
      </div>

      {data && (
        <>
          {/* Summary Cards */}
          <div className="comparison-summary">
            <div className="comparison-card">
              <span className="comparison-card-label">{formatMonthDisplay(data.current.month)}</span>
              <span className="comparison-card-value">{formatCurrency(data.current.totalAmount)}</span>
              <span className="comparison-card-txns">
                {data.current.transactionCount} {t('expenseReport.transactions')}
              </span>
            </div>

            <div className={`comparison-card comparison-card-diff ${diffClass(data.differenceAmount)}`}>
              <span className="comparison-card-label">{t('comparison.difference')}</span>
              <span className="comparison-card-value">
                {renderDiffIcon(data.differenceAmount)}
                {formatCurrency(Math.abs(data.differenceAmount))}
              </span>
              <span className="comparison-card-pct">
                {data.differencePercentage > 0 ? '+' : ''}{data.differencePercentage.toFixed(1)}%
              </span>
            </div>

            <div className="comparison-card">
              <span className="comparison-card-label">{formatMonthDisplay(data.comparison.month)}</span>
              <span className="comparison-card-value">{formatCurrency(data.comparison.totalAmount)}</span>
              <span className="comparison-card-txns">
                {data.comparison.transactionCount} {t('expenseReport.transactions')}
              </span>
            </div>
          </div>

          {/* Bar Chart */}
          {chartData.length > 0 && (
            <div className="comparison-section">
              <h3 className="comparison-section-title">{t('comparison.byCategory')}</h3>
              <div className="comparison-chart-wrapper">
                <ResponsiveContainer width="100%" height={Math.max(300, chartData.length * 50)}>
                  <BarChart data={chartData} layout="vertical" margin={{ top: 5, right: 30, left: 10, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" horizontal={false} />
                    <XAxis type="number" tickFormatter={v => formatCurrency(v)} tick={{ fontSize: 11 }} />
                    <YAxis type="category" dataKey="name" width={120} tick={{ fontSize: 12 }} />
                    <Tooltip content={renderChartTooltip} />
                    <Bar dataKey="current" name={formatMonthDisplay(data.current.month)} fill="#6366f1" barSize={16} radius={[0, 3, 3, 0]} />
                    <Bar dataKey="comparison" name={formatMonthDisplay(data.comparison.month)} fill="#d1d5db" barSize={16} radius={[0, 3, 3, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}

          {/* Detail Table */}
          {data.categoryComparisons.length > 0 && (
            <div className="comparison-section">
              <h3 className="comparison-section-title">{t('comparison.details')}</h3>
              <div className="comparison-table-wrapper">
                <table className="comparison-table">
                  <thead>
                    <tr>
                      <th>{t('expenseReport.category')}</th>
                      <th>{formatMonthDisplay(data.current.month)}</th>
                      <th>{formatMonthDisplay(data.comparison.month)}</th>
                      <th>{t('comparison.difference')}</th>
                      <th>%</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.categoryComparisons
                      .filter(c => c.currentAmount > 0 || c.comparisonAmount > 0)
                      .sort((a, b) => Math.abs(b.differenceAmount) - Math.abs(a.differenceAmount))
                      .map(c => (
                        <tr key={c.category.id ?? 'uncategorized'}>
                          <td>
                            <div className="comparison-cat-cell">
                              <span className="comparison-cat-dot" style={{ backgroundColor: c.category.color }} />
                              {c.category.name}
                            </div>
                          </td>
                          <td>{formatCurrency(c.currentAmount)}</td>
                          <td>{formatCurrency(c.comparisonAmount)}</td>
                          <td className={diffClass(c.differenceAmount)}>
                            <span className="comparison-diff-cell">
                              {renderDiffIcon(c.differenceAmount)}
                              {formatCurrency(Math.abs(c.differenceAmount))}
                            </span>
                          </td>
                          <td className={diffClass(c.differencePercentage)}>
                            {c.differencePercentage > 0 ? '+' : ''}{c.differencePercentage.toFixed(1)}%
                          </td>
                        </tr>
                      ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <td><strong>{t('expenseReport.totalAmount')}</strong></td>
                      <td><strong>{formatCurrency(data.current.totalAmount)}</strong></td>
                      <td><strong>{formatCurrency(data.comparison.totalAmount)}</strong></td>
                      <td className={diffClass(data.differenceAmount)}>
                        <strong className="comparison-diff-cell">
                          {renderDiffIcon(data.differenceAmount)}
                          {formatCurrency(Math.abs(data.differenceAmount))}
                        </strong>
                      </td>
                      <td className={diffClass(data.differencePercentage)}>
                        <strong>{data.differencePercentage > 0 ? '+' : ''}{data.differencePercentage.toFixed(1)}%</strong>
                      </td>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default MonthComparison;
