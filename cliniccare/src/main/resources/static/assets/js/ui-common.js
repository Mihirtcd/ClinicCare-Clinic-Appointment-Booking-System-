(function (window, $) {
  "use strict";

  const shared = window.ClinicCareUi || {};

  function doctorLabel(doctor) {
    if (!doctor) {
      return "Doctor";
    }

    const first = doctor.firstName || "";
    const last = doctor.lastName || "";
    const fullName = ("Dr. " + first + " " + last).replace(/\s+/g, " ").trim();
    if (doctor.specialization) {
      return fullName + " (" + doctor.specialization + ")";
    }
    return fullName;
  }

  function initDataTable(selector, options) {
    const defaults = {
      scrollY: "48vh",
      scrollCollapse: true
    };
    const config = $.extend(true, {}, defaults, options || {});
    return $(selector).DataTable(config);
  }

  shared.doctorLabel = doctorLabel;
  shared.initDataTable = initDataTable;
  window.ClinicCareUi = shared;
})(window, jQuery);
