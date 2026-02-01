import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { CreditCard, CreateCreditCardRequest, Bank, CreditCardGroup } from '../types/creditCard';
import './CreditCards.css';

const CreditCards: React.FC = () => {
  const { t } = useTranslation();
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [groupedCards, setGroupedCards] = useState<CreditCardGroup[]>([]);
  const [banks, setBanks] = useState<Bank[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingCard, setEditingCard] = useState<CreditCard | null>(null);
  const [showInactive, setShowInactive] = useState(false);

  const [formData, setFormData] = useState<CreateCreditCardRequest>({
    name: '',
    lastFourDigits: '',
    limit: 0,
    bankId: 0,
    cardType: 'PHYSICAL',
    parentCardId: undefined,
    cardholderName: ''
  });

  useEffect(() => {
    loadCreditCards();
    loadBanks();
    
    const token = localStorage.getItem('token');
    if (!token) {
      setError(t('common.noAuthToken'));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [t, showInactive]);

  const loadCreditCards = async () => {
    try {
      setLoading(true);
      const response = await apiService.getCreditCards(showInactive);
      setCreditCards(response.creditCards);
      setGroupedCards(response.groupedCards || []);
      setError(null);
    } catch (err) {
      setError(t('creditCards.failedToLoad'));
      console.error('Error loading credit cards:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadBanks = async () => {
    try {
      const banksData = await apiService.getBanks();
      setBanks(banksData);
    } catch (err) {
      console.error('Error loading banks:', err);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'limit' ? parseFloat(value) || 0 : value
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.name || !formData.lastFourDigits || formData.limit <= 0 || formData.bankId === 0) {
      setError(t('creditCards.pleaseFillFields'));
      return;
    }

    try {
      if (editingCard) {
        await apiService.updateCreditCard(editingCard.id, formData);
      } else {
        await apiService.createCreditCard(formData);
      }
      
      setFormData({ name: '', lastFourDigits: '', limit: 0, bankId: 0, cardType: 'PHYSICAL', parentCardId: undefined, cardholderName: '' });
      setShowCreateForm(false);
      setEditingCard(null);
      await loadCreditCards();
      setError(null);
    } catch (err) {
      setError(t('creditCards.failedToSave'));
      console.error('Error saving credit card:', err);
    }
  };

  const handleEdit = (card: CreditCard) => {
    setEditingCard(card);
    setFormData({
      name: card.name,
      lastFourDigits: card.lastFourDigits,
      limit: card.limit,
      bankId: banks.find(b => b.name === card.bankName)?.id || 0,
      cardType: card.cardType,
      parentCardId: card.parentCardId,
      cardholderName: card.cardholderName || ''
    });
    setShowCreateForm(true);
  };

  const handleDelete = async (id: number) => {
    if (window.confirm(t('creditCards.confirmDeactivate'))) {
      try {
        await apiService.deleteCreditCard(id);
        await loadCreditCards();
        setError(null);
      } catch (err) {
        setError(t('creditCards.failedToDeactivate'));
        console.error('Error deleting credit card:', err);
      }
    }
  };

  const handleActivate = async (id: number) => {
    if (window.confirm(t('creditCards.confirmActivate'))) {
      try {
        await apiService.activateCreditCard(id);
        await loadCreditCards();
        setError(null);
      } catch (err) {
        setError(t('creditCards.failedToActivate'));
        console.error('Error activating credit card:', err);
      }
    }
  };

  const handleCancel = () => {
    setShowCreateForm(false);
    setEditingCard(null);
    setFormData({ name: '', lastFourDigits: '', limit: 0, bankId: 0, cardType: 'PHYSICAL', parentCardId: undefined, cardholderName: '' });
    setError(null);
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(amount);
  };

  if (loading) {
    return <div className="loading">{t('creditCards.loading')}</div>;
  }

  return (
    <div className="credit-cards-container">
      <header className="credit-cards-header">
        <h1>{t('creditCards.title')}</h1>
        <div className="header-actions">
          <label className="show-inactive-toggle">
            <input
              type="checkbox"
              checked={showInactive}
              onChange={(e) => setShowInactive(e.target.checked)}
            />
            {t('creditCards.showInactive')}
          </label>
          <button 
            className="add-button"
            onClick={() => setShowCreateForm(true)}
          >
            {t('creditCards.addCard')}
          </button>
        </div>
      </header>

      {error && <div className="error-message">{error}</div>}

      {showCreateForm && (
        <div className="form-overlay">
          <div className="form-container">
            <h2>{editingCard ? t('creditCards.editCard') : t('creditCards.addNewCard')}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label htmlFor="name">{t('creditCards.cardName')}</label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder={t('creditCards.cardNamePlaceholder')}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="lastFourDigits">{t('creditCards.lastFourDigits')}</label>
                <input
                  type="text"
                  id="lastFourDigits"
                  name="lastFourDigits"
                  value={formData.lastFourDigits}
                  onChange={handleInputChange}
                  placeholder="1234"
                  maxLength={4}
                  pattern="[0-9]{4}"
                  required
                  disabled={!!editingCard}
                />
              </div>

              <div className="form-group">
                <label htmlFor="limit">{t('creditCards.creditLimit')}</label>
                <input
                  type="number"
                  id="limit"
                  name="limit"
                  value={formData.limit}
                  onChange={handleInputChange}
                  placeholder="5000.00"
                  step="0.01"
                  min="0"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="bankId">{t('creditCards.bank')}</label>
                <select
                  id="bankId"
                  name="bankId"
                  value={formData.bankId}
                  onChange={handleInputChange}
                  required
                >
                  <option value={0}>{t('creditCards.selectBank')}</option>
                  {banks.map(bank => (
                    <option key={bank.id} value={bank.id}>
                      {bank.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="cardType">{t('creditCards.cardType')}</label>
                <select
                  id="cardType"
                  name="cardType"
                  value={formData.cardType}
                  onChange={handleInputChange}
                  required
                >
                  <option value="PHYSICAL">{t('creditCards.cardTypePhysical')}</option>
                  <option value="VIRTUAL">{t('creditCards.cardTypeVirtual')}</option>
                  <option value="ADDITIONAL">{t('creditCards.cardTypeAdditional')}</option>
                </select>
              </div>

              {(formData.cardType === 'VIRTUAL' || formData.cardType === 'ADDITIONAL') && (
                <div className="form-group">
                  <label htmlFor="parentCardId">{t('creditCards.parentCard')}</label>
                  <select
                    id="parentCardId"
                    name="parentCardId"
                    value={formData.parentCardId || ''}
                    onChange={handleInputChange}
                  >
                    <option value="">{t('creditCards.noParentCard')}</option>
                    {creditCards
                      .filter(card => card.cardType === 'PHYSICAL' && card.id !== editingCard?.id)
                      .map(card => (
                        <option key={card.id} value={card.id}>
                          {card.name} (**** {card.lastFourDigits})
                        </option>
                      ))}
                  </select>
                </div>
              )}

              <div className="form-group">
                <label htmlFor="cardholderName">{t('creditCards.cardholderName')}</label>
                <input
                  type="text"
                  id="cardholderName"
                  name="cardholderName"
                  value={formData.cardholderName}
                  onChange={handleInputChange}
                  placeholder={t('creditCards.cardholderNamePlaceholder')}
                />
              </div>

              <div className="form-actions">
                <button type="submit" className="submit-button">
                  {editingCard ? t('creditCards.updateCard') : t('creditCards.createCard')}
                </button>
                <button type="button" onClick={handleCancel} className="cancel-button">
                  {t('common.cancel')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="credit-cards-groups">
        {groupedCards.length === 0 ? (
          <div className="empty-state">
            <p>{t('creditCards.emptyState')}</p>
          </div>
        ) : (
          groupedCards.map(group => (
            <div key={group.parentCard.id} className="credit-card-group">
              <div className="group-header">
                <h2>{group.parentCard.bankName} - {group.parentCard.name}</h2>
                <span className="group-limit">{t('creditCards.limitLabel')}: {formatCurrency(group.parentCard.limit)}</span>
              </div>

              <div className="sub-cards-list">
                {/* Render Parent Card as first item */}
                <div className={`sub-card-item ${!group.parentCard.active ? 'inactive' : ''}`}>
                  <div className="sub-card-info">
                    <span className="sub-card-name">
                      {group.parentCard.name} (Titular)
                      {!group.parentCard.active && <span className="inactive-badge">{t('creditCards.inactive')}</span>}
                    </span>
                    <span className="sub-card-meta">**** {group.parentCard.lastFourDigits} | {t('creditCards.cardTypePhysical')}</span>
                  </div>
                  <div className="sub-card-actions">
                    <button onClick={() => handleEdit(group.parentCard)} className="mini-button edit">{t('common.edit')}</button>
                    {group.parentCard.active ? (
                      <button onClick={() => handleDelete(group.parentCard.id)} className="mini-button delete">{t('creditCards.deactivate')}</button>
                    ) : (
                      <button onClick={() => handleActivate(group.parentCard.id)} className="mini-button activate">{t('creditCards.activate')}</button>
                    )}
                  </div>
                </div>

                {/* Render Sub Cards */}
                {group.subCards.map(subCard => (
                  <div key={subCard.id} className={`sub-card-item ${!subCard.active ? 'inactive' : ''}`}>
                    <div className="sub-card-info">
                      <span className="sub-card-name">
                        {subCard.name}
                        {!subCard.active && <span className="inactive-badge">{t('creditCards.inactive')}</span>}
                      </span>
                      <span className="sub-card-meta">
                        **** {subCard.lastFourDigits} | 
                        {subCard.cardType === 'VIRTUAL' ? t('creditCards.cardTypeVirtual') : t('creditCards.cardTypeAdditional')}
                        {subCard.cardholderName ? ` | ${subCard.cardholderName}` : ''}
                      </span>
                    </div>
                    <div className="sub-card-actions">
                      <button onClick={() => handleEdit(subCard)} className="mini-button edit">{t('common.edit')}</button>
                      {subCard.active ? (
                        <button onClick={() => handleDelete(subCard.id)} className="mini-button delete">{t('creditCards.deactivate')}</button>
                      ) : (
                        <button onClick={() => handleActivate(subCard.id)} className="mini-button activate">{t('creditCards.activate')}</button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default CreditCards; 