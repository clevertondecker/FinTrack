import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import i18n from '../../i18n';
import LanguageSelector from '../LanguageSelector';

// Mock the portal
jest.mock('react-dom', () => ({
  ...jest.requireActual('react-dom'),
  createPortal: (node: React.ReactNode) => node,
}));

const renderWithI18n = (component: React.ReactElement) => {
  return render(
    <I18nextProvider i18n={i18n}>
      {component}
    </I18nextProvider>
  );
};

describe('LanguageSelector', () => {
  beforeEach(() => {
    // Reset language to English
    i18n.changeLanguage('en');
  });

  it('renders language selector button', () => {
    renderWithI18n(<LanguageSelector />);
    
    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(screen.getByText('English')).toBeInTheDocument();
    expect(screen.getByText('🇺🇸')).toBeInTheDocument();
  });

  it('shows dropdown when clicked', () => {
    renderWithI18n(<LanguageSelector />);
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    expect(screen.getByText('English')).toBeInTheDocument();
    expect(screen.getByText('Português')).toBeInTheDocument();
  });

  it('changes language when option is clicked', () => {
    renderWithI18n(<LanguageSelector />);
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    const portugueseOption = screen.getByText('Português');
    fireEvent.click(portugueseOption);
    
    expect(i18n.language).toBe('pt');
  });

  it('closes dropdown after language selection', () => {
    renderWithI18n(<LanguageSelector />);
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    const portugueseOption = screen.getByText('Português');
    fireEvent.click(portugueseOption);
    
    // Dropdown should be closed
    expect(screen.queryByText('Português')).not.toBeInTheDocument();
  });
}); 