// Runs in <head> before first paint (W1 § 5): the saved theme, or light. The system theme is ignored on purpose
// (ADR-037 § 5). External file because the CSP allows only 'self' scripts.
(function () {
  var theme = "light";
  try {
    var saved = window.localStorage.getItem("fibrai-theme");
    if (saved === "dark" || saved === "light") theme = saved;
  } catch (e) {
    // Storage blocked: stay light.
  }
  document.documentElement.setAttribute("data-theme", theme);
})();
