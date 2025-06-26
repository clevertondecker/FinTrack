import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { Invoice, InvoiceItem, CreateInvoiceRequest, InvoiceFilters, InvoiceSummary, GroupedInvoices, Category } from '../types/invoice';
import { CreditCard } from '../types/creditCard';
import ShareItemModal from './ShareItemModal';
import './Invoices.css';

const Invoices: React.FC = () => {
  const { t } = useTranslation();
  
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [invoiceItems, setInvoiceItems] = useState<InvoiceItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);
  const [selectedItemForSharing, setSelectedItemForSharing] = useState<InvoiceItem | null>(null);
  const [showPayModal, setShowPayModal] = useState(false);
  const [invoiceToPay, setInvoiceToPay] = useState<Invoice | null>(null);
  const [payAmount, setPayAmount] = useState('');
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState<string | null>(null);
  const [addingItem, setAddingItem] = useState(false);
  const [removingItemId, setRemovingItemId] = useState<number | null>(null);
  const [itemError, setItemError] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<InvoiceFilters>({});
  const [filteredInvoices, setFilteredInvoices] = useState<Invoice[]>([]);
  const [summary, setSummary] = useState<InvoiceSummary>({
    totalInvoices: 0,
    totalAmount: 0,
    totalPaid: 0,
    totalRemaining: 0,
    overdueCount: 0,
    overdueAmount: 0,
    openCount: 0,
    openAmount: 0,
    partialCount: 0,
    partialAmount: 0,
    paidCount: 0,
    paidAmount: 0
  });

  const [formData, setFormData] = useState<CreateInvoiceRequest>({
    creditCardId: 0,
    dueDate: ''
  });

  const [itemForm, setItemForm] = useState({
    description: '',
    amount: '',
    categoryId: '',
    purchaseDate: ''
  });

  useEffect(() => {
    loadInvoices();
    loadCreditCards();
    loadCategories();
  }, []);

  useEffect(() => {
    applyFilters();
  }, [invoices, filters]);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      const response = await apiService.getInvoices();
      setInvoices(response.invoices);
      setError(null);
    } catch (err) {
      setError(t('invoices.failedToLoadInvoices'));
      console.error('Error loading invoices:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadCreditCards = async () => {
    try {
      const response = await apiService.getCreditCards();
      setCreditCards(response.creditCards);
    } catch (err) {
      console.error('Error loading credit cards:', err);
    }
  };

  const loadCategories = async () => {
    try {
      const response = await apiService.getCategories();
      setCategories(response.categories);
    } catch (err) {
      console.error('Error loading categories:', err);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: name === 'creditCardId' ? Number(value) : value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.creditCardId || !formData.dueDate) {
      setError(t('invoices.pleaseFillFields'));
      return;
    }

    try {
      await apiService.createInvoice(formData);
      setFormData({ creditCardId: 0, dueDate: '' });
      setShowCreateForm(false);
      await loadInvoices();
      setError(null);
    } catch (err) {
      setError(t('invoices.failedToCreateInvoice'));
      console.error('Error creating invoice:', err);
    }
  };

  const handleCancel = () => {
    setShowCreateForm(false);
    setFormData({ creditCardId: 0, dueDate: '' });
    setError(null);
  };

  const formatCurrency = (amount: number | null | undefined) => {
    if (amount === null || amount === undefined) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    if (!dateString || dateString === 'null' || dateString === 'undefined') return '-';
    
    try {
      const cleanDateString = dateString.trim();
      
      // Se já é uma string ISO válida
      if (cleanDateString.includes('T')) {
        const date = new Date(cleanDateString);
        if (!isNaN(date.getTime())) {
          return date.toLocaleDateString('pt-BR');
        }
      }
      
      // Se é apenas uma data (yyyy-MM-dd)
      if (cleanDateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
        const date = new Date(cleanDateString + 'T00:00:00');
        if (!isNaN(date.getTime())) {
          return date.toLocaleDateString('pt-BR');
        }
      }
      
      // Fallback: retorna a string original se não conseguir parsear
      return cleanDateString;
    } catch (error) {
      return dateString;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PAID':
        return 'status-paid';
      case 'OVERDUE':
        return 'status-overdue';
      case 'PARTIAL':
        return 'status-partial';
      default:
        return 'status-open';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'PAID':
        return t('invoices.status.paid');
      case 'OVERDUE':
        return t('invoices.status.overdue');
      case 'PARTIAL':
        return t('invoices.status.partial');
      default:
        return t('invoices.status.open');
    }
  };

  const getUrgencyText = (dueDate: string, status: string) => {
    if (status === 'PAID') return '';
    
    const today = new Date();
    const due = new Date(dueDate);
    const diffTime = due.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (status === 'OVERDUE') {
      return t('invoices.urgency.overdue', { days: Math.abs(diffDays) });
    } else if (diffDays === 0) {
      return t('invoices.urgency.dueToday');
    } else if (diffDays === 1) {
      return t('invoices.urgency.dueTomorrow');
    } else if (diffDays > 0) {
      return t('invoices.urgency.dueInDays', { days: diffDays });
    }
    
    return '';
  };

  const sortInvoices = (invoices: Invoice[]): Invoice[] => {
    const priority: Record<string, number> = { 'OVERDUE': 1, 'OPEN': 2, 'PARTIAL': 3, 'PAID': 4 };
    
    return invoices.sort((a, b) => {
      // Primeiro por status
      const statusDiff = (priority[a.status] || 5) - (priority[b.status] || 5);
      if (statusDiff !== 0) return statusDiff;
      
      // Depois por data de vencimento (mais próxima primeiro)
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    });
  };

  const groupInvoices = (invoices: Invoice[]): GroupedInvoices => {
    return {
      overdue: invoices.filter(i => i.status === 'OVERDUE'),
      open: invoices.filter(i => i.status === 'OPEN'),
      partial: invoices.filter(i => i.status === 'PARTIAL'),
      paid: invoices.filter(i => i.status === 'PAID')
    };
  };

  const calculateSummary = (invoices: Invoice[]): InvoiceSummary => {
    const summary: InvoiceSummary = {
      totalInvoices: invoices.length,
      totalAmount: 0,
      totalPaid: 0,
      totalRemaining: 0,
      overdueCount: 0,
      overdueAmount: 0,
      openCount: 0,
      openAmount: 0,
      partialCount: 0,
      partialAmount: 0,
      paidCount: 0,
      paidAmount: 0
    };

    invoices.forEach(invoice => {
      const total = invoice.totalAmount || 0;
      const paid = invoice.paidAmount || 0;
      
      summary.totalAmount += total;
      summary.totalPaid += paid;
      summary.totalRemaining += (total - paid);

      switch (invoice.status) {
        case 'OVERDUE':
          summary.overdueCount++;
          summary.overdueAmount += total;
          break;
        case 'OPEN':
          summary.openCount++;
          summary.openAmount += total;
          break;
        case 'PARTIAL':
          summary.partialCount++;
          summary.partialAmount += total;
          break;
        case 'PAID':
          summary.paidCount++;
          summary.paidAmount += total;
          break;
      }
    });

    return summary;
  };

  const applyFilters = () => {
    let filtered = [...invoices];

    if (filters.status) {
      filtered = filtered.filter(invoice => invoice.status === filters.status);
    }

    if (filters.creditCardId) {
      filtered = filtered.filter(invoice => invoice.creditCardId === filters.creditCardId);
    }

    if (filters.dateFrom) {
      filtered = filtered.filter(invoice => new Date(invoice.dueDate) >= new Date(filters.dateFrom!));
    }

    if (filters.dateTo) {
      filtered = filtered.filter(invoice => new Date(invoice.dueDate) <= new Date(filters.dateTo!));
    }

    if (filters.minAmount) {
      filtered = filtered.filter(invoice => (invoice.totalAmount || 0) >= filters.minAmount!);
    }

    if (filters.maxAmount) {
      filtered = filtered.filter(invoice => (invoice.totalAmount || 0) <= filters.maxAmount!);
    }

    const sorted = sortInvoices(filtered);
    setFilteredInvoices(sorted);
    setSummary(calculateSummary(sorted));
  };

  const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: name === 'creditCardId' || name === 'minAmount' || name === 'maxAmount' 
        ? (value ? Number(value) : undefined) 
        : value
    }));
  };

  const clearFilters = () => {
    setFilters({});
  };

  const handleViewDetails = async (invoice: Invoice) => {
    setSelectedInvoice(invoice);
    setShowDetailsModal(true);
    setLoadingItems(true);
    try {
      const response = await apiService.getInvoiceItems(invoice.id);
      setInvoiceItems(response.items);
    } catch (err) {
      setInvoiceItems([]);
    } finally {
      setLoadingItems(false);
    }
  };

  const handleCloseDetails = () => {
    setShowDetailsModal(false);
    setSelectedInvoice(null);
    setInvoiceItems([]);
  };

  const handleItemInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setItemForm(prev => ({ ...prev, [name]: value }));
  };

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemForm.description || !itemForm.amount || !itemForm.purchaseDate || isNaN(Number(itemForm.amount))) {
      setItemError('Preencha todos os campos obrigatórios corretamente');
      return;
    }
    if (!selectedInvoice) return;
    setAddingItem(true);
    setItemError(null);
    try {
      await apiService.createInvoiceItem(selectedInvoice.id, {
        description: itemForm.description,
        amount: Number(itemForm.amount),
        categoryId: itemForm.categoryId ? Number(itemForm.categoryId) : undefined,
        purchaseDate: itemForm.purchaseDate
      });
      
      // Atualiza lista de itens e dados da fatura
      const [itemsResponse, invoiceResponse] = await Promise.all([
        apiService.getInvoiceItems(selectedInvoice.id),
        apiService.getInvoice(selectedInvoice.id)
      ]);
      
      setInvoiceItems(itemsResponse.items);
      setSelectedInvoice(invoiceResponse.invoice);
      setItemForm({ description: '', amount: '', categoryId: '', purchaseDate: '' });
      
      // Atualiza também a lista principal de faturas para refletir o novo valor total
      await loadInvoices();
      
    } catch (err: any) {
      // Tenta extrair a mensagem de erro específica do backend
      const errorMessage = err.response?.data?.error || err.message || 'Erro ao adicionar item';
      setItemError(errorMessage);
      console.error('Error adding invoice item:', err);
    } finally {
      setAddingItem(false);
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    if (!selectedInvoice) return;
    setRemovingItemId(itemId);
    try {
      await apiService.deleteInvoiceItem(selectedInvoice.id, itemId);
      
      // Atualiza lista de itens e dados da fatura
      const [itemsResponse, invoiceResponse] = await Promise.all([
        apiService.getInvoiceItems(selectedInvoice.id),
        apiService.getInvoice(selectedInvoice.id)
      ]);
      
      setInvoiceItems(itemsResponse.items);
      setSelectedInvoice(invoiceResponse.invoice);
      
      // Atualiza também a lista principal de faturas para refletir o novo valor total
      await loadInvoices();
      
    } catch (err) {
      // Pode exibir erro se quiser
    } finally {
      setRemovingItemId(null);
    }
  };

  const handleShareItem = (item: InvoiceItem) => {
    setSelectedItemForSharing(item);
    setShowShareModal(true);
  };

  const handleCloseShareModal = () => {
    setShowShareModal(false);
    setSelectedItemForSharing(null);
  };

  const handleSharesUpdated = async () => {
    if (!selectedInvoice) return;
    
    // Recarregar os itens da fatura para mostrar informações atualizadas
    try {
      const [itemsResponse, invoiceResponse] = await Promise.all([
        apiService.getInvoiceItems(selectedInvoice.id),
        apiService.getInvoice(selectedInvoice.id)
      ]);
      
      setInvoiceItems(itemsResponse.items);
      setSelectedInvoice(invoiceResponse.invoice);
      
      // Atualiza também a lista principal de faturas
      await loadInvoices();
      
    } catch (err) {
      console.error('Error reloading invoice items:', err);
    }
  };

  const handleOpenPayModal = (invoice: Invoice) => {
    setInvoiceToPay(invoice);
    setPayAmount(((invoice.totalAmount || 0) - (invoice.paidAmount || 0)).toFixed(2));
    setShowPayModal(true);
    setPayError(null);
  };

  const handleClosePayModal = () => {
    setShowPayModal(false);
    setInvoiceToPay(null);
    setPayAmount('');
    setPayError(null);
  };

  const handlePayTotal = () => {
    if (!invoiceToPay) return;
    const totalRemaining = (invoiceToPay.totalAmount || 0) - (invoiceToPay.paidAmount || 0);
    setPayAmount(totalRemaining.toFixed(2));
  };

  const handlePayInvoice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!invoiceToPay) return;
    const amount = parseFloat(payAmount);
    if (isNaN(amount) || amount <= 0) {
      setPayError(t('invoices.enterValidAmount'));
      return;
    }
    setPaying(true);
    setPayError(null);
    try {
      await apiService.payInvoice(invoiceToPay.id, { amount });
      await loadInvoices();
      handleClosePayModal();
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || t('invoices.errorPayingInvoice');
      setPayError(errorMessage);
    } finally {
      setPaying(false);
    }
  };

  const groupedInvoices = groupInvoices(filteredInvoices);

  if (loading) {
    return <div className="loading">{t('invoices.loading')}</div>;
  }

  return (
    <div className="invoices-container">
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

      {/* Grouped Invoices */}
      <div className="invoices-groups">
        {/* Overdue Invoices */}
        {groupedInvoices.overdue.length > 0 && (
          <div className="invoice-group overdue">
            <div className="group-header">
              <h2>⚠️ {t('invoices.groups.overdue')} ({groupedInvoices.overdue.length})</h2>
              <div className="group-summary">
                {formatCurrency(groupedInvoices.overdue.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.overdue.map(invoice => (
                <div key={invoice.id} className="invoice-card overdue">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
                    <span className={`status ${getStatusColor(invoice.status)}`}>
                      {getStatusText(invoice.status)}
                    </span>
                  </div>
                  
                  <div className="invoice-details">
                    <div className="detail-item">
                      <span className="label">{t('invoices.dueDateLabel')}:</span>
                      <span className="value">{formatDate(invoice.dueDate)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.totalAmountLabel')}:</span>
                      <span className="value total">{formatCurrency(invoice.totalAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.paidAmountLabel')}:</span>
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.remainingAmountLabel')}:</span>
                      <span className="value remaining">{formatCurrency((invoice.totalAmount || 0) - (invoice.paidAmount || 0))}</span>
                    </div>
                    
                    <div className="urgency-badge">
                      {getUrgencyText(invoice.dueDate, invoice.status)}
                    </div>
                  </div>

                  <div className="invoice-actions">
                    <button 
                      onClick={() => handleViewDetails(invoice)}
                      className="view-button"
                    >
                      {t('invoices.viewButton')}
                    </button>
                    {(invoice.status === 'OPEN' || invoice.status === 'PARTIAL' || invoice.status === 'OVERDUE') && (
                      <button 
                        onClick={() => handleOpenPayModal(invoice)}
                        className="pay-button"
                      >
                        {t('invoices.payButton')}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Open Invoices */}
        {groupedInvoices.open.length > 0 && (
          <div className="invoice-group open">
            <div className="group-header">
              <h2>⏰ {t('invoices.groups.open')} ({groupedInvoices.open.length})</h2>
              <div className="group-summary">
                {formatCurrency(groupedInvoices.open.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.open.map(invoice => (
                <div key={invoice.id} className="invoice-card open">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
                    <span className={`status ${getStatusColor(invoice.status)}`}>
                      {getStatusText(invoice.status)}
                    </span>
                  </div>
                  
                  <div className="invoice-details">
                    <div className="detail-item">
                      <span className="label">{t('invoices.dueDateLabel')}:</span>
                      <span className="value">{formatDate(invoice.dueDate)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.totalAmountLabel')}:</span>
                      <span className="value total">{formatCurrency(invoice.totalAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.paidAmountLabel')}:</span>
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.remainingAmountLabel')}:</span>
                      <span className="value remaining">{formatCurrency((invoice.totalAmount || 0) - (invoice.paidAmount || 0))}</span>
                    </div>
                    
                    <div className="urgency-badge">
                      {getUrgencyText(invoice.dueDate, invoice.status)}
                    </div>
                  </div>

                  <div className="invoice-actions">
                    <button 
                      onClick={() => handleViewDetails(invoice)}
                      className="view-button"
                    >
                      {t('invoices.viewButton')}
                    </button>
                    <button 
                      onClick={() => handleOpenPayModal(invoice)}
                      className="pay-button"
                    >
                      {t('invoices.payButton')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Partial Invoices */}
        {groupedInvoices.partial.length > 0 && (
          <div className="invoice-group partial">
            <div className="group-header">
              <h2>🔄 {t('invoices.groups.partial')} ({groupedInvoices.partial.length})</h2>
              <div className="group-summary">
                {formatCurrency(groupedInvoices.partial.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.partial.map(invoice => (
                <div key={invoice.id} className="invoice-card partial">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
                    <span className={`status ${getStatusColor(invoice.status)}`}>
                      {getStatusText(invoice.status)}
                    </span>
                  </div>
                  
                  <div className="invoice-details">
                    <div className="detail-item">
                      <span className="label">{t('invoices.dueDateLabel')}:</span>
                      <span className="value">{formatDate(invoice.dueDate)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.totalAmountLabel')}:</span>
                      <span className="value total">{formatCurrency(invoice.totalAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.paidAmountLabel')}:</span>
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.remainingAmountLabel')}:</span>
                      <span className="value remaining">{formatCurrency((invoice.totalAmount || 0) - (invoice.paidAmount || 0))}</span>
                    </div>
                    
                    <div className="progress-bar">
                      <div 
                        className="progress-fill" 
                        style={{ 
                          width: `${((invoice.paidAmount || 0) / (invoice.totalAmount || 1)) * 100}%` 
                        }}
                      ></div>
                    </div>
                  </div>

                  <div className="invoice-actions">
                    <button 
                      onClick={() => handleViewDetails(invoice)}
                      className="view-button"
                    >
                      {t('invoices.viewButton')}
                    </button>
                    <button 
                      onClick={() => handleOpenPayModal(invoice)}
                      className="pay-button"
                    >
                      {t('invoices.payButton')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Paid Invoices */}
        {groupedInvoices.paid.length > 0 && (
          <div className="invoice-group paid">
            <div className="group-header">
              <h2>✅ {t('invoices.groups.paid')} ({groupedInvoices.paid.length})</h2>
              <div className="group-summary">
                {formatCurrency(groupedInvoices.paid.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.paid.map(invoice => (
                <div key={invoice.id} className="invoice-card paid">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
                    <span className={`status ${getStatusColor(invoice.status)}`}>
                      {getStatusText(invoice.status)}
                    </span>
                  </div>
                  
                  <div className="invoice-details">
                    <div className="detail-item">
                      <span className="label">{t('invoices.dueDateLabel')}:</span>
                      <span className="value">{formatDate(invoice.dueDate)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.totalAmountLabel')}:</span>
                      <span className="value total">{formatCurrency(invoice.totalAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.paidAmountLabel')}:</span>
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.createdAtLabel')}:</span>
                      <span className="value">{formatDate(invoice.createdAt)}</span>
                    </div>
                  </div>

                  <div className="invoice-actions">
                    <button 
                      onClick={() => handleViewDetails(invoice)}
                      className="view-button"
                    >
                      {t('invoices.viewButton')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Empty State */}
        {filteredInvoices.length === 0 && (
          <div className="empty-state">
            <p>{t('invoices.emptyState')}</p>
          </div>
        )}
      </div>

      {/* Details Modal */}
      {showDetailsModal && selectedInvoice && (
        <div className="modal-overlay">
          <div className="modal-container">
            <div className="modal-header">
              <h2>{t('invoices.detailsModalTitle')}</h2>
              <button onClick={handleCloseDetails} className="close-button">&times;</button>
            </div>
            
            <div className="modal-content">
              <div className="invoice-info">
                <div className="info-row">
                  <span className="label">{t('invoices.creditCardLabel')}:</span>
                  <span className="value">{selectedInvoice.creditCardName}</span>
                </div>
                <div className="info-row">
                  <span className="label">{t('invoices.dueDateLabel')}:</span>
                  <span className="value">{formatDate(selectedInvoice.dueDate)}</span>
                </div>
                <div className="info-row">
                  <span className="label">{t('invoices.statusLabel')}:</span>
                  <span className={`value status ${getStatusColor(selectedInvoice.status)}`}>
                    {getStatusText(selectedInvoice.status)}
                  </span>
                </div>
                <div className="info-row">
                  <span className="label">{t('invoices.totalAmountLabel')}:</span>
                  <span className="value">{formatCurrency(selectedInvoice.totalAmount)}</span>
                </div>
                <div className="info-row">
                  <span className="label">{t('invoices.paidAmountLabel')}:</span>
                  <span className="value">{formatCurrency(selectedInvoice.paidAmount)}</span>
                </div>
              </div>

              <div className="items-section">
                <h3>{t('invoices.itemsLabel')}</h3>
                
                {loadingItems ? (
                  <div className="loading">{t('invoices.loadingItems')}</div>
                ) : (
                  <>
                    {invoiceItems.length === 0 ? (
                      <p className="no-items">{t('invoices.noItems')}</p>
                    ) : (
                      <div className="items-list">
                        {invoiceItems.map(item => (
                          <div key={item.id} className="item-row">
                            <div className="item-info">
                              <div className="item-description">{item.description}</div>
                              <div className="item-meta">
                                <span className="item-category">{item.category || t('invoices.noCategory')}</span>
                                <span className="item-date">{formatDate(item.purchaseDate)}</span>
                              </div>
                            </div>
                            <div className="item-amount">{formatCurrency(item.amount)}</div>
                            <div className="item-actions">
                              <button 
                                onClick={() => handleShareItem(item)}
                                className="share-button"
                                title={t('invoices.shareItemTooltip')}
                              >
                                {t('invoices.shareButton')}
                              </button>
                              <button 
                                onClick={() => handleRemoveItem(item.id)}
                                className="remove-button"
                                title={t('invoices.removeItemTooltip')}
                                disabled={removingItemId === item.id}
                              >
                                {removingItemId === item.id ? t('invoices.removing') : t('invoices.removeButton')}
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}

                    <form onSubmit={handleAddItem} className="add-item-form">
                      <h4>{t('invoices.addItem')}</h4>
                      <div className="form-row">
                        <div className="form-group">
                          <label>{t('invoices.descriptionLabel')}</label>
                          <input
                            type="text"
                            name="description"
                            value={itemForm.description}
                            onChange={handleItemInputChange}
                            placeholder={t('invoices.descriptionPlaceholder')}
                            required
                          />
                        </div>
                        <div className="form-group">
                          <label>{t('invoices.amountLabel')}</label>
                          <input
                            type="number"
                            name="amount"
                            value={itemForm.amount}
                            onChange={handleItemInputChange}
                            placeholder={t('invoices.amountPlaceholder')}
                            step="0.01"
                            min="0"
                            required
                          />
                        </div>
                      </div>
                      <div className="form-row">
                        <div className="form-group">
                          <label>{t('invoices.categoryLabel')}</label>
                          <select
                            name="categoryId"
                            value={itemForm.categoryId}
                            onChange={handleItemInputChange}
                          >
                            <option value="">{t('invoices.selectCategory')}</option>
                            {categories.map(category => (
                              <option key={category.id} value={category.id}>
                                {category.name}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div className="form-group">
                          <label>{t('invoices.purchaseDateLabel')}</label>
                          <input
                            type="date"
                            name="purchaseDate"
                            value={itemForm.purchaseDate}
                            onChange={handleItemInputChange}
                            required
                          />
                        </div>
                      </div>
                      {itemError && <div className="error-message">{itemError}</div>}
                      <button type="submit" className="submit-button" disabled={addingItem}>
                        {addingItem ? t('invoices.adding') : t('invoices.addItem')}
                      </button>
                    </form>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Pay Modal */}
      {showPayModal && invoiceToPay && (
        <div className="modal-overlay">
          <div className="modal-container">
            <div className="modal-header">
              <h2>{t('invoices.payModalTitle')}</h2>
              <button onClick={handleClosePayModal} className="close-button">&times;</button>
            </div>
            
            <div className="modal-content">
              <div className="pay-info">
                <p><strong>{t('invoices.creditCardLabel')}:</strong> {invoiceToPay.creditCardName}</p>
                <p><strong>{t('invoices.totalAmountLabel')}:</strong> {formatCurrency(invoiceToPay.totalAmount)}</p>
                <p><strong>{t('invoices.paidAmountLabel')}:</strong> {formatCurrency(invoiceToPay.paidAmount)}</p>
                <p><strong>{t('invoices.remainingAmountLabel')}:</strong> {formatCurrency((invoiceToPay.totalAmount || 0) - (invoiceToPay.paidAmount || 0))}</p>
              </div>

              <form onSubmit={handlePayInvoice} className="pay-form">
                <div className="form-group">
                  <label htmlFor="payAmount">{t('invoices.payAmountLabel')}</label>
                  <div className="amount-input-group">
                    <input
                      type="number"
                      id="payAmount"
                      value={payAmount}
                      onChange={(e) => setPayAmount(e.target.value)}
                      step="0.01"
                      min="0"
                      max={(invoiceToPay.totalAmount || 0) - (invoiceToPay.paidAmount || 0)}
                      required
                    />
                    <button type="button" onClick={handlePayTotal} className="total-button">
                      {t('invoices.totalButton')}
                    </button>
                  </div>
                </div>
                
                {payError && <div className="error-message">{payError}</div>}
                
                <div className="form-actions">
                  <button type="submit" className="submit-button" disabled={paying}>
                    {paying ? t('invoices.paying') : t('invoices.payButton')}
                  </button>
                  <button type="button" onClick={handleClosePayModal} className="cancel-button">
                    {t('common.cancel')}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Share Modal */}
      {showShareModal && selectedItemForSharing && (
        <ShareItemModal
          isOpen={showShareModal}
          onClose={handleCloseShareModal}
          invoiceId={selectedInvoice?.id || 0}
          itemId={selectedItemForSharing.id}
          itemDescription={selectedItemForSharing.description}
          itemAmount={selectedItemForSharing.amount}
          onSharesUpdated={handleSharesUpdated}
        />
      )}
    </div>
  );
};

export default Invoices; 