import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { isAdminAuthenticated } from '../../services/auth';

function RecommendationAnalyticsPage() {
  const navigate = useNavigate();
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortKey, setSortKey] = useState('confidence');
  const [sortDirection, setSortDirection] = useState('desc');

  useEffect(() => {
    if (!isAdminAuthenticated()) {
      navigate('/login', { replace: true });
      return;
    }

    let isMounted = true;

    async function loadRecommendations() {
      setLoading(true);
      setError('');

      try {
        const data = await api.get('/recommendations');
        if (!isMounted) return;
        setRules(Array.isArray(data) ? data : []);
      } catch (err) {
        if (isMounted) {
          setError(err?.message || 'Unable to load recommendation analytics.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadRecommendations();

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  const summary = useMemo(() => {
    const totalRules = rules.length;
    const uniqueProducts = new Set();
    let highestConfidence = null;

    rules.forEach((rule) => {
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
      { label: 'Total Recommendation Rules', value: totalRules },
      { label: 'Unique Products', value: uniqueProducts.size },
      { label: 'Highest Confidence Rule', value: highestConfidence == null ? 'N/A' : Number(highestConfidence).toFixed(2) }
    ];
  }, [rules]);

  const filteredRules = useMemo(() => {
    const query = searchTerm.trim().toLowerCase();

    const list = rules.filter((rule) => {
      if (!query) return true;
      const productA = String(rule?.productName || '').toLowerCase();
      const productB = (rule?.recommendations || [])
        .map((item) => String(item?.productName || '').toLowerCase())
        .join(' ');
      return productA.includes(query) || productB.includes(query);
    });

    const sorted = [...list].sort((a, b) => {
      if (sortKey === 'productA') {
        return sortDirection === 'asc'
          ? String(a?.productName || '').localeCompare(String(b?.productName || ''))
          : String(b?.productName || '').localeCompare(String(a?.productName || ''));
      }

      if (sortKey === 'productB') {
        const aValue = (a?.recommendations || []).map((item) => item?.productName || '').join(', ');
        const bValue = (b?.recommendations || []).map((item) => item?.productName || '').join(', ');
        return sortDirection === 'asc'
          ? aValue.localeCompare(bValue)
          : bValue.localeCompare(aValue);
      }

      const aBestScore = getBestScore(a?.recommendations || []);
      const bBestScore = getBestScore(b?.recommendations || []);
      if (aBestScore == null && bBestScore == null) return 0;
      if (aBestScore == null) return 1;
      if (bBestScore == null) return -1;
      return sortDirection === 'asc' ? aBestScore - bBestScore : bBestScore - aBestScore;
    });

    return sorted;
  }, [rules, searchTerm, sortKey, sortDirection]);

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '40%', marginBottom: '0.5rem' }} />
          <div className="skeleton skeleton-text" style={{ width: '60%', marginBottom: '1rem' }} />

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
            {[0, 1, 2].map((item) => (
              <div key={item} className="panel-card" style={{ padding: '1rem', display: 'grid', gap: '0.4rem' }}>
                <div className="skeleton skeleton-text" style={{ width: '55%' }} />
                <div className="skeleton skeleton-title" style={{ width: '45%' }} />
              </div>
            ))}
          </div>
        </section>

        <section className="panel panel-padding">
          <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '1rem' }}>
            <div className="skeleton" style={{ height: '2.8rem', borderRadius: '10px', flex: 1 }} />
            <div className="skeleton" style={{ height: '2.8rem', borderRadius: '10px', width: '160px' }} />
          </div>

          <div className="stack-sm">
            {[0, 1, 2, 3, 4].map((row) => (
              <div key={row} className="panel-card" style={{ padding: '0.75rem', display: 'flex', justifyContent: 'space-between', gap: '1rem' }}>
                <div className="skeleton skeleton-text" style={{ flex: 1 }} />
                <div className="skeleton skeleton-text" style={{ flex: 1 }} />
                <div className="skeleton skeleton-text" style={{ width: '60px' }} />
                <div className="skeleton" style={{ width: '70px', height: '1.6rem', borderRadius: '999px' }} />
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
          <h2 className="page-title">Recommendation Analytics</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  if (filteredRules.length === 0) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <h2 className="page-title">Recommendation Analytics</h2>
          <p className="page-subtitle">Product association recommendations created from customer purchase history.</p>

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
          <p className="page-subtitle" style={{ margin: 0 }}>
            No recommendation data is available yet. Complete more customer purchases to generate product association recommendations.
          </p>
        </section>
      </div>
    );
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Recommendation Analytics</h2>
        <p className="page-subtitle">Product association recommendations generated by the backend.</p>

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
            placeholder="Search by product A or product B"
            className="form-control"
            style={{ flex: '1 1 240px' }}
          />

          <select value={sortKey} onChange={(event) => setSortKey(event.target.value)} className="form-control">
            <option value="confidence">Confidence</option>
            <option value="productA">Product A</option>
            <option value="productB">Product B</option>
          </select>

          <button type="button" onClick={() => setSortDirection((current) => (current === 'asc' ? 'desc' : 'asc'))} className="btn btn-secondary">
            {sortDirection === 'asc' ? 'Ascending' : 'Descending'}
          </button>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: 'var(--surface-muted)', textAlign: 'left' }}>
                <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Product A</th>
                <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Frequently Bought Together (Product B)</th>
                <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Confidence / Score</th>
                <th style={{ padding: '0.75rem', borderBottom: '1px solid var(--border)' }}>Recommendation Strength</th>
              </tr>
            </thead>
            <tbody>
              {filteredRules.map((rule) => {
                const recommendations = Array.isArray(rule?.recommendations) ? rule.recommendations : [];
                const topRecommendation = recommendations[0] || null;
                return (
                  <tr key={`${rule?.productId || 'product'}-${rule?.productName || 'name'}`}>
                    <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>{rule?.productName || 'Unknown product'}</td>
                    <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                      {recommendations.length === 0 ? '—' : recommendations.map((item) => item?.productName || 'Unknown').join(', ')}
                    </td>
                    <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                      {topRecommendation?.score != null ? Number(topRecommendation.score).toFixed(2) : '—'}
                    </td>
                    <td style={{ padding: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
                      {topRecommendation?.score != null ? (
                        <span style={{
                          borderRadius: '999px',
                          padding: '0.2rem 0.6rem',
                          fontSize: '0.8rem',
                          fontWeight: 700,
                          display: 'inline-block',
                          background: Number(topRecommendation.score) >= 0.7 ? '#dcfce7' : Number(topRecommendation.score) >= 0.4 ? '#fef9c3' : '#fee2e2',
                          color: Number(topRecommendation.score) >= 0.7 ? '#166534' : Number(topRecommendation.score) >= 0.4 ? '#854d0e' : '#b91c1c'
                        }}>
                          {Number(topRecommendation.score) >= 0.7 ? 'High' : Number(topRecommendation.score) >= 0.4 ? 'Medium' : 'Low'}
                        </span>
                      ) : '—'}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function getBestScore(recommendations) {
  if (!Array.isArray(recommendations) || recommendations.length === 0) return null;
  const scores = recommendations
    .map((item) => (item?.score != null ? Number(item.score) : null))
    .filter((value) => value != null && Number.isFinite(value));
  return scores.length > 0 ? Math.max(...scores) : null;
}

export default RecommendationAnalyticsPage;
