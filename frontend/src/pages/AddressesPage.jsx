import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { getToken } from '../services/auth';

function AddressesPage() {
  const navigate = useNavigate();
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({
    street: '',
    city: '',
    country: '',
    zipCode: ''
  });

  useEffect(() => {
    if (!getToken()) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadAddresses() {
      setLoading(true);
      setError('');

      try {
        const data = await api.get('/addresses');
        if (isMounted) {
          setAddresses(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (isMounted) {
          setError(err?.message || 'Unable to load your saved addresses.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadAddresses();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  async function handleSubmit(e) {
    e.preventDefault();

    if (!form.street.trim() || !form.city.trim() || !form.country.trim()) {
      setError('Please fill in the street, city, and country.');
      return;
    }

    setSubmitting(true);
    setError('');
    setSuccess('');

    try {
      const createdAddress = await api.post('/addresses', {
        street: form.street.trim(),
        city: form.city.trim(),
        country: form.country.trim(),
        zipCode: form.zipCode.trim()
      });

      setAddresses((prev) => [...prev, createdAddress]);
      setForm({ street: '', city: '', country: '', zipCode: '' });
      setSuccess('Address saved successfully.');
    } catch (err) {
      setError(err?.message || 'Unable to save your address.');
    } finally {
      setSubmitting(false);
    }
  }

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">My Addresses</h2>
        <p className="page-subtitle">Save a shipping address so checkout can be completed quickly.</p>

        <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '1rem 0' }} />

        <h3 className="section-title" style={{ marginTop: 0 }}>Add New Address</h3>

        <form onSubmit={handleSubmit} className="stack-sm" style={{ marginBottom: '1.5rem' }}>
          <label className="form-field">
            <span className="muted">Street</span>
            <input type="text" placeholder="Street" value={form.street} onChange={(e) => updateField('street', e.target.value)} className="form-control" required />
          </label>
          <label className="form-field">
            <span className="muted">City</span>
            <input type="text" placeholder="City" value={form.city} onChange={(e) => updateField('city', e.target.value)} className="form-control" required />
          </label>
          <label className="form-field">
            <span className="muted">Country</span>
            <input type="text" placeholder="Country" value={form.country} onChange={(e) => updateField('country', e.target.value)} className="form-control" required />
          </label>
          <label className="form-field">
            <span className="muted">Postal Code</span>
            <input type="text" placeholder="Postal Code" value={form.zipCode} onChange={(e) => updateField('zipCode', e.target.value)} className="form-control" />
          </label>
          <button type="submit" disabled={submitting} className="btn btn-primary">
            {submitting ? 'Saving...' : 'Add Address'}
          </button>
        </form>
      </section>

      <section className="panel panel-padding">
        <h3 className="section-title" style={{ marginTop: 0 }}>Saved Addresses</h3>

        {error ? <div className="status-message status-error" style={{ marginBottom: '1rem' }}>{error}</div> : null}
        {success ? <div className="status-message status-success" style={{ marginBottom: '1rem' }}>{success}</div> : null}

        {loading ? (
          <div className="stack-sm">
            {[0, 1, 2].map((item) => (
              <div key={item} className="panel-card" style={{ padding: '1rem', display: 'grid', gap: '0.4rem' }}>
                <div className="skeleton skeleton-title" style={{ width: '55%' }} />
                <div className="skeleton skeleton-text" style={{ width: '40%' }} />
              </div>
            ))}
          </div>
        ) : addresses.length === 0 ? (
          <div className="empty-state">No saved addresses yet. Add your first shipping address above.</div>
        ) : (
          <div className="stack-sm">
            {addresses.map((address) => (
              <div key={address.id} className="panel-card" style={{ padding: '1rem' }}>
                <p style={{ margin: '0 0 0.25rem', fontWeight: 600 }}>
                  <span style={{ marginRight: '0.4rem' }}>📍</span>
                  {address.street}
                </p>
                <p className="muted" style={{ margin: 0 }}>
                  {address.city}, {address.country} {address.zipCode}
                </p>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default AddressesPage;
