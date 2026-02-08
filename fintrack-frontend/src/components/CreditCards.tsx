import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { CreditCard, CreateCreditCardRequest, Bank, CreditCardGroup } from '../types/creditCard';
import { User } from '../types/user';
import './CreditCards.css';

const INITIAL_FORM_DATA: CreateCreditCardRequest = {
  name: '',
  lastFourDigits: '',
  limit: 0,
  bankId: 0,
  cardType: 'PHYSICAL',
  parentCardId: undefined,
  cardholderName: '',
  assignedUserId: undefined
};

function parseFormValue(name: string, value: string): string | number | undefined {
  if (name === 'limit') return parseFloat(value) || 0;
  if (name === 'assignedUserId') return value === '' ? undefined : Number(value);
  return value;
}

type ConnectErrorMessageFn = (key: string) => string;

function getConnectErrorMessage(err: unknown, t: ConnectErrorMessageFn): string {
  if (!err || typeof err !== 'object' || !('response' in err)) {
    return t('creditCards.connectUserErrorNotFound');
  }
  const res = (err as { response?: { data?: { error?: string; message?: string }; status?: number } }).response;
  const status = res?.status;
  const body = res?.data?.error ?? res?.data?.message ?? '';
  if (status === 404) return t('creditCards.connectUserErrorNotFound');
  if (status === 400 && body) {
    if (body.includes('yourself')) return t('creditCards.connectUserErrorSelf');
    if (body.includes('already connected')) return t('creditCards.connectUserErrorAlready');
    if (body.includes('Invalid')) return t('creditCards.connectUserErrorInvalid');
  }
  return t('creditCards.connectUserErrorNotFound');
}

const CreditCards: React.FC = () => {
  const { t } = useTranslation();
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [groupedCards, setGroupedCards] = useState<CreditCardGroup[]>([]);
  const [banks, setBanks] = useState<Bank[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingCard, setEditingCard] = useState<CreditCard | null>(null);
  const [showInactive, setShowInactive] = useState(false);
  const [formData, setFormData] = useState<CreateCreditCardRequest>(INITIAL_FORM_DATA);
  const [connectEmail, setConnectEmail] = useState('');
  const [connectMessage, setConnectMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [connectLoading, setConnectLoading] = useState(false);

  const loadCreditCards = useCallback(async () => {
    try {
      setLoading(true);
      const response = await apiService.getCreditCards(showInactive);
      setCreditCards(response.creditCards);
      setGroupedCards(response.groupedCards ?? []);
      setError(null);
    } catch (err) {
      setError(t('creditCards.failedToLoad'));
      console.error('Error loading credit cards:', err);
    } finally {
      setLoading(false);
    }
  }, [showInactive, t]);

  const loadBanks = useCallback(async () => {
    try {
      const data = await apiService.getBanks();
      setBanks(data);
    } catch (err) {
      console.error('Error loading banks:', err);
    }
  }, []);

  const loadUsers = useCallback(async () => {
    try {
      const response = await apiService.getUsers();
      setUsers(response.users ?? []);
    } catch (err) {
      console.error('Error loading users:', err);
    }
  }, []);

  useEffect(() => {
    loadCreditCards();
    loadBanks();
    loadUsers();
    const token = localStorage.getItem('token');
    if (!token) setError(t('common.noAuthToken'));
  }, [loadCreditCards, loadBanks, loadUsers, t]);

  const handleConnectUser = useCallback(async () => {
    const email = connectEmail.trim();
    if (!email) {
      setConnectMessage({ type: 'error', text: t('creditCards.connectUserErrorInvalid') });
      return;
    }
    setConnectLoading(true);
    setConnectMessage(null);
    try {
      await apiService.connectUser(email);
      setConnectMessage({ type: 'success', text: t('creditCards.connectUserSuccess') });
      setConnectEmail('');
      await loadUsers();
    } catch (err: unknown) {
      setConnectMessage({ type: 'error', text: getConnectErrorMessage(err, t) });
    } finally {
      setConnectLoading(false);
    }
  }, [connectEmail, loadUsers, t]);

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: parseFormValue(name, value) }));
  }, []);

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
      
      setFormData(INITIAL_FORM_DATA);
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
      cardholderName: card.cardholderName || '',
      assignedUserId: card.assignedUserId ?? undefined
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

  const handleCancel = useCallback(() => {
    setShowCreateForm(false);
    setEditingCard(null);
    setFormData(INITIAL_FORM_DATA);
    setError(null);
  }, []);

  const formatCurrency = useCallback((amount: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount);
  }, []);

  const displayGroups = useMemo((): CreditCardGroup[] => {
    if (groupedCards.length > 0) return groupedCards;
    if (creditCards.length === 0) return [];
    const byParent = new Map<number | null, CreditCard[]>();
    for (const card of creditCards) {
      const key = card.parentCardId ?? null;
      if (!byParent.has(key)) byParent.set(key, []);
      byParent.get(key)!.push(card);
    }
    const result: CreditCardGroup[] = [];
    byParent.forEach((cards, parentId) => {
      if (parentId === null) {
        cards.forEach(c => result.push({ parentCard: c, subCards: [] }));
      } else {
        const parentCard = creditCards.find(c => c.id === parentId);
        if (parentCard) {
          result.push({ parentCard, subCards: cards });
        } else {
          const first = cards[0];
          result.push({
            parentCard: {
              id: parentId,
              name: first.parentCardName || `Cartão ${parentId}`,
              lastFourDigits: '****',
              limit: 0,
              bankName: first.bankName,
              active: true,
              cardType: 'PHYSICAL',
              parentCardId: undefined,
              parentCardName: undefined,
              createdAt: '',
              updatedAt: ''
            },
            subCards: cards
          });
        }
      }
    });
    return result;
  }, [groupedCards, creditCards]);

  const parentCardOptions = useMemo((): CreditCard[] => {
    const byId = new Map<number, CreditCard>();
    creditCards.filter(c => c.cardType === 'PHYSICAL').forEach(c => byId.set(c.id, c));
    displayGroups.forEach(g => {
      if (!byId.has(g.parentCard.id)) byId.set(g.parentCard.id, g.parentCard);
    });
    return Array.from(byId.values());
  }, [creditCards, displayGroups]);

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
                    {parentCardOptions
                      .filter(card => card.id !== editingCard?.id)
                      .map(card => (
                        <option key={card.id} value={card.id}>
                          {card.name} ({card.lastFourDigits})
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

              <div className="form-group">
                <label htmlFor="assignedUserId">{t('creditCards.selectAssignedUser')}</label>
                <select
                  id="assignedUserId"
                  name="assignedUserId"
                  value={formData.assignedUserId ?? ''}
                  onChange={handleInputChange}
                >
                  <option value="">{t('creditCards.noAssignedUser')}</option>
                  {users.map(user => (
                    <option key={user.id} value={user.id}>
                      {user.name} ({user.email})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group connect-user-section">
                <label>{t('creditCards.connectUserTitle')}</label>
                <div className="connect-user-row">
                  <input
                    type="email"
                    value={connectEmail}
                    onChange={(e) => { setConnectEmail(e.target.value); setConnectMessage(null); }}
                    placeholder={t('creditCards.connectUserPlaceholder')}
                    disabled={connectLoading}
                    className="connect-email-input"
                  />
                  <button
                    type="button"
                    onClick={handleConnectUser}
                    disabled={connectLoading}
                    className="connect-user-button"
                  >
                    {connectLoading ? t('common.loading') : t('creditCards.connectUserButton')}
                  </button>
                </div>
                {connectMessage && (
                  <p className={`connect-message ${connectMessage.type}`}>{connectMessage.text}</p>
                )}
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
        {displayGroups.length === 0 ? (
          <div className="empty-state">
            <p>{t('creditCards.emptyState')}</p>
          </div>
        ) : (
          displayGroups.map(group => (
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
                    <span className="sub-card-meta">
                      {group.parentCard.lastFourDigits} | {t('creditCards.cardTypePhysical')}
                      {group.parentCard.assignedUserName ? ` | ${t('creditCards.assignedTo')}: ${group.parentCard.assignedUserName}` : ''}
                    </span>
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
                        {subCard.lastFourDigits} |
                        {subCard.cardType === 'VIRTUAL' ? t('creditCards.cardTypeVirtual') : t('creditCards.cardTypeAdditional')}
                        {subCard.cardholderName ? ` | ${subCard.cardholderName}` : ''}
                        {subCard.assignedUserName ? ` | ${t('creditCards.assignedTo')}: ${subCard.assignedUserName}` : ''}
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