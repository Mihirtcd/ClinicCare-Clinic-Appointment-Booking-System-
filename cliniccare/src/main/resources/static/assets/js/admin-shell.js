(function (window) {
  "use strict";

  const shared = window.ClinicCareUi || {};

  function renderAdminSidebar(activePage) {
    const sidebar = document.getElementById("adminSidebar");
    if (!sidebar) {
      return;
    }

    const links = [
      { key: "dashboard", href: "admin-dashboard.html", icon: "bi-speedometer2", label: "Dashboard" },
      { key: "users", href: "admin-users.html", icon: "bi-people", label: "Users" },
      { key: "doctors", href: "admin-doctors.html", icon: "bi-person-badge", label: "Doctors" },
      { key: "services", href: "admin-services.html", icon: "bi-clipboard2-pulse", label: "Services" },
      { key: "timeslots", href: "admin-timeslots.html", icon: "bi-clock-history", label: "Time Slots" }
    ];

    const navLinks = links.map(function (link) {
      const active = link.key === activePage ? " active" : "";
      return (
        '<a class="admin-nav-link' + active + '" href="' + link.href + '">' +
        '<i class="bi ' + link.icon + ' me-2"></i>' + link.label +
        "</a>"
      );
    }).join("");

    sidebar.innerHTML =
      '<div class="admin-brand">ClinicCare Admin</div>' +
      '<nav class="admin-nav">' +
      '<a class="admin-nav-link" href="index.html"><i class="bi bi-house-door me-2"></i>Home</a>' +
      navLinks +
      "</nav>" +
      '<button class="btn btn-sm btn-light mt-auto" id="logoutBtn"><i class="bi bi-box-arrow-right me-1"></i>Logout</button>';
  }

  shared.renderAdminSidebar = renderAdminSidebar;
  window.ClinicCareUi = shared;
})(window);
