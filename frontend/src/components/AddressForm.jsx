import { useState } from 'react';
import { api } from '../services/api';

/**
 * Shared shipping-address form used by both AddressesPage and CheckoutPage.
 * Owns its own field state and submission; reports the newly created
 * address back to the parent via onAddressAdded so each page can decide
 * what to do with it (e.g. select it, prepend it to a list, close itself).
 */
function AddressForm({ onAddressAdded, submitLabel = 'Add Address' }) {
  const [form, setForm] = useState({ street: '', city: '', country: '', zipCode: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  function updateField(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!form.street.trim() || !form.city.trim() || !form.country.trim()) {
      setError('Please fill in the street, city, and country.');
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const createdAddress = await api.post('/addresses', {
        street: form.street.trim(),
        city: form.city.trim(),
        country: form.country.trim(),
        zipCode: form.zipCode.trim()
      });

      setForm({ street: '', city: '', country: '', zipCode: '' });
      onAddressAdded?.(createdAddress);
    } catch (err) {
      setError(err?.message || 'Unable to save your address. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="stack-sm">
      {error ? <div className="status-message status-error" style={{ marginBottom: '0.25rem' }}>{error}</div> : null}

      <label className="form-field">
        <span className="muted">Street</span>
        <input type="text" placeholder="Street" value={form.street} onChange={(e) => updateField('street', e.target.value)} className="form-control" required />
      </label>
      <label className="form-field">
        <span className="muted">City</span>
        <input type="text" placeholder="City" value={form.city} onChange={(e) => updateField('city', e.target.value)} className="form-control" required />
      </label>
      <label className="form-field">
        <span className="muted">Country</span>
        <input type="text" placeholder="Country" value={form.country} onChange={(e) => updateField('country', e.target.value)} className="form-control" required />
      </label>
      <label className="form-field">
        <span className="muted">Postal Code</span>
        <input type="text" placeholder="Postal Code" value={form.zipCode} onChange={(e) => updateField('zipCode', e.target.value)} className="form-control" />
      </label>
      <button type="submit" disabled={submitting} className="btn btn-primary">
        {submitting ? 'Saving...' : submitLabel}
      </button>
    </form>
  );
}

export default AddressForm;
