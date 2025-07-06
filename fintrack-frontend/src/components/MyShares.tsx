import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { MySharesResponse, MyShareResponse } from '../types/itemShare';
import apiService from '../services/api';
import { getStatusColor, getStatusText, formatCurrency, formatDate } from '../utils/invoiceUtils';
import MarkShareAsPaidModal from './MarkShareAsPaidModal';
import './MyShares.css';

interface GroupedShares {
  [key: string]: {
    creditCardName: string;
    creditCardOwnerName: string;
    shares: MyShareResponse[];
  };
}

const MyShares: React.FC = () => {
  const { t, i18n } = useTranslation();
  const [currentLanguage, setCurrentLanguage] = useState(i18n.language);
  const [shares, setShares] = useState<MySharesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [groupedShares, setGroupedShares] = useState<GroupedShares>({});
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [selectedShare, setSelectedShare] = useState<MyShareResponse | null>(null);
  const [statusFilter, setStatusFilter] = useState<'all' | 'paid' | 'unpaid'>('all');
  const [showFilters, setShowFilters] = useState(false);

  // Atualizar idioma quando mudar
  useEffect(() => {
    const handleLanguageChange = () => {
      setCurrentLanguage(i18n.language);
    };

    i18n.on('languageChanged', handleLanguageChange);
    return () => {
      i18n.off('languageChanged', handleLanguageChange);
    };
  }, [i18n]);

  useEffect(() => {
    loadMyShares();
  }, []);

  const loadMyShares = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await apiService.getMyShares();
      setShares(response);

      // Group shares by credit card
      const grouped: GroupedShares = {};
      response.shares.forEach(share => {
        const key = `${share.creditCardName}-${share.creditCardOwnerName}`;
        if (!grouped[key]) {
          grouped[key] = {
            creditCardName: share.creditCardName,
            creditCardOwnerName: share.creditCardOwnerName,
            shares: []
          };
        }
        grouped[key].shares.push(share);
      });
      
      setGroupedShares(grouped);
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

  const pluralizeDivisao = (count: number) => {
    return count === 1 ? 'divisão' : 'divisões';
  };

  const filterShares = (shares: MyShareResponse[]) => {
    if (statusFilter === 'all') return shares;
    if (statusFilter === 'paid') return shares.filter(share => share.isPaid);
    if (statusFilter === 'unpaid') return shares.filter(share => !share.isPaid);
    return shares;
  };

  const getFilteredGroupedShares = () => {
    if (!shares) return {};
    
    const filteredShares = filterShares(shares.shares);
    const grouped: GroupedShares = {};
    
    filteredShares.forEach(share => {
      const key = `${share.creditCardName}-${share.creditCardOwnerName}`;
      if (!grouped[key]) {
        grouped[key] = {
          creditCardName: share.creditCardName,
          creditCardOwnerName: share.creditCardOwnerName,
          shares: []
        };
      }
      grouped[key].shares.push(share);
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

  const getStatusStats = () => {
    if (!shares) return { total: 0, paid: 0, unpaid: 0 };
    
    const total = shares.shares.length;
    const paid = shares.shares.filter(share => share.isPaid).length;
    const unpaid = total - paid;
    
    return { total, paid, unpaid };
  };

  const handleMarkAsPaid = (share: MyShareResponse) => {
    setSelectedShare(share);
    setShowPaymentModal(true);
  };

  const handleMarkAsUnpaid = async (share: MyShareResponse) => {
    try {
      await apiService.markShareAsUnpaid(share.shareId);
      await loadMyShares(); // Reload to get updated data
    } catch (err: any) {
      console.error('Error marking share as unpaid:', err);
      setError(err.response?.data?.message || 'Erro ao marcar como não pago');
    }
  };

  const handlePaymentMarked = () => {
    loadMyShares(); // Reload to get updated data
  };

  const getPaymentMethodDisplayName = (method: string | null) => {
    if (!method) return '';
    
    const methodMap: { [key: string]: string } = {
      'PIX': 'PIX',
      'BANK_TRANSFER': 'Transferência Bancária',
      'CASH': 'Dinheiro',
      'CREDIT_CARD': 'Cartão de Crédito',
      'DEBIT_CARD': 'Cartão de Débito',
      'OTHER': 'Outro'
    };
    
    return methodMap[method] || method;
  };

  if (loading) {
    return (
      <div className="my-shares-container">
        <div className="loading">Carregando suas divisões...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="my-shares-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={loadMyShares} className="retry-btn">
            Tentar Novamente
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
          <h3>Nenhuma divisão encontrada</h3>
          <p>Você não tem itens divididos com você por outros usuários ainda</p>
        </div>
      </div>
    );
  }

  return (
    <div className="my-shares-container">
      <div className="my-shares-header">
        <h2>Minhas Divisões</h2>
        <div className="header-actions">
          <button 
            className="filter-button"
            onClick={() => setShowFilters(!showFilters)}
          >
            {showFilters ? 'Ocultar Filtros' : 'Filtros'}
          </button>
        </div>
        <div className="shares-summary">
          <span className="total-shares">
            {getFilteredShareCount()} {pluralizeDivisao(getFilteredShareCount())}
          </span>
          <span className="total-amount">
            Total: {formatCurrency(getFilteredTotalAmount())}
          </span>
        </div>
      </div>

      {/* Filters Section */}
      {showFilters && (
        <div className="filters-section">
          <div className="filter-stats">
            <div className="stat-item">
              <span className="stat-label">Total:</span>
              <span className="stat-value">{getStatusStats().total}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">Pendentes:</span>
              <span className="stat-value unpaid">{getStatusStats().unpaid}</span>
            </div>
            <div className="stat-item">
              <span className="stat-label">Pagos:</span>
              <span className="stat-value paid">{getStatusStats().paid}</span>
            </div>
          </div>
          <div className="filter-group">
            <label>Status de Pagamento:</label>
            <div className="filter-options">
              <button
                className={`filter-option ${statusFilter === 'all' ? 'active' : ''}`}
                onClick={() => setStatusFilter('all')}
              >
                Todos ({getStatusStats().total})
              </button>
              <button
                className={`filter-option ${statusFilter === 'unpaid' ? 'active' : ''}`}
                onClick={() => setStatusFilter('unpaid')}
              >
                ⏳ Pendentes ({getStatusStats().unpaid})
              </button>
              <button
                className={`filter-option ${statusFilter === 'paid' ? 'active' : ''}`}
                onClick={() => setStatusFilter('paid')}
              >
                ✅ Pagos ({getStatusStats().paid})
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="shares-groups">
        {Object.entries(getFilteredGroupedShares()).length === 0 ? (
          <div className="no-results">
            <div className="no-results-icon">🔍</div>
            <h3>Nenhuma divisão encontrada</h3>
            <p>
              {statusFilter === 'all' && 'Você não tem divisões ainda.'}
              {statusFilter === 'paid' && 'Você não tem divisões pagas.'}
              {statusFilter === 'unpaid' && 'Você não tem divisões pendentes.'}
            </p>
            {statusFilter !== 'all' && (
              <button
                className="clear-filter-button"
                onClick={() => setStatusFilter('all')}
              >
                Ver todas as divisões
              </button>
            )}
          </div>
        ) : (
          Object.entries(getFilteredGroupedShares()).map(([key, group]) => {
            const owner = group.creditCardOwnerName;
            const count = group.shares.length;
            
            // Usar interpolação direta
            const cardTitle = `Cartão de ${owner} - ${count} ${pluralizeDivisao(count)}`;

            return (
              <div key={key} className="credit-card-group">
                <div className="group-header">
                  <div className="card-info">
                    <h3 className="card-name">{group.creditCardName}</h3>
                    <p className="card-owner">{cardTitle}</p>
                  </div>
                  <div className="group-summary">
                    <span className="group-count">
                      {group.shares.length} item{group.shares.length > 1 ? 's' : ''}
                    </span>
                    <span className="group-total">
                      {formatCurrency(
                        group.shares.reduce((sum, share) => sum + share.myAmount, 0)
                      )}
                    </span>
                  </div>
                </div>
                
                <div className="shares-list">
                  {group.shares.map((share) => (
                    <div key={share.shareId} className="share-item">
                      <div className="share-main-info">
                        <div className="item-description">
                          <h4>{share.itemDescription}</h4>
                          {share.totalInstallments > 1 && (
                            <span className="installment-info">
                              Parcela {share.installments} de {share.totalInstallments}
                              {share.remainingInstallments > 0 && (
                                <span className="remaining-installments">
                                  • Faltam {share.remainingInstallments} parcela{share.remainingInstallments > 1 ? 's' : ''}
                                </span>
                              )}
                            </span>
                          )}
                          <div className="item-meta">
                            <span className="invoice-info">
                              Fatura #{share.invoiceId} • Vencimento: {formatDate(share.invoiceDueDate)}
                            </span>
                            <span className={`invoice-status ${getStatusColor(share.invoiceStatus)}`}>
                              {getStatusText(share.invoiceStatus, t)}
                            </span>
                          </div>
                        </div>
                        
                        <div className="share-amounts">
                          <div className="amount-breakdown">
                            <div className="total-item">
                              <span className="label">Valor total:</span>
                              <span className="value">{formatCurrency(share.itemAmount)}</span>
                            </div>
                            {share.totalInstallments > 1 && (
                              <>
                                <div className="total-item-amount">
                                  <span className="label">Valor total do item:</span>
                                  <span className="value">{formatCurrency(share.totalItemAmount)}</span>
                                </div>
                                {share.remainingInstallments > 0 && (
                                  <div className="remaining-item-amount">
                                    <span className="label">Valor restante:</span>
                                    <span className="value remaining">{formatCurrency(share.remainingItemAmount)}</span>
                                  </div>
                                )}
                              </>
                            )}
                            <div className="my-share">
                              <span className="label">Minha parte:</span>
                              <span className="value highlight">
                                {formatCurrency(share.myAmount)} ({formatPercentage(share.myPercentage)})
                              </span>
                            </div>
                            
                            {share.isPaid && (
                              <div className="payment-info">
                                <span className="label">Status:</span>
                                <span className="value paid">✅ Pago</span>
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
                                <span className="label">Status:</span>
                                <span className="value unpaid">⏳ Pendente</span>
                              </div>
                            )}
                          </div>
                          
                          {share.isResponsible && (
                            <div className="responsible-badge">
                              <span>Responsável pelo pagamento</span>
                            </div>
                          )}
                        </div>
                      </div>
                      
                      <div className="share-footer">
                        <span className="share-date">
                          Dividido em {formatDate(share.shareCreatedAt)}
                        </span>
                      </div>
                      
                      <div className="share-actions">
                        {!share.isPaid ? (
                          <button
                            onClick={() => handleMarkAsPaid(share)}
                            className="mark-paid-button"
                          >
                            Marcar como Pago
                          </button>
                        ) : (
                          <button
                            onClick={() => handleMarkAsUnpaid(share)}
                            className="mark-unpaid-button"
                          >
                            Marcar como Não Pago
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
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