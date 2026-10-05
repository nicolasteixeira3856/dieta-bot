// Theme switch (W1 § 5): toggles data-theme on <html>, remembers the choice and keeps the button's name and
// pressed state in sync. The current theme's segment is filled (Figma Web/ThemeSwitch).
(function () {
  var root = document.documentElement;
  var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

  function current() {
    return root.getAttribute("data-theme") === "dark" ? "dark" : "light";
  }

  function sync(button) {
    var dark = current() === "dark";
    button.setAttribute("aria-pressed", dark ? "true" : "false");
    button.setAttribute("aria-label", dark ? "Ativar tema claro" : "Ativar tema escuro");
  }

  function apply(theme) {
    if (!reduceMotion.matches) {
      root.classList.add("theme-animating");
      window.setTimeout(function () {
        root.classList.remove("theme-animating");
      }, 300);
    }
    root.setAttribute("data-theme", theme);
    try {
      window.localStorage.setItem("fibrai-theme", theme);
    } catch (e) {
      // Storage blocked: the choice lasts for this page only.
    }
  }

  var buttons = document.querySelectorAll("[data-theme-switch]");
  Array.prototype.forEach.call(buttons, function (button) {
    sync(button);
    button.addEventListener("click", function () {
      apply(current() === "dark" ? "light" : "dark");
      Array.prototype.forEach.call(buttons, sync);
    });
  });
})();
