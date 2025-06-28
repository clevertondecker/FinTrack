import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { MySharesResponse, MyShareResponse } from '../types/itemShare';
import apiService from '../services/api';
import { getStatusColor, getStatusText, formatCurrency, formatDate } from '../utils/invoiceUtils';
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
    return `${percentage.toFixed(1)}%`;
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
        <div className="shares-summary">
          <span className="total-shares">
            {shares.shareCount} divisão{shares.shareCount > 1 ? 'ões' : ''}
          </span>
          <span className="total-amount">
            Total: {formatCurrency(
              shares.shares.reduce((sum, share) => sum + share.myAmount, 0)
            )}
          </span>
        </div>
      </div>

      <div className="shares-groups">
        {Object.entries(groupedShares).map(([key, group]) => {
          const owner = group.creditCardOwnerName;
          const count = group.shares.length;
          
          // Usar interpolação direta
          const cardTitle = `Cartão de ${owner} - ${count} divisão${count > 1 ? 'ões' : ''}`;

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
                          <div className="my-share">
                            <span className="label">Minha parte:</span>
                            <span className="value highlight">
                              {formatCurrency(share.myAmount)} ({formatPercentage(share.myPercentage)})
                            </span>
                          </div>
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
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default MyShares; 