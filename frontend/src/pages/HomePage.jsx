import { Link } from 'react-router-dom';

function HomePage() {
  return (
    <div className="page-shell">
      <section
        className="panel panel-padding"
        style={{
          background: 'linear-gradient(135deg, #eef2ff 0%, #f0fdf4 100%)',
          border: '1px solid #dbeafe'
        }}
      >
        <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ flex: '1 1 320px', display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
            <span className="muted" style={{ fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase' }}>
              Fresh picks for every need
            </span>
            <h2 className="page-title" style={{ margin: 0 }}>Welcome to iStore</h2>
            <p className="page-subtitle" style={{ margin: 0, maxWidth: '680px' }}>
              Discover quality electronics, enjoy a smooth shopping experience, and keep track of your orders with ease.
            </p>
            <Link to="/products" className="btn btn-primary" style={{ width: 'fit-content' }}>
              Shop Now
            </Link>
          </div>

          <div style={{ flex: '0 1 280px', display: 'grid', gridTemplateColumns: 'repeat(2, minmax(120px, 1fr))', gap: '0.8rem' }}>
            {[
              { emoji: '📱', label: 'Phones' },
              { emoji: '💻', label: 'Laptops' },
              { emoji: '🎧', label: 'Audio' },
              { emoji: '📷', label: 'Cameras' }
            ].map((item) => (
              <div
                key={item.label}
                style={{
                  padding: '0.75rem 1rem',
                  background: 'white',
                  border: '1px solid var(--border)',
                  borderRadius: '12px',
                  textAlign: 'center'
                }}
              >
                <div style={{ fontSize: '1.8rem', lineHeight: 1 }}>{item.emoji}</div>
                <div className="muted" style={{ fontSize: '0.9rem', marginTop: '0.25rem' }}>{item.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="grid-responsive">
        <article className="panel panel-card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
          <div style={{ fontSize: '1.5rem' }}>🚚</div>
          <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>Fast Delivery</h3>
          <p className="muted" style={{ margin: 0 }}>Orders processed and shipped quickly.</p>
        </article>

        <article className="panel panel-card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
          <div style={{ fontSize: '1.5rem' }}>🔒</div>
          <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>Secure Checkout</h3>
          <p className="muted" style={{ margin: 0 }}>Your payment info is always protected.</p>
        </article>

        <article className="panel panel-card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
          <div style={{ fontSize: '1.5rem' }}>📦</div>
          <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>Easy Returns</h3>
          <p className="muted" style={{ margin: 0 }}>Hassle-free returns within 30 days.</p>
        </article>
      </section>
    </div>
  );
}

export default HomePage;
