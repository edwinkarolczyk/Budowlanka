(() => {
  const q = (selector, scope = document) => scope.querySelector(selector);
  const qa = (selector, scope = document) => [...scope.querySelectorAll(selector)];

  q("#year").textContent = new Date().getFullYear();

  const menuButton = q(".menu-btn");
  const nav = q("#site-nav");

  const closeMenu = () => {
    nav.classList.remove("is-open");
    menuButton.setAttribute("aria-expanded", "false");
    menuButton.setAttribute("aria-label", "Otwórz menu");
    document.body.classList.remove("menu-open");
  };

  menuButton.addEventListener("click", () => {
    const open = !nav.classList.contains("is-open");
    nav.classList.toggle("is-open", open);
    menuButton.setAttribute("aria-expanded", String(open));
    menuButton.setAttribute("aria-label", open ? "Zamknij menu" : "Otwórz menu");
    document.body.classList.toggle("menu-open", open);
  });

  qa("#site-nav a").forEach(link => link.addEventListener("click", closeMenu));
  window.addEventListener("resize", () => {
    if (window.innerWidth > 980) closeMenu();
  });

  const revealTargets = qa(".service-card, .process-grid article, .trust-row p");
  if ("IntersectionObserver" in window && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
    revealTargets.forEach(el => el.classList.add("reveal"));
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.12 });
    revealTargets.forEach(el => observer.observe(el));
  }

  const form = q("#contact-form");
  const status = q("#form-status");
  const submit = q(".btn--submit", form);
  const consentLabel = q(".consent", form);

  const showStatus = (message, type = "") => {
    status.textContent = message;
    status.className = "form-status is-visible" + (type ? ` is-${type}` : "");
  };

  const fieldWrapper = input => input.closest(".field");

  const validateField = input => {
    if (input.name === "company") return true;
    let valid = true;

    if (input.required && !String(input.value || "").trim()) valid = false;
    if (input.type === "email" && input.value.trim()) {
      valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(input.value.trim());
    }
    if (input.type === "tel" && input.value.trim()) {
      valid = input.value.replace(/\D/g, "").length >= 7;
    }

    const wrapper = fieldWrapper(input);
    if (wrapper) wrapper.classList.toggle("is-invalid", !valid);
    return valid;
  };

  qa("input, select, textarea", form).forEach(input => {
    if (input.name === "consent" || input.name === "company") return;
    input.addEventListener("blur", () => validateField(input));
    input.addEventListener("input", () => {
      if (fieldWrapper(input)?.classList.contains("is-invalid")) validateField(input);
    });
    input.addEventListener("change", () => validateField(input));
  });

  q('input[name="consent"]', form).addEventListener("change", e => {
    consentLabel.classList.toggle("is-invalid", !e.target.checked);
  });

  const formatDate = value => {
    if (!value) return "do ustalenia";
    const [y, m, d] = value.split("-");
    return [d, m, y].filter(Boolean).join(".");
  };

  const buildBody = data => {
    const lines = [
      "NOWE ZAPYTANIE — BUDOWLANKA",
      "",
      "DANE KLIENTA",
      `Imię i nazwisko: ${data.get("name") || "-"}`,
      `Telefon: ${data.get("phone") || "-"}`,
      `E-mail: ${data.get("email") || "-"}`,
      "",
      "INWESTYCJA",
      `Lokalizacja: ${data.get("location") || "-"}`,
      `Proponowany termin: ${formatDate(data.get("term"))}`,
      `Zakres robót: ${data.get("scope") || "-"}`,
      `Orientacyjna powierzchnia: ${data.get("area") || "-"}`,
      `Liczba pomieszczeń: ${data.get("rooms") || "-"}`,
      "",
      "OPIS PRAC",
      data.get("message") || "-",
      "",
      "Wiadomość przygotowana przez formularz WWW BUDOWLANKA."
    ];
    return lines.join("\n");
  };

  form.addEventListener("submit", async event => {
    event.preventDefault();
    status.className = "form-status";

    const data = new FormData(form);
    if (String(data.get("company") || "").trim()) {
      form.reset();
      showStatus("Dziękujemy. Zapytanie zostało przyjęte.", "success");
      return;
    }

    const fields = qa("input, select, textarea", form).filter(input => !["consent", "company"].includes(input.name));
    const fieldsValid = fields.every(validateField);
    const consent = q('input[name="consent"]', form);
    const consentValid = consent.checked;
    consentLabel.classList.toggle("is-invalid", !consentValid);

    if (!fieldsValid || !consentValid) {
      showStatus("Sprawdź pola oznaczone na czerwono i uzupełnij wymagane dane.", "error");
      const firstInvalid = q(".is-invalid input, .is-invalid select, .is-invalid textarea", form);
      firstInvalid?.focus();
      return;
    }

    const email = String(window.BUDOWLANKA_SITE?.CONTACT_EMAIL || "").trim();
    const body = buildBody(data);
    const subject = `Zapytanie o wycenę — ${data.get("name")} — ${data.get("location")}`;

    if (!email) {
      try {
        await navigator.clipboard.writeText(body);
        showStatus("Formularz jest gotowy, ale adres odbiorcy nie został jeszcze ustawiony. Treść zapytania skopiowano do schowka.", "error");
      } catch {
        showStatus("Formularz jest gotowy, ale adres odbiorcy nie został jeszcze ustawiony w konfiguracji strony.", "error");
      }
      return;
    }

    submit.disabled = true;
    submit.querySelector("span:first-child").textContent = "Otwieram wiadomość…";

    const mailto = `mailto:${encodeURIComponent(email)}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
    window.location.href = mailto;

    window.setTimeout(() => {
      submit.disabled = false;
      submit.querySelector("span:first-child").textContent = "Wyślij zapytanie";
      showStatus("Wiadomość została przygotowana w Twoim programie pocztowym. Sprawdź dane i kliknij „Wyślij”.", "success");
    }, 650);
  });
})();
