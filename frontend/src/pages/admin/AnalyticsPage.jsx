import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { isAdminAuthenticated } from '../../services/auth';

function AnalyticsPage() {
  const navigate = useNavigate();
  const [recommendations, setRecommendations] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAdminAuthenticated()) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadAnalytics() {
      setLoading(true);
      setError('');

      try {
        const [recommendationData, customerData] = await Promise.all([
          api.get('/recommendations'),
          api.get('/admin/segments')
        ]);

        if (!isMounted) return;

        setRecommendations(Array.isArray(recommendationData) ? recommendationData : []);
        setCustomers(Array.isArray(customerData) ? customerData : []);
      } catch (err) {
        if (isMounted) {
          setError(err?.message || 'Unable to load analytics data.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadAnalytics();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const recommendationSummary = useMemo(() => {
    const totalRules = recommendations.length;
    const uniqueProducts = new Set();
    let highestConfidence = null;

    recommendations.forEach((rule) => {
      if (rule?.productName) {
        uniqueProducts.add(rule.productName);
      }

      if (Array.isArray(rule?.recommendations)) {
        rule.recommendations.forEach((item) => {
          if (item?.productName) {
            uniqueProducts.add(item.productName);
          }
          if (item?.score != null && (highestConfidence == null || Number(item.score) > Number(highestConfidence))) {
            highestConfidence = item.score;
          }
        });
      }
    });

    return [
      { label: 'Recommendation Rules', value: totalRules },
      { label: 'Unique Products', value: uniqueProducts.size },
      { label: 'Highest Confidence', value: highestConfidence == null ? 'N/A' : Number(highestConfidence).toFixed(2) }
    ];
  }, [recommendations]);

  const customerSummary = useMemo(() => {
    const vipCount = customers.filter((customer) => normalizeSegment(customer.segment) === 'VIP').length;
    const regularCount = customers.filter((customer) => normalizeSegment(customer.segment) === 'REGULAR').length;
    const lowValueCount = customers.filter((customer) => normalizeSegment(customer.segment) === 'LOW_VALUE').length;

    return [
      { label: 'Total Customers', value: customers.length },
      { label: 'VIP', value: vipCount },
      { label: 'Regular', value: regularCount },
      { label: 'Low Value', value: lowValueCount }
    ];
  }, [customers]);

  const topRecommendations = useMemo(() => {
    return [...recommendations]
      .map((rule) => ({
        productName: rule?.productName || 'Unknown product',
        recommendations: Array.isArray(rule?.recommendations) ? rule.recommendations : [],
        bestScore: getBestScore(rule?.recommendations || [])
      }))
      .sort((a, b) => (b.bestScore ?? -1) - (a.bestScore ?? -1))
      .slice(0, 5);
  }, [recommendations]);

  const topCustomers = useMemo(() => {
    return [...customers]
      .sort((a, b) => Number(b.totalSpent || 0) - Number(a.totalSpent || 0))
      .slice(0, 5);
  }, [customers]);

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
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <h2 className="page-title">Analytics</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Analytics</h2>
        <p className="page-subtitle">Review customer behavior and product recommendation performance in one place.</p>
      </section>

      <section className="panel panel-padding">
        <h3 className="section-title" style={{ marginTop: 0 }}>Recommendation Analytics</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
          {recommendationSummary.map((item) => (
            <div key={item.label} className="panel-card" style={{ padding: '1rem', borderLeft: '4px solid var(--primary)', borderRadius: '0 12px 12px 0' }}>
              <div className="muted" style={{ fontSize: '0.9rem' }}>{item.label}</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '0.35rem' }}>{item.value}</div>
            </div>
          ))}
        </div>

        {topRecommendations.length === 0 ? (
          <div className="empty-state">No recommendation data is available yet.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: 'var(--surface-muted)', textAlign: 'left' }}>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Product A</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Recommended With</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Confidence</th>
                </tr>
              </thead>
              <tbody>
                {topRecommendations.map((rule) => {
                  const topItem = rule.recommendations[0];
                  return (
                    <tr key={rule.productName}>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>{rule.productName}</td>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                        {rule.recommendations.length === 0 ? '—' : rule.recommendations.map((item) => item?.productName || 'Unknown').join(', ')}
                      </td>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                        {topItem?.score != null ? Number(topItem.score).toFixed(2) : '—'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="panel panel-padding">
        <h3 className="section-title" style={{ marginTop: 0 }}>Customer Segmentation</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
          {customerSummary.map((item) => (
            <div key={item.label} className="panel-card" style={{ padding: '1rem', borderLeft: '4px solid var(--success)', borderRadius: '0 12px 12px 0' }}>
              <div className="muted" style={{ fontSize: '0.9rem' }}>{item.label}</div>
              <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '0.35rem' }}>{item.value}</div>
            </div>
          ))}
        </div>

        {topCustomers.length === 0 ? (
          <div className="empty-state">No customer segmentation data is available yet.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: 'var(--surface-muted)', textAlign: 'left' }}>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Customer</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Email</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Spending</th>
                  <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Segment</th>
                </tr>
              </thead>
              <tbody>
                {topCustomers.map((customer) => {
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

function getBestScore(recommendations) {
  return recommendations.reduce((bestScore, item) => {
    const score = Number(item?.score ?? -1);
    return Number.isFinite(score) && score > bestScore ? score : bestScore;
  }, -1);
}

function normalizeSegment(segment) {
  return String(segment || '').trim().toUpperCase();
}

function badgeStyle(segment) {
  const normalized = normalizeSegment(segment);

  if (normalized === 'VIP') {
    return { background: 'var(--warning-bg)', color: 'var(--warning-text)' };
  }

  if (normalized === 'REGULAR') {
    return { background: 'var(--info-bg)', color: 'var(--info-text)' };
  }

  if (normalized === 'LOW_VALUE') {
    return { background: 'var(--success-bg)', color: 'var(--success-text)' };
  }

  return { background: 'var(--neutral-bg)', color: 'var(--neutral-text)' };
}

export default AnalyticsPage;
