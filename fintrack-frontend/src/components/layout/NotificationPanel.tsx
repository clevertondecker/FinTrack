import React, { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import {
  Bell,
  AlertTriangle,
  AlertCircle,
  Info,
  CreditCard,
  Target,
  Users,
  RefreshCw,
  ChevronRight,
  X,
  CheckCheck,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  fetchNotifications,
  AppNotification,
  NotificationSeverity,
  NotificationType,
} from '../../services/notificationService';

const DISMISSED_STORAGE_KEY = 'fintrack_dismissed_notifications';

const SEVERITY_STYLES: Record<NotificationSeverity, { bg: string; border: string; icon: string }> = {
  urgent: { bg: 'bg-red-50', border: 'border-red-200', icon: 'text-red-500' },
  warning: { bg: 'bg-amber-50', border: 'border-amber-200', icon: 'text-amber-500' },
  info: { bg: 'bg-blue-50', border: 'border-blue-200', icon: 'text-blue-500' },
};

const SEVERITY_ICONS: Record<NotificationSeverity, React.ElementType> = {
  urgent: AlertCircle,
  warning: AlertTriangle,
  info: Info,
};

const TYPE_ICONS: Partial<Record<NotificationType, React.ElementType>> = {
  OVERDUE_INVOICE: CreditCard,
  UPCOMING_INVOICE: CreditCard,
  BUDGET_EXCEEDED: Target,
  BUDGET_NEAR_LIMIT: Target,
  PENDING_SHARES: Users,
  SUBSCRIPTION_SUGGESTION: RefreshCw,
};

function loadDismissedIds(): Set<string> {
  try {
    const raw = localStorage.getItem(DISMISSED_STORAGE_KEY);
    if (!raw) return new Set();
    return new Set(JSON.parse(raw) as string[]);
  } catch {
    return new Set();
  }
}

function persistDismissedIds(ids: Set<string>) {
  localStorage.setItem(DISMISSED_STORAGE_KEY, JSON.stringify(Array.from(ids)));
}

function pruneStaleIds(dismissed: Set<string>, activeIds: Set<string>): Set<string> {
  const pruned = new Set(Array.from(dismissed).filter((id) => activeIds.has(id)));
  if (pruned.size !== dismissed.size) persistDismissedIds(pruned);
  return pruned;
}

interface NotificationItemProps {
  notification: AppNotification;
  onNavigate: (notification: AppNotification) => void;
  onDismiss: (e: React.MouseEvent, id: string) => void;
}

function NotificationItem({ notification, onNavigate, onDismiss }: NotificationItemProps) {
  const { t } = useTranslation();
  const style = SEVERITY_STYLES[notification.severity];
  const SeverityIcon = SEVERITY_ICONS[notification.severity];
  const TypeIconComponent = TYPE_ICONS[notification.type];

  return (
    <li>
      <div
        className={`w-full flex items-start gap-3 px-4 py-3 text-left transition-colors ${style.bg} ${style.border} border-l-4`}
      >
        <div className="mt-0.5 flex-shrink-0">
          <SeverityIcon size={18} className={style.icon} />
        </div>

        <button
          onClick={() => onNavigate(notification)}
          className="flex-1 min-w-0 text-left hover:opacity-80"
        >
          <div className="flex items-center gap-1.5 mb-0.5">
            {TypeIconComponent && <TypeIconComponent size={14} className="text-gray-400" />}
            <span className="text-sm font-medium text-gray-800 truncate">
              {t(notification.titleKey)}
            </span>
          </div>
          <p className="text-xs text-gray-500 leading-relaxed">
            {t(notification.descriptionKey, notification.descriptionParams ?? {})}
          </p>
        </button>

        <div className="flex items-center gap-1 flex-shrink-0 mt-0.5">
          <button
            onClick={(e) => onDismiss(e, notification.id)}
            className="p-1 rounded hover:bg-gray-200 text-gray-300 hover:text-gray-500 transition-colors"
            title={t('notifications.dismiss')}
          >
            <X size={14} />
          </button>
          <ChevronRight size={16} className="text-gray-300" />
        </div>
      </div>
    </li>
  );
}

export default function NotificationPanel() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [allNotifications, setAllNotifications] = useState<AppNotification[]>([]);
  const [dismissedIds, setDismissedIds] = useState<Set<string>>(() => loadDismissedIds());
  const [loading, setLoading] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  const visibleNotifications = useMemo(
    () => allNotifications.filter((n) => !dismissedIds.has(n.id)),
    [allNotifications, dismissedIds]
  );

  const { urgentCount, totalCount, badgeCount } = useMemo(() => {
    const urgent = visibleNotifications.filter((n) => n.severity === 'urgent').length;
    const total = visibleNotifications.length;
    return { urgentCount: urgent, totalCount: total, badgeCount: urgent > 0 ? urgent : total };
  }, [visibleNotifications]);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchNotifications();
      setAllNotifications(data);
      const activeIds = new Set(data.map((n) => n.id));
      setDismissedIds((prev) => pruneStaleIds(prev, activeIds));
    } catch {
      setAllNotifications([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleToggle = useCallback(() => {
    setIsOpen((prev) => {
      if (!prev) loadNotifications();
      return !prev;
    });
  }, [loadNotifications]);

  const handleDismiss = useCallback((e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    setDismissedIds((prev) => {
      const next = new Set(prev);
      next.add(id);
      persistDismissedIds(next);
      return next;
    });
  }, []);

  const handleDismissAll = useCallback(() => {
    const allIds = new Set(allNotifications.map((n) => n.id));
    setDismissedIds(allIds);
    persistDismissedIds(allIds);
  }, [allNotifications]);

  const handleNotificationClick = useCallback(
    (notification: AppNotification) => {
      setIsOpen(false);
      navigate(notification.route);
    },
    [navigate]
  );

  useEffect(() => {
    if (!isOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  return (
    <div className="relative" ref={panelRef}>
      <button
        onClick={handleToggle}
        className="relative p-2 rounded-full hover:bg-gray-100 transition-colors"
        aria-label={t('notifications.title')}
      >
        <Bell size={20} className="md:w-[22px] md:h-[22px]" />
        {badgeCount > 0 && !isOpen && (
          <span
            className={`absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] flex items-center justify-center text-[10px] font-bold text-white rounded-full px-1 ${
              urgentCount > 0 ? 'bg-red-500' : 'bg-blue-500'
            }`}
          >
            {badgeCount > 9 ? '9+' : badgeCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 md:w-96 bg-white rounded-xl shadow-2xl border border-gray-200 z-50 overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 bg-gray-50">
            <h3 className="text-sm font-semibold text-gray-800">
              {t('notifications.title')}
            </h3>
            <div className="flex items-center gap-2">
              {totalCount > 0 && (
                <>
                  <span className="text-xs text-gray-500">
                    {t('notifications.count', { count: totalCount })}
                  </span>
                  <button
                    onClick={handleDismissAll}
                    className="p-1 rounded hover:bg-gray-200 text-gray-400"
                    title={t('notifications.dismissAll')}
                  >
                    <CheckCheck size={16} />
                  </button>
                </>
              )}
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 rounded hover:bg-gray-200 text-gray-400"
              >
                <X size={16} />
              </button>
            </div>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-8 text-sm text-gray-500">
                {t('common.loading')}
              </div>
            ) : visibleNotifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-gray-400">
                <Bell size={32} className="mb-2 opacity-40" />
                <span className="text-sm">{t('notifications.empty')}</span>
              </div>
            ) : (
              <ul className="divide-y divide-gray-50">
                {visibleNotifications.map((n) => (
                  <NotificationItem
                    key={n.id}
                    notification={n}
                    onNavigate={handleNotificationClick}
                    onDismiss={handleDismiss}
                  />
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
