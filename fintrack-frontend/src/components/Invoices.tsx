import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { Invoice, InvoiceItem, CreateInvoiceRequest, CreateInvoiceItemRequest, InvoiceFilters, InvoiceSummary, GroupedInvoices, Category } from '../types/invoice';
import { CreditCard } from '../types/creditCard';
import { getStatusColor, getStatusText, getUrgencyText, formatCurrency, formatDate } from '../utils/invoiceUtils';
import ShareItemModal from './ShareItemModal';
import './Invoices.css';
import { useAuth } from '../contexts/AuthContext';

const Invoices: React.FC = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  
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

  const [showAddMore, setShowAddMore] = useState(false);
  const [quickAddMode, setQuickAddMode] = useState(false);

  // Funções auxiliares para formatação de valores
  const formatInvoiceAmount = (invoice: Invoice): string => {
    if (invoice.userShare !== null && invoice.userShare !== undefined && invoice.userShare !== invoice.totalAmount) {
      return `${formatCurrency(invoice.userShare)} (${t('invoices.myShare')})`;
    }
    return formatCurrency(invoice.totalAmount);
  };

  const getInvoiceUserAmount = (invoice: Invoice): number => {
    return invoice.userShare !== null && invoice.userShare !== undefined ? invoice.userShare : (invoice.totalAmount || 0);
  };

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

  const sortInvoices = (invoices: Invoice[]): Invoice[] => {
    const priority: Record<string, number> = { 'OVERDUE': 1, 'OPEN': 2, 'PARTIAL': 3, 'PAID': 4, 'CLOSED': 5 };
    
    return invoices.sort((a, b) => {
      // Primeiro por status
      const statusDiff = (priority[a.status] || 6) - (priority[b.status] || 6);
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
      paid: invoices.filter(i => i.status === 'PAID'),
      closed: invoices.filter(i => i.status === 'CLOSED')
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
      // Use userShare if available, otherwise use totalAmount
      const userAmount = invoice.userShare !== null && invoice.userShare !== undefined ? invoice.userShare : (invoice.totalAmount || 0);
      const paid = invoice.paidAmount || 0;
      
      summary.totalAmount += userAmount;
      summary.totalPaid += paid;
      summary.totalRemaining += (userAmount - paid);

      switch (invoice.status) {
        case 'OVERDUE':
          summary.overdueCount++;
          summary.overdueAmount += userAmount;
          break;
        case 'OPEN':
          summary.openCount++;
          summary.openAmount += userAmount;
          break;
        case 'PARTIAL':
          summary.partialCount++;
          summary.partialAmount += userAmount;
          break;
        case 'PAID':
          summary.paidCount++;
          summary.paidAmount += userAmount;
          break;
        case 'CLOSED':
          // Faturas fechadas não são contabilizadas no resumo
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

  const handleQuickAdd = () => {
    setQuickAddMode(true);
    setItemForm({
      description: '',
      amount: '',
      categoryId: '',
      purchaseDate: new Date().toISOString().split('T')[0] // Data atual como padrão
    });
  };

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemForm.description || !itemForm.amount || isNaN(Number(itemForm.amount))) {
      setItemError('Descrição e valor são obrigatórios');
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
        purchaseDate: itemForm.purchaseDate || new Date().toISOString().split('T')[0]
      });
      
      // Atualiza lista de itens e dados da fatura
      const [itemsResponse, invoiceResponse] = await Promise.all([
        apiService.getInvoiceItems(selectedInvoice.id),
        apiService.getInvoice(selectedInvoice.id)
      ]);
      
      setInvoiceItems(itemsResponse.items);
      setSelectedInvoice(invoiceResponse.invoice);
      
      // Limpa apenas descrição e valor, mantém categoria e data
      setItemForm(prev => ({ 
        ...prev, 
        description: '', 
        amount: '' 
      }));
      
      setShowAddMore(true);
      setQuickAddMode(false);
      
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

  const handleAddMore = () => {
    setShowAddMore(false);
    setQuickAddMode(true);
  };

  const handleFinishAdding = () => {
    setShowAddMore(false);
    setQuickAddMode(false);
    setItemForm({ description: '', amount: '', categoryId: '', purchaseDate: '' });
  };

  const handleQuickCategorySelect = (categoryId: string) => {
    setItemForm(prev => ({ ...prev, categoryId }));
  };

  const handleQuickAmountSelect = (amount: string) => {
    setItemForm(prev => ({ ...prev, amount }));
  };

  const handleQuickDateSelect = (date: string) => {
    setItemForm(prev => ({ ...prev, purchaseDate: date }));
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

  const handleDeleteInvoice = async (invoiceId: number) => {
    if (!window.confirm(t('invoices.confirmDeleteInvoice') || 'Tem certeza que deseja excluir esta fatura?')) return;
    try {
      await apiService.deleteInvoice(invoiceId);
      await loadInvoices();
    } catch (err) {
      alert(t('invoices.failedToDeleteInvoice') || 'Erro ao excluir fatura');
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

      {/* Grouped Invoices */}
      <div className="invoices-groups">
        {/* Overdue Invoices */}
        {groupedInvoices.overdue.length > 0 && (
          <div className="invoice-group overdue">
            <div className="group-header">
              <h2>⚠️ {t('invoices.groups.overdue')} ({groupedInvoices.overdue.length})</h2>
              <div className="group-summary">
                {formatCurrency(groupedInvoices.overdue.reduce((sum, inv) => sum + getInvoiceUserAmount(inv), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.overdue.map(invoice => (
                <div key={invoice.id} className="invoice-card overdue">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
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
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.remainingAmountLabel')}:</span>
                      <span className="value remaining">{formatCurrency(getInvoiceUserAmount(invoice) - (invoice.paidAmount || 0))}</span>
                    </div>
                    
                    <div className="urgency-badge">
                      {getUrgencyText(invoice.dueDate, invoice.status, t)}
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
                    {user?.roles?.includes('ADMIN') && (
                      <button 
                        onClick={() => handleDeleteInvoice(invoice.id)}
                        className="delete-invoice-btn"
                      >
                        Excluir Fatura
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
                {formatCurrency(groupedInvoices.open.reduce((sum, inv) => sum + getInvoiceUserAmount(inv), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.open.map(invoice => (
                <div key={invoice.id} className="invoice-card open">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
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
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.remainingAmountLabel')}:</span>
                      <span className="value remaining">{formatCurrency(getInvoiceUserAmount(invoice) - (invoice.paidAmount || 0))}</span>
                    </div>
                    
                    <div className="urgency-badge">
                      {getUrgencyText(invoice.dueDate, invoice.status, t)}
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
                    {user?.roles?.includes('ADMIN') && (
                      <button 
                        onClick={() => handleDeleteInvoice(invoice.id)}
                        className="delete-invoice-btn"
                      >
                        Excluir Fatura
                      </button>
                    )}
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
                {formatCurrency(groupedInvoices.partial.reduce((sum, inv) => sum + getInvoiceUserAmount(inv), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.partial.map(invoice => (
                <div key={invoice.id} className="invoice-card partial">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
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
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.remainingAmountLabel')}:</span>
                      <span className="value remaining">{formatCurrency(getInvoiceUserAmount(invoice) - (invoice.paidAmount || 0))}</span>
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
                    {user?.roles?.includes('ADMIN') && (
                      <button 
                        onClick={() => handleDeleteInvoice(invoice.id)}
                        className="delete-invoice-btn"
                      >
                        Excluir Fatura
                      </button>
                    )}
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
                {formatCurrency(groupedInvoices.paid.reduce((sum, inv) => sum + getInvoiceUserAmount(inv), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.paid.map(invoice => (
                <div key={invoice.id} className="invoice-card paid">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
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
                    {user?.roles?.includes('ADMIN') && (
                      <button 
                        onClick={() => handleDeleteInvoice(invoice.id)}
                        className="delete-invoice-btn"
                      >
                        Excluir Fatura
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Closed Invoices */}
        {groupedInvoices.closed.length > 0 && (
          <div className="invoice-group closed">
            <div className="group-header">
              <h2>✅ Faturas Fechadas ({groupedInvoices.closed.length})</h2>
              <div className="group-summary">
                {formatCurrency(groupedInvoices.closed.reduce((sum, inv) => sum + getInvoiceUserAmount(inv), 0))}
              </div>
            </div>
            <div className="invoices-grid">
              {groupedInvoices.closed.map(invoice => (
                <div key={invoice.id} className="invoice-card closed">
                  <div className="invoice-header">
                    <h3>{invoice.creditCardName}</h3>
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
                      <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                    </div>
                    
                    <div className="detail-item">
                      <span className="label">{t('invoices.remainingAmountLabel')}:</span>
                      <span className="value remaining">{formatCurrency(getInvoiceUserAmount(invoice) - (invoice.paidAmount || 0))}</span>
                    </div>
                    
                    <div className="urgency-badge">
                      {getUrgencyText(invoice.dueDate, invoice.status, t)}
                    </div>
                  </div>

                  <div className="invoice-actions">
                    <button 
                      onClick={() => handleViewDetails(invoice)}
                      className="view-button"
                    >
                      {t('invoices.viewButton')}
                    </button>
                    {user?.roles?.includes('ADMIN') && (
                      <button 
                        onClick={() => handleDeleteInvoice(invoice.id)}
                        className="delete-invoice-btn"
                      >
                        Excluir Fatura
                      </button>
                    )}
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
                  <span className={`value status ${getStatusColor(selectedInvoice.status)}`}>{getStatusText(selectedInvoice.status, t)}</span>
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

              <div className="invoice-items-section">
                <div className="section-header">
                  <h3>{t('invoices.itemsLabel')}</h3>
                  {/* Botão sempre visível para todas as faturas, incluindo fechadas */}
                  {!quickAddMode && selectedInvoice && (
                    <button 
                      className="quick-add-button"
                      onClick={handleQuickAdd}
                    >
                      + {t('invoices.addItem')}
                    </button>
                  )}
                </div>

                {loadingItems ? (
                  <div className="loading">{t('invoices.loadingItems')}</div>
                ) : (
                  <>
                    {invoiceItems.length === 0 ? (
                      <p className="no-items">{t('invoices.noItems')}</p>
                    ) : (
                      <>
                        <div className="items-header">
                          <div className="header-description">{t('invoices.descriptionLabel')}</div>
                          <div className="header-category">{t('invoices.categoryLabel')}</div>
                          <div className="header-date">{t('invoices.purchaseDateLabel')}</div>
                          <div className="header-amount">{t('invoices.amountLabel')}</div>
                          <div className="header-installments">{t('invoiceItems.installments')}</div>
                          <div className="header-actions">{t('invoices.actionsLabel')}</div>
                        </div>
                        <div className="items-list">
                          {invoiceItems.map(item => (
                            <div key={item.id} className="item-row">
                              <div className="item-description">
                                {item.description}
                                {item.isShared && (
                                  <span className="shared-indicator" title={t('invoices.itemShared')}>
                                    👥
                                  </span>
                                )}
                              </div>
                              <div className="item-category">{item.category || t('invoices.noCategory')}</div>
                              <div className="item-date">{formatDate(item.purchaseDate)}</div>
                              <div className="item-amount">{formatCurrency(item.amount)}</div>
                              <div className="item-installments">
                                {item.totalInstallments && item.totalInstallments > 1
                                  ? `${item.installments}/${item.totalInstallments}`
                                  : t('invoiceItems.singlePayment')}
                              </div>
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
                      </>
                    )}

                    {quickAddMode && (
                      <div className="compact-item-form">
                        <div className="form-header">
                          <h4>{t('invoices.addItem')}</h4>
                          <button 
                            className="close-form-button"
                            onClick={handleFinishAdding}
                          >
                            ×
                          </button>
                        </div>

                        {/* Atalhos de Categorias */}
                        <div className="quick-shortcuts">
                          <div className="shortcut-group">
                            <label>Categorias:</label>
                            <div className="shortcut-buttons">
                              {categories.slice(0, 5).map(category => (
                                <button
                                  key={category.id}
                                  type="button"
                                  className={`shortcut-btn ${itemForm.categoryId === category.id.toString() ? 'active' : ''}`}
                                  onClick={() => handleQuickCategorySelect(category.id.toString())}
                                >
                                  {category.name}
                                </button>
                              ))}
                            </div>
                          </div>

                          <div className="shortcut-group">
                            <label>Valores comuns:</label>
                            <div className="shortcut-buttons">
                              {['10', '20', '50', '100', '200'].map(amount => (
                                <button
                                  key={amount}
                                  type="button"
                                  className={`shortcut-btn ${itemForm.amount === amount ? 'active' : ''}`}
                                  onClick={() => handleQuickAmountSelect(amount)}
                                >
                                  R$ {amount}
                                </button>
                              ))}
                            </div>
                          </div>

                          <div className="shortcut-group">
                            <label>Data:</label>
                            <div className="shortcut-buttons">
                              <button
                                type="button"
                                className={`shortcut-btn ${itemForm.purchaseDate === new Date().toISOString().split('T')[0] ? 'active' : ''}`}
                                onClick={() => handleQuickDateSelect(new Date().toISOString().split('T')[0])}
                              >
                                Hoje
                              </button>
                              <button
                                type="button"
                                className={`shortcut-btn ${itemForm.purchaseDate === new Date(Date.now() - 24*60*60*1000).toISOString().split('T')[0] ? 'active' : ''}`}
                                onClick={() => handleQuickDateSelect(new Date(Date.now() - 24*60*60*1000).toISOString().split('T')[0])}
                              >
                                Ontem
                              </button>
                            </div>
                          </div>
                        </div>

                        {/* Formulário Compacto */}
                        <form onSubmit={handleAddItem} className="compact-form">
                          <div className="form-row-compact">
                            <div className="form-group-compact">
                              <input
                                type="text"
                                name="description"
                                value={itemForm.description}
                                onChange={handleItemInputChange}
                                placeholder={t('invoices.descriptionPlaceholder')}
                                required
                                className="description-input"
                              />
                            </div>
                            <div className="form-group-compact">
                              <input
                                type="number"
                                name="amount"
                                value={itemForm.amount}
                                onChange={handleItemInputChange}
                                placeholder="R$ 0,00"
                                step="0.01"
                                min="0"
                                required
                                className="amount-input"
                              />
                            </div>
                            <div className="form-group-compact">
                              <select
                                name="categoryId"
                                value={itemForm.categoryId}
                                onChange={handleItemInputChange}
                                className="category-select"
                              >
                                <option value="">{t('invoices.selectCategory')}</option>
                                {categories.map(category => (
                                  <option key={category.id} value={category.id}>
                                    {category.name}
                                  </option>
                                ))}
                              </select>
                            </div>
                            <div className="form-group-compact">
                              <input
                                type="date"
                                name="purchaseDate"
                                value={itemForm.purchaseDate}
                                onChange={handleItemInputChange}
                                className="date-input"
                              />
                            </div>
                            <button type="submit" className="add-button-compact" disabled={addingItem}>
                              {addingItem ? '...' : '+'}
                            </button>
                          </div>
                          {itemError && <div className="error-message">{itemError}</div>}
                        </form>
                      </div>
                    )}

                    {showAddMore && (
                      <div className="add-more-section">
                        <p>Item adicionado com sucesso!</p>
                        <div className="add-more-buttons">
                          <button 
                            className="add-more-button"
                            onClick={handleAddMore}
                          >
                            + Adicionar outro item
                          </button>
                          <button 
                            className="finish-button"
                            onClick={handleFinishAdding}
                          >
                            Finalizar
                          </button>
                        </div>
                      </div>
                    )}
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
                <p><strong>{t('invoices.totalAmountLabel')}:</strong> {formatInvoiceAmount(invoiceToPay)}</p>
                <p><strong>{t('invoices.paidAmountLabel')}:</strong> {formatCurrency(invoiceToPay.paidAmount)}</p>
                <p><strong>{t('invoices.remainingAmountLabel')}:</strong> {formatCurrency(getInvoiceUserAmount(invoiceToPay) - (invoiceToPay.paidAmount || 0))}</p>
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