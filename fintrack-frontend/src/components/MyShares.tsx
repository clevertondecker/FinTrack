import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { MySharesResponse, MyShareResponse } from '../types/itemShare';
import apiService from '../services/api';
import { getStatusColor, getStatusText, formatCurrency, formatDate } from '../utils/invoiceUtils';
import MarkShareAsPaidModal from './MarkShareAsPaidModal';
import './MyShares.css';

interface InvoiceGroup {
  invoiceId: number;
  dueDate: string;
  shares: MyShareResponse[];
}

interface GroupedShares {
  [key: string]: {
    creditCardName: string;
    creditCardOwnerName: string;
    invoices: InvoiceGroup[];
  };
}

const MyShares: React.FC = () => {
  const { t } = useTranslation();
  const [shares, setShares] = useState<MySharesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [selectedShare, setSelectedShare] = useState<MyShareResponse | null>(null);
  const [statusFilter, setStatusFilter] = useState<'all' | 'paid' | 'unpaid'>('all');
  const [monthFilter, setMonthFilter] = useState<string | 'all'>('all');
  const [showFilters, setShowFilters] = useState(false);
  const [collapsedCards, setCollapsedCards] = useState<Set<string>>(new Set());
  const [selectedShareIds, setSelectedShareIds] = useState<number[]>([]);
  const [bulkPaymentMethod, setBulkPaymentMethod] = useState('PIX');
  const [bulkPaymentDate, setBulkPaymentDate] = useState(new Date().toISOString().slice(0, 16));

  useEffect(() => {
    loadMyShares();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadMyShares = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiService.getMyShares();
      setShares(response);
    } catch (err: any) {
      console.error('Error loading shares:', err);
      setError(err.response?.data?.message || t('shares.errorLoadingShares'));
    } finally {
      setLoading(false);
    }
  };

  const formatPercentage = (percentage: number) => {
    return `${(percentage * 100).toFixed(1)}%`;
  };

  const getAvailableMonths = () => {
    if (!shares) return [];
    const unique = new Map<string, string>();
    shares.shares.forEach(share => {
      const ym = share.invoiceDueDate.substring(0, 7); // "YYYY-MM"
      if (!unique.has(ym)) {
        const [year, monthIdx] = ym.split('-');
        const monthName = t(`shares.monthNames.${parseInt(monthIdx, 10)}`);
        unique.set(ym, `${monthName}/${year}`);
      }
    });
    return Array.from(unique.entries())
      .map(([month, label]) => ({ month, label }))
      .sort((a, b) => b.month.localeCompare(a.month));
  };

  const handleMonthFilterChange = (month: string | 'all') => {
    setMonthFilter(month);
  };

  const filterByMonth = (sharesList: MyShareResponse[]) => {
    if (monthFilter === 'all') return sharesList;
    return sharesList.filter(s => s.invoiceDueDate.substring(0, 7) === monthFilter);
  };

  const filterShares = (shares: MyShareResponse[]) => {
    let filtered = filterByMonth(shares);
    if (statusFilter === 'paid') filtered = filtered.filter(share => share.isPaid);
    if (statusFilter === 'unpaid') filtered = filtered.filter(share => !share.isPaid);
    return filtered;
  };

  const getFilteredGroupedShares = () => {
    if (!shares) return {};

    const filteredShares = filterShares(shares.shares);
    const temp: { [cardKey: string]: { creditCardName: string; creditCardOwnerName: string; invoiceMap: { [invoiceId: number]: InvoiceGroup } } } = {};

    filteredShares.forEach(share => {
      const cardKey = `${share.creditCardName}-${share.creditCardOwnerName}`;
      if (!temp[cardKey]) {
        temp[cardKey] = {
          creditCardName: share.creditCardName,
          creditCardOwnerName: share.creditCardOwnerName,
          invoiceMap: {}
        };
      }
      if (!temp[cardKey].invoiceMap[share.invoiceId]) {
        temp[cardKey].invoiceMap[share.invoiceId] = {
          invoiceId: share.invoiceId,
          dueDate: share.invoiceDueDate,
          shares: []
        };
      }
      temp[cardKey].invoiceMap[share.invoiceId].shares.push(share);
    });

    const grouped: GroupedShares = {};
    Object.entries(temp).forEach(([cardKey, card]) => {
      const invoices = Object.values(card.invoiceMap).sort(
        (a, b) => new Date(b.dueDate).getTime() - new Date(a.dueDate).getTime()
      );
      grouped[cardKey] = {
        creditCardName: card.creditCardName,
        creditCardOwnerName: card.creditCardOwnerName,
        invoices
      };
    });

    return grouped;
  };

  const getFilteredShareCount = () => {
    if (!shares) return 0;
    return filterShares(shares.shares).length;
  };

  const getFilteredTotalAmount = () => {
    if (!shares) return 0;
    return filterShares(shares.shares).reduce((sum, share) => sum + share.myAmount, 0);
  };

  const getTotalAmountAll = () => {
    if (!shares) return 0;
    return filterByMonth(shares.shares).reduce((sum, share) => sum + share.myAmount, 0);
  };

  const getStatusStats = () => {
    if (!shares) return { total: 0, paid: 0, unpaid: 0 };

    const monthFiltered = filterByMonth(shares.shares);
    const total = monthFiltered.length;
    const paid = monthFiltered.filter(share => share.isPaid).length;
    const unpaid = total - paid;

    return { total, paid, unpaid };
  };

  const getGroupTotalShares = (group: GroupedShares[string]) => {
    return group.invoices.reduce((sum, inv) => sum + inv.shares.length, 0);
  };

  const getGroupTotalAmount = (group: GroupedShares[string]) => {
    return group.invoices.reduce(
      (sum, inv) => sum + inv.shares.reduce((s, share) => s + share.myAmount, 0), 0
    );
  };

  const handleMarkAsPaid = (share: MyShareResponse) => {
    setSelectedShare(share);
    setShowPaymentModal(true);
  };

  const handleMarkAsUnpaid = async (share: MyShareResponse) => {
    try {
      await apiService.markShareAsUnpaid(share.shareId);
      await loadMyShares();
    } catch (err: any) {
      console.error('Error marking share as unpaid:', err);
      setError(err.response?.data?.message || t('shares.errorMarkUnpaid'));
    }
  };

  const handlePaymentMarked = () => {
    loadMyShares();
  };

  const getPaymentMethodDisplayName = (method: string | null) => {
    if (!method) return '';
    return t(`shares.paymentMethod.${method}`, method);
  };

  const toggleSelectShare = (id: number) => {
    setSelectedShareIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]);
  };

  const handleClearSelection = () => setSelectedShareIds([]);

  const handleSelectAllFiltered = () => {
    if (!shares) return;
    const filtered = filterShares(shares.shares);
    const allIds = filtered.map(s => s.shareId);
    const allSelected = allIds.every(id => selectedShareIds.includes(id));
    if (allSelected) {
      setSelectedShareIds(prev => prev.filter(id => !allIds.includes(id)));
    } else {
      setSelectedShareIds(prev => Array.from(new Set([...prev, ...allIds])));
    }
  };

  const toggleCardCollapse = (cardKey: string) => {
    setCollapsedCards(prev => {
      const next = new Set(prev);
      if (next.has(cardKey)) next.delete(cardKey);
      else next.add(cardKey);
      return next;
    });
  };

  const handleBulkMarkAsPaid = async () => {
    if (selectedShareIds.length === 0) return;
    if (!shares) return;
    const idsToSend = selectedShareIds.filter(id => {
      const s = shares.shares.find(sh => sh.shareId === id);
      return s && !s.isPaid;
    });
    if (idsToSend.length === 0) {
      setError(t('shares.noPendingItems'));
      return;
    }
    try {
      await apiService.markSharesAsPaidBulk(idsToSend, {
        paymentMethod: bulkPaymentMethod,
        paidAt: new Date(bulkPaymentDate).toISOString()
      });
      await loadMyShares();
      setSelectedShareIds([]);
    } catch (err: any) {
      console.error('Error marking shares as paid in bulk:', err);
      setError(err.response?.data?.message || t('shares.errorBulkPaid'));
    }
  };

  const getFilteredShareIds = () => {
    if (!shares) return [];
    return filterShares(shares.shares).map(s => s.shareId);
  };

  const isAllFilteredSelected = () => {
    const ids = getFilteredShareIds();
    return ids.length > 0 && ids.every(id => selectedShareIds.includes(id));
  };

  if (loading) {
    return (
      <div className="my-shares-container">
        <div className="loading">{t('shares.loadingShares')}</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="my-shares-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={loadMyShares} className="retry-btn">
            {t('common.retry')}
          </button>
        </div>
      </div>
    );
  }

  if (!shares || shares.shareCount === 0) {
    return (
      <div className="my-shares-container">
        <div className="empty-state">
          <div className="empty-icon">📋</div>
          <h3>{t('shares.noShares')}</h3>
          <p>{t('shares.noSharesDescription')}</p>
        </div>
      </div>
    );
  }

  const filteredCount = getFilteredShareCount();

  return (
    <div className="my-shares-container">
      <div className="my-shares-header">
        <h2>{t('shares.title')}</h2>
        <div className="header-actions">
          <button
            className="filter-button"
            onClick={() => setShowFilters(!showFilters)}
          >
            {showFilters ? t('shares.hideFilters') : t('shares.showFilters')}
          </button>
        </div>
        <div className="shares-summary">
          <span className="total-shares">
            {t('shares.divisionCount', { count: filteredCount })}
          </span>
          <span className="total-amount">
            {statusFilter === 'all'
              ? `${t('shares.totalLabel')} ${formatCurrency(getFilteredTotalAmount())}`
              : `${t('shares.totalFiltered')} ${formatCurrency(getFilteredTotalAmount())}`}
          </span>
          {statusFilter !== 'all' && (
            <span className="total-amount-all" title={t('shares.totalGeneral')}>
              {t('shares.totalGeneral')} {formatCurrency(getTotalAmountAll())}
            </span>
          )}
        </div>
      </div>

      {selectedShareIds.length > 0 && (
        <div className="bulk-actions">
          <div className="bulk-actions-top">
            <span className="bulk-count">{t('shares.selectedCount', { count: selectedShareIds.length })}</span>
            <button className="bulk-clear-btn" onClick={handleClearSelection}>
              {t('shares.clearSelection')}
            </button>
          </div>
          <div className="bulk-actions-controls">
            <div className="bulk-field">
              <label>{t('shares.methodLabel')}</label>
              <select className="filter-select" value={bulkPaymentMethod} onChange={(e) => setBulkPaymentMethod(e.target.value)}>
                <option value="PIX">{t('shares.paymentMethodShort.PIX')}</option>
                <option value="BANK_TRANSFER">{t('shares.paymentMethodShort.BANK_TRANSFER')}</option>
                <option value="CASH">{t('shares.paymentMethodShort.CASH')}</option>
                <option value="CREDIT_CARD">{t('shares.paymentMethodShort.CREDIT_CARD')}</option>
                <option value="DEBIT_CARD">{t('shares.paymentMethodShort.DEBIT_CARD')}</option>
                <option value="OTHER">{t('shares.paymentMethodShort.OTHER')}</option>
              </select>
            </div>
            <div className="bulk-field">
              <label>{t('shares.dateLabel')}</label>
              <input className="bulk-date-input" type="datetime-local" value={bulkPaymentDate} onChange={(e) => setBulkPaymentDate(e.target.value)} />
            </div>
            <button className="mark-paid-button" onClick={handleBulkMarkAsPaid}>
              {t('shares.markAsPaid')}
            </button>
          </div>
        </div>
      )}

      {/* Filters Section */}
      {showFilters && (
        <div className="filters-section">
          <div className="filters-row">
            {getAvailableMonths().length > 1 && (
              <div className="filter-inline">
                <label>{t('shares.monthLabel')}</label>
                <select
                  className="filter-select"
                  value={monthFilter}
                  onChange={(e) => handleMonthFilterChange(e.target.value)}
                >
                  <option value="all">{t('shares.allMonths')}</option>
                  {getAvailableMonths().map(m => (
                    <option key={m.month} value={m.month}>{m.label}</option>
                  ))}
                </select>
              </div>
            )}
            <div className="filter-inline">
              <label>{t('shares.statusFilterLabel')}</label>
              <div className="status-toggle">
                <button
                  className={`toggle-option ${statusFilter === 'all' ? 'active' : ''}`}
                  onClick={() => setStatusFilter('all')}
                >
                  {t('shares.filterAll')}
                </button>
                <button
                  className={`toggle-option ${statusFilter === 'unpaid' ? 'active unpaid' : ''}`}
                  onClick={() => setStatusFilter('unpaid')}
                >
                  {t('shares.filterPending')}
                </button>
                <button
                  className={`toggle-option ${statusFilter === 'paid' ? 'active paid' : ''}`}
                  onClick={() => setStatusFilter('paid')}
                >
                  {t('shares.filterPaid')}
                </button>
              </div>
            </div>
            <div className="filter-inline">
              <button
                className={`toggle-option select-all-btn ${isAllFilteredSelected() ? 'active' : ''}`}
                onClick={handleSelectAllFiltered}
              >
                {isAllFilteredSelected() ? t('shares.deselectAll') : t('shares.selectAll')}
              </button>
            </div>
          </div>
          <div className="filter-summary">
            <span className="summary-badge">{t('shares.divisionCount', { count: getStatusStats().total })}</span>
            <span className="summary-badge unpaid">{t('shares.pendingCount', { count: getStatusStats().unpaid })}</span>
            <span className="summary-badge paid">{t('shares.paidCount', { count: getStatusStats().paid })}</span>
          </div>
        </div>
      )}

      <div className="shares-groups">
        {Object.entries(getFilteredGroupedShares()).length === 0 ? (
          <div className="no-results">
            <div className="no-results-icon">🔍</div>
            <h3>{t('shares.noShares')}</h3>
            <p>
              {statusFilter === 'all' && t('shares.noSharesYet')}
              {statusFilter === 'paid' && t('shares.noSharesPaid')}
              {statusFilter === 'unpaid' && t('shares.noSharesPending')}
            </p>
            {(statusFilter !== 'all' || monthFilter !== 'all') && (
              <button
                className="clear-filter-button"
                onClick={() => { setStatusFilter('all'); setMonthFilter('all'); }}
              >
                {t('shares.clearFilters')}
              </button>
            )}
          </div>
        ) : (
          Object.entries(getFilteredGroupedShares()).map(([key, group]) => {
            const totalShares = getGroupTotalShares(group);
            const cardTitle = t('shares.cardOf', { owner: group.creditCardOwnerName, count: totalShares });

            const isCollapsed = collapsedCards.has(key);

            return (
              <div key={key} className="credit-card-group">
                <div className="group-header" onClick={() => toggleCardCollapse(key)} style={{ cursor: 'pointer' }}>
                  <div className="card-info">
                    <h3 className="card-name">
                      <span className="collapse-icon">{isCollapsed ? '\u25B6' : '\u25BC'}</span>
                      {group.creditCardName}
                    </h3>
                    <p className="card-owner">{cardTitle}</p>
                  </div>
                  <div className="group-summary">
                    <span className="group-count">
                      {t('shares.itemCount', { count: totalShares })}
                    </span>
                    <span className="group-total">
                      {formatCurrency(getGroupTotalAmount(group))}
                    </span>
                  </div>
                </div>

                {!isCollapsed && group.invoices.map((invoice) => {
                  const invoiceTotal = invoice.shares.reduce((sum, s) => sum + s.myAmount, 0);
                  return (
                    <div key={invoice.invoiceId} className="invoice-subgroup">
                      <div className="invoice-subgroup-header">
                        <div className="invoice-subgroup-info">
                          <span className="invoice-subgroup-label">
                            {t('shares.invoiceDue', { date: formatDate(invoice.dueDate) })}
                          </span>
                          <span className="invoice-subgroup-count">
                            {t('shares.divisionCount', { count: invoice.shares.length })}
                          </span>
                        </div>
                        <span className="invoice-subgroup-total">
                          {formatCurrency(invoiceTotal)}
                        </span>
                      </div>

                      <div className="shares-list">
                        {invoice.shares.map((share) => (
                          <div key={share.shareId} className="share-item">
                            <div className="share-select" style={{ marginRight: 8 }}>
                              <input
                                type="checkbox"
                                checked={selectedShareIds.includes(share.shareId)}
                                onChange={() => toggleSelectShare(share.shareId)}
                              />
                            </div>
                            <div className="share-main-info">
                              <div className="item-description">
                                <h4>{share.itemDescription}</h4>
                                {share.totalInstallments > 1 && (
                                  <span className="installment-info">
                                    {t('shares.installmentOf', { current: share.installments, total: share.totalInstallments })}
                                    {share.remainingInstallments > 0 && (
                                      <span className="remaining-installments">
                                        {t('shares.remainingInstallments', { count: share.remainingInstallments })}
                                      </span>
                                    )}
                                  </span>
                                )}
                                <div className="item-meta">
                                  <span className={`invoice-status ${getStatusColor(share.invoiceStatus)}`}>
                                    {getStatusText(share.invoiceStatus, t)}
                                  </span>
                                </div>
                              </div>

                              <div className="share-amounts">
                                <div className="amount-breakdown">
                                  <div className="total-item">
                                    <span className="label">{t('shares.totalAmount')}</span>
                                    <span className="value">{formatCurrency(share.itemAmount)}</span>
                                  </div>
                                  {share.totalInstallments > 1 && (
                                    <>
                                      <div className="total-item-amount">
                                        <span className="label">{t('shares.totalItemAmount')}</span>
                                        <span className="value">{formatCurrency(share.totalItemAmount)}</span>
                                      </div>
                                      {share.remainingInstallments > 0 && (
                                        <div className="remaining-item-amount">
                                          <span className="label">{t('shares.remainingItemAmount')}</span>
                                          <span className="value remaining">{formatCurrency(share.remainingItemAmount)}</span>
                                        </div>
                                      )}
                                    </>
                                  )}
                                  <div className="my-share">
                                    <span className="label">{t('shares.myShare')}</span>
                                    <span className="value highlight">
                                      {formatCurrency(share.myAmount)} ({formatPercentage(share.myPercentage)})
                                    </span>
                                  </div>

                                  {share.isPaid && (
                                    <div className="payment-info">
                                      <span className="label">{t('common.status')}:</span>
                                      <span className="value paid">{t('shares.paidStatus')}</span>
                                      <div className="payment-details">
                                        <span className="payment-method">
                                          {getPaymentMethodDisplayName(share.paymentMethod)}
                                        </span>
                                        <span className="payment-date">
                                          {formatDate(share.paidAt || '')}
                                        </span>
                                      </div>
                                    </div>
                                  )}

                                  {!share.isPaid && (
                                    <div className="payment-info">
                                      <span className="label">{t('common.status')}:</span>
                                      <span className="value unpaid">{t('shares.pendingStatus')}</span>
                                    </div>
                                  )}
                                </div>

                                {share.isResponsible && (
                                  <div className="responsible-badge">
                                    <span>{t('shares.responsibleForPayment')}</span>
                                  </div>
                                )}
                              </div>
                            </div>

                            <div className="share-footer">
                              <span className="share-date">
                                {t('shares.sharedOn', { date: formatDate(share.shareCreatedAt) })}
                              </span>
                            </div>

                            <div className="share-actions">
                              {!share.isPaid ? (
                                <button
                                  onClick={() => handleMarkAsPaid(share)}
                                  className="mark-paid-button"
                                >
                                  {t('shares.markAsPaid')}
                                </button>
                              ) : (
                                <button
                                  onClick={() => handleMarkAsUnpaid(share)}
                                  className="mark-unpaid-button"
                                >
                                  {t('shares.markAsUnpaid')}
                                </button>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </div>
            );
          })
        )}
      </div>

      {/* Payment Modal */}
      {showPaymentModal && selectedShare && (
        <MarkShareAsPaidModal
          isOpen={showPaymentModal}
          onClose={() => {
            setShowPaymentModal(false);
            setSelectedShare(null);
          }}
          shareId={selectedShare.shareId}
          shareDescription={selectedShare.itemDescription}
          shareAmount={selectedShare.myAmount}
          onPaymentMarked={handlePaymentMarked}
        />
      )}
    </div>
  );
};

export default MyShares;
