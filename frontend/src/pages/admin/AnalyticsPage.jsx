import { useEffect, useMemo, useState } from 'react';
import { User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { isAdminAuthenticated } from '../../services/auth';

function AnalyticsPage() {
  const navigate = useNavigate();
  const [recommendations, setRecommendations] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [products, setProducts] = useState([]);
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
        const [recommendationData, customerData, productData] = await Promise.all([
          api.get('/recommendations'),
          api.get('/admin/segments'),
          api.get('/products').catch(() => [])
        ]);

        if (!isMounted) return;

        setRecommendations(Array.isArray(recommendationData) ? recommendationData : []);
        setCustomers(Array.isArray(customerData) ? customerData : []);
        setProducts(Array.isArray(productData) ? productData : []);
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

  const productLookup = useMemo(() => {
    const lookup = new Map();

    products.forEach((product) => {
      if (product?.name) {
        lookup.set(normalizeProductName(product.name), product);
      }
    });

    return lookup;
  }, [products]);

  const topRecommendations = useMemo(() => {
    const seen = new Set();
    const deduplicated = recommendations.filter((rule) => {
      const recommended = rule?.recommendations?.[0]?.productName;
      if (!recommended || !rule?.productName) return true;
      const key = [normalizeProductName(rule.productName), normalizeProductName(recommended)].sort().join('||');
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });

    return [...deduplicated]
      .map((rule) => ({
        productName: rule?.productName || 'Unknown product',
        productImage: getProductImage(rule?.productName, productLookup),
        recommendations: Array.isArray(rule?.recommendations)
          ? rule.recommendations.map((item) => ({
              ...item,
              productImage: getProductImage(item?.productName, productLookup)
            }))
          : [],
        bestScore: getBestScore(rule?.recommendations || [])
      }))
      .sort((a, b) => (b.bestScore ?? -1) - (a.bestScore ?? -1))
      .slice(0, 5);
  }, [recommendations, productLookup]);

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
                {topRecommendations.map((rule, index) => {
                  const topItem = rule.recommendations[0];
                  return (
                    <tr key={`${rule.productName}-${index}`}>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                          <img
                            src={rule.productImage || 'https://picsum.photos/seed/default/600/400'}
                            alt={rule.productName}
                            style={{ width: '56px', height: '56px', objectFit: 'cover', borderRadius: '8px' }}
                          />
                          <span>{rule.productName}</span>
                        </div>
                      </td>
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)', verticalAlign: 'top' }}>
                        {rule.recommendations.length === 0 ? '—' : (
                          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '0.45rem', minWidth: '220px' }}>
                            {rule.recommendations.map((item, itemIndex) => (
                              <div key={`${item?.productName || 'recommendation'}-${itemIndex}`} style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', padding: '0.4rem 0.6rem', background: 'var(--surface-muted)', borderRadius: '10px', minWidth: 0 }}>
                                <img
                                  src={item.productImage || 'https://picsum.photos/seed/default/600/400'}
                                  alt={item?.productName || 'Recommended product'}
                                  style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '8px', flexShrink: 0 }}
                                />
                                <span style={{ fontSize: '0.92rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item?.productName || 'Unknown'}</span>
                              </div>
                            ))}
                          </div>
                        )}
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
            <div key={item.label} className="panel-card" style={{ padding: '1rem', borderLeft: '4px solid var(--success-text)', borderRadius: '0 12px 12px 0' }}>
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
                      <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                          <div style={{ width: '56px', height: '56px', borderRadius: '50%', background: 'linear-gradient(135deg, var(--primary), #8b5cf6)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontWeight: 700, fontSize: '1.1rem' }}>
                            <User size={28} color="white" />
                          </div>
                          <span>{customer.name || 'Unknown customer'}</span>
                        </div>
                      </td>
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

function getProductImage(productName, productLookup) {
  const matchedProduct = productLookup.get(normalizeProductName(productName));
  return matchedProduct?.imageUrl || 'https://picsum.photos/seed/default/600/400';
}

function normalizeProductName(productName) {
  return String(productName || '').trim().toLowerCase();
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
