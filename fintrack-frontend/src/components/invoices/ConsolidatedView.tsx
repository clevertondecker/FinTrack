import React from 'react';
import { useTranslation } from 'react-i18next';
import { Invoice, InvoiceItem, Category } from '../../types/invoice';
import { formatCurrency, getStatusColor, getStatusText } from '../../utils/invoiceUtils';
import InvoiceItemRow from './InvoiceItemRow';
import ItemsTableHeader from './ItemsTableHeader';

export interface ConsolidatedViewProps {
  cardItemGroups: { invoice: Invoice; items: InvoiceItem[] }[];
  activeCardTab: number;
  setActiveCardTab: React.Dispatch<React.SetStateAction<number>>;
  invoiceItems: InvoiceItem[];
  loadingItems: boolean;
  sortedCategories: Category[];
  categoryLookup: (itemCategory: string | null) => number | string;
  handleUpdateItemCategory: (itemId: number, categoryId: number | null) => Promise<void>;
  updatingCategoryItemId: number | null;
  handleShareItem: (item: InvoiceItem) => void;
}

const ConsolidatedView: React.FC<ConsolidatedViewProps> = ({
  cardItemGroups,
  activeCardTab,
  setActiveCardTab,
  invoiceItems,
  loadingItems,
  sortedCategories,
  categoryLookup,
  handleUpdateItemCategory,
  updatingCategoryItemId,
  handleShareItem,
}) => {
  const { t } = useTranslation();

  return (
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
  );
};

export default ConsolidatedView;
