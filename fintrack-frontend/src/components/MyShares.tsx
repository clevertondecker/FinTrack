import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { MySharesResponse, MyShareResponse } from '../types/itemShare';
import { apiService } from '../services/api';
import './MyShares.css';

interface GroupedShares {
  [key: string]: {
    creditCardName: string;
    creditCardOwnerName: string;
    shares: MyShareResponse[];
  };
}

const MyShares: React.FC = () => {
  const { t } = useTranslation();
  const [shares, setShares] = useState<MySharesResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [groupedShares, setGroupedShares] = useState<GroupedShares>({});

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

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-BR');
  };

  const formatPercentage = (percentage: number) => {
    return `${(percentage * 100).toFixed(1)}%`;
  };

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'PAID':
        return 'status-paid';
      case 'PARTIAL':
        return 'status-partial';
      case 'OVERDUE':
        return 'status-overdue';
      default:
        return 'status-open';
    }
  };

  const getStatusText = (status: string) => {
    switch (status.toUpperCase()) {
      case 'PAID':
        return t('invoices.status.paid');
      case 'PARTIAL':
        return t('invoices.status.partial');
      case 'OVERDUE':
        return t('invoices.status.overdue');
      default:
        return t('invoices.status.open');
    }
  };

  if (loading) {
    return (
      <div className="my-shares-container">
        <div className="loading">{t('shares.loadingShares')}</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="my-shares-container">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={loadMyShares} className="retry-btn">
            {t('common.retry')}
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
          <h3>{t('shares.noShares')}</h3>
          <p>{t('shares.noSharesDescription')}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="my-shares-container">
      <div className="my-shares-header">
        <h2>{t('shares.title')}</h2>
        <div className="shares-summary">
          <span className="total-shares">
            {t('shares.shareCount', { count: shares.shareCount })}
          </span>
          <span className="total-amount">
            {t('common.total')}: {formatCurrency(
              shares.shares.reduce((sum, share) => sum + share.myAmount, 0)
            )}
          </span>
        </div>
      </div>

      <div className="shares-groups">
        {Object.entries(groupedShares).map(([key, group]) => (
          <div key={key} className="credit-card-group">
            <div className="group-header">
              <div className="card-info">
                <h3 className="card-name">{group.creditCardName}</h3>
                <p className="card-owner">{t('shares.cardOf', { owner: group.creditCardOwnerName })}</p>
              </div>
              <div className="group-summary">
                <span className="group-count">
                  {t('shares.itemCount', { count: group.shares.length })}
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
                          {t('shares.invoiceNumber', { number: share.invoiceId })} • {t('shares.dueDate', { date: formatDate(share.invoiceDueDate) })}
                        </span>
                        <span className={`invoice-status ${getStatusColor(share.invoiceStatus)}`}>
                          {getStatusText(share.invoiceStatus)}
                        </span>
                      </div>
                    </div>
                    
                    <div className="share-amounts">
                      <div className="amount-breakdown">
                        <div className="total-item">
                          <span className="label">{t('shares.totalAmount')}</span>
                          <span className="value">{formatCurrency(share.itemAmount)}</span>
                        </div>
                        <div className="my-share">
                          <span className="label">{t('shares.myShare')}</span>
                          <span className="value highlight">
                            {formatCurrency(share.myAmount)} ({formatPercentage(share.myPercentage)})
                          </span>
                        </div>
                      </div>
                      
                      {share.isResponsible && (
                        <div className="responsible-badge">
                          <span>{t('shares.responsibleForPayment')}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  
                  <div className="share-footer">
                    <span className="share-date">
                      {t('shares.sharedOn', { date: formatDate(share.shareCreatedAt) })}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default MyShares; 