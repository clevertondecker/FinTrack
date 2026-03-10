import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  RefreshCw, Plus, AlertTriangle, CheckCircle, XCircle, Clock,
  TrendingDown, DollarSign, Sparkles, Trash2, Edit3, X, Check
} from 'lucide-react';
import apiService from '../services/api';
import {
  SubscriptionResponse,
  SubscriptionSuggestion,
  BillingCycle,
  CreateSubscriptionRequest,
  UpdateSubscriptionRequest
} from '../types/subscription';
import { CreditCard } from '../types/creditCard';
import { Category } from '../types/invoice';
import './Subscriptions.css';

const BILLING_CYCLE_OPTIONS: { value: BillingCycle; labelKey: string }[] = [
  { value: 'MONTHLY', labelKey: 'subscriptions.cycle.monthly' },
  { value: 'QUARTERLY', labelKey: 'subscriptions.cycle.quarterly' },
  { value: 'SEMI_ANNUAL', labelKey: 'subscriptions.cycle.semiAnnual' },
  { value: 'ANNUAL', labelKey: 'subscriptions.cycle.annual' },
];

const Subscriptions: React.FC = () => {
  const { t, i18n } = useTranslation();

  const [subscriptions, setSubscriptions] = useState<SubscriptionResponse[]>([]);
  const [suggestions, setSuggestions] = useState<SubscriptionSuggestion[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const [formName, setFormName] = useState('');
  const [formMerchantKey, setFormMerchantKey] = useState('');
  const [formAmount, setFormAmount] = useState('');
  const [formCycle, setFormCycle] = useState<BillingCycle>('MONTHLY');
  const [formCategoryId, setFormCategoryId] = useState<number | undefined>();
  const [formCardId, setFormCardId] = useState<number | undefined>();

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [subs, sugs, cardsResp, catsResp] = await Promise.all([
        apiService.getSubscriptions(),
        apiService.getSubscriptionSuggestions(),
        apiService.getCreditCards(),
        apiService.getCategories(),
      ]);
      setSubscriptions(subs);
      setSuggestions(sugs);
      setCreditCards(cardsResp.creditCards || []);
      setCategories(catsResp.categories || []);
    } catch {
      setError(t('subscriptions.loadError'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => { loadData(); }, [loadData]);

  const summary = useMemo(() => {
    const active = subscriptions.filter(s => s.status === 'ACTIVE');
    const totalMonthly = active.reduce((sum, s) => sum + s.expectedAmount, 0);
    const detected = active.filter(s => s.monthStatus === 'DETECTED').length;
    const missed = active.filter(s => s.monthStatus === 'MISSED').length;
    const priceChanged = active.filter(s => s.monthStatus === 'PRICE_CHANGED').length;
    return { totalMonthly, activeCount: active.length, detected, missed, priceChanged };
  }, [subscriptions]);

  const formatCurrency = useCallback((value: number) => {
    return new Intl.NumberFormat(i18n.language === 'pt' ? 'pt-BR' : 'en-US', {
      style: 'currency',
      currency: i18n.language === 'pt' ? 'BRL' : 'USD',
    }).format(value);
  }, [i18n.language]);

  const resetForm = useCallback(() => {
    setFormName('');
    setFormMerchantKey('');
    setFormAmount('');
    setFormCycle('MONTHLY');
    setFormCategoryId(undefined);
    setFormCardId(undefined);
    setShowCreateForm(false);
    setEditingId(null);
  }, []);

  const handleCreate = useCallback(async () => {
    const amount = parseFloat(formAmount);
    if (!formName || !formMerchantKey || isNaN(amount) || amount <= 0) {
      return;
    }
    const data: CreateSubscriptionRequest = {
      name: formName,
      merchantKey: formMerchantKey,
      expectedAmount: amount,
      billingCycle: formCycle,
      categoryId: formCategoryId,
      creditCardId: formCardId,
    };
    try {
      await apiService.createSubscription(data);
      resetForm();
      loadData();
    } catch {
      setError(t('subscriptions.createError'));
    }
  }, [formName, formMerchantKey, formAmount, formCycle, formCategoryId, formCardId, resetForm, loadData, t]);

  const handleUpdate = useCallback(async () => {
    if (!editingId) { return; }
    const amount = parseFloat(formAmount);
    if (!formName || isNaN(amount) || amount <= 0) { return; }
    const data: UpdateSubscriptionRequest = {
      name: formName,
      expectedAmount: amount,
      billingCycle: formCycle,
      categoryId: formCategoryId,
      creditCardId: formCardId,
    };
    try {
      await apiService.updateSubscription(editingId, data);
      resetForm();
      loadData();
    } catch {
      setError(t('subscriptions.updateError'));
    }
  }, [editingId, formName, formAmount, formCycle, formCategoryId, formCardId, resetForm, loadData, t]);

  const handleCancel = useCallback(async (id: number) => {
    try {
      await apiService.cancelSubscription(id);
      loadData();
    } catch {
      setError(t('subscriptions.cancelError'));
    }
  }, [loadData, t]);

  const handleConfirmSuggestion = useCallback(async (merchantKey: string) => {
    try {
      await apiService.confirmSubscriptionSuggestion(merchantKey);
      loadData();
    } catch {
      setError(t('subscriptions.confirmError'));
    }
  }, [loadData, t]);

  const handleDismissSuggestion = useCallback(async (merchantKey: string) => {
    try {
      await apiService.dismissSubscriptionSuggestion(merchantKey);
      setSuggestions(prev => prev.filter(s => s.merchantKey !== merchantKey));
    } catch {
      setError(t('subscriptions.dismissError'));
    }
  }, [t]);

  const startEdit = useCallback((sub: SubscriptionResponse) => {
    setEditingId(sub.id);
    setFormName(sub.name);
    setFormMerchantKey(sub.merchantKey);
    setFormAmount(sub.expectedAmount.toString());
    setFormCycle(sub.billingCycle);
    setFormCategoryId(sub.category?.id ?? undefined);
    setFormCardId(sub.creditCardId ?? undefined);
    setShowCreateForm(true);
  }, []);

  const getStatusIcon = useCallback((monthStatus: string) => {
    switch (monthStatus) {
      case 'DETECTED': return <CheckCircle size={16} className="status-icon detected" />;
      case 'MISSED': return <XCircle size={16} className="status-icon missed" />;
      case 'PRICE_CHANGED': return <AlertTriangle size={16} className="status-icon price-changed" />;
      case 'PAUSED': return <Clock size={16} className="status-icon paused" />;
      default: return <Clock size={16} className="status-icon" />;
    }
  }, []);

  const getStatusLabel = useCallback((monthStatus: string) => {
    switch (monthStatus) {
      case 'DETECTED': return t('subscriptions.status.detected');
      case 'MISSED': return t('subscriptions.status.missed');
      case 'PRICE_CHANGED': return t('subscriptions.status.priceChanged');
      case 'PAUSED': return t('subscriptions.status.paused');
      case 'CANCELLED': return t('subscriptions.status.cancelled');
      default: return t('subscriptions.status.active');
    }
  }, [t]);

  const getCycleLabel = useCallback((cycle: BillingCycle) => {
    const option = BILLING_CYCLE_OPTIONS.find(o => o.value === cycle);
    return option ? t(option.labelKey) : cycle;
  }, [t]);

  if (loading) {
    return (
      <div className="subscriptions-loading">
        <div className="spinner" />
        <p>{t('common.loading')}</p>
      </div>
    );
  }

  return (
    <div className="subscriptions-container">
      {error && (
        <div className="subscriptions-error">
          <AlertTriangle size={16} />
          <span>{error}</span>
          <button onClick={() => setError(null)}>
            <X size={14} />
          </button>
        </div>
      )}

      {/* Summary Cards */}
      <div className="subscriptions-summary">
        <div className="summary-card total">
          <DollarSign size={20} />
          <div>
            <span className="summary-value">{formatCurrency(summary.totalMonthly)}</span>
            <span className="summary-label">{t('subscriptions.summary.monthlyTotal')}</span>
          </div>
        </div>
        <div className="summary-card active">
          <CheckCircle size={20} />
          <div>
            <span className="summary-value">{summary.activeCount}</span>
            <span className="summary-label">{t('subscriptions.summary.active')}</span>
          </div>
        </div>
        <div className="summary-card detected">
          <Check size={20} />
          <div>
            <span className="summary-value">{summary.detected}</span>
            <span className="summary-label">{t('subscriptions.summary.detected')}</span>
          </div>
        </div>
        {summary.missed > 0 && (
          <div className="summary-card missed">
            <XCircle size={20} />
            <div>
              <span className="summary-value">{summary.missed}</span>
              <span className="summary-label">{t('subscriptions.summary.missed')}</span>
            </div>
          </div>
        )}
        {summary.priceChanged > 0 && (
          <div className="summary-card price-changed">
            <TrendingDown size={20} />
            <div>
              <span className="summary-value">{summary.priceChanged}</span>
              <span className="summary-label">{t('subscriptions.summary.priceChanged')}</span>
            </div>
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="subscriptions-actions">
        <button className="btn-create" onClick={() => { resetForm(); setShowCreateForm(true); }}>
          <Plus size={16} />
          {t('subscriptions.addManual')}
        </button>
        <button className="btn-refresh" onClick={loadData}>
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Suggestions */}
      {suggestions.length > 0 && (
        <div className="suggestions-section">
          <div className="section-header">
            <Sparkles size={18} className="sparkle-icon" />
            <h3>{t('subscriptions.suggestions.title')}</h3>
            <span className="badge">{suggestions.length}</span>
          </div>
          <p className="section-description">{t('subscriptions.suggestions.description')}</p>
          <div className="suggestions-grid">
            {suggestions.map((sug) => (
              <div key={sug.merchantKey} className="suggestion-card">
                <div className="suggestion-header">
                  <span className="suggestion-name">{sug.displayName}</span>
                  {sug.categoryName && (
                    <span
                      className="category-badge"
                      style={{ backgroundColor: sug.categoryColor ?? '#666' }}
                    >
                      {sug.categoryName}
                    </span>
                  )}
                </div>
                <div className="suggestion-details">
                  <span className="suggestion-amount">{formatCurrency(sug.averageAmount)}</span>
                  <span className="suggestion-meta">
                    {t('subscriptions.suggestions.occurrences', { count: sug.occurrences })}
                  </span>
                  {sug.cardName && (
                    <span className="suggestion-card-name">{sug.cardName}</span>
                  )}
                </div>
                <div className="suggestion-actions">
                  <button
                    className="btn-confirm-suggestion"
                    onClick={() => handleConfirmSuggestion(sug.merchantKey)}
                  >
                    <Check size={14} />
                    {t('subscriptions.suggestions.confirm')}
                  </button>
                  <button
                    className="btn-dismiss-suggestion"
                    onClick={() => handleDismissSuggestion(sug.merchantKey)}
                  >
                    <X size={14} />
                    {t('subscriptions.suggestions.dismiss')}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Create/Edit Form */}
      {showCreateForm && (
        <div className="subscription-form-overlay">
          <div className="subscription-form">
            <div className="form-header">
              <h3>{editingId ? t('subscriptions.form.editTitle') : t('subscriptions.form.createTitle')}</h3>
              <button className="btn-close" onClick={resetForm}>
                <X size={18} />
              </button>
            </div>
            <div className="form-body">
              <div className="form-row">
                <label>{t('subscriptions.form.name')}</label>
                <input
                  type="text"
                  value={formName}
                  onChange={e => setFormName(e.target.value)}
                  placeholder={t('subscriptions.form.namePlaceholder')}
                />
              </div>
              {!editingId && (
                <div className="form-row">
                  <label>{t('subscriptions.form.merchantKey')}</label>
                  <input
                    type="text"
                    value={formMerchantKey}
                    onChange={e => setFormMerchantKey(e.target.value)}
                    placeholder={t('subscriptions.form.merchantKeyPlaceholder')}
                  />
                </div>
              )}
              <div className="form-row">
                <label>{t('subscriptions.form.amount')}</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={formAmount}
                  onChange={e => setFormAmount(e.target.value)}
                  placeholder="0.00"
                />
              </div>
              <div className="form-row">
                <label>{t('subscriptions.form.cycle')}</label>
                <select value={formCycle} onChange={e => setFormCycle(e.target.value as BillingCycle)}>
                  {BILLING_CYCLE_OPTIONS.map(opt => (
                    <option key={opt.value} value={opt.value}>{t(opt.labelKey)}</option>
                  ))}
                </select>
              </div>
              <div className="form-row">
                <label>{t('subscriptions.form.category')}</label>
                <select
                  value={formCategoryId ?? ''}
                  onChange={e => setFormCategoryId(e.target.value ? Number(e.target.value) : undefined)}
                >
                  <option value="">{t('subscriptions.form.noCategory')}</option>
                  {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>
              <div className="form-row">
                <label>{t('subscriptions.form.creditCard')}</label>
                <select
                  value={formCardId ?? ''}
                  onChange={e => setFormCardId(e.target.value ? Number(e.target.value) : undefined)}
                >
                  <option value="">{t('subscriptions.form.noCard')}</option>
                  {creditCards.map(card => (
                    <option key={card.id} value={card.id}>{card.name}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="form-actions">
              <button className="btn-cancel" onClick={resetForm}>
                {t('common.cancel')}
              </button>
              <button className="btn-save" onClick={editingId ? handleUpdate : handleCreate}>
                {t('common.save')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Subscriptions List */}
      {subscriptions.length === 0 ? (
        <div className="subscriptions-empty">
          <RefreshCw size={40} />
          <p>{t('subscriptions.empty')}</p>
        </div>
      ) : (
        <div className="subscriptions-list">
          {subscriptions.map(sub => (
            <div key={sub.id} className={`subscription-item status-${sub.monthStatus.toLowerCase()}`}>
              <div className="subscription-left">
                {getStatusIcon(sub.monthStatus)}
                <div className="subscription-info">
                  <span className="subscription-name">{sub.name}</span>
                  <div className="subscription-meta">
                    <span className="subscription-cycle">{getCycleLabel(sub.billingCycle)}</span>
                    {sub.creditCardName && (
                      <span className="subscription-card">{sub.creditCardName}</span>
                    )}
                    {sub.category && (
                      <span
                        className="category-badge small"
                        style={{ backgroundColor: sub.category.color }}
                      >
                        {sub.category.name}
                      </span>
                    )}
                  </div>
                </div>
              </div>
              <div className="subscription-right">
                <div className="subscription-amounts">
                  <span className="expected-amount">{formatCurrency(sub.expectedAmount)}</span>
                  {sub.monthStatus === 'PRICE_CHANGED' && sub.lastDetectedAmount != null && (
                    <span className="detected-amount changed">
                      {t('subscriptions.detectedValue')}: {formatCurrency(sub.lastDetectedAmount)}
                    </span>
                  )}
                </div>
                <span className={`month-status-badge ${sub.monthStatus.toLowerCase()}`}>
                  {getStatusLabel(sub.monthStatus)}
                </span>
                <div className="subscription-actions">
                  <button className="btn-edit" onClick={() => startEdit(sub)} title={t('common.edit')}>
                    <Edit3 size={14} />
                  </button>
                  <button className="btn-delete" onClick={() => handleCancel(sub.id)} title={t('common.delete')}>
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Subscriptions;
