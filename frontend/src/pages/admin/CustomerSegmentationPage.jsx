import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { isAdminAuthenticated } from '../../services/auth';

const FILTER_OPTIONS = ['All', 'VIP', 'REGULAR', 'LOW_VALUE'];
const SORT_OPTIONS = [
  { value: 'name', label: 'Customer Name' },
  { value: 'spending', label: 'Total Spending' },
  { value: 'segment', label: 'Segment' }
];

function CustomerSegmentationPage() {
  const navigate = useNavigate();
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [segmentFilter, setSegmentFilter] = useState('All');
  const [sortKey, setSortKey] = useState('spending');
  const [sortDirection, setSortDirection] = useState('desc');

  useEffect(() => {
    if (!isAdminAuthenticated()) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadSegments() {
      setLoading(true);
      setError('');

      try {
        const data = await api.get('/admin/segments');
        if (!isMounted) return;
        setCustomers(Array.isArray(data) ? data : []);
      } catch (err) {
        if (isMounted) {
          setError(err?.message || 'Unable to load customer segmentation data.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadSegments();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const summary = useMemo(() => {
    const totalCustomers = customers.length;
    const vipCount = customers.filter((customer) => normalizeSegment(customer.segment) === 'VIP').length;
    const regularCount = customers.filter((customer) => normalizeSegment(customer.segment) === 'REGULAR').length;
    const lowValueCount = customers.filter((customer) => normalizeSegment(customer.segment) === 'LOW_VALUE').length;

    return [
      { label: 'Total Customers', value: totalCustomers },
      { label: 'VIP Customers', value: vipCount },
      { label: 'Regular Customers', value: regularCount },
      { label: 'Low Value Customers', value: lowValueCount }
    ];
  }, [customers]);

  const filteredCustomers = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    return customers
      .filter((customer) => {
        const segment = normalizeSegment(customer.segment);
        if (segmentFilter !== 'All' && segment !== segmentFilter) {
          return false;
        }

        if (!query) return true;

        const name = String(customer.name || '').toLowerCase();
        const email = String(customer.email || '').toLowerCase();
        return name.includes(query) || email.includes(query);
      })
      .sort((a, b) => {
        let comparison = 0;
        if (sortKey === 'name') {
          comparison = String(a.name || '').localeCompare(String(b.name || ''));
        } else if (sortKey === 'spending') {
          const aValue = Number(a.totalSpent || 0);
          const bValue = Number(b.totalSpent || 0);
          comparison = aValue - bValue;
        } else if (sortKey === 'segment') {
          comparison = String(normalizeSegment(a.segment)).localeCompare(String(normalizeSegment(b.segment)));
        }

        return sortDirection === 'desc' ? -comparison : comparison;
      });
  }, [customers, searchTerm, segmentFilter, sortKey, sortDirection]);

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '35%', marginBottom: '0.5rem' }} />
          <div className="skeleton skeleton-text" style={{ width: '55%', marginBottom: '1rem' }} />

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
            {[0, 1, 2, 3].map((item) => (
              <div key={item} className="panel-card" style={{ padding: '1rem', display: 'grid', gap: '0.4rem' }}>
                <div className="skeleton skeleton-text" style={{ width: '50%' }} />
                <div className="skeleton skeleton-title" style={{ width: '40%' }} />
              </div>
            ))}
          </div>
        </section>

        <section className="panel panel-padding">
          <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem' }}>
            <div className="skeleton" style={{ height: '2.8rem', width: '100%', borderRadius: '10px' }} />
            <div className="skeleton" style={{ height: '2.8rem', width: '140px', borderRadius: '10px' }} />
            <div className="skeleton" style={{ height: '2.8rem', width: '140px', borderRadius: '10px' }} />
          </div>

          <div className="stack-sm">
            {[0, 1, 2, 3, 4].map((row) => (
              <div key={row} className="panel-card" style={{ padding: '0.75rem', display: 'flex', justifyContent: 'space-between' }}>
                <div style={{ display: 'grid', gap: '0.35rem' }}>
                  <div className="skeleton skeleton-text" style={{ width: '140px' }} />
                  <div className="skeleton skeleton-text" style={{ width: '180px' }} />
                </div>
                <div style={{ display: 'flex', gap: '0.75rem' }}>
                  <div className="skeleton skeleton-text" style={{ width: '70px' }} />
                  <div className="skeleton" style={{ width: '80px', height: '1.6rem', borderRadius: '999px' }} />
                </div>
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
          <h2 className="page-title">Customer Segmentation</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Customer Segmentation</h2>
        <p className="page-subtitle">Review customer segments provided by the backend.</p>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
          {summary.map((item) => (
            <div key={item.label} className="panel-card" style={{ padding: '1rem', borderLeft: '4px solid var(--primary)', borderRadius: '0 12px 12px 0' }}>
              <div className="muted" style={{ fontSize: '0.9rem' }}>{item.label}</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '0.35rem' }}>{item.value}</div>
            </div>
          ))}
        </div>
      </section>

      <section className="panel panel-padding">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
          <input
            type="text"
            value={searchTerm}
            onChange={(event) => setSearchTerm(event.target.value)}
            placeholder="Search by customer name or email"
            className="form-control"
            style={{ flex: '1 1 240px' }}
          />

          <select value={segmentFilter} onChange={(event) => setSegmentFilter(event.target.value)} className="form-control">
            {FILTER_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option === 'All' ? 'All Segments' : option}
              </option>
            ))}
          </select>

          <select value={sortKey} onChange={(event) => setSortKey(event.target.value)} className="form-control">
            {SORT_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>

          <button type="button" onClick={() => setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))} className="btn btn-secondary">
            {sortDirection === 'asc' ? 'Ascending' : 'Descending'}
          </button>
        </div>

        {filteredCustomers.length === 0 ? (
          <div className="empty-state">No customers match the current search and filter.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: 'var(--surface-muted)', textAlign: 'left' }}>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Customer Name</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Email</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Total Spending</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Customer Segment</th>
                </tr>
              </thead>
              <tbody>
                {filteredCustomers.map((customer) => {
                  const segment = normalizeSegment(customer.segment);
                  return (
                    <tr key={customer.userId ?? `${customer.name}-${customer.totalSpent}`}>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>{customer.name || 'Unknown customer'}</td>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>{customer.email || '—'}</td>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>${Number(customer.totalSpent || 0).toFixed(2)}</td>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                        <span style={{ ...badgeStyle(segment), display: 'inline-block', padding: '0.3rem 0.6rem', borderRadius: '999px', fontSize: '0.85rem', fontWeight: 600 }}>
                          {segment || 'UNKNOWN'}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function normalizeSegment(segment) {
  return String(segment || '').trim().toUpperCase();
}

function badgeStyle(segment) {
  const normalized = normalizeSegment(segment);

  if (normalized === 'VIP') {
    return { background: '#fef3c7', color: '#92400e' };
  }

  if (normalized === 'REGULAR') {
    return { background: '#dbeafe', color: '#1d4ed8' };
  }

  if (normalized === 'LOW_VALUE') {
    return { background: '#dcfce7', color: '#166534' };
  }

  return { background: '#f3f4f6', color: '#374151' };
}

export default CustomerSegmentationPage;
