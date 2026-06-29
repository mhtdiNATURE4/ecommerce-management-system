import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../services/api';
import { getToken } from '../services/auth';

function ProductDetailsPage() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [recommendationsLoading, setRecommendationsLoading] = useState(false);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(false);
  const [cartMessage, setCartMessage] = useState('');
  const [cartError, setCartError] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const isAuthenticated = Boolean(getToken());
  const navigate = useNavigate();

  useEffect(() => {
    let isMounted = true;

    async function loadProduct() {
      setLoading(true);
      setError('');
      setNotFound(false);
      setProduct(null);
      setRecommendations([]);

      try {
        const productData = await api.get(`/products/${id}`);

        if (!isMounted) {
          return;
        }

        setProduct(productData);
      } catch (err) {
        if (!isMounted) {
          return;
        }

        if (err?.status === 404) {
          setNotFound(true);
        } else {
          setError('Unable to load product details right now.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadProduct();

    return () => {
      isMounted = false;
    };
  }, [id]);

  useEffect(() => {
    let isMounted = true;

    async function loadRecommendations() {
      if (!product?.id || !isAuthenticated) {
        setRecommendations([]);
        return;
      }

      setRecommendationsLoading(true);

      try {
        const recommendationData = await api.get(`/recommendations/product/${product.id}`);
        const recommendedItems = Array.isArray(recommendationData?.recommendations) ? recommendationData.recommendations : [];

        const detailedRecommendations = await Promise.all(
          recommendedItems.map(async (item) => {
            try {
              const detail = await api.get(`/products/${item.productId}`);
              return {
                id: item.productId,
                name: item.productName,
                imageUrl: detail.imageUrl,
                price: detail.price
              };
            } catch {
              return {
                id: item.productId,
                name: item.productName,
                imageUrl: '',
                price: null
              };
            }
          })
        );

        if (!isMounted) {
          return;
        }

        setRecommendations(detailedRecommendations);
      } catch {
        if (isMounted) {
          setRecommendations([]);
        }
      } finally {
        if (isMounted) {
          setRecommendationsLoading(false);
        }
      }
    }

    loadRecommendations();

    return () => {
      isMounted = false;
    };
  }, [product?.id, isAuthenticated]);

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding" style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
          <div style={{ flex: '1 1 300px' }}>
            <div className="skeleton" style={{ height: '320px', borderRadius: '12px' }} />
          </div>

          <div style={{ flex: '1 1 300px', display: 'grid', gap: '0.75rem' }}>
            <div className="skeleton skeleton-title" style={{ width: '70%' }} />
            <div className="skeleton skeleton-text" style={{ width: '30%' }} />
            <div className="skeleton skeleton-text" style={{ width: '50%' }} />
            <div className="skeleton" style={{ height: '2.8rem', borderRadius: '10px', width: '160px' }} />
          </div>
        </section>
      </div>
    );
  }

  if (notFound) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <h2 className="page-title">Product not found</h2>
          <p className="page-subtitle">The requested product could not be found.</p>
          <Link to="/products" className="btn btn-secondary" style={{ marginTop: '1rem' }}>
            Back to products
          </Link>
        </section>
      </div>
    );
  }

  if (error) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <h2 className="page-title">Product unavailable</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  async function handleAddToCart() {
    setCartMessage('');
    setCartError('');

    if (!isAuthenticated) {
      navigate('/login', { replace: true });
      return;
    }

    setIsAdding(true);

    try {
      await api.post('/cart', { productId: product.id, quantity: 1 });
      setCartMessage('Added to cart successfully.');
      window.dispatchEvent(new Event('cart-updated'));
    } catch (err) {
      setCartError(err?.message || 'Unable to add item to cart. Please try again.');
    } finally {
      setIsAdding(false);
    }
  }

  if (!product) {
    return null;
  }

  const isOutOfStock = Number(product.stock) <= 0;
  const imageUrl = product.imageUrl || 'https://picsum.photos/seed/default/600/400';

  return (
    <div className="page-shell">
      <section className="panel panel-padding" style={{ display: 'grid', gap: '1rem' }}>
        <Link to="/products" className="muted" style={{ fontWeight: 600 }}>
          ← Back to Products
        </Link>

        <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', alignItems: 'start' }}>
          <img src={imageUrl} alt={product.name} style={{ width: '100%', height: '320px', objectFit: 'cover', borderRadius: '12px' }} />

          <div style={{ display: 'grid', gap: '0.8rem' }}>
            <h2 className="page-title" style={{ margin: 0 }}>{product.name}</h2>
            <p className="muted" style={{ margin: 0 }}>{product.categoryName || 'Uncategorized'}</p>
            <p style={{ margin: 0, lineHeight: 1.6 }}>{product.description}</p>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
              <strong style={{ fontSize: '1.25rem' }}>${product.price}</strong>
              <span className="muted">Available stock: {product.stock}</span>
            </div>

            <div style={{ display: 'grid', gap: '0.75rem' }}>
              {isAuthenticated ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    disabled={isOutOfStock || isAdding}
                    onClick={handleAddToCart}
                    className={`btn ${isOutOfStock ? 'btn-secondary' : 'btn-primary'}`}
                  >
                    {isOutOfStock ? 'Out of Stock' : isAdding ? 'Adding...' : 'Add to Cart'}
                  </button>
                  {isOutOfStock ? <span style={{ color: '#b91c1c', fontWeight: 600 }}>Out of Stock</span> : null}
                </div>
              ) : null}
              {cartMessage ? <div className="status-message status-success">{cartMessage}</div> : null}
              {cartError ? <div className="status-message status-error">{cartError}</div> : null}
            </div>
          </div>
        </div>
      </section>

      <section className="panel panel-padding">
        <h3 className="section-title">Customers who bought this also bought</h3>

        {!isAuthenticated ? (
          <p className="muted">Log in to receive personalized recommendations.</p>
        ) : recommendationsLoading ? (
          <p className="muted">Loading recommendations...</p>
        ) : recommendations.length > 0 ? (
          <div className="grid-responsive" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))' }}>
            {recommendations.map((item) => (
              <Link key={item.id} to={`/products/${item.id}`} className="panel-card" style={{ display: 'grid', gap: '0.6rem', padding: '0.9rem', color: 'inherit', textDecoration: 'none' }}>
                <img src={item.imageUrl || 'https://picsum.photos/seed/default/600/400'} alt={item.name} style={{ width: '100%', height: '140px', objectFit: 'cover', borderRadius: '8px' }} />
                <div>
                  <h4 style={{ margin: '0 0 0.25rem' }}>{item.name}</h4>
                  <p style={{ margin: 0, fontWeight: 600 }}>${item.price ?? 'N/A'}</p>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <p className="muted">No recommendations are available yet. As more completed purchases are made, personalized suggestions will appear.</p>
        )}
      </section>
    </div>
  );
}

export default ProductDetailsPage;
