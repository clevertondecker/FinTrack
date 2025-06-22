import React, { useState, useEffect } from 'react';
import apiService from '../services/api';
import { Invoice, CreateInvoiceRequest, InvoiceItem, Category } from '../types/invoice';
import { CreditCard } from '../types/creditCard';
import ShareItemModal from './ShareItemModal';
import './Invoices.css';

const Invoices: React.FC = () => {
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
      setError('Failed to load invoices');
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
      setError('Please fill in all fields correctly');
      return;
    }

    try {
      await apiService.createInvoice(formData);
      
      setFormData({ creditCardId: 0, dueDate: '' });
      setShowCreateForm(false);
      await loadInvoices();
      setError(null);
    } catch (err) {
      setError('Failed to create invoice');
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
    if (!dateString) return '-';
    try {
      // Tenta diferentes formatos de data
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        // Se não conseguir parsear, retorna a string original
        return dateString;
      }
      return date.toLocaleDateString('pt-BR');
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
        return 'Paga';
      case 'OVERDUE':
        return 'Vencida';
      case 'PARTIAL':
        return 'Parcial';
      default:
        return 'Aberta';
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
      const response = await apiService.getInvoiceItems(selectedInvoice.id);
      setInvoiceItems(response.items);
    } catch (err) {
      console.error('Error reloading invoice items:', err);
    }
  };

  if (loading) {
    return <div className="loading">Loading invoices...</div>;
  }

  return (
    <div className="invoices-container">
      <header className="invoices-header">
        <h1>Invoices</h1>
        <button 
          className="add-button"
          onClick={() => setShowCreateForm(true)}
        >
          Add New Invoice
        </button>
      </header>

      {error && <div className="error-message">{error}</div>}

      {showCreateForm && (
        <div className="form-overlay">
          <div className="form-container">
            <h2>Add New Invoice</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="creditCardId">Credit Card</label>
                <select
                  id="creditCardId"
                  name="creditCardId"
                  value={formData.creditCardId}
                  onChange={handleInputChange}
                  required
                >
                  <option value={0}>Select a credit card</option>
                  {creditCards.map(card => (
                    <option key={card.id} value={card.id}>
                      {card.name} - **** {card.lastFourDigits}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="dueDate">Due Date</label>
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
                  Create Invoice
                </button>
                <button type="button" onClick={handleCancel} className="cancel-button">
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="invoices-grid">
        {invoices.length === 0 ? (
          <div className="empty-state">
            <p>No invoices found. Add your first invoice to get started!</p>
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
                  <span className="label">Due Date:</span>
                  <span className="value">{formatDate(invoice.dueDate)}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Total Amount:</span>
                  <span className="value total">{formatCurrency(invoice.totalAmount)}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Paid Amount:</span>
                  <span className="value paid">{formatCurrency(invoice.paidAmount)}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Remaining:</span>
                  <span className="value remaining">{formatCurrency((invoice.totalAmount || 0) - (invoice.paidAmount || 0))}</span>
                </div>
                
                <div className="detail-item">
                  <span className="label">Created:</span>
                  <span className="value">{formatDate(invoice.createdAt)}</span>
                </div>
              </div>

              <div className="invoice-actions">
                <button 
                  onClick={() => handleViewDetails(invoice)}
                  className="view-button"
                >
                  View Details
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {showDetailsModal && selectedInvoice && (
        <div className="modal-overlay">
          <div className="modal-container">
            <button className="close-modal" onClick={handleCloseDetails}>&times;</button>
            <h2>Detalhes da Fatura</h2>
            <div className="invoice-summary">
              <div><b>Cartão:</b> {selectedInvoice.creditCardName}</div>
              <div><b>Vencimento:</b> {formatDate(selectedInvoice.dueDate)}</div>
              <div><b>Status:</b> {getStatusText(selectedInvoice.status)}</div>
              <div><b>Total:</b> {formatCurrency(selectedInvoice.totalAmount)}</div>
              <div><b>Pago:</b> {formatCurrency(selectedInvoice.paidAmount)}</div>
              <div><b>Restante:</b> {formatCurrency((selectedInvoice.totalAmount || 0) - (selectedInvoice.paidAmount || 0))}</div>
            </div>
            <h3>Itens da Fatura</h3>
            <form className="add-item-form" onSubmit={handleAddItem}>
              <input
                type="text"
                name="description"
                placeholder="Descrição"
                value={itemForm.description}
                onChange={handleItemInputChange}
                required
              />
              <input
                type="number"
                name="amount"
                placeholder="Valor"
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
                <option value="">Selecione uma categoria</option>
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
                {addingItem ? 'Adicionando...' : 'Adicionar'}
              </button>
            </form>
            {itemError && <div className="error-message">{itemError}</div>}
            {loadingItems ? (
              <div>Carregando itens...</div>
            ) : invoiceItems.length === 0 ? (
              <div>Nenhum item nesta fatura.</div>
            ) : (
              <div className="table-wrapper">
                <table className="invoice-items-table">
                  <thead>
                    <tr>
                      <th>Descrição</th>
                      <th>Valor</th>
                      <th>Categoria</th>
                      <th>Data de Compra</th>
                      <th>Ações</th>
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
                            title="Dividir item"
                          >
                            Dividir
                          </button>
                          <button
                            className="remove-item-btn"
                            onClick={() => handleRemoveItem(item.id)}
                            disabled={removingItemId === item.id}
                            title="Remover item"
                          >
                            {removingItemId === item.id ? 'Removendo...' : 'Remover'}
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
    </div>
  );
};

export default Invoices; 