import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { clearToken, getToken, saveToken } from '../services/auth';

function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [touched, setTouched] = useState({ email: false, password: false });

  const isAuthenticated = Boolean(getToken());

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  function validateForm() {
    if (!email.trim()) {
      setError('Please enter your email address.');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError('Please enter a valid email address.');
      return false;
    }

    if (!password) {
      setError('Please enter your password.');
      return false;
    }

    return true;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setTouched({ email: true, password: true });
    setError('');

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const response = await api.post('/auth/login', {
        email,
        password
      });

      if (!response?.token) {
        throw new Error('Unable to complete login. Please try again.');
      }

      clearToken();
      saveToken(response.token);
      navigate('/', { replace: true });
    } catch (err) {
      const message = err?.message || 'Unable to sign in. Please check your credentials and try again.';
      const isNetworkError =
        err instanceof TypeError ||
        typeof message === 'string' && /failed to fetch|network|connection refused|timeout/i.test(message);

      setError(
        isNetworkError
          ? 'Unable to connect to the server. Please try again later.'
          : message
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isAuthenticated) {
    return null;
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}>
      <div className="panel" style={{ width: '100%', maxWidth: '420px', padding: '2rem' }}>
        <div style={{ height: '4px', background: 'var(--primary)', borderRadius: '16px 16px 0 0', margin: '-2rem -2rem 1.5rem -2rem' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
          <img src="/favicon.svg" alt="iStore logo" style={{ width: '36px', height: '36px', borderRadius: '10px' }} />
          <h2 className="page-title" style={{ margin: 0 }}>iStore Login</h2>
        </div>
        <Link to="/" className="btn btn-secondary" style={{ marginBottom: '1rem', width: 'fit-content' }}>
          ← Back to Home
        </Link>
        <p className="page-subtitle" style={{ marginBottom: '1.25rem' }}>Sign in to your account to continue.</p>

        {error ? <div className="status-message status-error" style={{ marginBottom: '1rem' }}>{error}</div> : null}

        <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '1rem' }} noValidate>
          <label className="form-field">
            <span className="muted">Email</span>
            <input
              type="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                if (touched.email) {
                  setError('');
                }
              }}
              onBlur={() => setTouched((prev) => ({ ...prev, email: true }))}
              autoComplete="email"
              placeholder="you@istore.com"
              className="form-control"
              aria-invalid={touched.email && !email.trim()}
            />
          </label>

          <label className="form-field">
            <span className="muted">Password</span>
            <input
              type="password"
              value={password}
              onChange={(event) => {
                setPassword(event.target.value);
                if (touched.password) {
                  setError('');
                }
              }}
              onBlur={() => setTouched((prev) => ({ ...prev, password: true }))}
              autoComplete="current-password"
              placeholder="Enter your password"
              className="form-control"
              aria-invalid={touched.password && !password}
            />
          </label>

          <button type="submit" disabled={isSubmitting} className="btn btn-primary">
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '0.75rem', fontSize: '0.95rem', color: 'var(--muted)' }}>
          Don't have an account?{' '}
          <Link to="/register" style={{ fontWeight: 600, color: 'var(--text)', textDecoration: 'underline' }}>
            Create one
          </Link>
        </p>

        <p style={{ textAlign: 'center', fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.5rem' }}>
          iStore © 2025
        </p>
      </div>
    </div>
  );
}

export default LoginPage;
