import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { getToken } from '../services/auth';

function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All Categories');
  const [sortBy, setSortBy] = useState('name');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [cartError, setCartError] = useState('');
  const [addingProductId, setAddingProductId] = useState(null);
  const navigate = useNavigate();
  const isAuthenticated = Boolean(getToken());

  useEffect(() => {
    let isMounted = true;

    async function loadProductsPage() {
      try {
        const [productsData, categoriesData] = await Promise.all([
          api.get('/products').catch(() => []),
          api.get('/categories').catch(() => [])
        ]);

        if (!isMounted) {
          return;
        }

        setProducts(Array.isArray(productsData) ? productsData : []);
        setCategories(Array.isArray(categoriesData) ? categoriesData : []);
      } catch (err) {
        if (isMounted) {
          setError('Unable to load products right now.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadProductsPage();

    return () => {
      isMounted = false;
    };
  }, []);

  const filteredProducts = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();

    const filtered = products.filter((product) => {
      const matchesSearch = !normalizedSearch || product.name?.toLowerCase().includes(normalizedSearch);
      const matchesCategory = selectedCategory === 'All Categories' || product.categoryName === selectedCategory;
      return matchesSearch && matchesCategory;
    });

    const sorted = [...filtered].sort((a, b) => {
      if (sortBy === 'price-asc') {
        return Number(a.price) - Number(b.price);
      }
      if (sortBy === 'price-desc') {
        return Number(b.price) - Number(a.price);
      }
      return a.name.localeCompare(b.name);
    });

    return sorted;
  }, [products, search, selectedCategory, sortBy]);

  if (loading) {
    return (
      <div className="page-shell">
        <section className="panel panel-padding">
          <div className="skeleton skeleton-title" style={{ width: '40%', marginBottom: '0.5rem' }} />
          <div className="skeleton skeleton-text" style={{ width: '60%' }} />
        </section>

        <section className="panel panel-padding">
          <div className="grid-responsive">
            {[0, 1, 2].map((item) => (
              <div key={item}>
                <div className="skeleton skeleton-text" style={{ width: '30%', marginBottom: '0.4rem' }} />
                <div className="skeleton" style={{ height: '2.8rem', borderRadius: '10px' }} />
              </div>
            ))}
          </div>
        </section>

        <div className="grid-responsive">
          {[0, 1, 2, 3, 4, 5].map((item) => (
            <div key={item} className="panel panel-padding" style={{ display: 'grid', gap: '0.8rem' }}>
              <div className="skeleton" style={{ height: '180px', borderRadius: '10px' }} />
              <div className="skeleton skeleton-title" style={{ width: '70%' }} />
              <div className="skeleton skeleton-text" style={{ width: '40%' }} />
              <div className="skeleton" style={{ height: '2.5rem', borderRadius: '10px' }} />
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
          <h2 className="page-title">Products are temporarily unavailable</h2>
          <p className="status-message status-error" style={{ marginTop: '0.75rem' }}>{error}</p>
        </section>
      </div>
    );
  }

  async function handleAddToCart(productId) {
    setMessage('');
    setCartError('');

    if (!getToken()) {
      navigate('/login', { replace: true });
      return;
    }

    setAddingProductId(productId);

    try {
      await api.post('/cart', { productId, quantity: 1 });
      setMessage('Added to cart successfully.');
      window.dispatchEvent(new Event('cart-updated'));
    } catch (err) {
      setCartError(err?.message || 'Unable to add item to cart. Please try again.');
    } finally {
      setAddingProductId(null);
    }
  }

  return (
    <div className="page-shell">
      <section className="panel panel-padding">
        <h2 className="page-title">Products</h2>
        <p className="page-subtitle">Browse the available electronics from the store catalog.</p>
      </section>
      {message ? <div className="status-message status-success">{message}</div> : null}
      {cartError ? <div className="status-message status-error">{cartError}</div> : null}

      <section className="panel panel-padding">
        <div className="grid-responsive" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))' }}>
          <label className="form-field">
            <span className="muted">Search</span>
            <input
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by product name"
              className="form-control"
            />
          </label>

          <label className="form-field">
            <span className="muted">Category</span>
            <select value={selectedCategory} onChange={(event) => setSelectedCategory(event.target.value)} className="form-control">
              <option value="All Categories">All Categories</option>
              {categories.map((category) => (
                <option key={category.id} value={category.name}>
                  {category.name}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span className="muted">Sort by</span>
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)} className="form-control">
              <option value="name">Name (A–Z)</option>
              <option value="price-asc">Price (Low → High)</option>
              <option value="price-desc">Price (High → Low)</option>
            </select>
          </label>
        </div>
      </section>

      {filteredProducts.length === 0 ? (
        <div className="panel panel-padding">
          <p className="empty-state" style={{ margin: 0 }}>No products match your current filters.</p>
        </div>
      ) : (
        <div className="grid-responsive" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
          {filteredProducts.map((product) => {
            const isOutOfStock = Number(product.stock) <= 0;

            return (
              <div key={product.id} className="panel panel-padding" style={{ display: 'grid', gap: '0.8rem' }}>
                <img
                  src={product.imageUrl || 'https://picsum.photos/seed/default/600/400'}
                  alt={product.name}
                  style={{ width: '100%', height: '180px', objectFit: 'cover', borderRadius: '10px' }}
                />
                <div>
                  <h4 style={{ margin: '0 0 0.25rem' }}>{product.name}</h4>
                  <p className="muted" style={{ margin: 0 }}>{product.categoryName || 'Uncategorized'}</p>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <strong>${product.price}</strong>
                  <span className="muted">Stock: {product.stock}</span>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  <Link to={`/products/${product.id}`} className="btn btn-secondary" style={{ flex: 1, minWidth: '110px' }}>
                    View Details
                  </Link>
                  {isAuthenticated ? (
                    <button
                      type="button"
                      disabled={isOutOfStock || addingProductId === product.id}
                      onClick={() => handleAddToCart(product.id)}
                      className={`btn ${isOutOfStock ? 'btn-secondary' : 'btn-primary'}`}
                      style={{ flex: 1, minWidth: '120px' }}
                    >
                      {isOutOfStock ? 'Out of Stock' : addingProductId === product.id ? 'Adding...' : 'Add to Cart'}
                    </button>
                  ) : null}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default ProductsPage;
