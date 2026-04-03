import { useState, useEffect, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import apiService from '../services/api';
import { Invoice, InvoiceItem, CreateInvoiceRequest, InvoiceFilters, InvoiceSummary, Category, InvoiceDeleteInfo } from '../types/invoice';
import { CreditCard } from '../types/creditCard';
import { formatCurrency, sortInvoices, groupInvoices, consolidateInvoices, calculateSummary } from '../utils/invoiceUtils';

export interface UseInvoiceDataReturn {
  // Tab state
  activeTab: 'invoices' | 'subscriptions';
  setActiveTab: React.Dispatch<React.SetStateAction<'invoices' | 'subscriptions'>>;

  // Core data
  invoices: Invoice[];
  creditCards: CreditCard[];
  categories: Category[];
  loading: boolean;
  error: string | null;
  setError: React.Dispatch<React.SetStateAction<string | null>>;

  // Create form
  showCreateForm: boolean;
  setShowCreateForm: React.Dispatch<React.SetStateAction<boolean>>;
  formData: CreateInvoiceRequest;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  handleSubmit: (e: React.FormEvent) => Promise<void>;
  handleCancel: () => void;

  // Filters
  showFilters: boolean;
  setShowFilters: React.Dispatch<React.SetStateAction<boolean>>;
  filters: InvoiceFilters;
  filteredInvoices: Invoice[];
  summary: InvoiceSummary;
  handleFilterChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  clearFilters: () => void;
  groupedInvoices: ReturnType<typeof groupInvoices>;

  // Details modal
  showDetailsModal: boolean;
  setShowDetailsModal: React.Dispatch<React.SetStateAction<boolean>>;
  selectedInvoice: Invoice | null;
  setSelectedInvoice: React.Dispatch<React.SetStateAction<Invoice | null>>;
  invoiceItems: InvoiceItem[];
  setInvoiceItems: React.Dispatch<React.SetStateAction<InvoiceItem[]>>;
  loadingItems: boolean;

  // Consolidated view
  isConsolidatedView: boolean;
  cardItemGroups: { invoice: Invoice; items: InvoiceItem[] }[];
  setCardItemGroups: React.Dispatch<React.SetStateAction<{ invoice: Invoice; items: InvoiceItem[] }[]>>;
  activeCardTab: number;
  setActiveCardTab: React.Dispatch<React.SetStateAction<number>>;

  // Item form
  itemForm: { description: string; amount: string; categoryId: string; purchaseDate: string };
  setItemForm: React.Dispatch<React.SetStateAction<{ description: string; amount: string; categoryId: string; purchaseDate: string }>>;
  addingItem: boolean;
  removingItemId: number | null;
  itemError: string | null;
  setItemError: React.Dispatch<React.SetStateAction<string | null>>;
  showAddMore: boolean;
  quickAddMode: boolean;
  updatingCategoryItemId: number | null;

  // Item handlers
  handleItemInputChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  handleQuickAdd: () => void;
  handleAddItem: (e: React.FormEvent) => Promise<void>;
  handleAddMore: () => void;
  handleFinishAdding: () => void;
  handleQuickCategorySelect: (categoryId: string) => void;
  handleQuickAmountSelect: (amount: string) => void;
  handleQuickDateSelect: (date: string) => void;
  handleRemoveItem: (itemId: number) => Promise<void>;

  // Detail view handlers
  handleViewDetails: (invoice: Invoice) => Promise<void>;
  handleViewConsolidatedDetails: (invoice: Invoice) => Promise<void>;
  handleCloseDetails: () => void;

  // Category
  categoryMap: Map<number, string>;
  sortedCategories: Category[];
  categoryLookup: (itemCategory: string | null) => number | string;
  handleUpdateItemCategory: (itemId: number, categoryId: number | null) => Promise<void>;

  // Share
  showShareModal: boolean;
  selectedItemForSharing: InvoiceItem | null;
  handleShareItem: (item: InvoiceItem) => void;
  handleCloseShareModal: () => void;
  handleSharesUpdated: () => Promise<void>;

  // Pay
  showPayModal: boolean;
  invoiceToPay: Invoice | null;
  payAmount: string;
  setPayAmount: React.Dispatch<React.SetStateAction<string>>;
  paying: boolean;
  payError: string | null;
  handleOpenPayModal: (invoice: Invoice) => void;
  handleClosePayModal: () => void;
  handlePayTotal: () => void;
  handlePayInvoice: (e: React.FormEvent) => Promise<void>;

  // Delete
  showDeleteModal: boolean;
  invoiceToDelete: Invoice | null;
  deleteInfo: InvoiceDeleteInfo | null;
  loadingDeleteInfo: boolean;
  deleting: boolean;
  handleDeleteInvoice: (invoice: Invoice) => Promise<void>;
  handleConfirmDelete: () => Promise<void>;
  handleCloseDeleteModal: () => void;

  // Helpers
  formatInvoiceAmount: (invoice: Invoice) => string;
  getInvoiceUserAmount: (invoice: Invoice) => number;
  getInvoiceUserPaid: (invoice: Invoice) => number;
  getInvoiceUserRemaining: (invoice: Invoice) => number;
  getConsolidatedCards: (invoice: Invoice) => Invoice[] | undefined;
  isConsolidated: (invoice: Invoice) => boolean;

  // Projection
  projectingInstallments: boolean;
  handleProjectInstallments: () => Promise<void>;

  // Reload
  loadInvoices: () => Promise<void>;
}

export function useInvoiceData(): UseInvoiceDataReturn {
  const { t } = useTranslation();

  const [activeTab, setActiveTab] = useState<'invoices' | 'subscriptions'>('invoices');
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [creditCards, setCreditCards] = useState<CreditCard[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null);
  const [invoiceItems, setInvoiceItems] = useState<InvoiceItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [showShareModal, setShowShareModal] = useState(false);
  const [selectedItemForSharing, setSelectedItemForSharing] = useState<InvoiceItem | null>(null);
  const [showPayModal, setShowPayModal] = useState(false);
  const [invoiceToPay, setInvoiceToPay] = useState<Invoice | null>(null);
  const [payAmount, setPayAmount] = useState('');
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState<string | null>(null);
  const [addingItem, setAddingItem] = useState(false);
  const [removingItemId, setRemovingItemId] = useState<number | null>(null);
  const [itemError, setItemError] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<InvoiceFilters>({});
  const [filteredInvoices, setFilteredInvoices] = useState<Invoice[]>([]);
  const [updatingCategoryItemId, setUpdatingCategoryItemId] = useState<number | null>(null);
  const [summary, setSummary] = useState<InvoiceSummary>({
    totalInvoices: 0,
    totalAmount: 0,
    totalPaid: 0,
    totalRemaining: 0,
    overdueCount: 0,
    overdueAmount: 0,
    openCount: 0,
    openAmount: 0,
    partialCount: 0,
    partialAmount: 0,
    paidCount: 0,
    paidAmount: 0
  });

  const [formData, setFormData] = useState<CreateInvoiceRequest>({
    creditCardId: 0,
    dueDate: ''
  });

  const [itemForm, setItemForm] = useState({
    description: '',
    amount: '',
    categoryId: '',
    purchaseDate: ''
  });

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [invoiceToDelete, setInvoiceToDelete] = useState<Invoice | null>(null);
  const [deleteInfo, setDeleteInfo] = useState<InvoiceDeleteInfo | null>(null);
  const [loadingDeleteInfo, setLoadingDeleteInfo] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [projectingInstallments, setProjectingInstallments] = useState(false);

  const [showAddMore, setShowAddMore] = useState(false);
  const [quickAddMode, setQuickAddMode] = useState(false);
  const [isConsolidatedView, setIsConsolidatedView] = useState(false);
  const [cardItemGroups, setCardItemGroups] = useState<{ invoice: Invoice; items: InvoiceItem[] }[]>([]);
  const [activeCardTab, setActiveCardTab] = useState<number>(0);

  // --- Helpers ---

  const formatInvoiceAmount = useCallback((invoice: Invoice): string => {
    if (invoice.userShare !== null && invoice.userShare !== undefined && invoice.userShare !== invoice.totalAmount) {
      return `${formatCurrency(invoice.userShare)} (${t('invoices.myShare')})`;
    }
    return formatCurrency(invoice.totalAmount);
  }, [t]);

  const getInvoiceUserAmount = useCallback((invoice: Invoice): number => {
    return invoice.userShare !== null && invoice.userShare !== undefined ? invoice.userShare : (invoice.totalAmount || 0);
  }, []);

  const getInvoiceUserPaid = useCallback((invoice: Invoice): number => {
    const userAmount = getInvoiceUserAmount(invoice);
    const totalInvoiceAmount = invoice.totalAmount || 0;
    const totalPaid = invoice.paidAmount || 0;

    if (totalInvoiceAmount > 0 && userAmount !== totalInvoiceAmount) {
      const paidPercentage = Math.min(totalPaid / totalInvoiceAmount, 1);
      return userAmount * paidPercentage;
    }
    return Math.min(totalPaid, userAmount);
  }, [getInvoiceUserAmount]);

  const getInvoiceUserRemaining = useCallback((invoice: Invoice): number => {
    return Math.max(0, getInvoiceUserAmount(invoice) - getInvoiceUserPaid(invoice));
  }, [getInvoiceUserAmount, getInvoiceUserPaid]);

  const getConsolidatedCards = useCallback((invoice: Invoice): Invoice[] | undefined => {
    return invoice._consolidatedCards;
  }, []);

  const isConsolidated = useCallback((invoice: Invoice): boolean => {
    return !!invoice._consolidatedCards;
  }, []);

  // --- Data fetching ---

  const loadInvoices = useCallback(async () => {
    try {
      setLoading(true);
      const response = await apiService.getInvoices();
      setInvoices(response.invoices);
      setError(null);
    } catch (err) {
      setError(t('invoices.failedToLoadInvoices'));
      console.error('Error loading invoices:', err);
    } finally {
      setLoading(false);
    }
  }, [t]);

  const updateInvoiceInList = useCallback((updated: Invoice) => {
    setInvoices(prev => prev.map(inv => inv.id === updated.id ? updated : inv));
  }, []);

  const loadCreditCards = useCallback(async () => {
    try {
      const response = await apiService.getCreditCards();
      setCreditCards(response.creditCards);
    } catch (err) {
      console.error('Error loading credit cards:', err);
    }
  }, []);

  const loadCategories = useCallback(async () => {
    try {
      const response = await apiService.getCategories();

      const uniqueByName = new Map<string, Category>();
      response.categories.forEach(category => {
        if (!uniqueByName.has(category.name)) {
          uniqueByName.set(category.name, category);
        }
      });

      const uniqueCategories = Array.from(uniqueByName.values());
      setCategories(uniqueCategories);
    } catch (err) {
      console.error('Error loading categories:', err);
    }
  }, []);

  useEffect(() => {
    loadInvoices();
    loadCreditCards();
    loadCategories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // --- Filters ---

  const applyFilters = useCallback(() => {
    let filtered = [...invoices];

    if (filters.status) {
      filtered = filtered.filter(invoice => invoice.status === filters.status);
    }
    if (filters.creditCardId) {
      filtered = filtered.filter(invoice => invoice.creditCardId === filters.creditCardId);
    }
    if (filters.dateFrom) {
      filtered = filtered.filter(invoice => new Date(invoice.dueDate) >= new Date(filters.dateFrom!));
    }
    if (filters.dateTo) {
      filtered = filtered.filter(invoice => new Date(invoice.dueDate) <= new Date(filters.dateTo!));
    }
    if (filters.minAmount) {
      filtered = filtered.filter(invoice => (invoice.totalAmount || 0) >= filters.minAmount!);
    }
    if (filters.maxAmount) {
      filtered = filtered.filter(invoice => (invoice.totalAmount || 0) <= filters.maxAmount!);
    }

    const consolidated = consolidateInvoices(filtered);
    const sorted = sortInvoices(consolidated);
    setFilteredInvoices(sorted);
    setSummary(calculateSummary(sorted));
  }, [invoices, filters]);

  useEffect(() => {
    applyFilters();
  }, [applyFilters]);

  const handleFilterChange = useCallback((e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: name === 'creditCardId' || name === 'minAmount' || name === 'maxAmount'
        ? (value ? Number(value) : undefined)
        : value
    }));
  }, []);

  const clearFilters = useCallback(() => {
    setFilters({});
  }, []);

  // --- Create form ---

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: name === 'creditCardId' ? Number(value) : value }));
  }, []);

  const handleSubmit = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.creditCardId || !formData.dueDate) {
      setError(t('invoices.pleaseFillFields'));
      return;
    }

    try {
      await apiService.createInvoice(formData);
      setFormData({ creditCardId: 0, dueDate: '' });
      setShowCreateForm(false);
      await loadInvoices();
      setError(null);
    } catch (err) {
      setError(t('invoices.failedToCreateInvoice'));
      console.error('Error creating invoice:', err);
    }
  }, [formData, t, loadInvoices]);

  const handleCancel = useCallback(() => {
    setShowCreateForm(false);
    setFormData({ creditCardId: 0, dueDate: '' });
    setError(null);
  }, []);

  // --- Detail view ---

  const handleViewDetails = useCallback(async (invoice: Invoice) => {
    setIsConsolidatedView(false);
    setCardItemGroups([]);
    setActiveCardTab(0);
    setSelectedInvoice(invoice);
    setShowDetailsModal(true);
    setLoadingItems(true);
    try {
      const response = await apiService.getInvoiceItems(invoice.id);
      setInvoiceItems(response.items);
    } catch {
      setInvoiceItems([]);
    } finally {
      setLoadingItems(false);
    }
  }, []);

  const handleViewConsolidatedDetails = useCallback(async (invoice: Invoice) => {
    const cards = invoice._consolidatedCards;
    if (!cards || cards.length === 0) {
      handleViewDetails(invoice);
      return;
    }

    setIsConsolidatedView(true);
    setActiveCardTab(0);
    setSelectedInvoice(invoice);
    setShowDetailsModal(true);
    setLoadingItems(true);
    setInvoiceItems([]);

    try {
      const results = await Promise.all(
        cards.map(async (sub) => {
          try {
            const response = await apiService.getInvoiceItems(sub.id);
            return { invoice: sub, items: response.items };
          } catch {
            return { invoice: sub, items: [] as InvoiceItem[] };
          }
        })
      );
      setCardItemGroups(results);
      const allItems = results.flatMap(r => r.items);
      setInvoiceItems(allItems);
    } catch {
      setCardItemGroups([]);
      setInvoiceItems([]);
    } finally {
      setLoadingItems(false);
    }
  }, [handleViewDetails]);

  const handleCloseDetails = useCallback(() => {
    setShowDetailsModal(false);
    setSelectedInvoice(null);
    setInvoiceItems([]);
    setIsConsolidatedView(false);
    setCardItemGroups([]);
    setActiveCardTab(0);
  }, []);

  // --- Item form handlers ---

  const handleItemInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setItemForm(prev => ({ ...prev, [name]: value }));
  }, []);

  const handleQuickAdd = useCallback(() => {
    setQuickAddMode(true);
    setItemForm({
      description: '',
      amount: '',
      categoryId: '',
      purchaseDate: new Date().toISOString().split('T')[0]
    });
  }, []);

  const handleAddItem = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemForm.description || !itemForm.amount || isNaN(Number(itemForm.amount))) {
      setItemError('Descrição e valor são obrigatórios');
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
        purchaseDate: itemForm.purchaseDate || new Date().toISOString().split('T')[0]
      });

      const [itemsResponse, invoiceResponse] = await Promise.all([
        apiService.getInvoiceItems(selectedInvoice.id),
        apiService.getInvoice(selectedInvoice.id)
      ]);

      setInvoiceItems(itemsResponse.items);
      setSelectedInvoice(invoiceResponse.invoice);
      updateInvoiceInList(invoiceResponse.invoice);

      setItemForm(prev => ({
        ...prev,
        description: '',
        amount: ''
      }));

      setShowAddMore(true);
      setQuickAddMode(false);
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Erro ao adicionar item';
      setItemError(errorMessage);
      console.error('Error adding invoice item:', err);
    } finally {
      setAddingItem(false);
    }
  }, [itemForm, selectedInvoice, updateInvoiceInList]);

  const handleAddMore = useCallback(() => {
    setShowAddMore(false);
    setQuickAddMode(true);
  }, []);

  const handleFinishAdding = useCallback(() => {
    setShowAddMore(false);
    setQuickAddMode(false);
    setItemForm({ description: '', amount: '', categoryId: '', purchaseDate: '' });
  }, []);

  const handleQuickCategorySelect = useCallback((categoryId: string) => {
    setItemForm(prev => ({ ...prev, categoryId }));
  }, []);

  const handleQuickAmountSelect = useCallback((amount: string) => {
    setItemForm(prev => ({ ...prev, amount }));
  }, []);

  const handleQuickDateSelect = useCallback((date: string) => {
    setItemForm(prev => ({ ...prev, purchaseDate: date }));
  }, []);

  const handleRemoveItem = useCallback(async (itemId: number) => {
    const targetItem = invoiceItems.find(i => i.id === itemId);
    const invoiceId = targetItem?.invoiceId ?? selectedInvoice?.id;
    if (!invoiceId) return;

    setRemovingItemId(itemId);
    try {
      await apiService.deleteInvoiceItem(invoiceId, itemId);

      const [itemsResponse, invoiceResponse] = await Promise.all([
        apiService.getInvoiceItems(invoiceId),
        apiService.getInvoice(invoiceId)
      ]);

      setInvoiceItems(prev => prev.filter(i => i.id !== itemId));
      if (isConsolidatedView) {
        setCardItemGroups(prevGroups =>
          prevGroups.map(group =>
            group.invoice.id === invoiceId
              ? { invoice: invoiceResponse.invoice, items: itemsResponse.items }
              : group
          )
        );
      } else {
        setInvoiceItems(itemsResponse.items);
        setSelectedInvoice(invoiceResponse.invoice);
      }
      updateInvoiceInList(invoiceResponse.invoice);
    } catch {
      // Error silently handled
    } finally {
      setRemovingItemId(null);
    }
  }, [invoiceItems, selectedInvoice, isConsolidatedView, updateInvoiceInList]);

  // --- Sharing ---

  const handleShareItem = useCallback((item: InvoiceItem) => {
    setSelectedItemForSharing(item);
    setShowShareModal(true);
  }, []);

  const handleCloseShareModal = useCallback(() => {
    setShowShareModal(false);
    setSelectedItemForSharing(null);
  }, []);

  const handleSharesUpdated = useCallback(async () => {
    const invoiceId = selectedItemForSharing?.invoiceId ?? selectedInvoice?.id;
    if (!invoiceId) return;

    try {
      const [itemsResponse, invoiceResponse] = await Promise.all([
        apiService.getInvoiceItems(invoiceId),
        apiService.getInvoice(invoiceId)
      ]);

      if (isConsolidatedView) {
        setCardItemGroups(prevGroups =>
          prevGroups.map(group =>
            group.invoice.id === invoiceId
              ? { invoice: invoiceResponse.invoice, items: itemsResponse.items }
              : group
          )
        );
        setInvoiceItems(prev =>
          prev.map(item => {
            const updated = itemsResponse.items.find(i => i.id === item.id);
            return updated ?? item;
          })
        );
      } else {
        setInvoiceItems(itemsResponse.items);
        setSelectedInvoice(invoiceResponse.invoice);
      }
      updateInvoiceInList(invoiceResponse.invoice);
    } catch (err) {
      console.error('Error reloading invoice items:', err);
    }
  }, [selectedItemForSharing, selectedInvoice, isConsolidatedView, updateInvoiceInList]);

  // --- Category memoization ---

  const categoryMap = useMemo(() => {
    return new Map(categories.map(category => [category.id, category.name]));
  }, [categories]);

  const sortedCategories = useMemo(() => {
    const uniqueByName = new Map<string, Category>();
    categories.forEach(category => {
      if (!uniqueByName.has(category.name)) {
        uniqueByName.set(category.name, category);
      }
    });
    const uniqueCategories = Array.from(uniqueByName.values());
    return uniqueCategories.sort((a, b) => a.name.localeCompare(b.name));
  }, [categories]);

  const categoryLookup = useCallback((itemCategory: string | null): number | string => {
    if (!itemCategory) return '';
    const found = categories.find(c => c.name === itemCategory);
    return found ? found.id : '';
  }, [categories]);

  const extractErrorMessage = useCallback((err: any, defaultMessage: string): string => {
    return err.response?.data?.error || err.message || defaultMessage;
  }, []);

  const handleUpdateItemCategory = useCallback(async (itemId: number, categoryId: number | null) => {
    const targetItem = invoiceItems.find(i => i.id === itemId);
    const invoiceId = targetItem?.invoiceId ?? selectedInvoice?.id;
    if (!invoiceId) return;

    setUpdatingCategoryItemId(itemId);
    setItemError(null);

    try {
      await apiService.updateInvoiceItemCategory(invoiceId, itemId, categoryId);

      const newCategoryName = categoryId ? categoryMap.get(categoryId) || null : null;
      setInvoiceItems(prevItems =>
        prevItems.map(item =>
          item.id === itemId
            ? { ...item, category: newCategoryName }
            : item
        )
      );

      if (isConsolidatedView) {
        setCardItemGroups(prevGroups =>
          prevGroups.map(group => ({
            ...group,
            items: group.items.map(item =>
              item.id === itemId ? { ...item, category: newCategoryName } : item
            )
          }))
        );
      }
    } catch (err: any) {
      const errorMessage = extractErrorMessage(err, t('invoices.errorUpdatingCategory'));
      setItemError(errorMessage);
      setTimeout(() => setItemError(null), 5000);
    } finally {
      setUpdatingCategoryItemId(null);
    }
  }, [invoiceItems, selectedInvoice, isConsolidatedView, categoryMap, t, extractErrorMessage]);

  // --- Payment ---

  const handleOpenPayModal = useCallback((invoice: Invoice) => {
    setInvoiceToPay(invoice);
    setPayAmount(((invoice.totalAmount || 0) - (invoice.paidAmount || 0)).toFixed(2));
    setShowPayModal(true);
    setPayError(null);
  }, []);

  const handleClosePayModal = useCallback(() => {
    setShowPayModal(false);
    setInvoiceToPay(null);
    setPayAmount('');
    setPayError(null);
  }, []);

  const handlePayTotal = useCallback(() => {
    if (!invoiceToPay) return;
    const totalRemaining = (invoiceToPay.totalAmount || 0) - (invoiceToPay.paidAmount || 0);
    setPayAmount(totalRemaining.toFixed(2));
  }, [invoiceToPay]);

  const handlePayInvoice = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    if (!invoiceToPay) return;
    const amount = parseFloat(payAmount);
    if (isNaN(amount) || amount <= 0) {
      setPayError(t('invoices.enterValidAmount'));
      return;
    }
    setPaying(true);
    setPayError(null);
    try {
      const cards = invoiceToPay._consolidatedCards;
      if (cards && cards.length > 1) {
        const totalRemaining = cards.reduce((s, c) => s + (c.totalAmount || 0) - (c.paidAmount || 0), 0);
        for (const card of cards) {
          const cardRemaining = (card.totalAmount || 0) - (card.paidAmount || 0);
          if (cardRemaining <= 0) continue;
          const cardPayment = Math.round((cardRemaining / totalRemaining) * amount * 100) / 100;
          if (cardPayment > 0) {
            await apiService.payInvoice(card.id, { amount: cardPayment });
          }
        }
      } else {
        await apiService.payInvoice(invoiceToPay.id, { amount });
      }
      await loadInvoices();
      handleClosePayModal();
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || t('invoices.errorPayingInvoice');
      setPayError(errorMessage);
    } finally {
      setPaying(false);
    }
  }, [invoiceToPay, payAmount, t, loadInvoices, handleClosePayModal]);

  // --- Delete ---

  const handleDeleteInvoice = useCallback(async (invoice: Invoice) => {
    setInvoiceToDelete(invoice);
    setShowDeleteModal(true);
    setLoadingDeleteInfo(true);
    setDeleteInfo(null);
    try {
      const info = await apiService.getInvoiceDeleteInfo(invoice.id);
      setDeleteInfo(info);
    } catch {
      setDeleteInfo(null);
    } finally {
      setLoadingDeleteInfo(false);
    }
  }, []);

  const handleConfirmDelete = useCallback(async () => {
    if (!invoiceToDelete) return;
    setDeleting(true);
    try {
      await apiService.deleteInvoice(invoiceToDelete.id);
      setShowDeleteModal(false);
      setInvoiceToDelete(null);
      setDeleteInfo(null);
      await loadInvoices();
    } catch {
      setError(t('invoices.failedToDeleteInvoice') || 'Erro ao excluir fatura');
    } finally {
      setDeleting(false);
    }
  }, [invoiceToDelete, loadInvoices, t]);

  const handleCloseDeleteModal = useCallback(() => {
    setShowDeleteModal(false);
    setInvoiceToDelete(null);
    setDeleteInfo(null);
  }, []);

  const handleProjectInstallments = useCallback(async () => {
    if (!selectedInvoice) return;
    setProjectingInstallments(true);
    try {
      const result = await apiService.projectInstallments(selectedInvoice.id);
      if (result.projectedCount > 0) {
        alert(t('invoices.projectedSuccess', {
          count: result.projectedCount,
          defaultValue: `${result.projectedCount} item(ns) projetado(s) com sucesso!`
        }));
        await loadInvoices();
      } else {
        alert(t('invoices.noInstallmentsToProject',
          'Nenhuma parcela para projetar nesta fatura.'));
      }
    } catch {
      setError(t('invoices.projectionError',
        'Erro ao projetar parcelas.'));
    } finally {
      setProjectingInstallments(false);
    }
  }, [selectedInvoice, loadInvoices, t]);

  // --- Computed ---

  const computedGroupedInvoices = groupInvoices(filteredInvoices);

  return {
    activeTab,
    setActiveTab,
    invoices,
    creditCards,
    categories,
    loading,
    error,
    setError,
    showCreateForm,
    setShowCreateForm,
    formData,
    handleInputChange,
    handleSubmit,
    handleCancel,
    showFilters,
    setShowFilters,
    filters,
    filteredInvoices,
    summary,
    handleFilterChange,
    clearFilters,
    groupedInvoices: computedGroupedInvoices,
    showDetailsModal,
    setShowDetailsModal,
    selectedInvoice,
    setSelectedInvoice,
    invoiceItems,
    setInvoiceItems,
    loadingItems,
    isConsolidatedView,
    cardItemGroups,
    setCardItemGroups,
    activeCardTab,
    setActiveCardTab,
    itemForm,
    setItemForm,
    addingItem,
    removingItemId,
    itemError,
    setItemError,
    showAddMore,
    quickAddMode,
    updatingCategoryItemId,
    handleItemInputChange,
    handleQuickAdd,
    handleAddItem,
    handleAddMore,
    handleFinishAdding,
    handleQuickCategorySelect,
    handleQuickAmountSelect,
    handleQuickDateSelect,
    handleRemoveItem,
    handleViewDetails,
    handleViewConsolidatedDetails,
    handleCloseDetails,
    categoryMap,
    sortedCategories,
    categoryLookup,
    handleUpdateItemCategory,
    showShareModal,
    selectedItemForSharing,
    handleShareItem,
    handleCloseShareModal,
    handleSharesUpdated,
    showPayModal,
    invoiceToPay,
    payAmount,
    setPayAmount,
    paying,
    payError,
    handleOpenPayModal,
    handleClosePayModal,
    handlePayTotal,
    handlePayInvoice,
    showDeleteModal,
    invoiceToDelete,
    deleteInfo,
    loadingDeleteInfo,
    deleting,
    handleDeleteInvoice,
    handleConfirmDelete,
    handleCloseDeleteModal,
    projectingInstallments,
    handleProjectInstallments,
    formatInvoiceAmount,
    getInvoiceUserAmount,
    getInvoiceUserPaid,
    getInvoiceUserRemaining,
    getConsolidatedCards,
    isConsolidated,
    loadInvoices,
  };
}
