# Strona WWW — BUDOWLANKA

Ta gałąź zawiera stronę internetową BUDOWLANKA opartą wizualnie na aplikacji Android.

## Pliki strony

- `index.html` — struktura strony i formularz kontaktowy,
- `styles.css` — styl zgodny z paletą aplikacji,
- `script.js` — menu, walidacja formularza i przygotowanie wiadomości,
- `site-config.js` — adres odbiorcy formularza,
- `.github/workflows/pages.yml` — automatyczne wdrożenie na GitHub Pages.

## Kolory zgodne z aplikacją

- tło: `#0B0F12`
- panel: `#151A1E`
- panel 2: `#1B2024`
- pomarańczowy: `#FF7A00`
- aktywny: `#FFB000`
- aktywny mocny: `#FFC247`
- tekst: `#F4F5F6`

## Formularz

Formularz waliduje dane po stronie przeglądarki i buduje gotową wiadomość e-mail.
Aby ustawić adres odbiorcy, zmień tylko jedną wartość w `site-config.js`:

```js
window.BUDOWLANKA_SITE = {
  CONTACT_EMAIL: "kontakt@twojadomena.pl"
};
```

Nie dodawaj prywatnych kluczy API ani haseł do repozytorium.

## GitHub Pages

Workflow uruchamia się po zmianach plików strony na gałęzi `website`.
Docelowy adres GitHub Pages powinien mieć postać:

`https://edwinkarolczyk.github.io/Budowlanka/`

Jeżeli GitHub Pages nie był wcześniej aktywny w repozytorium, workflow próbuje go włączyć przez `actions/configure-pages`.
