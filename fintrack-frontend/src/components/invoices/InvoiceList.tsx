import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Invoice, InvoiceFilters, InvoiceSummary, CreateInvoiceRequest, GroupedInvoices } from '../../types/invoice';
import { CreditCard } from '../../types/creditCard';
import { getStatusColor, getStatusText, getUrgencyText, formatCurrency, formatDate, formatMonthLabel } from '../../utils/invoiceUtils';

export interface InvoiceListProps {
  error: string | null;
  showCreateForm: boolean;
  setShowCreateForm: React.Dispatch<React.SetStateAction<boolean>>;
  formData: CreateInvoiceRequest;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  handleSubmit: (e: React.FormEvent) => Promise<void>;
  handleCancel: () => void;
  creditCards: CreditCard[];
  showFilters: boolean;
  setShowFilters: React.Dispatch<React.SetStateAction<boolean>>;
  filters: InvoiceFilters;
  filteredInvoices: Invoice[];
  summary: InvoiceSummary;
  handleFilterChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  clearFilters: () => void;
  groupedInvoices: GroupedInvoices;
  formatInvoiceAmount: (invoice: Invoice) => string;
  getInvoiceUserAmount: (invoice: Invoice) => number;
  getInvoiceUserPaid: (invoice: Invoice) => number;
  getInvoiceUserRemaining: (invoice: Invoice) => number;
  getConsolidatedCards: (invoice: Invoice) => Invoice[] | undefined;
  isConsolidated: (invoice: Invoice) => boolean;
  handleViewDetails: (invoice: Invoice) => Promise<void>;
  handleViewConsolidatedDetails: (invoice: Invoice) => Promise<void>;
  handleOpenPayModal: (invoice: Invoice) => void;
  handleDeleteInvoice: (invoice: Invoice) => Promise<void>;
}

const InvoiceList: React.FC<InvoiceListProps> = ({
  error,
  showCreateForm,
  setShowCreateForm,
  formData,
  handleInputChange,
  handleSubmit,
  handleCancel,
  creditCards,
  showFilters,
  setShowFilters,
  filters,
  filteredInvoices,
  summary,
  handleFilterChange,
  clearFilters,
  groupedInvoices,
  formatInvoiceAmount,
  getInvoiceUserAmount,
  getInvoiceUserPaid,
  getInvoiceUserRemaining,
  getConsolidatedCards,
  isConsolidated,
  handleViewDetails,
  handleViewConsolidatedDetails,
  handleOpenPayModal,
  handleDeleteInvoice,
}) => {
  const { t, i18n } = useTranslation();

  const { urgentInvoices, featuredInvoice, upcomingInvoices, paidInvoices } = useMemo(() => {
    const urgent = [
      ...groupedInvoices.overdue,
      ...groupedInvoices.partial,
    ];
    const openSorted = [...groupedInvoices.open].sort(
      (a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime()
    );
    const featured = openSorted.length > 0 ? openSorted[0] : null;
    const upcoming = openSorted.slice(1);
    const paid = [...groupedInvoices.paid, ...groupedInvoices.closed];

    return { urgentInvoices: urgent, featuredInvoice: featured, upcomingInvoices: upcoming, paidInvoices: paid };
  }, [groupedInvoices]);

  const getMonthLabel = (invoice: Invoice): string => {
    if (invoice.invoiceMonth) {
      return formatMonthLabel(invoice.invoiceMonth, i18n.language, 'long');
    }
    return formatDate(invoice.dueDate);
  };

  const getCardCount = (invoice: Invoice): number => {
    const cards = getConsolidatedCards(invoice);
    return cards ? cards.length : 1;
  };

  const renderInvoiceCard = (invoice: Invoice, statusClass: string) => {
    const consolidated = isConsolidated(invoice);
    const cards = getConsolidatedCards(invoice);
    return (
      <div key={invoice.invoiceMonth || invoice.id} className={`invoice-card ${statusClass}`}>
        <div className="invoice-header">
          <h3>{consolidated ? t('invoices.consolidatedBill', 'Fatura Consolidada') : invoice.creditCardName}</h3>
          <span className={`status ${getStatusColor(invoice.status)}`}>{getStatusText(invoice.status, t)}</span>
        </div>

        <div className="invoice-details">
          <div className="detail-item">
            <span className="label">{t('invoices.dueDateLabel')}:</span>
            <span className="value">{formatDate(invoice.dueDate)}</span>
          </div>

          <div className="detail-item">
            <span className="label">{t('invoices.totalAmountLabel')}:</span>
            <span className="value total">{formatInvoiceAmount(invoice)}</span>
          </div>

          <div className="detail-item">
            <span className="label">{t('invoices.paidAmountLabel')}:</span>
            <span className="value paid">{formatCurrency(getInvoiceUserPaid(invoice))}</span>
          </div>

          {getInvoiceUserRemaining(invoice) > 0 && (
            <div className="detail-item">
              <span className="label">{t('invoices.remainingAmountLabel')}:</span>
              <span className="value remaining">{formatCurrency(getInvoiceUserRemaining(invoice))}</span>
            </div>
          )}

          {(statusClass === 'overdue' || statusClass === 'open') && (
            <div className="urgency-badge">
              {getUrgencyText(invoice.dueDate, invoice.status, t)}
            </div>
          )}

          {statusClass === 'partial' && (
            <div className="progress-bar">
              <div
                className="progress-fill"
                style={{
                  width: `${((invoice.paidAmount || 0) / (invoice.totalAmount || 1)) * 100}%`
                }}
              ></div>
            </div>
          )}
        </div>

        {consolidated && cards && (
          <div className="consolidated-breakdown">
            <div className="breakdown-title">{t('invoices.cardBreakdown', 'Cartões nesta fatura')}:</div>
            {cards.map(sub => (
              <div key={sub.id} className="breakdown-row">
                <span className="breakdown-card-name">{sub.creditCardName}</span>
                <span className="breakdown-amount">{formatCurrency(sub.totalAmount || 0)}</span>
                <button
                  onClick={() => handleViewDetails(sub)}
                  className="breakdown-detail-btn"
                >
                  {t('invoices.viewButton')}
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="invoice-actions">
          {consolidated ? (
            <button
              onClick={() => handleViewConsolidatedDetails(invoice)}
              className="view-button"
            >
              {t('invoices.viewAllDetails', 'Ver Fatura Completa')}
            </button>
          ) : (
            <button
              onClick={() => handleViewDetails(invoice)}
              className="view-button"
            >
              {t('invoices.viewButton')}
            </button>
          )}
          {(invoice.status === 'OPEN' || invoice.status === 'PARTIAL' || invoice.status === 'OVERDUE') && (
            <button
              onClick={() => handleOpenPayModal(invoice)}
              className="pay-button"
            >
              {t('invoices.payButton')}
            </button>
          )}
          {!consolidated && (
            <button
              onClick={() => handleDeleteInvoice(invoice)}
              className="delete-invoice-btn"
            >
              {t('invoices.deleteButton', 'Excluir')}
            </button>
          )}
        </div>
      </div>
    );
  };

  const renderFeaturedCard = (invoice: Invoice) => {
    const consolidated = isConsolidated(invoice);
    const cards = getConsolidatedCards(invoice);
    const statusClass = invoice.status.toLowerCase();

    return (
      <div className={`featured-invoice ${statusClass}`}>
        <div className="featured-header">
          <div className="featured-title-row">
            <span className="featured-label">{t('invoices.featured.currentBill')}</span>
            <h2 className="featured-month">{getMonthLabel(invoice)}</h2>
          </div>
          <span className={`status ${getStatusColor(invoice.status)}`}>{getStatusText(invoice.status, t)}</span>
        </div>

        <div className="featured-amount-section">
          <div className="featured-amount">
            <span className="featured-amount-label">{t('invoices.totalAmountLabel')}</span>
            <span className="featured-amount-value">{formatInvoiceAmount(invoice)}</span>
          </div>

          <div className="featured-meta">
            <div className="featured-meta-item">
              <span className="meta-label">{t('invoices.dueDateLabel')}</span>
              <span className="meta-value">{formatDate(invoice.dueDate)}</span>
            </div>
            <div className="featured-meta-item">
              <span className="meta-label">{t('invoices.paidAmountLabel')}</span>
              <span className="meta-value">{formatCurrency(getInvoiceUserPaid(invoice))}</span>
            </div>
            {getInvoiceUserRemaining(invoice) > 0 && (
              <div className="featured-meta-item">
                <span className="meta-label">{t('invoices.remainingAmountLabel')}</span>
                <span className="meta-value">{formatCurrency(getInvoiceUserRemaining(invoice))}</span>
              </div>
            )}
          </div>

          {(invoice.status === 'OPEN' || invoice.status === 'OVERDUE') && (
            <div className="featured-urgency">
              {getUrgencyText(invoice.dueDate, invoice.status, t)}
            </div>
          )}
        </div>

        {consolidated && cards && (
          <div className="featured-breakdown">
            <div className="breakdown-title">{t('invoices.cardBreakdown')}:</div>
            {cards.map(sub => (
              <div key={sub.id} className="featured-breakdown-row">
                <span className="breakdown-card-name">{sub.creditCardName}</span>
                <span className="breakdown-amount">{formatCurrency(sub.totalAmount || 0)}</span>
                <button
                  onClick={() => handleViewDetails(sub)}
                  className="breakdown-detail-btn"
                >
                  {t('invoices.viewButton')}
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="featured-actions">
          {consolidated ? (
            <button
              onClick={() => handleViewConsolidatedDetails(invoice)}
              className="view-button"
            >
              {t('invoices.viewAllDetails')}
            </button>
          ) : (
            <button
              onClick={() => handleViewDetails(invoice)}
              className="view-button"
            >
              {t('invoices.viewButton')}
            </button>
          )}
          <button
            onClick={() => handleOpenPayModal(invoice)}
            className="pay-button"
          >
            {t('invoices.payButton')}
          </button>
        </div>
      </div>
    );
  };

  const renderCompactRow = (invoice: Invoice, statusClass: string) => {
    const consolidated = isConsolidated(invoice);
    const cardCount = getCardCount(invoice);

    const handleRowClick = () => {
      if (consolidated) {
        handleViewConsolidatedDetails(invoice);
      } else {
        handleViewDetails(invoice);
      }
    };

    return (
      <div
        key={invoice.invoiceMonth || invoice.id}
        className={`compact-row ${statusClass}`}
        onClick={handleRowClick}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleRowClick(); }}
      >
        <div className="compact-month">
          <span className="compact-month-label">{getMonthLabel(invoice)}</span>
          {cardCount > 1 && (
            <span className="compact-card-count">
              {t('invoices.compact.cardCount', { count: cardCount })}
            </span>
          )}
        </div>

        <div className="compact-info">
          <span className="compact-amount">{formatInvoiceAmount(invoice)}</span>
          <span className={`compact-status ${getStatusColor(invoice.status)}`}>
            {getStatusText(invoice.status, t)}
          </span>
        </div>

        <div className="compact-due">
          <span className="compact-due-date">{formatDate(invoice.dueDate)}</span>
        </div>

        <div className="compact-actions" onClick={(e) => e.stopPropagation()}>
          {(invoice.status === 'OPEN' || invoice.status === 'PARTIAL' || invoice.status === 'OVERDUE') && (
            <button
              onClick={() => handleOpenPayModal(invoice)}
              className="compact-pay-btn"
            >
              {t('invoices.payButton')}
            </button>
          )}
          <span className="compact-chevron">›</span>
        </div>
      </div>
    );
  };

  return (
    <>
      <header className="invoices-header">
        <h1>{t('invoices.title')}</h1>
        <div className="header-actions">
          <button
            className="filter-button"
            onClick={() => setShowFilters(!showFilters)}
          >
            {t('invoices.filters.title')}
          </button>
          <button
            className="add-button"
            onClick={() => setShowCreateForm(true)}
          >
            {t('invoices.addNewInvoice')}
          </button>
        </div>
      </header>

      {error && <div className="error-message">{error}</div>}

      {/* Summary Section */}
      <div className="invoices-summary">
        <div className="summary-card total">
          <h3>{t('invoices.summary.totalInvoices')}</h3>
          <div className="summary-value">{summary.totalInvoices}</div>
        </div>
        <div className="summary-card amount">
          <h3>{t('invoices.summary.totalAmount')}</h3>
          <div className="summary-value">{formatCurrency(summary.totalAmount)}</div>
        </div>
        <div className="summary-card paid">
          <h3>{t('invoices.summary.totalPaid')}</h3>
          <div className="summary-value">{formatCurrency(summary.totalPaid)}</div>
        </div>
        <div className="summary-card remaining">
          <h3>{t('invoices.summary.totalRemaining')}</h3>
          <div className="summary-value">{formatCurrency(summary.totalRemaining)}</div>
        </div>
      </div>

      {/* Filters Section */}
      {showFilters && (
        <div className="filters-section">
          <h3>{t('invoices.filters.title')}</h3>
          <div className="filters-grid">
            <div className="filter-group">
              <label>{t('invoices.filters.status')}</label>
              <select name="status" value={filters.status || ''} onChange={handleFilterChange}>
                <option value="">{t('invoices.filters.allStatus')}</option>
                <option value="OVERDUE">{t('invoices.status.overdue')}</option>
                <option value="OPEN">{t('invoices.status.open')}</option>
                <option value="PARTIAL">{t('invoices.status.partial')}</option>
                <option value="PAID">{t('invoices.status.paid')}</option>
                <option value="CLOSED">{t('invoices.status.closed')}</option>
              </select>
            </div>
            <div className="filter-group">
              <label>{t('invoices.filters.creditCard')}</label>
              <select name="creditCardId" value={filters.creditCardId || ''} onChange={handleFilterChange}>
                <option value="">{t('invoices.filters.allCards')}</option>
                {creditCards.map(card => (
                  <option key={card.id} value={card.id}>
                    {card.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="filter-group">
              <label>{t('invoices.filters.dateFrom')}</label>
              <input
                type="date"
                name="dateFrom"
                value={filters.dateFrom || ''}
                onChange={handleFilterChange}
              />
            </div>
            <div className="filter-group">
              <label>{t('invoices.filters.dateTo')}</label>
              <input
                type="date"
                name="dateTo"
                value={filters.dateTo || ''}
                onChange={handleFilterChange}
              />
            </div>
            <div className="filter-group">
              <label>{t('invoices.filters.minAmount')}</label>
              <input
                type="number"
                name="minAmount"
                value={filters.minAmount || ''}
                onChange={handleFilterChange}
                step="0.01"
                min="0"
              />
            </div>
            <div className="filter-group">
              <label>{t('invoices.filters.maxAmount')}</label>
              <input
                type="number"
                name="maxAmount"
                value={filters.maxAmount || ''}
                onChange={handleFilterChange}
                step="0.01"
                min="0"
              />
            </div>
          </div>
          <div className="filter-actions">
            <button onClick={clearFilters} className="clear-filters">
              {t('invoices.filters.clearFilters')}
            </button>
          </div>
        </div>
      )}

      {/* Create Invoice Form */}
      {showCreateForm && (
        <div className="form-overlay">
          <div className="form-container">
            <h2>{t('invoices.addFormTitle')}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="creditCardId">{t('invoices.creditCardLabel')}</label>
                <select
                  id="creditCardId"
                  name="creditCardId"
                  value={formData.creditCardId}
                  onChange={handleInputChange}
                  required
                >
                  <option value={0}>{t('invoices.selectCreditCard')}</option>
                  {creditCards.map(card => (
                    <option key={card.id} value={card.id}>
                      {card.name} - **** {card.lastFourDigits}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="dueDate">{t('invoices.dueDateLabel')}</label>
                <input
                  type="date"
                  id="dueDate"
                  name="dueDate"
                  value={formData.dueDate}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="submit-button">
                  {t('invoices.createButton')}
                </button>
                <button type="button" onClick={handleCancel} className="cancel-button">
                  {t('common.cancel')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Invoice Layout - Featured Card + Compact List */}
      {filteredInvoices.length > 0 ? (
        <div className="invoices-layout">
          {/* Urgent Invoices (Overdue + Partial) */}
          {urgentInvoices.length > 0 && (
            <div className="invoice-section urgent-section">
              <div className="section-header">
                <h2>{t('invoices.sections.urgent')}</h2>
                <span className="section-count">{urgentInvoices.length}</span>
              </div>
              <div className="invoices-grid">
                {urgentInvoices.map(invoice =>
                  renderInvoiceCard(invoice, invoice.status === 'OVERDUE' ? 'overdue' : 'partial')
                )}
              </div>
            </div>
          )}

          {/* Featured Invoice (Current/Next Open) */}
          {featuredInvoice && (
            <div className="invoice-section featured-section">
              {renderFeaturedCard(featuredInvoice)}
            </div>
          )}

          {/* Upcoming Invoices (Remaining Open) */}
          {upcomingInvoices.length > 0 && (
            <div className="invoice-section upcoming-section">
              <div className="section-header">
                <h2>{t('invoices.sections.upcoming')}</h2>
                <span className="section-total">
                  {formatCurrency(upcomingInvoices.reduce((sum, inv) => sum + getInvoiceUserAmount(inv), 0))}
                </span>
              </div>
              <div className="compact-list">
                {upcomingInvoices.map(invoice => renderCompactRow(invoice, 'open'))}
              </div>
            </div>
          )}

          {/* Paid/Closed Invoices */}
          {paidInvoices.length > 0 && (
            <div className="invoice-section paid-section">
              <div className="section-header">
                <h2>{t('invoices.sections.paid')}</h2>
                <span className="section-count">{paidInvoices.length}</span>
              </div>
              <div className="compact-list">
                {paidInvoices.map(invoice =>
                  renderCompactRow(invoice, invoice.status === 'PAID' ? 'paid' : 'closed')
                )}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="empty-state">
          <p>{t('invoices.emptyState')}</p>
        </div>
      )}
    </>
  );
};

export default InvoiceList;
