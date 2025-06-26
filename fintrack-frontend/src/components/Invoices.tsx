import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { Invoice, CreateInvoiceRequest, InvoiceItem, Category, InvoicePaymentRequest, InvoicePaymentResponse } from '../types/invoice';
import { CreditCard } from '../types/creditCard';
import ShareItemModal from './ShareItemModal';
import './Invoices.css';

const Invoices: React.FC = () => {
  const { t } = useTranslation();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [invoiceItems, setInvoiceItems] = useState<InvoiceItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);
  const [selectedItemForSharing, setSelectedItemForSharing] = useState<InvoiceItem | null>(null);
  const [showPayModal, setShowPayModal] = useState(false);
  const [payAmount, setPayAmount] = useState('');
  const [payError, setPayError] = useState<string | null>(null);
  const [paying, setPaying] = useState(false);
  const [invoiceToPay, setInvoiceToPay] = useState<Invoice | null>(null);

  const [formData, setFormData] = useState<CreateInvoiceRequest>({
    creditCardId: 0,
    dueDate: ''
  });

  const [categories, setCategories] = useState<Category[]>([]);
  const [itemForm, setItemForm] = useState({ 
    description: '', 
    amount: '', 
    categoryId: '', 
    purchaseDate: '' 
  });
  const [itemError, setItemError] = useState<string | null>(null);
  const [addingItem, setAddingItem] = useState(false);
  const [removingItemId, setRemovingItemId] = useState<number | null>(null);

  useEffect(() => {
    loadInvoices();
    loadCreditCards();
    loadCategories();
    
    const token = localStorage.getItem('token');
    if (!token) {
      setError('No authentication token found. Please login again.');
    }
  }, []);

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
    setFormData(prev => ({
      ...prev,
      [name]: name === 'creditCardId' ? parseInt(value) || 0 : value
    }));
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
    if (amount === null || amount === undefined || isNaN(amount)) {
      return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
      }).format(0);
    }
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

  if (loading) {
    return <div className="loading">Loading invoices...</div>;
  }

  return (
    <div className="invoices-container">
      <header className="invoices-header">
        <h1>{t('invoices.title')}</h1>
        <button 
          className="add-button"
          onClick={() => setShowCreateForm(true)}
        >
          {t('invoices.addNewInvoice')}
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

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

      <div className="invoices-grid">
        {invoices.length === 0 ? (
          <div className="empty-state">
            <p>{t('invoices.emptyState')}</p>
          </div>
        ) : (
          invoices.map(invoice => (
            <div key={invoice.id} className="invoice-card">
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
                  {t('invoices.viewDetails')}
                </button>
                {(invoice.status === 'OPEN' || invoice.status === 'PARTIAL' || invoice.status === 'OVERDUE') && (
                  <button
                    onClick={() => handleOpenPayModal(invoice)}
                    className="pay-button"
                  >
                    {t('invoices.pay')}
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {showDetailsModal && selectedInvoice && (
        <div className="modal-overlay">
          <div className="modal-container">
            <button className="close-modal" onClick={handleCloseDetails}>&times;</button>
            <h2>{t('invoices.detailsModalTitle')}</h2>
            <div className="invoice-summary">
              <div><b>{t('invoices.creditCardLabel')}:</b> {selectedInvoice.creditCardName}</div>
              <div><b>{t('invoices.dueDateLabel')}:</b> {formatDate(selectedInvoice.dueDate)}</div>
              <div><b>{t('invoices.statusLabel')}:</b> {getStatusText(selectedInvoice.status)}</div>
              <div><b>{t('invoices.totalAmountLabel')}:</b> {formatCurrency(selectedInvoice.totalAmount)}</div>
              <div><b>{t('invoices.paidAmountLabel')}:</b> {formatCurrency(selectedInvoice.paidAmount)}</div>
              <div><b>{t('invoices.remainingAmountLabel')}:</b> {formatCurrency((selectedInvoice.totalAmount || 0) - (selectedInvoice.paidAmount || 0))}</div>
            </div>
            <h3>{t('invoices.itemsLabel')}</h3>
            <form className="add-item-form" onSubmit={handleAddItem}>
              <input
                type="text"
                name="description"
                placeholder={t('invoices.descriptionPlaceholder')}
                value={itemForm.description}
                onChange={handleItemInputChange}
                required
              />
              <input
                type="number"
                name="amount"
                placeholder={t('invoices.amountPlaceholder')}
                value={itemForm.amount}
                onChange={handleItemInputChange}
                min="0.01"
                step="0.01"
                required
              />
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
              <input
                type="date"
                name="purchaseDate"
                value={itemForm.purchaseDate}
                onChange={handleItemInputChange}
                required
              />
              <button type="submit" disabled={addingItem}>
                {addingItem ? t('invoices.adding') : t('invoices.addInvoice')}
              </button>
            </form>
            {itemError && <div className="error-message">{itemError}</div>}
            {loadingItems ? (
              <div>{t('invoices.loadingItems')}</div>
            ) : invoiceItems.length === 0 ? (
              <div>{t('invoices.noItems')}</div>
            ) : (
              <div className="table-wrapper">
                <table className="invoice-items-table">
                  <thead>
                    <tr>
                      <th>{t('invoiceItems.itemDescription')}</th>
                      <th>{t('common.amount')}</th>
                      <th>{t('invoiceItems.category')}</th>
                      <th>{t('invoiceItems.purchaseDate')}</th>
                      <th>{t('common.actions')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invoiceItems.map(item => (
                      <tr key={item.id}>
                        <td>{item.description}</td>
                        <td>{formatCurrency(item.amount)}</td>
                        <td>{item.category || '-'}</td>
                        <td>{formatDate(item.purchaseDate)}</td>
                        <td className="item-actions">
                          <button
                            className="share-item-btn"
                            onClick={() => handleShareItem(item)}
                            title={t('invoices.shareItem')}
                          >
                            {t('invoices.shareButton')}
                          </button>
                          <button
                            className="remove-item-btn"
                            onClick={() => handleRemoveItem(item.id)}
                            disabled={removingItemId === item.id}
                            title={t('invoices.removeItem')}
                          >
                            {removingItemId === item.id ? t('common.loading') : t('common.delete')}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {showShareModal && selectedItemForSharing && selectedInvoice && (
        <ShareItemModal
          isOpen={showShareModal}
          onClose={handleCloseShareModal}
          invoiceId={selectedInvoice.id}
          itemId={selectedItemForSharing.id}
          itemDescription={selectedItemForSharing.description}
          itemAmount={selectedItemForSharing.amount}
          onSharesUpdated={handleSharesUpdated}
        />
      )}

      {showPayModal && invoiceToPay && (
        <div className="modal-overlay">
          <div className="modal-container">
            <button className="close-modal" onClick={handleClosePayModal}>&times;</button>
            <h2>{t('invoices.payInvoice')}</h2>
            <form onSubmit={handlePayInvoice}>
              <div className="form-group">
                <label>{t('invoices.amountToPay')}</label>
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                  <input
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={payAmount}
                    onChange={e => setPayAmount(e.target.value)}
                    required
                    style={{ flex: 1 }}
                  />
                  <button 
                    type="button" 
                    onClick={handlePayTotal}
                    style={{ 
                      padding: '8px 12px', 
                      backgroundColor: '#28a745', 
                      color: 'white', 
                      border: 'none', 
                      borderRadius: '4px',
                      cursor: 'pointer'
                    }}
                  >
                    {t('common.total')}
                  </button>
                </div>
              </div>
              {payError && <div className="error-message">{payError}</div>}
              <div className="form-actions">
                <button type="submit" className="submit-button" disabled={paying}>
                  {paying ? t('invoices.paying') : t('invoices.pay')}
                </button>
                <button type="button" onClick={handleClosePayModal} className="cancel-button">
                  {t('common.cancel')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Invoices; 