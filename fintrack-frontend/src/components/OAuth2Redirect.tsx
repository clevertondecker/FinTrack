import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './OAuth2Redirect.css';

const OAuth2Redirect: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { updateAuthContext } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const handleOAuth2Redirect = async () => {
      try {
        // Verificar se há token na URL (vindo do backend)
        const token = searchParams.get('token');
        const error = searchParams.get('error');

        if (error) {
          setError('Authentication failed. Please try again.');
          setLoading(false);
          return;
        }

        if (token) {
          // Atualizar o contexto de autenticação com o novo token
          await updateAuthContext(token);
          
          // Redirecionar para o dashboard
          navigate('/dashboard');
        } else {
          setError('No authentication token received.');
          setLoading(false);
        }
      } catch (error) {
        console.error('OAuth2 redirect error:', error);
        setError('Authentication failed. Please try again.');
        setLoading(false);
      }
    };

    handleOAuth2Redirect();
  }, [searchParams, navigate, updateAuthContext]);

  if (loading) {
    return (
      <div className="oauth2-redirect-container">
        <div className="oauth2-redirect-card">
          <div className="loading-spinner"></div>
          <h2>Completing authentication...</h2>
          <p>Please wait while we complete your sign-in.</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="oauth2-redirect-container">
        <div className="oauth2-redirect-card">
          <div className="error-icon">❌</div>
          <h2>Authentication Failed</h2>
          <p>{error}</p>
          <button 
            className="retry-button"
            onClick={() => navigate('/login')}
          >
            Back to Login
          </button>
        </div>
      </div>
    );
  }

  return null;
};

export default OAuth2Redirect; 