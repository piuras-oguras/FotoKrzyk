# FotoHałas

**Autor:** Szymon Piórkowski \
**Przedmiot:** Programowanie Aplikacji Mobilnych [LAB]

## 1. Wstęp

**FotoHałas** to aplikacja mobilna służąca do rejestrowania miejsc o podwyższonym poziomie hałasu. Użytkownik może dokonać pomiaru natężenia dźwięku (w decybelach), pobrać aktualną lokalizację GPS oraz wykonać zdjęcie dokumentujące źródło hałasu. Wszystkie pomiary są zapisywane w lokalnej bazie danych i mogą być przeglądane w historii lub wyeksportowane do wiadomości SMS w formacie CSV.

### Główne funkcjonalności:
* 🎙️ **Pomiar hałasu:** Szacunkowy pomiar natężenia dźwięku (dB) przy użyciu mikrofonu.
* 📍 **Geolokalizacja:** Automatyczne pobieranie współrzędnych (szerokość i długość geograficzna).
* 📸 **Dokumentacja wizualna:** Możliwość zrobienia zdjęcia miejsca zdarzenia.
* 🗄️ **Historia pomiarów:** Przeglądanie zapisanych zgłoszeń z podziałem na karty.
* 📤 **Eksport danych:** Udostępnianie historii pomiarów przez SMS (format CSV).

<p align="center">
  <img src="img/img.png" width="45%" />
  <img src="img/img_1.png" width="45%" />
</p>
---

## 2. Stos Technologiczny (Tech Stack)

Aplikacja została napisana w języku **Kotlin** z wykorzystaniem nowoczesnych bibliotek Android Jetpack.

* **UI:** Jetpack Compose (Material Design 3).
* **Architektura:** MVVM (Model-View-ViewModel).
* **Baza danych:** Room Database (SQLite wrapper).
* **Asynchroniczność:** Kotlin Coroutines & Flow.
* **Nawigacja:** Jetpack Navigation Compose.
---

## 3. Architektura Aplikacji

Projekt realizuje wzorzec **MVVM**.

### 3.1. Warstwa Danych (Model)
Odpowiada za trwałe przechowywanie danych oraz logikę dostępu do nich.
* **`Measurement.kt`**: Encja bazy danych. Reprezentuje pojedynczy pomiar (id, czas, GPS, dB, ścieżka do zdjęcia).
* **`MeasurementDao.kt`**: Interfejs dostępu do danych. Zawiera metody `insert`, `delete` oraz `observeAll` (zwracającą `Flow<List>`).
* **`AppDatabase.kt`**: Główna klasa bazy danych Room. Implementuje wzorzec Singleton.
* **`Repository.kt`**: Pośrednik między ViewModel a DAO. Abstrakcja źródła danych.

### 3.2. Warstwa Logiki (ViewModel)
Zarządza stanem ekranów i komunikuje się z Repozytorium oraz sensorami.
* **`HomeViewModel.kt`**:
    * Obsługuje logikę mikrofonu do obliczania amplitudy.
    * Obsługuje pobieranie lokalizacji.
    * Przelicza amplitudę na przybliżone decybele wg wzoru: `20 * log10(maxAmplitude)`.
    * Zarządza stanem ekranu głównego.
* **`HistoryViewModel.kt`**:
    * Pobiera listę pomiarów z bazy w formie strumienia.
    * Generuje plik CSV z historią i przygotowuje Intent do wysyłki SMS.
    * Obsługuje czyszczenie bazy danych.

### 3.3. Warstwa Prezentacji (View)
Ekrany zbudowane w Jetpack Compose.
* **`MainActivity.kt`**: Punkt wejścia aplikacji, ustawia motyw i nawigację.
* **`AppNav.kt` / `Routes.kt`**: Konfiguracja grafu nawigacji (Ekrany: Home, History).
* **`HomeScreen.kt`**: Ekran główny z przyciskami do pomiaru, zdjęcia i nawigacji. Wyświetla status pomiaru.
* **`HistoryScreen.kt`**: Ekran listy z użyciem `LazyColumn`.
* **`MeasurementCard.kt`**: Komponent UI wyświetlający pojedynczy wpis (zdjęcie, dane, data).

---

## 4. Kluczowe Rozwiązania Implementacyjne

### 4.1. Pomiar Hałasu
Aplikacja wykorzystuje klasę `MediaRecorder` do próbkowania dźwięku. Nie nagrywa ona dźwięku w sposób ciągły do pliku w celu odsłuchu, lecz analizuje maksymalną amplitudę (`maxAmplitude`).

### 4.2. Obsługa Uprawnień
Aplikacja dynamicznie prosi użytkownika o wymagane uprawnienia przy użyciu `ActivityResultContracts.RequestMultiplePermissions`:

`RECORD_AUDIO` – do pomiaru hałasu.
`ACCESS_FINE_LOCATION` – do precyzyjnej lokalizacji.
`CAMERA` – (obsługiwane przez osobny Intent aparatu).

### 4.3. Zapis Zdjęć
Zdjęcia nie są zapisywane jako Blob w bazie danych (co spowolniłoby aplikację), lecz jako pliki w pamięci podręcznej (cacheDir), a w bazie przechowywany jest tylko ciąg znaków Uri (String). Do wyświetlania zdjęć użyto biblioteki Coil.

### 4.4. Eksport Danych (CSV via SMS)
Funkcja shareCsvBySms w HistoryViewModel iteruje po liście pomiarów, buduje łańcuch znaków w formacie CSV i uruchamia systemowy Intent.ACTION_SENDTO.
```kotlin
// Przykład generowanego formatu
id,timestamp,lat,lng,approxDb,photoUri
1,1706543210000,52.2297,21.0122,65.5,content://...
```