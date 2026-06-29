import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { getToken } from '../services/auth';
import AddressForm from '../components/AddressForm';

function AddressesPage() {
  const navigate = useNavigate();
  const [addresses, setAddresses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

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

  function handleAddressAdded(createdAddress) {
    setAddresses((prev) => [...prev, createdAddress]);
    setError('');
    setSuccess('Address saved successfully.');
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        {loading ? (
          <>
            <div className="skeleton skeleton-title" style={{ width: '35%', marginBottom: '0.5rem' }} />
            <div className="skeleton skeleton-text" style={{ width: '55%', marginBottom: '1rem' }} />
          </>
        ) : (
          <>
            <h2 className="page-title">My Addresses</h2>
            <p className="page-subtitle">Save a shipping address so checkout can be completed quickly.</p>
          </>
        )}

        <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '1rem 0' }} />

        <h3 className="section-title" style={{ marginTop: 0 }}>Add New Address</h3>

        <div style={{ marginBottom: '1.5rem' }}>
          <AddressForm onAddressAdded={handleAddressAdded} submitLabel="Add Address" />
        </div>
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
