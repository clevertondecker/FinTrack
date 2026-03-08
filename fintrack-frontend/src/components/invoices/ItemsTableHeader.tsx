import React from 'react';
import { useTranslation } from 'react-i18next';

const ItemsTableHeader: React.FC = () => {
  const { t } = useTranslation();

  return (
    <div className="items-header">
      <div className="header-description">{t('invoices.descriptionLabel')}</div>
      <div className="header-category">{t('invoices.categoryLabel')}</div>
      <div className="header-date">{t('invoices.purchaseDateLabel')}</div>
      <div className="header-amount">{t('invoices.amountLabel')}</div>
      <div className="header-installments">{t('invoiceItems.installments')}</div>
      <div className="header-actions">{t('invoices.actionsLabel')}</div>
    </div>
  );
};

export default React.memo(ItemsTableHeader);
