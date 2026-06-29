import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { isAdminAuthenticated } from '../../services/auth';

const STATUS_FILTERS = ['All', 'CREATED', 'PROCESSING', 'COMPLETED', 'CANCELLED'];

function getStatusBadgeStyle(status) {
  const normalized = String(status || '').toUpperCase();

  switch (normalized) {
    case 'CREATED':
      return { background: 'var(--secondary)', color: 'var(--text)' };
    case 'PROCESSING':
      return { background: '#dbeafe', color: '#1e40af' };
    case 'COMPLETED':
      return { background: 'var(--success-bg)', color: 'var(--success-text)' };
    case 'CANCELLED':
      return { background: 'var(--error-bg)', color: 'var(--error-text)' };
    default:
      return { background: 'var(--secondary)', color: 'var(--text)' };
  }
}

function AdminOrdersPage() {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedOrderId, setExpandedOrderId] = useState(null);
  const [filter, setFilter] = useState('All');
  const [search, setSearch] = useState('');
  const [busyOrderId, setBusyOrderId] = useState(null);
  const [actionMessage, setActionMessage] = useState('');
  const [actionError, setActionError] = useState('');

  useEffect(() => {
    if (!isAdminAuthenticated()) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadOrders() {
      setLoading(true);
      setError('');

      try {
        const data = await api.get('/orders/all');
        if (isMounted) {
          setOrders(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (isMounted) {
          setError(err?.message || 'Unable to load orders right now.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadOrders();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const filteredOrders = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();

    return orders.filter((order) => {
      const matchesFilter = filter === 'All' || String(order.status || '').toUpperCase() === filter.toUpperCase();
      const matchesSearch = !normalizedSearch || String(order.id).includes(normalizedSearch);
      return matchesFilter && matchesSearch;
    });
  }, [orders, filter, search]);

  async function handleTransition(orderId, action) {
    setBusyOrderId(orderId);
    setActionError('');
    setActionMessage('');

    try {
      const path = action === 'process'
        ? `/orders/${orderId}/process`
        : action === 'complete'
          ? `/orders/${orderId}/complete`
          : `/orders/${orderId}/cancel`;

      await api.put(path, {});
      setActionMessage(`Order ${orderId} updated successfully.`);
      const refreshed = await api.get('/orders/all');
      setOrders(Array.isArray(refreshed) ? refreshed : []);
    } catch (err) {
      setActionError(err?.message || 'Unable to update the order.');
    } finally {
      setBusyOrderId(null);
    }
  }

  function renderActions(order) {
    const status = String(order.status || '').toUpperCase();

    if (status === 'COMPLETED') {
      return <span style={{ color: 'var(--success-text)', fontWeight: 600 }}>Completed ✓</span>;
    }

    if (status === 'CANCELLED') {
      return <span style={{ color: 'var(--error-text)', fontWeight: 600 }}>Cancelled ✖</span>;
    }

    const isBusy = busyOrderId === order.id;

    return (
      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        {status === 'CREATED' ? (
          <>
            <button type="button" disabled={isBusy} onClick={() => handleTransition(order.id, 'process')} className="btn btn-primary">
              {isBusy ? 'Working...' : 'Start Processing'}
            </button>
            <button type="button" disabled={isBusy} onClick={() => handleTransition(order.id, 'cancel')} className="btn btn-secondary">
              {isBusy ? 'Working...' : 'Cancel'}
            </button>
          </>
        ) : null}

        {status === 'PROCESSING' ? (
          <>
            <button type="button" disabled={isBusy} onClick={() => handleTransition(order.id, 'complete')} className="btn btn-primary">
              {isBusy ? 'Working...' : 'Complete'}
            </button>
            <button type="button" disabled={isBusy} onClick={() => handleTransition(order.id, 'cancel')} className="btn btn-secondary">
              {isBusy ? 'Working...' : 'Cancel'}
            </button>
          </>
        ) : null}
      </div>
    );
  }

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '30%' }} />
        </section>

        <div className="stack-sm">
          {[0, 1, 2, 3].map((item) => (
            <div key={item} className="panel-card" style={{ padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'grid', gap: '0.4rem' }}>
                <div className="skeleton skeleton-title" style={{ width: '140px' }} />
                <div className="skeleton skeleton-text" style={{ width: '220px' }} />
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <div className="skeleton" style={{ width: '80px', height: '1.8rem', borderRadius: '999px' }} />
                <div className="skeleton skeleton-title" style={{ width: '70px' }} />
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
          <h2 className="page-title">Admin Orders</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Admin Orders</h2>
        <p className="page-subtitle">Manage customer orders and update their lifecycle.</p>

        {actionMessage ? (
          <div className="status-message status-success" style={{ marginBottom: '1rem' }}>{actionMessage}</div>
        ) : null}
        {actionError ? (
          <div className="status-message status-error" style={{ marginBottom: '1rem' }}>{actionError}</div>
        ) : null}

        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', marginBottom: '1rem' }}>
          <input
            type="text"
            placeholder="Search by order ID"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="form-control"
          />

          <select value={filter} onChange={(e) => setFilter(e.target.value)} className="form-control">
            {STATUS_FILTERS.map((status) => (
              <option key={status} value={status}>{status}</option>
            ))}
          </select>
        </div>

        {filteredOrders.length === 0 ? (
          <div className="empty-state">No orders match the current filters.</div>
        ) : (
          <div style={{ display: 'grid', gap: '1rem' }}>
            {filteredOrders.map((order) => {
              const status = String(order.status || '').toUpperCase();
              const itemCount = Array.isArray(order.items) ? order.items.length : 0;
              const subtotal = parseFloat(order.totalAmount || '0');

              return (
                <div key={order.id} className="panel-card" style={{ padding: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                    <div>
                      <div style={{ fontWeight: 700 }}>Order #{order.id}</div>
                      <div className="muted" style={{ fontSize: '0.95rem', marginTop: '0.2rem' }}>
                        Customer: {order.customerName || 'N/A'} • Items: {itemCount}
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                      <span style={{ padding: '0.35rem 0.7rem', borderRadius: '999px', fontWeight: 600, ...getStatusBadgeStyle(status) }}>{status}</span>
                      <strong>${subtotal.toFixed(2)}</strong>
                    </div>
                  </div>

                  <div className="muted" style={{ marginTop: '0.75rem', fontSize: '0.95rem' }}>
                    Shipping address: {order.shippingAddress ? `${order.shippingAddress.street}, ${order.shippingAddress.city}, ${order.shippingAddress.country}${order.shippingAddress.zipCode ? `, ${order.shippingAddress.zipCode}` : ''}` : 'N/A'}
                  </div>

                  <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                    <button type="button" onClick={() => setExpandedOrderId((prev) => prev === order.id ? null : order.id)} className="btn btn-secondary">
                      {expandedOrderId === order.id ? 'Hide details' : 'View details'}
                    </button>
                    {renderActions(order)}
                  </div>

                  {expandedOrderId === order.id ? (
                    <div className="panel panel-padding" style={{ marginTop: '1rem' }}>
                      <div style={{ fontWeight: 700, marginBottom: '0.75rem' }}>Purchased products</div>
                      {Array.isArray(order.items) && order.items.length > 0 ? (
                        <div style={{ display: 'grid', gap: '0.75rem' }}>
                          {order.items.map((item) => (
                            <div key={item.id} className="panel-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap', padding: '0.7rem 1rem' }}>
                              <div>
                                <div style={{ fontWeight: 600 }}>{item.productName}</div>
                                <div className="muted" style={{ fontSize: '0.9rem' }}>Qty: {item.quantity}</div>
                              </div>
                              <div style={{ textAlign: 'right' }}>
                                <div className="muted" style={{ fontSize: '0.9rem' }}>Unit price: ${parseFloat(item.price || '0').toFixed(2)}</div>
                                <div style={{ fontWeight: 600 }}>Line total: ${(parseFloat(item.price || '0') * item.quantity).toFixed(2)}</div>
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <div className="muted">No items available for this order.</div>
                      )}

                      <div style={{ marginTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid #e5e7eb', paddingTop: '0.75rem' }}>
                        <span style={{ fontWeight: 700 }}>Order Total</span>
                        <strong>${subtotal.toFixed(2)}</strong>
                      </div>
                    </div>
                  ) : null}
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

export default AdminOrdersPage;
