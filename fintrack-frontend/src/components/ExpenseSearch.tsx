import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Search, ChevronLeft, ChevronRight, X, SlidersHorizontal, DollarSign, Receipt } from 'lucide-react';
import apiService from '../services/api';
import { ExpenseSearchParams, ExpenseSearchResponse, ExpenseSearchResult } from '../types/search';
import { Category } from '../types/invoice';
import { formatCurrency } from '../utils/invoiceUtils';
import './ExpenseSearch.css';

interface CardOption {
  id: number;
  name: string;
  lastFourDigits: string;
}

const PAGE_SIZE = 20;
const MAX_VISIBLE_PAGES = 7;
const PAGE_EDGE_THRESHOLD = 3;

const formatInstallments = (item: ExpenseSearchResult): string => {
  if (item.totalInstallments <= 1) return '';
  return `${item.installments}/${item.totalInstallments}`;
};

const ExpenseSearch: React.FC = () => {
  const { t } = useTranslation();

  const [searchText, setSearchText] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [results, setResults] = useState<ExpenseSearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(0);

  const [categoryId, setCategoryId] = useState<number | undefined>();
  const [cardId, setCardId] = useState<number | undefined>();
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [amountMin, setAmountMin] = useState('');
  const [amountMax, setAmountMax] = useState('');

  const [categories, setCategories] = useState<Category[]>([]);
  const [cards, setCards] = useState<CardOption[]>([]);

  useEffect(() => {
    const loadFilterOptions = async () => {
      try {
        const [catResponse, cardResponse] = await Promise.all([
          apiService.getCategories(),
          apiService.getCreditCards()
        ]);
        setCategories(catResponse.categories);
        setCards(
          cardResponse.creditCards.map(
            (c: { id: number; name: string; lastFourDigits: string }) => ({
              id: c.id,
              name: c.name,
              lastFourDigits: c.lastFourDigits,
            })
          )
        );
      } catch {
        // Filter options are non-critical
      }
    };
    loadFilterOptions();
  }, []);

  const activeFilterCount = useMemo(() => {
    return [categoryId, cardId, dateFrom, dateTo, amountMin, amountMax]
      .filter(Boolean).length;
  }, [categoryId, cardId, dateFrom, dateTo, amountMin, amountMax]);

  const hasActiveFilters = activeFilterCount > 0;

  const executeSearch = useCallback(async (query: string, pageNum: number) => {
    setError(null);
    setLoading(true);
    try {
      const params: ExpenseSearchParams = {
        query: query || undefined,
        categoryId,
        cardId,
        dateFrom: dateFrom || undefined,
        dateTo: dateTo || undefined,
        amountMin: amountMin ? parseFloat(amountMin) : undefined,
        amountMax: amountMax ? parseFloat(amountMax) : undefined,
        page: pageNum,
        size: PAGE_SIZE,
      };
      const data = await apiService.searchExpenses(params);
      setResults(data);
    } catch {
      setError(t('search.errorLoading'));
    } finally {
      setLoading(false);
    }
  }, [categoryId, cardId, dateFrom, dateTo, amountMin, amountMax, t]);

  const handleSearch = useCallback(() => {
    setPage(0);
    setSubmittedQuery(searchText);
    executeSearch(searchText, 0);
  }, [searchText, executeSearch]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  }, [handleSearch]);

  const handlePageChange = useCallback((newPage: number) => {
    setPage(newPage);
    executeSearch(submittedQuery, newPage);
  }, [submittedQuery, executeSearch]);

  const clearFilters = useCallback(() => {
    setCategoryId(undefined);
    setCardId(undefined);
    setDateFrom('');
    setDateTo('');
    setAmountMin('');
    setAmountMax('');
  }, []);

  const formatInvoiceMonth = useCallback((month: string): string => {
    if (!month) return '-';
    try {
      const [year, monthNum] = month.split('-');
      const date = new Date(Number(year), Number(monthNum) - 1);
      const formatted = date.toLocaleDateString(undefined, { month: 'short', year: 'numeric' });
      return formatted.charAt(0).toUpperCase() + formatted.slice(1);
    } catch {
      return month;
    }
  }, []);

  const pageNumbers = useMemo(() => {
    if (!results || results.totalPages <= 1) return [];
    const total = results.totalPages;
    const count = Math.min(total, MAX_VISIBLE_PAGES);
    return Array.from({ length: count }, (_, i) => {
      if (total <= MAX_VISIBLE_PAGES) return i;
      if (page < PAGE_EDGE_THRESHOLD) return i;
      if (page > total - PAGE_EDGE_THRESHOLD - 1) return total - MAX_VISIBLE_PAGES + i;
      return page - PAGE_EDGE_THRESHOLD + i;
    });
  }, [results, page]);

  return (
    <div className="expense-search">
      {/* Search Bar */}
      <div className="search-hero">
        <div className="search-bar-row">
          <div className="search-input-group">
            <Search size={20} className="search-input-icon" />
            <input
              type="text"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={t('search.placeholder')}
              className="search-main-input"
              autoFocus
            />
            {searchText && (
              <button onClick={() => setSearchText('')} className="search-input-clear">
                <X size={16} />
              </button>
            )}
          </div>
          <button onClick={handleSearch} className="search-submit-btn">
            <Search size={18} />
            {t('search.searchButton')}
          </button>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`search-filter-toggle ${showFilters ? 'open' : ''} ${hasActiveFilters ? 'has-active' : ''}`}
            title={t('search.applyFilters')}
          >
            <SlidersHorizontal size={18} />
            {activeFilterCount > 0 && (
              <span className="filter-count-badge">{activeFilterCount}</span>
            )}
          </button>
        </div>

        {showFilters && (
          <div className="search-filters-panel">
            <div className="filters-grid">
              <div className="filter-field">
                <label>{t('search.category')}</label>
                <select
                  value={categoryId ?? ''}
                  onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}
                >
                  <option value="">{t('search.allCategories')}</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>

              <div className="filter-field">
                <label>{t('search.card')}</label>
                <select
                  value={cardId ?? ''}
                  onChange={(e) => setCardId(e.target.value ? Number(e.target.value) : undefined)}
                >
                  <option value="">{t('search.allCards')}</option>
                  {cards.map((card) => (
                    <option key={card.id} value={card.id}>
                      {card.name} (••{card.lastFourDigits})
                    </option>
                  ))}
                </select>
              </div>

              <div className="filter-field">
                <label>{t('search.dateFrom')}</label>
                <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
              </div>

              <div className="filter-field">
                <label>{t('search.dateTo')}</label>
                <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
              </div>

              <div className="filter-field">
                <label>{t('search.amountMin')}</label>
                <input
                  type="number" value={amountMin} onChange={(e) => setAmountMin(e.target.value)}
                  placeholder="0,00" min="0" step="0.01"
                />
              </div>

              <div className="filter-field">
                <label>{t('search.amountMax')}</label>
                <input
                  type="number" value={amountMax} onChange={(e) => setAmountMax(e.target.value)}
                  placeholder="0,00" min="0" step="0.01"
                />
              </div>
            </div>

            <div className="filter-bar-actions">
              {hasActiveFilters && (
                <button onClick={clearFilters} className="filter-clear-btn">
                  <X size={14} />
                  {t('search.clearFilters')}
                </button>
              )}
              <button onClick={handleSearch} className="filter-apply-btn">
                {t('search.applyFilters')}
              </button>
            </div>
          </div>
        )}
      </div>

      {error && (
        <div className="search-error-banner">
          <p>{error}</p>
          <button onClick={handleSearch}>{t('expenseReport.retry')}</button>
        </div>
      )}

      {loading && (
        <div className="search-loading">
          <div className="spinner" />
          <p>{t('common.loading')}</p>
        </div>
      )}

      {!loading && results && (
        <>
          <div className="search-summary-strip">
            <div className="summary-stat">
              <Receipt size={16} />
              <span>{t('search.resultsCount', { count: results.totalResults })}</span>
            </div>
            {results.totalAmount > 0 && (
              <div className="summary-stat summary-total">
                <DollarSign size={16} />
                <span>{t('search.totalAmount')}: <strong>{formatCurrency(results.totalAmount)}</strong></span>
              </div>
            )}
          </div>

          {results.results.length === 0 ? (
            <div className="search-no-results">
              <Search size={40} strokeWidth={1.5} />
              <p>{t('search.noResults')}</p>
            </div>
          ) : (
            <>
              <div className="search-results-container">
                <table className="search-table">
                  <thead>
                    <tr>
                      <th>{t('search.description')}</th>
                      <th className="text-right">{t('search.amount')}</th>
                      <th>{t('search.date')}</th>
                      <th>{t('search.categoryCol')}</th>
                      <th>{t('search.cardCol')}</th>
                      <th>{t('search.invoiceMonthCol')}</th>
                      <th>{t('search.installmentsCol')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.results.map((item) => (
                      <tr key={`${item.invoiceId}-${item.itemId}`}>
                        <td className="col-description" title={item.description}>
                          {item.description}
                        </td>
                        <td className="col-amount">{formatCurrency(item.amount)}</td>
                        <td className="col-date">
                          {new Date(item.purchaseDate + 'T00:00:00').toLocaleDateString()}
                        </td>
                        <td className="col-category">
                          <span
                            className="cat-pill"
                            style={{ backgroundColor: item.category?.color || '#94a3b8' }}
                          >
                            {item.category?.name || '-'}
                          </span>
                        </td>
                        <td className="col-card">
                          <div className="card-info-cell">
                            <span className="card-label">{item.cardName}</span>
                            <span className="card-suffix">••{item.lastFourDigits}</span>
                          </div>
                        </td>
                        <td className="col-month">{formatInvoiceMonth(item.invoiceMonth)}</td>
                        <td className="col-installments">{formatInstallments(item) || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {results.totalPages > 1 && (
                <div className="search-pagination">
                  <button
                    onClick={() => handlePageChange(page - 1)}
                    disabled={page === 0}
                    className="page-btn"
                  >
                    <ChevronLeft size={16} />
                    {t('common.previous')}
                  </button>
                  <div className="page-numbers">
                    {pageNumbers.map((pageNum) => (
                      <button
                        key={pageNum}
                        onClick={() => handlePageChange(pageNum)}
                        className={`page-number ${page === pageNum ? 'active' : ''}`}
                      >
                        {pageNum + 1}
                      </button>
                    ))}
                  </div>
                  <button
                    onClick={() => handlePageChange(page + 1)}
                    disabled={page >= results.totalPages - 1}
                    className="page-btn"
                  >
                    {t('common.next')}
                    <ChevronRight size={16} />
                  </button>
                </div>
              )}
            </>
          )}
        </>
      )}

      {!loading && !results && !error && (
        <div className="search-initial-state">
          <div className="initial-state-icon">
            <Search size={56} strokeWidth={1.2} />
          </div>
          <h3>{t('search.emptyStateTitle')}</h3>
          <p>{t('search.emptyStateDescription')}</p>
        </div>
      )}
    </div>
  );
};

export default ExpenseSearch;
