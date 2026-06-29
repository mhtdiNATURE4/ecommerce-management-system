import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { getToken } from '../services/auth';

function CartPage() {
  const navigate = useNavigate();
  const [cartItems, setCartItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [updatingItemId, setUpdatingItemId] = useState(null);
  const [removingItemId, setRemovingItemId] = useState(null);

  const isAuthenticated = Boolean(getToken());

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadCart() {
      setLoading(true);
      setError('');

      try {
        const items = await api.get('/cart');
        const normalizedItems = Array.isArray(items) ? items : [];

        if (!isMounted) {
          return;
        }

        const itemsWithImages = await Promise.all(
          normalizedItems.map(async (item) => {
            if (!item?.productId) {
              return { ...item, imageUrl: '' };
            }

            try {
              const product = await api.get(`/products/${item.productId}`);
              return { ...item, imageUrl: product?.imageUrl || '' };
            } catch {
              return { ...item, imageUrl: '' };
            }
          })
        );

        setCartItems(itemsWithImages);
      } catch (err) {
        if (isMounted) {
          setError('Unable to load your cart. Please try again.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadCart();

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, navigate]);

  async function handleUpdateQuantity(cartItemId, newQuantity) {
    const parsedQuantity = Number(newQuantity);
    if (!Number.isInteger(parsedQuantity) || parsedQuantity < 1) {
      return;
    }

    setUpdatingItemId(cartItemId);
    setError('');

    try {
      const response = await api.put(`/cart/${cartItemId}/quantity`, { quantity: newQuantity });
      setCartItems((prevItems) =>
        prevItems.map((item) =>
          item.cartItemId === cartItemId
            ? {
                ...item,
                quantity: response.quantity,
                totalPrice: response.totalPrice
              }
            : item
        )
      );
      window.dispatchEvent(new Event('cart-updated'));
    } catch (err) {
      setError(err?.message || 'Unable to update quantity. Please try again.');
    } finally {
      setUpdatingItemId(null);
    }
  }

  async function handleRemoveItem(cartItemId) {
    setRemovingItemId(cartItemId);
    setError('');

    try {
      await api.delete(`/cart/${cartItemId}`);
      setCartItems((prevItems) => prevItems.filter((item) => item.cartItemId !== cartItemId));
      window.dispatchEvent(new Event('cart-updated'));
    } catch (err) {
      setError(err?.message || 'Unable to remove item. Please try again.');
      setRemovingItemId(null);
    }
  }

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '35%', marginBottom: '0.5rem' }} />
          <div className="skeleton skeleton-text" style={{ width: '25%' }} />
        </section>

        <div className="stack-sm">
          {[0, 1, 2].map((item) => (
            <div key={item} className="panel-card" style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: '1rem', padding: '1rem' }}>
              <div className="skeleton" style={{ height: '120px', borderRadius: '8px' }} />
              <div style={{ display: 'grid', gap: '0.75rem' }}>
                <div className="skeleton skeleton-title" style={{ width: '60%' }} />
                <div className="skeleton skeleton-text" style={{ width: '35%' }} />
                <div className="skeleton" style={{ height: '2.5rem', borderRadius: '10px', width: '130px' }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <h2 className="page-title">Unable to load your cart</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  const totalItems = cartItems.reduce((sum, item) => sum + item.quantity, 0);
  const totalAmount = cartItems.reduce(
    (sum, item) => sum + (parseFloat(item.totalPrice) || 0),
    0
  );

  if (cartItems.length === 0) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding" style={{ textAlign: 'center' }}>
          <h2 className="page-title">Your Shopping Cart</h2>
          <p className="page-subtitle" style={{ marginBottom: '1rem' }}>Your shopping cart is empty.</p>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', fontSize: '0.85rem', marginBottom: '1rem' }}>
            <span style={{ fontWeight: 700, color: 'var(--primary)' }}>🛒 Cart</span>
            <span style={{ color: 'var(--muted)' }}>→</span>
            <span style={{ color: 'var(--muted)' }}>📦 Checkout</span>
            <span style={{ color: 'var(--muted)' }}>→</span>
            <span style={{ color: 'var(--muted)' }}>✅ Confirm</span>
          </div>
          <Link to="/products" className="btn btn-primary">
            Continue Shopping
          </Link>
        </section>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Your Shopping Cart</h2>
        <p className="page-subtitle" style={{ marginBottom: '1.25rem' }}>
          {totalItems} item{totalItems !== 1 ? 's' : ''} in your cart
        </p>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', marginBottom: '1.25rem' }}>
          <span style={{ fontWeight: 700, color: 'var(--primary)' }}>🛒 Cart</span>
          <span style={{ color: 'var(--muted)' }}>→</span>
          <span style={{ color: 'var(--muted)' }}>📦 Checkout</span>
          <span style={{ color: 'var(--muted)' }}>→</span>
          <span style={{ color: 'var(--muted)' }}>✅ Confirm</span>
        </div>

        <div className="stack-sm">
          {cartItems.map((item) => (
            <div key={item.cartItemId} className="panel-card" style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: '1rem', padding: '1rem', alignItems: 'start', transition: 'box-shadow 0.2s ease' }}>
              <img
                src={item.imageUrl || 'https://picsum.photos/seed/product/200/200'}
                alt={item.productName}
                style={{ width: '100%', height: '120px', objectFit: 'cover', borderRadius: '8px' }}
              />

              <div style={{ display: 'grid', gap: '0.75rem' }}>
                <div>
                  <h4 style={{ margin: '0 0 0.25rem' }}>{item.productName}</h4>
                  <p className="muted" style={{ margin: 0 }}>Unit Price: ${parseFloat(item.unitPrice).toFixed(2)}</p>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                  <label className="form-field" style={{ minWidth: '130px' }}>
                    <span className="muted">Quantity</span>
                    <input
                      type="number"
                      min="1"
                      value={item.quantity}
                      onChange={(e) => handleUpdateQuantity(item.cartItemId, parseInt(e.target.value, 10))}
                      disabled={updatingItemId === item.cartItemId}
                      className="form-control"
                      style={{ width: '70px' }}
                    />
                  </label>

                  <button type="button" onClick={() => handleRemoveItem(item.cartItemId)} disabled={removingItemId === item.cartItemId} className="btn btn-secondary">
                    {removingItemId === item.cartItemId ? 'Removing...' : 'Remove'}
                  </button>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.25rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <span className="muted">Line Total:</span>
                  <strong style={{ fontSize: '1.1rem' }}>${parseFloat(item.totalPrice).toFixed(2)}</strong>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="panel panel-padding" style={{ display: 'grid', gap: '1rem' }}>
        <h3 className="section-title" style={{ marginTop: 0 }}>Order Summary</h3>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingBottom: '1rem', borderBottom: '1px solid var(--border)' }}>
          <span style={{ fontSize: '1.05rem', fontWeight: 600 }}>Total Items:</span>
          <span>{totalItems}</span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '1.15rem', fontWeight: 600 }}>Total Amount:</span>
          <strong style={{ fontSize: '1.6rem' }}>${totalAmount.toFixed(2)}</strong>
        </div>

        <div style={{ display: 'grid', gap: '0.75rem' }}>
          <button type="button" onClick={() => navigate('/checkout')} className="btn btn-primary" style={{ width: '100%' }}>
            Proceed to Checkout
          </button>

          <Link to="/products" className="btn btn-secondary">
            Continue Shopping
          </Link>
        </div>
      </section>
    </div>
  );
}

export default CartPage;
