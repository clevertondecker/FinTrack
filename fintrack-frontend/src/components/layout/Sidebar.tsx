import React from 'react';
import { CreditCard, FileText, Users, BarChart2, Home, Upload } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

export default function Sidebar() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const menuItems = [
    { icon: <Home size={22} />, label: t('sidebar.dashboard'), view: 'main' },
    { icon: <CreditCard size={22} />, label: t('sidebar.creditCards'), view: 'creditCards' },
    { icon: <FileText size={22} />, label: t('sidebar.invoices'), view: 'invoices' },
    { icon: <Upload size={22} />, label: t('sidebar.importInvoices'), view: 'importInvoices' },
    { icon: <Users size={22} />, label: t('sidebar.shares'), view: 'myShares' },
    { icon: <BarChart2 size={22} />, label: t('sidebar.reports'), view: 'reports', disabled: true },
  ];

  const handleMenuClick = (view: string) => {
    navigate('/dashboard', { state: { view } });
  };

  return (
    <aside className="fixed left-0 top-0 h-full w-60 bg-darkBg text-lightText flex flex-col shadow-lg z-20">
      <div className="flex items-center gap-2 px-6 py-5 border-b border-gray-700">
        <span className="text-2xl font-bold tracking-tight">FinTrack</span>
      </div>
      <nav className="flex-1 py-4">
        <ul className="space-y-2">
          {menuItems.map((item) => (
            <li key={item.label}>
              <button
                onClick={() => !item.disabled && handleMenuClick(item.view)}
                className={`w-full flex items-center gap-3 px-6 py-3 rounded-lg transition-colors hover:bg-primary/20 ${item.disabled ? 'opacity-50 pointer-events-none' : 'cursor-pointer'}`}
              >
                {item.icon}
                <span className="text-base font-medium">{item.label}</span>
              </button>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  );
} 