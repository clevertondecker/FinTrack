import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import './LanguageSelector.css';

/**
 * Language selector component with dropdown menu.
 * Uses React Portal to ensure dropdown appears above all other content.
 */
const LanguageSelector: React.FC = () => {
  const { i18n, t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const [dropdownPosition, setDropdownPosition] = useState({ top: 0, left: 0 });
  const buttonRef = useRef<HTMLButtonElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  /**
   * Changes the application language and closes the dropdown.
   */
  const changeLanguage = (lng: string) => {
    i18n.changeLanguage(lng);
    setIsOpen(false);
  };

  /**
   * Toggles the dropdown visibility and calculates its position.
   */
  const toggleDropdown = () => {
    if (buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      setDropdownPosition({
        top: rect.bottom + window.scrollY + 8,
        left: rect.right - 140 // 140px is the min-width of dropdown
      });
    }
    setIsOpen(!isOpen);
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (buttonRef.current && !buttonRef.current.contains(event.target as Node) &&
          dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  /**
   * Renders the dropdown menu using React Portal.
   * This ensures it appears above all other content.
   */
  const renderDropdown = () => {
    if (!isOpen) return null;

    return createPortal(
      <div 
        className="language-options-portal"
        ref={dropdownRef}
        style={{
          position: 'absolute',
          top: dropdownPosition.top,
          left: dropdownPosition.left,
          zIndex: 999999
        }}
      >
        <button
          className={`language-option ${i18n.language === 'en' ? 'active' : ''}`}
          onClick={() => changeLanguage('en')}
        >
          <span className="flag-icon">🇺🇸</span>
          <span>{t('language.en')}</span>
        </button>
        
        <button
          className={`language-option ${i18n.language === 'pt' ? 'active' : ''}`}
          onClick={() => changeLanguage('pt')}
        >
          <span className="flag-icon">🇧🇷</span>
          <span>{t('language.pt')}</span>
        </button>
      </div>,
      document.body
    );
  };

  return (
    <div className="language-selector">
      <div className="language-dropdown">
        <button className="language-button" onClick={toggleDropdown} ref={buttonRef}>
          <span className="flag-icon">
            {i18n.language === 'pt' ? '🇧🇷' : '🇺🇸'}
          </span>
          <span className="language-text">
            {i18n.language === 'pt' ? t('language.pt') : t('language.en')}
          </span>
          <span className={`dropdown-arrow ${isOpen ? 'rotated' : ''}`}>▼</span>
        </button>
        
        {renderDropdown()}
      </div>
    </div>
  );
};

export default LanguageSelector; 