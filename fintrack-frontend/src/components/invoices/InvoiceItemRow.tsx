import React from 'react';
import { useTranslation } from 'react-i18next';
import { InvoiceItem, Category } from '../../types/invoice';
import { formatCurrency, formatDate } from '../../utils/invoiceUtils';

interface InvoiceItemRowProps {
  item: InvoiceItem;
  sortedCategories: Category[];
  categoryLookup: (itemCategory: string | null) => number | string;
  onCategoryChange: (itemId: number, categoryId: number | null) => void;
  onShare: (item: InvoiceItem) => void;
  onRemove?: (itemId: number) => void;
  updatingCategoryItemId: number | null;
  removingItemId?: number | null;
}

const InvoiceItemRow: React.FC<InvoiceItemRowProps> = ({
  item,
  sortedCategories,
  categoryLookup,
  onCategoryChange,
  onShare,
  onRemove,
  updatingCategoryItemId,
  removingItemId,
}) => {
  const { t } = useTranslation();

  return (
    <div className={`item-row${item.projected ? ' projected-item' : ''}`}>
      <div className="item-description">
        {item.description}
        {item.projected && (
          <span className="projected-badge" title={t('invoices.projectedItem')}>
            📅
          </span>
        )}
        {item.isShared && (
          <span className="shared-indicator" title={t('invoices.itemShared')}>
            👥
          </span>
        )}
      </div>
      <div className="item-category">
        <select
          value={categoryLookup(item.category) || ''}
          onChange={(e) => onCategoryChange(item.id, e.target.value ? Number(e.target.value) : null)}
          disabled={updatingCategoryItemId === item.id}
          className="category-select"
          title={item.category || t('invoices.noCategory')}
        >
          <option value="">{t('invoices.noCategory')}</option>
          {sortedCategories.map(category => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        {updatingCategoryItemId === item.id && (
          <span className="updating-indicator" title={t('invoices.updatingCategory')}>⏳</span>
        )}
      </div>
      <div className="item-date">{formatDate(item.purchaseDate)}</div>
      <div className="item-amount">{formatCurrency(item.amount)}</div>
      <div className="item-installments">
        {item.totalInstallments && item.totalInstallments > 1
          ? `${item.installments}/${item.totalInstallments}`
          : t('invoiceItems.singlePayment')}
      </div>
      <div className="item-actions">
        <button
          onClick={() => onShare(item)}
          className="share-button"
          title={t('invoices.shareItemTooltip')}
        >
          {t('invoices.shareButton')}
        </button>
        {onRemove && (
          <button
            onClick={() => onRemove(item.id)}
            className="remove-button"
            title={t('invoices.removeItemTooltip')}
            disabled={removingItemId === item.id}
          >
            {removingItemId === item.id ? t('invoices.removing') : t('invoices.removeButton')}
          </button>
        )}
      </div>
    </div>
  );
};

export default React.memo(InvoiceItemRow);
