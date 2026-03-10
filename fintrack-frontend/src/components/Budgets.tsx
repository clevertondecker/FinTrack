import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar, Plus, Pencil, Trash2, X } from 'lucide-react';
import apiService from '../services/api';
import { BudgetStatusResponse } from '../types/budget';
import { Category } from '../types/invoice';
import { formatCurrency } from '../utils/invoiceUtils';
import './Budgets.css';

const Budgets: React.FC = () => {
  const { t } = useTranslation();
  const [budgets, setBudgets] = useState<BudgetStatusResponse[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<string>(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  });
  const [showModal, setShowModal] = useState(false);
  const [editingBudgetId, setEditingBudgetId] = useState<number | null>(null);
  const [formCategoryId, setFormCategoryId] = useState<string>('');
  const [formLimit, setFormLimit] = useState('');
  const [formRecurring, setFormRecurring] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadBudgets = useCallback(async () => {
    setError(null);
    try {
      setLoading(true);
      const data = await apiService.getBudgets(selectedMonth);
      setBudgets(data);
    } catch (err) {
      console.error('Error loading budgets:', err);
      setError(t('budgets.errorLoading'));
    } finally {
      setLoading(false);
    }
  }, [selectedMonth, t]);

  useEffect(() => {
    loadBudgets();
  }, [loadBudgets]);

  useEffect(() => {
    apiService.getCategories()
      .then(res => {
        const unique = new Map<string, Category>();
        res.categories.forEach(c => { if (!unique.has(c.name)) unique.set(c.name, c); });
        setCategories(Array.from(unique.values()));
      })
      .catch(err => console.error('Error loading categories:', err));
  }, []);

  const openAddModal = () => {
    setEditingBudgetId(null);
    setFormCategoryId('');
    setFormLimit('');
    setFormRecurring(false);
    setShowModal(true);
  };

  const openEditModal = (budget: BudgetStatusResponse) => {
    setEditingBudgetId(budget.budgetId);
    setFormCategoryId(budget.category?.id?.toString() || '');
    setFormLimit(budget.budgetLimit.toString());
    setFormRecurring(false);
    setShowModal(true);
  };

  const handleSave = async () => {
    const limitAmount = parseFloat(formLimit);
    if (isNaN(limitAmount) || limitAmount <= 0) return;

    setSaving(true);
    try {
      if (editingBudgetId) {
        await apiService.updateBudget(editingBudgetId, { limitAmount });
      } else {
        await apiService.createBudget({
          categoryId: formCategoryId ? parseInt(formCategoryId) : null,
          limitAmount,
          month: formRecurring ? undefined : selectedMonth,
        });
      }
      setShowModal(false);
      loadBudgets();
    } catch (err) {
      console.error('Error saving budget:', err);
      setError(t('budgets.errorLoading'));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm(t('budgets.confirmDelete'))) return;
    try {
      await apiService.deleteBudget(id);
      loadBudgets();
    } catch (err) {
      console.error('Error deleting budget:', err);
      setError(t('budgets.errorLoading'));
    }
  };

  const getUtilizationClass = (percent: number) => {
    if (percent >= 100) return 'utilization-danger';
    if (percent >= 80) return 'utilization-warning';
    return 'utilization-safe';
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'UNDER_BUDGET': return t('budgets.statusUnder');
      case 'NEAR_LIMIT': return t('budgets.statusNear');
      case 'OVER_BUDGET': return t('budgets.statusOver');
      default: return status;
    }
  };

  const getStatusClass = (status: string) => {
    switch (status) {
      case 'UNDER_BUDGET': return 'budget-status-under';
      case 'NEAR_LIMIT': return 'budget-status-near';
      case 'OVER_BUDGET': return 'budget-status-over';
      default: return '';
    }
  };

  return (
    <div className="budgets-container">
      <div className="budgets-header">
        <h1 className="budgets-title">{t('budgets.title')}</h1>
        <div className="budgets-header-actions">
          <div className="budgets-month-selector">
            <Calendar size={18} />
            <input
              type="month"
              value={selectedMonth}
              onChange={(e) => setSelectedMonth(e.target.value)}
              className="month-input"
            />
          </div>
          <button className="budgets-add-btn" onClick={openAddModal}>
            <Plus size={18} />
            {t('budgets.addBudget')}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading-container">
          <div className="spinner" />
          <p>{t('common.loading')}</p>
        </div>
      ) : error ? (
        <div className="loading-container">
          <p className="error-message">{error}</p>
          <button onClick={loadBudgets} className="retry-button">{t('expenseReport.retry')}</button>
        </div>
      ) : budgets.length === 0 ? (
        <div className="budgets-empty">
          <p>{t('budgets.noBudgets')}</p>
          <button className="budgets-add-btn" onClick={openAddModal}>
            <Plus size={18} />
            {t('budgets.addFirst')}
          </button>
        </div>
      ) : (
        <div className="budgets-table-wrapper">
          <table className="budgets-table">
            <thead>
              <tr>
                <th>{t('budgets.categoryColumn')}</th>
                <th>{t('budgets.limitColumn')}</th>
                <th>{t('budgets.spentColumn')}</th>
                <th>{t('budgets.remainingColumn')}</th>
                <th>{t('budgets.utilizationColumn')}</th>
                <th>{t('common.status')}</th>
                <th>{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody>
              {budgets.map((b) => (
                <tr key={b.budgetId}>
                  <td>
                    <div className="budget-category-cell">
                      {b.category && (
                        <span
                          className="budget-category-dot"
                          style={{ backgroundColor: b.category.color || '#a3a3a3' }}
                        />
                      )}
                      <span>{b.category?.name || t('budgets.general')}</span>
                    </div>
                  </td>
                  <td className="budget-amount">{formatCurrency(b.budgetLimit)}</td>
                  <td className="budget-amount">{formatCurrency(b.actualSpent)}</td>
                  <td className={`budget-amount ${b.remaining < 0 ? 'budget-negative' : ''}`}>
                    {formatCurrency(b.remaining)}
                  </td>
                  <td>
                    <div className="budget-utilization">
                      <div className="budget-progress-track">
                        <div
                          className={`budget-progress-fill ${getUtilizationClass(b.utilizationPercent)}`}
                          style={{ width: `${Math.min(b.utilizationPercent, 100)}%` }}
                        />
                      </div>
                      <span className="budget-progress-label">{b.utilizationPercent.toFixed(0)}%</span>
                    </div>
                  </td>
                  <td>
                    <span className={`budget-status-badge ${getStatusClass(b.status)}`}>
                      {getStatusLabel(b.status)}
                    </span>
                  </td>
                  <td>
                    <div className="budget-actions">
                      <button
                        className="budget-action-btn budget-edit-btn"
                        onClick={() => openEditModal(b)}
                        title={t('common.edit')}
                      >
                        <Pencil size={16} />
                      </button>
                      <button
                        className="budget-action-btn budget-delete-btn"
                        onClick={() => handleDelete(b.budgetId)}
                        title={t('common.delete')}
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="budget-modal-overlay" onClick={() => setShowModal(false)}>
          <div className="budget-modal" onClick={(e) => e.stopPropagation()}>
            <div className="budget-modal-header">
              <h2>{editingBudgetId ? t('budgets.editBudget') : t('budgets.addBudget')}</h2>
              <button className="budget-modal-close" onClick={() => setShowModal(false)}>
                <X size={20} />
              </button>
            </div>
            <div className="budget-modal-body">
              {!editingBudgetId && (
                <div className="budget-form-group">
                  <label>{t('budgets.categoryColumn')}</label>
                  <select
                    value={formCategoryId}
                    onChange={(e) => setFormCategoryId(e.target.value)}
                    className="budget-form-select"
                  >
                    <option value="">{t('budgets.general')}</option>
                    {categories.map(c => (
                      <option key={c.id} value={c.id?.toString() || ''}>{c.name}</option>
                    ))}
                  </select>
                </div>
              )}
              <div className="budget-form-group">
                <label>{t('budgets.limitColumn')}</label>
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={formLimit}
                  onChange={(e) => setFormLimit(e.target.value)}
                  className="budget-form-input"
                  placeholder="0.00"
                />
              </div>
              {!editingBudgetId && (
                <div className="budget-form-group budget-form-toggle">
                  <label className="budget-recurring-label">
                    <input
                      type="checkbox"
                      checked={formRecurring}
                      onChange={(e) => setFormRecurring(e.target.checked)}
                    />
                    <span>{t('budgets.recurring')}</span>
                  </label>
                  <span className="budget-recurring-hint">{t('budgets.recurringHint')}</span>
                </div>
              )}
            </div>
            <div className="budget-modal-footer">
              <button className="budget-cancel-btn" onClick={() => setShowModal(false)}>
                {t('common.cancel')}
              </button>
              <button
                className="budget-save-btn"
                onClick={handleSave}
                disabled={saving || !formLimit || parseFloat(formLimit) <= 0}
              >
                {saving ? t('common.loading') : t('common.save')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Budgets;
