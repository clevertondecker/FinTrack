import React from 'react';
import { CreditCard, FileText, Users, BarChart2, Home, Upload, X, UserCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const menuItems = [
    { icon: <Home size={22} />, label: t('sidebar.dashboard'), view: 'main' },
    { icon: <CreditCard size={22} />, label: t('sidebar.creditCards'), view: 'creditCards' },
    { icon: <FileText size={22} />, label: t('sidebar.invoices'), view: 'invoices' },
    { icon: <Upload size={22} />, label: t('sidebar.importInvoices'), view: 'importInvoices' },
    { icon: <UserCircle size={22} />, label: t('sidebar.people'), view: 'people' },
    { icon: <Users size={22} />, label: t('sidebar.shares'), view: 'myShares' },
    { icon: <BarChart2 size={22} />, label: t('sidebar.reports'), view: 'expenseReport', disabled: false },
  ];

  const handleMenuClick = (view: string) => {
    // Close sidebar on mobile after navigation
    onClose();
    
    // Usar URLs específicas para cada seção
    switch (view) {
      case 'main':
        navigate('/dashboard');
        break;
      case 'creditCards':
        navigate('/dashboard/credit-cards');
        break;
      case 'invoices':
        navigate('/dashboard/invoices');
        break;
      case 'importInvoices':
        navigate('/dashboard/import-invoices');
        break;
      case 'people':
        navigate('/dashboard/people');
        break;
      case 'myShares':
        navigate('/dashboard/my-shares');
        break;
      case 'expenseReport':
        navigate('/dashboard/expense-report');
        break;
      default:
        navigate('/dashboard');
    }
  };

  return (
    <>
      {/* Overlay for mobile */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-30 md:hidden"
          onClick={onClose}
        />
      )}
      
      {/* Sidebar */}
      <aside
        className={`
          fixed left-0 top-0 h-full w-60 bg-darkBg text-lightText flex flex-col shadow-lg z-40
          transform transition-transform duration-300 ease-in-out
          ${isOpen ? 'translate-x-0' : '-translate-x-full'}
          md:translate-x-0
        `}
      >
        <div className="flex items-center justify-between px-6 py-5 border-b border-gray-700">
          <span className="text-2xl font-bold tracking-tight">FinTrack</span>
          {/* Close button for mobile */}
          <button
            onClick={onClose}
            className="p-1 rounded-lg hover:bg-gray-700 transition-colors md:hidden"
          >
            <X size={24} />
          </button>
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
    </>
  );
} 