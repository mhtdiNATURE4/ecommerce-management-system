const TOKEN_KEY = 'ecommerce_token';

function parseJwtPayload(token) {
  if (!token) return null;

  try {
    const payload = token.split('.')[1];
    if (!payload) return null;

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(normalized);
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

export function saveToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export function getUserRole() {
  const token = getToken();
  const payload = parseJwtPayload(token);

  if (!payload) return null;

  const role = payload.role || payload.rol || payload.roles || payload.authorities;

  if (Array.isArray(role)) {
    return role.find(Boolean) || null;
  }

  return typeof role === 'string' ? role : null;
}

export function getUserName() {
  const token = getToken();
  const payload = parseJwtPayload(token);

  if (!payload) return null;

  const candidateValues = [
    payload.name,
    payload.fullName,
    payload.full_name,
    payload.firstName,
    payload.first_name,
    payload.lastName,
    payload.last_name,
    payload.username,
    payload.sub,
    payload.email
  ];

  const matchedValue = candidateValues.find((value) => typeof value === 'string' && value.trim());

  if (!matchedValue) return null;

  const normalizedName = matchedValue.trim();
  return normalizedName.includes('@') ? normalizedName.split('@')[0] : normalizedName;
}

export function isAdminAuthenticated() {
  const role = getUserRole();
  return isRoleAdmin(role);
}

export function normalizeRoleName(r) {
  if (!r && r !== 0) return '';
  return String(r).toUpperCase().replace(/^ROLE_/, '').replace(/^ROLE-/, '');
}

export function isRoleMatch(candidate, role) {
  return normalizeRoleName(candidate) === normalizeRoleName(role);
}

export function isRoleAdmin(role) {
  return normalizeRoleName(role) === 'ADMIN';
}
