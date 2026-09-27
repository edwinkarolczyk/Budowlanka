# Budowlanka — wersja rozwojowa 0.5.4

Androidowa aplikacja offline-first do wycen robót budowlanych. Nazwa produktu jest tymczasowa — docelowa nazwa zostanie wybrana później.

## Zakres 0.5

### 0.5.4 — widoczność zaznaczeń i katalog Małopolskie
- mocniejszy pomarańczowy stan aktywny we wszystkich głównych kontrolkach,
- wyraźne zaznaczenie dolnej nawigacji, etapów kosztorysu, filtrów i wybranych pozycji,
- jaśniejsze obramowanie aktywnych pól i list rozwijanych,
- wybór ceny pozycji: MIN / KATALOG / MAX / WŁASNA,
- filtr katalogu: Wszystkie / Małopolskie 2026 / Moje,
- praktyczne pakiety robót: malowanie, gładź+malowanie, łazienka, dach i elewacja,
- migracja Room 3→4 bez kasowania danych i automatyczna kopia bazy przed migracją.

### 0.5.3 — ergonomia i bezpieczeństwo
- własna stawka robocizny dla konkretnej pozycji kosztorysu bez zmiany katalogu,
- czytelne: ilość × cena jednostkowa = wartość robocizny,
- wyszukiwanie robót podczas dodawania pozycji,
- duplikowanie pozycji, pomieszczeń i całych kosztorysów,
- wielokrotne zaznaczanie i usuwanie pozycji kosztorysu,
- edycja pomieszczeń,
- automatyczny snapshot pliku bazy przed migracją schematu,
- migracja Room 2→3 bez kasowania danych.

### Funkcje bazowe

- lokalna baza Room/SQLite; internet nie jest potrzebny do wycen,
- własna baza robót, jednostek, stawek i norm czasu,
- baza materiałów z normą zużycia, historią cen oraz klasami tani / standard / premium,
- materiał wybierany osobno dla każdej pozycji, bez sztywnego wariantu całej wyceny,
- pakiety robót i żywy kosztorys,
- pomieszczenia/strefy: długość, szerokość, wysokość i odjęcie okien/drzwi,
- wycena tylko robocizny albo robocizny z materiałem,
- marża robocizny, materiałów i całego zlecenia,
- rabat własny i sugerowany rabat stałego klienta na podstawie historii,
- dojazd: A→B→A × liczba dni × stawka/km + opcjonalna stała kwota,
- dodatkowe koszty: gruz, rusztowanie, wynajem, nocleg, transport itd.,
- ekipa 1–6 osób,
- oddzielny udział w pracy i w zysku,
- rozliczenie: procent zysku / godzina / dzień / stała kwota,
- automatyczne roboczogodziny, termin techniczny i termin finansowy,
- cel miesięczny oraz wymagany dochód dzienny,
- baza narzędzi powiązana z robotami,
- po akceptacji: lista zakupów i checklista narzędzi,
- stany zakupów: mam / do kupienia / zamówione / kupione / dostarczone,
- kalendarz terminów i ostrzeganie o kolizjach narzędzi,
- baza klientów z wieloma adresami/inwestycjami,
- zdjęcia przypisane do zlecenia albo pomieszczenia,
- podpis klienta rysowany na ekranie,
- PDF dla klienta oraz osobny PDF wewnętrzny,
- kopia ZIP danych i dostępnych zdjęć,
- jeden kanał aktualizacji z manifestu GitHub,
- Android na start; architektura przygotowana do późniejszego klienta PC.

## Dane startowe

Po pierwszym uruchomieniu aplikacja tworzy przykładowy katalog, m.in. pakiet „Ogrzewanie podłogowe — pełny zakres”, malowanie, gładź, materiały PEX i podstawowe narzędzia. Wszystkie pozycje można rozwijać własnymi danymi.

## Budowanie

Projekt korzysta z Android Gradle Plugin 8.4.2, Kotlin 1.9.24, Jetpack Compose oraz Room.

GitHub Actions buduje APK Debug automatycznie dla gałęzi `development-0.5` i `main`. APK znajduje się w artefakcie workflow `Budowlanka-APK`.

Lokalnie, przy zainstalowanym Gradle 8.6 i JDK 17:

```bash
gradle :app:assembleDebug
```

## Aktualizacje

Aplikacja sprawdza:
`https://raw.githubusercontent.com/edwinkarolczyk/Budowlanka/main/updates/manifest.json`

Po wydaniu tagu `v*` workflow tworzy GitHub Release z plikiem `Budowlanka.apk`.

## Zasada danych

Aktualizacje aplikacji nie powinny kasować bazy użytkownika. Każda przyszła zmiana schematu Room musi mieć migrację. W 0.5.4 schemat ma wersję 4. Migracje 1→2 i 2→3 są jawne; przed migracją tworzona jest lokalna kopia pliku bazy.
