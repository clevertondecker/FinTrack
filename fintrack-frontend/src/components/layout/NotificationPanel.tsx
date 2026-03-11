import React, { useState, useRef, useEffect, useCallback } from 'react';
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
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  fetchNotifications,
  AppNotification,
  NotificationSeverity,
} from '../../services/notificationService';

const SEVERITY_STYLES: Record<NotificationSeverity, { bg: string; border: string; icon: string }> = {
  urgent: { bg: 'bg-red-50', border: 'border-red-200', icon: 'text-red-500' },
  warning: { bg: 'bg-amber-50', border: 'border-amber-200', icon: 'text-amber-500' },
  info: { bg: 'bg-blue-50', border: 'border-blue-200', icon: 'text-blue-500' },
};

function SeverityIcon({ severity }: { severity: NotificationSeverity }) {
  const style = SEVERITY_STYLES[severity];
  switch (severity) {
    case 'urgent':
      return <AlertCircle size={18} className={style.icon} />;
    case 'warning':
      return <AlertTriangle size={18} className={style.icon} />;
    default:
      return <Info size={18} className={style.icon} />;
  }
}

function TypeIcon({ type }: { type: string }) {
  switch (type) {
    case 'OVERDUE_INVOICE':
    case 'UPCOMING_INVOICE':
      return <CreditCard size={14} className="text-gray-400" />;
    case 'BUDGET_EXCEEDED':
    case 'BUDGET_NEAR_LIMIT':
      return <Target size={14} className="text-gray-400" />;
    case 'PENDING_SHARES':
      return <Users size={14} className="text-gray-400" />;
    case 'SUBSCRIPTION_SUGGESTION':
      return <RefreshCw size={14} className="text-gray-400" />;
    default:
      return null;
  }
}

export default function NotificationPanel() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchNotifications();
      setNotifications(data);
    } catch {
      setNotifications([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const handleToggle = useCallback(() => {
    setIsOpen((prev) => {
      const opening = !prev;
      if (opening) {
        loadNotifications();
      }
      return opening;
    });
  }, [loadNotifications]);

  const handleNotificationClick = useCallback(
    (notification: AppNotification) => {
      setIsOpen(false);
      navigate(notification.route);
    },
    [navigate]
  );

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const urgentCount = notifications.filter((n) => n.severity === 'urgent').length;
  const totalCount = notifications.length;
  const badgeCount = urgentCount > 0 ? urgentCount : totalCount;

  return (
    <div className="relative" ref={panelRef}>
      {/* Bell Button */}
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

      {/* Dropdown Panel */}
      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 md:w-96 bg-white rounded-xl shadow-2xl border border-gray-200 z-50 overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 bg-gray-50">
            <h3 className="text-sm font-semibold text-gray-800">
              {t('notifications.title')}
            </h3>
            <div className="flex items-center gap-2">
              {totalCount > 0 && (
                <span className="text-xs text-gray-500">
                  {t('notifications.count', { count: totalCount })}
                </span>
              )}
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 rounded hover:bg-gray-200 text-gray-400"
              >
                <X size={16} />
              </button>
            </div>
          </div>

          {/* Content */}
          <div className="max-h-80 overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-8 text-sm text-gray-500">
                {t('common.loading')}
              </div>
            ) : notifications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-8 text-gray-400">
                <Bell size={32} className="mb-2 opacity-40" />
                <span className="text-sm">{t('notifications.empty')}</span>
              </div>
            ) : (
              <ul className="divide-y divide-gray-50">
                {notifications.map((n) => {
                  const style = SEVERITY_STYLES[n.severity];
                  return (
                    <li key={n.id}>
                      <button
                        onClick={() => handleNotificationClick(n)}
                        className={`w-full flex items-start gap-3 px-4 py-3 text-left hover:bg-gray-50 transition-colors ${style.bg} ${style.border} border-l-4`}
                      >
                        <div className="mt-0.5 flex-shrink-0">
                          <SeverityIcon severity={n.severity} />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-1.5 mb-0.5">
                            <TypeIcon type={n.type} />
                            <span className="text-sm font-medium text-gray-800 truncate">
                              {t(n.titleKey)}
                            </span>
                          </div>
                          <p className="text-xs text-gray-500 leading-relaxed">
                            {t(n.descriptionKey, n.descriptionParams ?? {})}
                          </p>
                        </div>
                        <ChevronRight size={16} className="mt-1 flex-shrink-0 text-gray-300" />
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
