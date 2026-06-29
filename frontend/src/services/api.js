const API_BASE_URL = '/api';

async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  const token = localStorage.getItem('ecommerce_token');
  const normalizedToken = typeof token === 'string' ? token.trim() : '';
  const isJwtLike = normalizedToken && normalizedToken.split('.').length === 3;

  if (isJwtLike) {
    headers.Authorization = `Bearer ${normalizedToken}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  const contentType = response.headers.get('content-type') || '';
  const isJson = contentType.includes('application/json');

  if (!response.ok) {
    let responseBody = null;
    if (isJson) {
      try {
        responseBody = await response.json();
      } catch {
        responseBody = null;
      }
    } else {
      try {
        responseBody = await response.text();
      } catch {
        responseBody = null;
      }
    }

    const message =
      responseBody?.message ||
      responseBody?.error ||
      (Array.isArray(responseBody?.errors) && responseBody.errors[0]) ||
      response.statusText ||
      `Request failed with status ${response.status}`;

    const error = new Error(message || `Request failed with status ${response.status}`);
    error.status = response.status;
    error.details = responseBody;
    throw error;
  }

  if (isJson) {
    return response.json();
  }

  return response.text();
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' })
};
