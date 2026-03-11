import React, { useState, useRef, useEffect, useCallback } from 'react';
import { HelpCircle, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useHelp } from '../../contexts/HelpContext';

type Position = 'top' | 'bottom' | 'left' | 'right';

interface HelpTooltipProps {
  textKey: string;
  position?: Position;
  children: React.ReactNode;
}

const POSITION_CLASSES: Record<Position, string> = {
  top: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
  bottom: 'top-full left-1/2 -translate-x-1/2 mt-2',
  left: 'right-full top-1/2 -translate-y-1/2 mr-2',
  right: 'left-full top-1/2 -translate-y-1/2 ml-2',
};

const ARROW_CLASSES: Record<Position, string> = {
  top: 'top-full left-1/2 -translate-x-1/2 border-t-white border-x-transparent border-b-transparent',
  bottom: 'bottom-full left-1/2 -translate-x-1/2 border-b-white border-x-transparent border-t-transparent',
  left: 'left-full top-1/2 -translate-y-1/2 border-l-white border-y-transparent border-r-transparent',
  right: 'right-full top-1/2 -translate-y-1/2 border-r-white border-y-transparent border-l-transparent',
};

export default function HelpTooltip({ textKey, position = 'top', children }: HelpTooltipProps) {
  const { t } = useTranslation();
  const { isHelpActive } = useHelp();
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);
  const popoverRef = useRef<HTMLDivElement>(null);

  const handleBadgeClick = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    setIsPopoverOpen((prev) => !prev);
  }, []);

  useEffect(() => {
    if (!isPopoverOpen) return;
    function handleClickOutside(e: MouseEvent) {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        setIsPopoverOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isPopoverOpen]);

  useEffect(() => {
    if (!isHelpActive) setIsPopoverOpen(false);
  }, [isHelpActive]);

  if (!isHelpActive) return <>{children}</>;

  return (
    <div className="relative inline-block">
      {children}
      <div ref={popoverRef} className="absolute -top-1 -right-1 z-40">
        <button
          onClick={handleBadgeClick}
          className="flex items-center justify-center w-5 h-5 rounded-full bg-blue-500 text-white shadow-md hover:bg-blue-600 transition-colors animate-pulse-slow"
          aria-label={t('help.tooltip')}
        >
          <HelpCircle size={12} />
        </button>

        {isPopoverOpen && (
          <div className={`absolute z-50 ${POSITION_CLASSES[position]}`}>
            <div className="bg-white rounded-lg shadow-xl border border-gray-200 p-3 w-64 text-left">
              <div className="flex items-start justify-between gap-2">
                <p className="text-xs text-gray-600 leading-relaxed">{t(textKey)}</p>
                <button
                  onClick={(e) => { e.stopPropagation(); setIsPopoverOpen(false); }}
                  className="flex-shrink-0 p-0.5 rounded hover:bg-gray-100 text-gray-400"
                >
                  <X size={12} />
                </button>
              </div>
            </div>
            <div className={`absolute w-0 h-0 border-[6px] ${ARROW_CLASSES[position]}`} />
          </div>
        )}
      </div>
    </div>
  );
}
