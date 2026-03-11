import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { FileText, RefreshCw } from 'lucide-react';
import apiService from '../services/api';
import { Invoice, InvoiceItem, CreateInvoiceRequest, InvoiceFilters, InvoiceSummary, Category, InvoiceDeleteInfo } from '../types/invoice';
import { CreditCard } from '../types/creditCard';
import { getStatusColor, getStatusText, getUrgencyText, formatCurrency, formatDate, sortInvoices, groupInvoices, consolidateInvoices, calculateSummary } from '../utils/invoiceUtils';
import ShareItemModal from './ShareItemModal';
import InvoiceItemRow from './invoices/InvoiceItemRow';
import ItemsTableHeader from './invoices/ItemsTableHeader';
import Subscriptions from './Subscriptions';
import './Invoices.css';
import { useAuth } from '../contexts/AuthContext';

type InvoiceTab = 'invoices' | 'subscriptions';

const Invoices: React.FC = () => {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<InvoiceTab>('invoices');
  
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
  const [updatingCategoryItemId, setUpdatingCategoryItemId] = useState<number | null>(null);
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

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [invoiceToDelete, setInvoiceToDelete] = useState<Invoice | null>(null);
  const [deleteInfo, setDeleteInfo] = useState<InvoiceDeleteInfo | null>(null);
  const [loadingDeleteInfo, setLoadingDeleteInfo] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const [showAddMore, setShowAddMore] = useState(false);
  const [quickAddMode, setQuickAddMode] = useState(false);
  const [isConsolidatedView, setIsConsolidatedView] = useState(false);
  const [cardItemGroups, setCardItemGroups] = useState<{invoice: Invoice, items: InvoiceItem[]}[]>([]);
  const [activeCardTab, setActiveCardTab] = useState<number>(0);

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

  // Calculate user's proportional paid amount for shared invoices
  const getInvoiceUserPaid = (invoice: Invoice): number => {
    const userAmount = getInvoiceUserAmount(invoice);
    const totalInvoiceAmount = invoice.totalAmount || 0;
    const totalPaid = invoice.paidAmount || 0;
    
    if (totalInvoiceAmount > 0 && userAmount !== totalInvoiceAmount) {
      // Shared invoice: calculate user's proportional share of the payment
      const paidPercentage = Math.min(totalPaid / totalInvoiceAmount, 1);
      return userAmount * paidPercentage;
    }
    // Not shared: user's paid amount is the invoice paid amount (capped at user amount)
    return Math.min(totalPaid, userAmount);
  };

  // Calculate user's remaining amount
  const getInvoiceUserRemaining = (invoice: Invoice): number => {
    return Math.max(0, getInvoiceUserAmount(invoice) - getInvoiceUserPaid(invoice));
  };

  useEffect(() => {
    loadInvoices();
    loadCreditCards();
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    applyFilters();
    // eslint-disable-next-line react-hooks/exhaustive-deps
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

  const updateInvoiceInList = (updated: Invoice) => {
    setInvoices(prev => prev.map(inv => inv.id === updated.id ? updated : inv));
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
      
      const uniqueByName = new Map<string, Category>();
      response.categories.forEach(category => {
        if (!uniqueByName.has(category.name)) {
          uniqueByName.set(category.name, category);
        }
      });
      
      const uniqueCategories = Array.from(uniqueByName.values());
      setCategories(uniqueCategories);
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

    const consolidated = consolidateInvoices(filtered);
    const sorted = sortInvoices(consolidated);
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
    setIsConsolidatedView(false);
    setCardItemGroups([]);
    setActiveCardTab(0);
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

  const handleViewConsolidatedDetails = async (invoice: Invoice) => {
    const cards = getConsolidatedCards(invoice);
    if (!cards || cards.length === 0) {
      handleViewDetails(invoice);
      return;
    }

    setIsConsolidatedView(true);
    setActiveCardTab(0);
    setSelectedInvoice(invoice);
    setShowDetailsModal(true);
    setLoadingItems(true);
    setInvoiceItems([]);

    try {
      const results = await Promise.all(
        cards.map(async (sub) => {
          try {
            const response = await apiService.getInvoiceItems(sub.id);
            return { invoice: sub, items: response.items };
          } catch {
            return { invoice: sub, items: [] };
          }
        })
      );
      setCardItemGroups(results);

      const allItems = results.flatMap(r => r.items);
      setInvoiceItems(allItems);
    } catch {
      setCardItemGroups([]);
      setInvoiceItems([]);
    } finally {
      setLoadingItems(false);
    }
  };

  const handleCloseDetails = () => {
    setShowDetailsModal(false);
    setSelectedInvoice(null);
    setInvoiceItems([]);
    setIsConsolidatedView(false);
    setCardItemGroups([]);
    setActiveCardTab(0);
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
      updateInvoiceInList(invoiceResponse.invoice);

      // Limpa apenas descrição e valor, mantém categoria e data
      setItemForm(prev => ({
        ...prev,
        description: '',
        amount: ''
      }));

      setShowAddMore(true);
      setQuickAddMode(false);
      
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
      updateInvoiceInList(invoiceResponse.invoice);
      
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
      updateInvoiceInList(invoiceResponse.invoice);
      
    } catch (err) {
      console.error('Error reloading invoice items:', err);
    }
  };

  // Memoize category lookup for performance
  const categoryMap = useMemo(() => {
    return new Map(categories.map(category => [category.id, category.name]));
  }, [categories]);

  // Memoize sorted categories to prevent re-renders and duplicates
  const sortedCategories = useMemo(() => {
    // Remove duplicates by NAME (not ID) since we have duplicate names with different IDs
    const uniqueByName = new Map<string, Category>();
    categories.forEach(category => {
      if (!uniqueByName.has(category.name)) {
        uniqueByName.set(category.name, category);
      }
    });
    
    const uniqueCategories = Array.from(uniqueByName.values());
    return uniqueCategories.sort((a, b) => a.name.localeCompare(b.name));
  }, [categories]);

  const categoryLookup = useCallback((itemCategory: string | null): number | string => {
    if (!itemCategory) return '';
    const found = categories.find(c => c.name === itemCategory);
    return found ? found.id : '';
  }, [categories]);

  // Extract error message helper for better reusability
  const extractErrorMessage = useCallback((err: any, defaultMessage: string): string => {
    return err.response?.data?.error || err.message || defaultMessage;
  }, []);

  const handleUpdateItemCategory = useCallback(async (itemId: number, categoryId: number | null) => {
    if (!selectedInvoice) return;
    
    setUpdatingCategoryItemId(itemId);
    setItemError(null);
    
    try {
      await apiService.updateInvoiceItemCategory(selectedInvoice.id, itemId, categoryId);
      
      // Optimistic update with memoized category lookup
      setInvoiceItems(prevItems => 
        prevItems.map(item => 
          item.id === itemId 
            ? { ...item, category: categoryId ? categoryMap.get(categoryId) || null : null }
            : item
        )
      );
      
    } catch (err: any) {
      const errorMessage = extractErrorMessage(err, t('invoices.errorUpdatingCategory'));
      setItemError(errorMessage);
      setTimeout(() => setItemError(null), 5000);
    } finally {
      setUpdatingCategoryItemId(null);
    }
  }, [selectedInvoice, categoryMap, t, extractErrorMessage]);

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
      const cards = invoiceToPay._consolidatedCards;
      if (cards && cards.length > 1) {
        // Consolidated: distribute payment proportionally across sub-invoices
        const totalRemaining = cards.reduce((s, c) => s + (c.totalAmount || 0) - (c.paidAmount || 0), 0);
        for (const card of cards) {
          const cardRemaining = (card.totalAmount || 0) - (card.paidAmount || 0);
          if (cardRemaining <= 0) continue;
          const cardPayment = Math.round((cardRemaining / totalRemaining) * amount * 100) / 100;
          if (cardPayment > 0) {
            await apiService.payInvoice(card.id, { amount: cardPayment });
          }
        }
      } else {
        await apiService.payInvoice(invoiceToPay.id, { amount });
      }
      await loadInvoices();
      handleClosePayModal();
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || t('invoices.errorPayingInvoice');
      setPayError(errorMessage);
    } finally {
      setPaying(false);
    }
  };

  const handleDeleteInvoice = async (invoice: Invoice) => {
    setInvoiceToDelete(invoice);
    setShowDeleteModal(true);
    setLoadingDeleteInfo(true);
    setDeleteInfo(null);
    try {
      const info = await apiService.getInvoiceDeleteInfo(invoice.id);
      setDeleteInfo(info);
    } catch {
      setDeleteInfo(null);
    } finally {
      setLoadingDeleteInfo(false);
    }
  };

  const handleConfirmDelete = async () => {
    if (!invoiceToDelete) return;
    setDeleting(true);
    try {
      await apiService.deleteInvoice(invoiceToDelete.id);
      setShowDeleteModal(false);
      setInvoiceToDelete(null);
      setDeleteInfo(null);
      await loadInvoices();
    } catch {
      setError(t('invoices.failedToDeleteInvoice') || 'Erro ao excluir fatura');
    } finally {
      setDeleting(false);
    }
  };

  const handleCloseDeleteModal = () => {
    setShowDeleteModal(false);
    setInvoiceToDelete(null);
    setDeleteInfo(null);
  };

  const getConsolidatedCards = (invoice: Invoice): Invoice[] | undefined => {
    return invoice._consolidatedCards;
  };

  const isConsolidated = (invoice: Invoice): boolean => {
    return !!invoice._consolidatedCards;
  };

  const groupedInvoices = groupInvoices(filteredInvoices);

  if (loading) {
    return <div className="loading">{t('invoices.loading')}</div>;
  }

  const renderInvoiceCard = (invoice: Invoice, statusClass: string) => {
    const consolidated = isConsolidated(invoice);
    const cards = getConsolidatedCards(invoice);
    return (
      <div key={invoice.importGroupId || invoice.id} className={`invoice-card ${statusClass}`}>
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

  return (
    <div className="invoices-container">
      {/* Tab Navigation */}
      <div className="invoices-tabs">
        <button
          className={`invoices-tab ${activeTab === 'invoices' ? 'active' : ''}`}
          onClick={() => setActiveTab('invoices')}
        >
          <FileText size={18} />
          {t('invoices.tabInvoices')}
        </button>
        <button
          className={`invoices-tab ${activeTab === 'subscriptions' ? 'active' : ''}`}
          onClick={() => setActiveTab('subscriptions')}
        >
          <RefreshCw size={18} />
          {t('invoices.tabSubscriptions')}
        </button>
      </div>

      {activeTab === 'subscriptions' && <Subscriptions />}

      {activeTab === 'invoices' && (
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
              {groupedInvoices.overdue.map(invoice => renderInvoiceCard(invoice, 'overdue'))}
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
              {groupedInvoices.open.map(invoice => renderInvoiceCard(invoice, 'open'))}
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
              {groupedInvoices.partial.map(invoice => renderInvoiceCard(invoice, 'partial'))}
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
              {groupedInvoices.paid.map(invoice => renderInvoiceCard(invoice, 'paid'))}
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
              {groupedInvoices.closed.map(invoice => renderInvoiceCard(invoice, 'closed'))}
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
          <div className="modal-container consolidated-modal">
            <div className="modal-header">
              <h2>{isConsolidatedView ? t('invoices.consolidatedDetailsTitle', 'Fatura Consolidada') : t('invoices.detailsModalTitle')}</h2>
              <button onClick={handleCloseDetails} className="close-button">&times;</button>
            </div>
            
            <div className="modal-content">
              <div className="invoice-info">
                {!isConsolidatedView && (
                  <div className="info-row">
                    <span className="label">{t('invoices.creditCardLabel')}:</span>
                    <span className="value">{selectedInvoice.creditCardName}</span>
                  </div>
                )}
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

              {selectedInvoice.contactShares && selectedInvoice.contactShares.length > 0 && (
                <div className="contact-shares-section">
                  <h4>{t('invoices.contactSharesTitle')}</h4>
                  {selectedInvoice.contactShares.map(cs => (
                    <div key={cs.contactEmail} className="contact-share-row">
                      <span className="contact-name">{cs.contactName}</span>
                      <span className="contact-amount">{formatCurrency(cs.totalAmount)}</span>
                    </div>
                  ))}
                </div>
              )}

              {isConsolidatedView && cardItemGroups.length > 0 ? (
                <div className="invoice-items-section">
                  <div className="section-header">
                    <h3>{t('invoices.itemsLabel')} ({invoiceItems.length})</h3>
                  </div>

                  {loadingItems ? (
                    <div className="loading">{t('invoices.loadingItems')}</div>
                  ) : (
                    <>
                      <div className="card-tabs">
                        <button
                          className={`card-tab ${activeCardTab === -1 ? 'active' : ''}`}
                          onClick={() => setActiveCardTab(-1)}
                        >
                          {t('invoices.allCards', 'Todos')} ({invoiceItems.length})
                        </button>
                        {cardItemGroups.map((group, idx) => (
                          <button
                            key={group.invoice.id}
                            className={`card-tab ${activeCardTab === idx ? 'active' : ''}`}
                            onClick={() => setActiveCardTab(idx)}
                          >
                            {group.invoice.creditCardName} ({group.items.length})
                          </button>
                        ))}
                      </div>

                      {(activeCardTab === -1 ? cardItemGroups : [cardItemGroups[activeCardTab]].filter(Boolean))
                        .map((group) => (
                          <div key={group.invoice.id} className={`card-section ${activeCardTab !== -1 ? 'active-tab-section' : ''}`}>
                            <div className="card-section-header">
                              <span className="card-section-name">{group.invoice.creditCardName}</span>
                              <span className="card-section-total">{formatCurrency(group.invoice.totalAmount)}</span>
                              <span className={`card-section-status ${getStatusColor(group.invoice.status)}`}>
                                {getStatusText(group.invoice.status, t)}
                              </span>
                            </div>
                            {group.items.length === 0 ? (
                              <p className="no-items">{t('invoices.noItems')}</p>
                            ) : (
                              <>
                                <ItemsTableHeader />
                                <div className="items-list">
                                  {group.items.map(item => (
                                    <InvoiceItemRow
                                      key={item.id}
                                      item={item}
                                      sortedCategories={sortedCategories}
                                      categoryLookup={categoryLookup}
                                      onCategoryChange={handleUpdateItemCategory}
                                      onShare={handleShareItem}
                                      updatingCategoryItemId={updatingCategoryItemId}
                                    />
                                  ))}
                                </div>
                              </>
                            )}
                          </div>
                        ))
                      }
                    </>
                  )}
                </div>
              ) : (
                <div className="invoice-items-section">
                  <div className="section-header">
                    <h3>{t('invoices.itemsLabel')}</h3>
                    {!quickAddMode && selectedInvoice && !isConsolidatedView && (
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
                          <ItemsTableHeader />
                          <div className="items-list">
                            {invoiceItems.map(item => (
                              <InvoiceItemRow
                                key={item.id}
                                item={item}
                                sortedCategories={sortedCategories}
                                categoryLookup={categoryLookup}
                                onCategoryChange={handleUpdateItemCategory}
                                onShare={handleShareItem}
                                onRemove={handleRemoveItem}
                                updatingCategoryItemId={updatingCategoryItemId}
                                removingItemId={removingItemId}
                              />
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
              )}
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
                <p><strong>{t('invoices.paidAmountLabel')}:</strong> {formatCurrency(getInvoiceUserPaid(invoiceToPay))}</p>
                <p><strong>{t('invoices.remainingAmountLabel')}:</strong> {formatCurrency(getInvoiceUserRemaining(invoiceToPay))}</p>
              </div>
              {invoiceToPay._consolidatedCards && invoiceToPay._consolidatedCards.length > 1 && (
                <div className="consolidated-pay-breakdown">
                  <p style={{ fontSize: '0.85rem', color: '#666', marginBottom: '0.5rem' }}>
                    {t('invoices.paymentDistribution', 'O pagamento será distribuído proporcionalmente:')}
                  </p>
                  {invoiceToPay._consolidatedCards.map(card => {
                    const remaining = (card.totalAmount || 0) - (card.paidAmount || 0);
                    return remaining > 0 ? (
                      <div key={card.id} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', padding: '2px 0' }}>
                        <span>{card.creditCardName}</span>
                        <span>{formatCurrency(remaining)}</span>
                      </div>
                    ) : null;
                  })}
                </div>
              )}

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

      {/* Delete Confirmation Modal */}
      {showDeleteModal && invoiceToDelete && (
        <div className="modal-overlay">
          <div className="modal-container" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h2>{t('invoices.deleteModalTitle', 'Excluir Fatura')}</h2>
              <button onClick={handleCloseDeleteModal} className="close-button">&times;</button>
            </div>
            <div className="modal-content">
              <div className="invoice-info" style={{ marginBottom: '1rem' }}>
                <div className="info-row">
                  <span className="label">{t('invoices.creditCardLabel')}:</span>
                  <span className="value">{invoiceToDelete.creditCardName}</span>
                </div>
                <div className="info-row">
                  <span className="label">{t('invoices.dueDateLabel')}:</span>
                  <span className="value">{formatDate(invoiceToDelete.dueDate)}</span>
                </div>
                <div className="info-row">
                  <span className="label">{t('invoices.totalAmountLabel')}:</span>
                  <span className="value">{formatCurrency(invoiceToDelete.totalAmount)}</span>
                </div>
              </div>

              {loadingDeleteInfo ? (
                <div className="loading" style={{ padding: '1rem 0' }}>{t('common.loading', 'Carregando...')}</div>
              ) : deleteInfo && deleteInfo.sharedItems > 0 ? (
                <div style={{
                  background: '#fef3c7',
                  border: '1px solid #f59e0b',
                  borderRadius: '8px',
                  padding: '1rem',
                  marginBottom: '1rem'
                }}>
                  <p style={{ fontWeight: 600, color: '#92400e', marginBottom: '0.5rem', fontSize: '0.95rem' }}>
                    {t('invoices.deleteWarningShared', 'Esta fatura contém itens compartilhados!')}
                  </p>
                  <ul style={{ margin: 0, paddingLeft: '1.25rem', color: '#78350f', fontSize: '0.9rem', lineHeight: '1.6' }}>
                    <li>
                      {t('invoices.deleteSharedItemsCount', {
                        count: deleteInfo.sharedItems,
                        total: deleteInfo.totalItems,
                        defaultValue: `${deleteInfo.sharedItems} de ${deleteInfo.totalItems} itens possuem compartilhamentos`
                      })}
                    </li>
                    <li>
                      {t('invoices.deleteSharesCount', {
                        count: deleteInfo.totalShares,
                        defaultValue: `${deleteInfo.totalShares} compartilhamento(s) serão removidos`
                      })}
                    </li>
                    {deleteInfo.paidShares > 0 && (
                      <li style={{ fontWeight: 600 }}>
                        {t('invoices.deletePaidSharesWarning', {
                          count: deleteInfo.paidShares,
                          defaultValue: `${deleteInfo.paidShares} compartilhamento(s) já foram marcados como pagos`
                        })}
                      </li>
                    )}
                  </ul>
                  <p style={{ color: '#92400e', fontSize: '0.85rem', marginTop: '0.75rem', marginBottom: 0 }}>
                    {t('invoices.deleteSharedConsequence', 'Os outros usuários perderão o acesso a esses itens e seu histórico de compartilhamento.')}
                  </p>
                </div>
              ) : deleteInfo ? (
                <p style={{ color: '#6b7280', fontSize: '0.9rem', marginBottom: '1rem' }}>
                  {t('invoices.deleteNoShares', {
                    count: deleteInfo.totalItems,
                    defaultValue: `Esta fatura possui ${deleteInfo.totalItems} item(ns) e nenhum compartilhamento.`
                  })}
                </p>
              ) : null}

              <p style={{ color: '#dc2626', fontWeight: 500, fontSize: '0.9rem', marginBottom: '1.25rem' }}>
                {t('invoices.deleteConfirmQuestion', 'Tem certeza que deseja excluir esta fatura? Esta ação não pode ser desfeita.')}
              </p>

              <div className="form-actions">
                <button
                  onClick={handleConfirmDelete}
                  className="submit-button"
                  disabled={deleting}
                  style={{ background: '#dc2626' }}
                >
                  {deleting
                    ? t('common.deleting', 'Excluindo...')
                    : t('invoices.confirmDelete', 'Sim, excluir fatura')}
                </button>
                <button
                  onClick={handleCloseDeleteModal}
                  className="cancel-button"
                  disabled={deleting}
                >
                  {t('common.cancel')}
                </button>
              </div>
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
      </>
      )}
    </div>
  );
};

export default Invoices; 