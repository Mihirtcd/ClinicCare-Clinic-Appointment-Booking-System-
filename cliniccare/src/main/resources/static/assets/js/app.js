(function (window, $) {
  "use strict";

  const TOKEN_KEY = "cliniccare_token";
  const ROLE_KEY = "cliniccare_role";
  const USER_KEY = "cliniccare_user";
  const API_BASE = "/api";

  function setAuth(token, user) {
    localStorage.setItem(TOKEN_KEY, token || "");
    localStorage.setItem(ROLE_KEY, user && user.role ? user.role : "");
    localStorage.setItem(USER_KEY, JSON.stringify(user || {}));
  }

  function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function getRole() {
    return localStorage.getItem(ROLE_KEY);
  }

  function getUser() {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return {};
    }
    try {
      return JSON.parse(raw);
    } catch (e) {
      return {};
    }
  }

  function authHeaders(headers) {
    const token = getToken();
    const merged = Object.assign({}, headers || {});
    if (token) {
      merged.Authorization = "Bearer " + token;
    }
    return merged;
  }

  function extractToken(xhr) {
    if (!xhr || !xhr.getResponseHeader) {
      return "";
    }
    const header = xhr.getResponseHeader("Authorization");
    if (!header) {
      return "";
    }
    if (header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return header;
  }

  function api(options) {
    const opts = options || {};
    const method = (opts.method || "GET").toUpperCase();
    const withAuth = opts.auth !== false;
    const url = (opts.absolute ? "" : API_BASE) + (opts.url || "");
    const ajaxOptions = {
      method: method,
      url: url,
      headers: withAuth ? authHeaders(opts.headers) : (opts.headers || {}),
      dataType: opts.dataType || "json"
    };

    if (typeof opts.data !== "undefined") {
      ajaxOptions.contentType = "application/json";
      ajaxOptions.data = JSON.stringify(opts.data);
    }

    return $.ajax(ajaxOptions);
  }

  function extractErrorMessage(xhr) {
    if (!xhr) {
      return "Request failed.";
    }
    const body = xhr.responseJSON;
    if (body) {
      if (body.validationErrors && Object.keys(body.validationErrors).length > 0) {
        const lines = Object.entries(body.validationErrors).map(function (entry) {
          return entry[0] + ": " + entry[1];
        });
        return lines.join(" | ");
      }
      if (body.message) {
        return body.message;
      }
    }
    if (xhr.responseText) {
      return xhr.responseText;
    }
    return "Request failed with status " + xhr.status + ".";
  }

  function showAlert(containerSelector, type, message) {
    const html =
      '<div class="alert alert-' + type + ' alert-dismissible fade show" role="alert">' +
      message +
      '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>' +
      "</div>";
    $(containerSelector).html(html);
  }

  function logout() {
    clearAuth();
    window.location.href = "login.html";
  }

  function redirectByRole(role) {
    if (role === "ADMIN") {
      window.location.href = "admin-dashboard.html";
      return;
    }
    if (role === "DOCTOR") {
      window.location.href = "doctor-dashboard.html";
      return;
    }
    window.location.href = "patient-dashboard.html";
  }

  function requireAuth(allowedRoles) {
    const token = getToken();
    const role = getRole();

    if (!token || !role) {
      window.location.href = "login.html";
      return false;
    }

    if (Array.isArray(allowedRoles) && allowedRoles.length > 0) {
      if (!allowedRoles.includes(role)) {
        redirectByRole(role);
        return false;
      }
    }
    return true;
  }

  function formatDateTime(value) {
    if (!value) {
      return "";
    }
    try {
      return new Date(value).toLocaleString();
    } catch (e) {
      return value;
    }
  }

  function normalizeDateTimeLocal(value) {
    if (!value) {
      return "";
    }
    if (value.length === 16) {
      return value + ":00";
    }
    return value;
  }

  function statusBadge(status) {
    const normalized = (status || "").toUpperCase();
    let cls = "bg-secondary";
    if (normalized === "PENDING") {
      cls = "bg-warning text-dark";
    } else if (normalized === "CONFIRMED") {
      cls = "bg-info text-dark";
    } else if (normalized === "COMPLETED") {
      cls = "bg-success";
    } else if (normalized === "REJECTED") {
      cls = "bg-danger";
    } else if (normalized === "AVAILABLE") {
      cls = "bg-success";
    } else if (normalized === "RESERVED") {
      cls = "bg-secondary";
    }
    return '<span class="badge ' + cls + ' badge-status">' + normalized + "</span>";
  }

  window.ClinicCare = {
    setAuth: setAuth,
    clearAuth: clearAuth,
    getToken: getToken,
    getRole: getRole,
    getUser: getUser,
    authHeaders: authHeaders,
    extractToken: extractToken,
    api: api,
    extractErrorMessage: extractErrorMessage,
    showAlert: showAlert,
    logout: logout,
    redirectByRole: redirectByRole,
    requireAuth: requireAuth,
    formatDateTime: formatDateTime,
    normalizeDateTimeLocal: normalizeDateTimeLocal,
    statusBadge: statusBadge
  };
})(window, jQuery);
