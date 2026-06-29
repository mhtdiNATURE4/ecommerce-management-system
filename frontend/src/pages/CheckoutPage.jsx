import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { getToken } from '../services/auth';

function CheckoutPage() {
  const navigate = useNavigate();
  const [cartItems, setCartItems] = useState([]);
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [placing, setPlacing] = useState(false);
  const [success, setSuccess] = useState(false);
  const [addressForm, setAddressForm] = useState({ street: '', city: '', country: '', zipCode: '' });
  const [addressSubmitting, setAddressSubmitting] = useState(false);
  const [addressError, setAddressError] = useState('');
  const [addressSuccess, setAddressSuccess] = useState('');
  const [isAddressFormOpen, setIsAddressFormOpen] = useState(false);

  const isAuthenticated = Boolean(getToken());

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadCheckoutData() {
      setLoading(true);
      setError('');

      try {
        const [items, userAddresses] = await Promise.all([
          api.get('/cart'),
          api.get('/addresses')
        ]);

        if (isMounted) {
          const itemList = Array.isArray(items) ? items : [];
          const userAddressList = Array.isArray(userAddresses) ? userAddresses : [];
          if (itemList.length === 0) {
            navigate('/cart', { replace: true });
            return;
          }
          setCartItems(itemList);
          setAddresses(userAddressList);
          if (userAddressList.length > 0) {
            setSelectedAddressId(String(userAddressList[0].id));
          }
        }
      } catch (err) {
        if (isMounted) {
          setError('Unable to load checkout data. Please try again.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadCheckoutData();

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, navigate]);

  async function handleAddShippingAddress(event) {
    event.preventDefault();

    if (!addressForm.street.trim() || !addressForm.city.trim() || !addressForm.country.trim()) {
      setAddressError('Please fill in the street, city, and country.');
      return;
    }

    setAddressSubmitting(true);
    setAddressError('');
    setAddressSuccess('');

    try {
      const createdAddress = await api.post('/addresses', {
        street: addressForm.street.trim(),
        city: addressForm.city.trim(),
        country: addressForm.country.trim(),
        zipCode: addressForm.zipCode.trim()
      });

      const updatedAddresses = [...addresses, createdAddress];
      setAddresses(updatedAddresses);
      setSelectedAddressId(String(createdAddress.id));
      setAddressForm({ street: '', city: '', country: '', zipCode: '' });
      setAddressSuccess('Shipping address saved successfully.');
      setIsAddressFormOpen(false);
    } catch (err) {
      setAddressError(err?.message || 'Unable to save your address. Please try again.');
    } finally {
      setAddressSubmitting(false);
    }
  }

  async function handlePlaceOrder() {
    if (!selectedAddressId) {
      setError('Please select a shipping address.');
      return;
    }

    setPlacing(true);
    setError('');

    try {
      await api.post('/orders/checkout', {
        shippingAddressId: parseInt(selectedAddressId, 10)
      });

      setSuccess(true);
      window.dispatchEvent(new Event('cart-updated'));
      setTimeout(() => {
        navigate('/orders', { replace: true });
      }, 1500);
    } catch (err) {
      setError(err?.message || 'Unable to place order. Please try again.');
      setPlacing(false);
    }
  }

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '30%', marginBottom: '0.5rem' }} />
          <div className="skeleton skeleton-text" style={{ width: '50%', marginBottom: '1.5rem' }} />

          <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
            <div style={{ flex: '1 1 280px' }}>
              <div className="skeleton" style={{ height: '180px', borderRadius: '12px' }} />
            </div>

            <div style={{ flex: '1 1 280px', display: 'grid', gap: '0.75rem' }}>
              {[0, 1, 2, 3].map((item) => (
                <div key={item} className="skeleton skeleton-text" />
              ))}
              <div className="skeleton" style={{ height: '2.8rem', borderRadius: '10px' }} />
            </div>
          </div>
        </section>
      </div>
    );
  }

  if (success) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding" style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>✅</div>
          <h2 className="page-title">Order Placed Successfully!</h2>
          <p className="page-subtitle">Your order is confirmed and being processed.</p>
          <p className="muted" style={{ marginTop: '0.5rem' }}>Redirecting you to your orders...</p>
        </section>
      </div>
    );
  }

  const totalAmount = cartItems.reduce(
    (sum, item) => sum + (parseFloat(item.totalPrice) || 0),
    0
  );

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Checkout</h2>
        <p className="page-subtitle" style={{ marginBottom: '1rem' }}>Review your items and confirm your delivery details.</p>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', marginBottom: '1.25rem' }}>
          <span style={{ fontWeight: 700, color: 'var(--primary)' }}>🛒 Cart</span>
          <span style={{ color: 'var(--muted)' }}>→</span>
          <span style={{ fontWeight: 700, color: 'var(--primary)' }}>📦 Checkout</span>
          <span style={{ color: 'var(--muted)' }}>→</span>
          <span style={{ color: 'var(--muted)' }}>✅ Confirm</span>
        </div>

        {error ? <div className="status-message status-error" style={{ marginBottom: '1rem' }}>{error}</div> : null}

        <div style={{ marginBottom: '1.5rem' }}>
          <h3 className="section-title" style={{ marginTop: 0, marginBottom: '0.75rem' }}>Delivery Details</h3>
          <div className="panel-card" style={{ padding: '1rem', display: 'grid', gap: '1rem' }}>
            <button
              type="button"
              onClick={() => setIsAddressFormOpen((prev) => !prev)}
              className="btn btn-secondary"
              style={{ width: '100%', justifyContent: 'space-between', display: 'flex' }}
            >
              <span>Add a new shipping address</span>
              <span>{isAddressFormOpen ? '−' : '+'}</span>
            </button>

            {isAddressFormOpen ? (
              <div>
                {addressError ? <div className="status-message status-error" style={{ marginBottom: '0.75rem' }}>{addressError}</div> : null}
                {addressSuccess ? <div className="status-message status-success" style={{ marginBottom: '0.75rem' }}>{addressSuccess}</div> : null}
                <form onSubmit={handleAddShippingAddress} className="stack-sm">
                  <label className="form-field">
                    <span className="muted">Street</span>
                    <input type="text" value={addressForm.street} onChange={(e) => setAddressForm((prev) => ({ ...prev, street: e.target.value }))} className="form-control" placeholder="Street" />
                  </label>
                  <label className="form-field">
                    <span className="muted">City</span>
                    <input type="text" value={addressForm.city} onChange={(e) => setAddressForm((prev) => ({ ...prev, city: e.target.value }))} className="form-control" placeholder="City" />
                  </label>
                  <label className="form-field">
                    <span className="muted">Country</span>
                    <input type="text" value={addressForm.country} onChange={(e) => setAddressForm((prev) => ({ ...prev, country: e.target.value }))} className="form-control" placeholder="Country" />
                  </label>
                  <label className="form-field">
                    <span className="muted">Postal Code</span>
                    <input type="text" value={addressForm.zipCode} onChange={(e) => setAddressForm((prev) => ({ ...prev, zipCode: e.target.value }))} className="form-control" placeholder="Postal Code" />
                  </label>
                  <button type="submit" disabled={addressSubmitting} className="btn btn-secondary">
                    {addressSubmitting ? 'Saving...' : 'Save Address'}
                  </button>
                </form>
              </div>
            ) : null}

            <div>
              <h3 style={{ marginTop: 0, marginBottom: '0.75rem' }}>Shipping Address</h3>
              {addresses.length === 0 ? null : (
                <label className="form-field">
                  <span className="muted">Select delivery address</span>
                  <select
                    value={selectedAddressId}
                    onChange={(e) => setSelectedAddressId(e.target.value)}
                    disabled={placing}
                    className="form-control"
                    style={{ cursor: placing ? 'not-allowed' : 'pointer' }}
                  >
                    <option value="">-- Select an address --</option>
                    {addresses.map((address) => (
                      <option key={address.id} value={String(address.id)}>
                        {address.street}, {address.city}, {address.country} {address.zipCode}
                      </option>
                    ))}
                  </select>
                </label>
              )}
            </div>
          </div>
        </div>
      </section>

      <section className="panel panel-padding">
        <h3 className="section-title" style={{ marginTop: 0 }}>Order Summary</h3>
        <div className="stack-sm">
          {cartItems.map((item) => (
            <div key={item.cartItemId} className="panel-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.9rem 1rem', flexWrap: 'wrap', gap: '0.75rem' }}>
              <div>
                <p style={{ margin: '0 0 0.2rem', fontWeight: 600 }}>{item.productName}</p>
                <p className="muted" style={{ margin: 0, fontSize: '0.92rem' }}>
                  Qty: {item.quantity} × ${parseFloat(item.unitPrice).toFixed(2)}
                </p>
              </div>
              <strong>${parseFloat(item.totalPrice).toFixed(2)}</strong>
            </div>
          ))}
        </div>

        <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '2px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
          <span style={{ fontSize: '1.1rem', fontWeight: 600 }}>Total:</span>
          <strong style={{ fontSize: '1.4rem' }}>${totalAmount.toFixed(2)}</strong>
        </div>

        <button type="button" onClick={handlePlaceOrder} disabled={placing || addresses.length === 0} className="btn btn-primary" style={{ marginTop: '1.25rem', width: '100%' }}>
          {placing ? 'Placing Order...' : 'Place Order'}
        </button>
      </section>
    </div>
  );
}

export default CheckoutPage;
