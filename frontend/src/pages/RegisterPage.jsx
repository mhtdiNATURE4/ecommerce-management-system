import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';

function RegisterPage() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [touched, setTouched] = useState({ fullName: false, email: false, password: false, confirmPassword: false });

  function validateForm() {
    if (!fullName.trim()) {
      setError('Please enter your full name.');
      return false;
    }

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
      setError('Please choose a password.');
      return false;
    }

    if (!confirmPassword) {
      setError('Please confirm your password.');
      return false;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return false;
    }

    return true;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setTouched({ fullName: true, email: true, password: true, confirmPassword: true });
    setError('');
    setSuccess('');

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);

    try {
      await api.post('/auth/register', {
        name: fullName,
        email,
        password
      });

      setSuccess('Registration successful. Redirecting to login...');
      setTimeout(() => {
        navigate('/login', { replace: true });
      }, 1500);
    } catch (err) {
      const message = err?.message || 'Unable to register. Please check your details and try again.';
      const isNetworkError =
        err instanceof TypeError ||
        (typeof message === 'string' && /failed to fetch|network|connection refused|timeout/i.test(message));

      setError(
        isNetworkError
          ? 'Unable to connect to the server. Please try again later.'
          : message
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}>
      <div className="panel" style={{ width: '100%', maxWidth: '420px', padding: '2rem' }}>
        <div style={{ height: '4px', background: 'var(--primary)', borderRadius: '16px 16px 0 0', margin: '-2rem -2rem 1.5rem -2rem' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
          <img src="/favicon.svg" alt="iStore logo" style={{ width: '36px', height: '36px', borderRadius: '10px' }} />
          <h2 className="page-title" style={{ margin: 0 }}>iStore Register</h2>
        </div>
        <Link to="/" className="btn btn-secondary" style={{ marginBottom: '1rem', width: 'fit-content' }}>
          ← Back to Home
        </Link>
        <p className="page-subtitle" style={{ marginBottom: '1.25rem' }}>Create a new account to get started.</p>

        {error ? <div className="status-message status-error" style={{ marginBottom: '1rem' }}>{error}</div> : null}
        {success ? <div className="status-message status-success" style={{ marginBottom: '1rem' }}>{success}</div> : null}

        <form onSubmit={handleSubmit} style={{ display: 'grid', gap: '1rem' }} noValidate>
          <label className="form-field">
            <span className="muted">Full Name</span>
            <input
              type="text"
              value={fullName}
              onChange={(event) => {
                setFullName(event.target.value);
                if (touched.fullName) setError('');
              }}
              onBlur={() => setTouched((prev) => ({ ...prev, fullName: true }))}
              placeholder="Your full name"
              className="form-control"
              aria-invalid={touched.fullName && !fullName.trim()}
            />
          </label>

          <label className="form-field">
            <span className="muted">Email</span>
            <input
              type="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                if (touched.email) setError('');
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
                if (touched.password) setError('');
              }}
              onBlur={() => setTouched((prev) => ({ ...prev, password: true }))}
              autoComplete="new-password"
              placeholder="Enter your password"
              className="form-control"
              aria-invalid={touched.password && !password}
            />
            <p style={{ margin: '0.25rem 0 0', fontSize: '0.8rem', color: 'var(--muted)' }}>
              Use at least 8 characters for a strong password.
            </p>
          </label>

          <label className="form-field">
            <span className="muted">Confirm Password</span>
            <input
              type="password"
              value={confirmPassword}
              onChange={(event) => {
                setConfirmPassword(event.target.value);
                if (touched.confirmPassword) setError('');
              }}
              onBlur={() => setTouched((prev) => ({ ...prev, confirmPassword: true }))}
              autoComplete="new-password"
              placeholder="Confirm your password"
              className="form-control"
              aria-invalid={touched.confirmPassword && !confirmPassword}
            />
          </label>

          <button type="submit" disabled={isSubmitting} className="btn btn-primary">
            {isSubmitting ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '0.75rem', fontSize: '0.95rem', color: 'var(--muted)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ fontWeight: 600, color: 'var(--text)', textDecoration: 'underline' }}>
            Sign in
          </Link>
        </p>

        <p style={{ textAlign: 'center', fontSize: '0.8rem', color: 'var(--muted)', marginTop: '0.5rem' }}>
          iStore © 2025
        </p>
      </div>
    </div>
  );
}

export default RegisterPage;
