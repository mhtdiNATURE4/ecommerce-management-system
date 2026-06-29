import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { getToken } from '../services/auth';

function OrdersPage() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedOrderId, setExpandedOrderId] = useState(null);

  const isAuthenticated = Boolean(getToken());

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadOrders() {
      setLoading(true);
      setError('');
      try {
        const data = await api.get('/orders');
        if (isMounted) {
          setOrders(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (isMounted) setError(err?.message || 'Unable to load your orders. Please try again.');
      } finally {
        if (isMounted) setLoading(false);
      }
    }

    loadOrders();

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, navigate]);

  function toggleExpand(id) {
    setExpandedOrderId((prev) => (prev === id ? null : id));
  }

  function formatDate(value) {
    if (!value) return 'N/A';

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'N/A';
    }

    return date.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  function getStatusStyle(status) {
    const normalizedStatus = String(status || '').toUpperCase();

    switch (normalizedStatus) {
      case 'CREATED':
      case 'PENDING':
        return { background: 'var(--secondary)', color: 'var(--text)' };
      case 'PROCESSING':
      case 'CONFIRMED':
        return { background: '#dbeafe', color: '#1e40af' };
      case 'SHIPPED':
        return { background: '#fef9c3', color: '#854d0e' };
      case 'DELIVERED':
      case 'COMPLETED':
        return { background: 'var(--success-bg)', color: 'var(--success-text)' };
      case 'CANCELLED':
        return { background: 'var(--error-bg)', color: 'var(--error-text)' };
      default:
        return { background: 'var(--secondary)', color: 'var(--text)' };
    }
  }

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '30%', marginBottom: '0.5rem' }} />
        </section>

        <div className="stack-sm">
          {[0, 1, 2, 3].map((item) => (
            <div key={item} className="panel-card" style={{ padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'grid', gap: '0.4rem' }}>
                <div className="skeleton skeleton-title" style={{ width: '120px' }} />
                <div className="skeleton skeleton-text" style={{ width: '200px' }} />
              </div>
              <div style={{ display: 'grid', gap: '0.4rem', alignItems: 'flex-end' }}>
                <div className="skeleton skeleton-title" style={{ width: '80px' }} />
                <div className="skeleton" style={{ height: '2.2rem', width: '110px', borderRadius: '10px' }} />
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
          <h2 className="page-title">Unable to load your orders</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  if (!orders || orders.length === 0) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding" style={{ textAlign: 'center' }}>
          <h2 className="page-title">Your Orders</h2>
          <p className="page-subtitle">You have not placed any orders yet.</p>
          <Link to="/products" className="btn btn-primary" style={{ marginTop: '1rem' }}>
            Start Shopping
          </Link>
        </section>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Your Orders</h2>

        <div className="stack-sm">
          {orders.map((order, index) => (
            <div key={order.id} className="panel-card" style={{ padding: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', flexWrap: 'wrap' }}>
                <div style={{ display: 'grid', gap: '0.4rem' }}>
                  <div style={{ fontWeight: 700 }}>Order #{index + 1}</div>
                  <div className="muted" style={{ fontSize: '0.95rem' }}>
                    Date: {formatDate(order.createdAt)}
                  </div>
                  <span style={{ ...getStatusStyle(order.status), borderRadius: '999px', padding: '0.2rem 0.65rem', fontSize: '0.8rem', fontWeight: 700, display: 'inline-block', width: 'fit-content' }}>
                    {order.status}
                  </span>
                  <button type="button" onClick={() => toggleExpand(order.id)} className="btn btn-secondary" style={{ marginTop: '0.25rem', width: 'fit-content' }}>
                    {expandedOrderId === order.id ? 'Hide details' : 'View details'}
                  </button>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontWeight: 700 }}>${parseFloat(order.totalAmount || '0').toFixed(2)}</div>
                </div>
              </div>

              {expandedOrderId === order.id && (
                <div style={{ marginTop: '1rem', display: 'grid', gap: '0.75rem' }}>
                  <div className="muted" style={{ fontSize: '0.85rem', marginBottom: '0.25rem' }}>Items in this order</div>
                  {Array.isArray(order.items) && order.items.length > 0 ? (
                    order.items.map((it) => (
                      <div key={it.id} className="panel-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                        <div>
                          <div style={{ fontWeight: 600 }}>{it.productName}</div>
                          <div className="muted" style={{ fontSize: '0.92rem' }}>Qty: {it.quantity}</div>
                        </div>
                        <div style={{ fontWeight: 700 }}>${parseFloat(it.price || '0').toFixed(2)}</div>
                      </div>
                    ))
                  ) : (
                    <div className="muted">No items available for this order.</div>
                  )}

                  <div style={{ marginTop: '0.25rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <span style={{ fontWeight: 700 }}>Order Total</span>
                    <strong>${parseFloat(order.totalAmount || '0').toFixed(2)}</strong>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

export default OrdersPage;
