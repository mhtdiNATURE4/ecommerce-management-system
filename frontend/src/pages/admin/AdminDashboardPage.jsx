import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { isAdminAuthenticated } from '../../services/auth';

function AdminDashboardPage() {
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState(null);
  const [orders, setOrders] = useState([]);
  const [lowStockProducts, setLowStockProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAdminAuthenticated()) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadDashboardData() {
      setLoading(true);
      setError('');

      try {
        const [dashboardData, allOrders, lowStockData] = await Promise.all([
          api.get('/admin/dashboard'),
          api.get('/orders/all').catch(() => []),
          api.get('/admin/reports/low-stock?threshold=10').catch(() => [])
        ]);

        if (!isMounted) return;

        setDashboard(dashboardData || null);
        setOrders(Array.isArray(allOrders) ? allOrders : []);
        setLowStockProducts(Array.isArray(lowStockData) ? lowStockData : []);
      } catch (err) {
        if (isMounted) {
          setError(err?.message || 'Unable to load the admin dashboard right now.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadDashboardData();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const metrics = useMemo(() => {
    if (!dashboard) return [];

    const createdCount = orders.filter((order) => String(order.status || '').toUpperCase() === 'CREATED').length;
    const processingCount = orders.filter((order) => String(order.status || '').toUpperCase() === 'PROCESSING').length;

    return [
      { label: 'Total Products', value: dashboard.totalProducts ?? 'N/A' },
      { label: 'Total Customers', value: dashboard.totalUsers ?? 'N/A' },
      { label: 'Total Orders', value: dashboard.totalOrders ?? 'N/A' },
      { label: 'Total Revenue', value: dashboard.totalRevenue != null ? `$${Number(dashboard.totalRevenue).toFixed(2)}` : 'N/A' },
      { label: 'Orders in CREATED', value: createdCount },
      { label: 'Orders in PROCESSING', value: processingCount },
      { label: 'Low-stock products', value: lowStockProducts.length }
    ];
  }, [dashboard, orders, lowStockProducts]);

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '35%', marginBottom: '0.5rem' }} />
          <div className="skeleton skeleton-text" style={{ width: '55%' }} />

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginTop: '1rem' }}>
            {[0, 1, 2, 3, 4, 5, 6].map((item) => (
              <div key={item} className="panel panel-card" style={{ padding: '1.25rem', borderLeft: '4px solid var(--border)', borderRadius: '0 12px 12px 0' }}>
                <div className="skeleton skeleton-text" style={{ width: '50%', marginBottom: '0.5rem' }} />
                <div className="skeleton skeleton-title" style={{ width: '60%' }} />
              </div>
            ))}
          </div>
        </section>

        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '30%', marginBottom: '1rem' }} />
          <div className="stack-sm">
            {[0, 1, 2].map((item) => (
              <div key={item} className="panel-card" style={{ padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div className="skeleton skeleton-text" style={{ width: '40%' }} />
                <div className="skeleton" style={{ height: '1.6rem', width: '80px', borderRadius: '999px' }} />
              </div>
            ))}
          </div>
        </section>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <h2 className="page-title">Admin Dashboard</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Admin Dashboard</h2>
        <p className="page-subtitle">Overview of the store’s current performance.</p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginTop: '1rem' }}>
          {metrics.map((metric) => (
            <div
              key={metric.label}
              className="panel panel-card"
              style={{
                padding: '1.25rem',
                borderLeft: '4px solid var(--primary)',
                borderRadius: '0 12px 12px 0'
              }}
            >
              <div className="muted" style={{ fontSize: '0.85rem', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                {metric.label}
              </div>
              <div style={{ fontSize: '2rem', fontWeight: 700, marginTop: '0.35rem' }}>{metric.value}</div>
            </div>
          ))}
        </div>
      </section>

      <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '0.5rem 0' }} />

      <section className="panel panel-padding">
        <h3 className="section-title" style={{ marginTop: 0 }}>Low-stock Products</h3>
        {lowStockProducts.length === 0 ? (
          <p className="page-subtitle">No low-stock products were returned by the backend.</p>
        ) : (
          <div className="stack-sm">
            {lowStockProducts.map((product) => (
              <div key={product.id} className="panel-card" style={{ padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontWeight: 600 }}>{product.name}</div>
                  <div className="muted" style={{ fontSize: '0.85rem', marginTop: '0.2rem' }}>
                    {product.category || product.id || 'Inventory item'}
                  </div>
                </div>
                <span style={{ background: 'var(--error-bg)', color: 'var(--error-text)', borderRadius: '999px', padding: '0.2rem 0.65rem', fontSize: '0.8rem', fontWeight: 700 }}>
                  Stock: {product.stock}
                </span>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="panel panel-padding">
        <h3 className="section-title" style={{ marginTop: 0 }}>Quick Actions</h3>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <Link to="/admin/orders" className="btn btn-primary">View All Orders</Link>
          <Link to="/admin/reports" className="btn btn-secondary">View Reports</Link>
          <Link to="/admin/segments" className="btn btn-secondary">Customer Segments</Link>
        </div>
      </section>
    </div>
  );
}

export default AdminDashboardPage;
