# Storm DLC 2.0 — Dynamic Island i powiadomienia

Minecraft 1.21.4 Fabric, Java 21, Mojmap. Wersja pozostaje `2.0.0+1.21.4`.

## Interface

W ClickGUI przejdź do **Render → Interface → Interface**.

| Ustawienie | Działanie |
| --- | --- |
| Notifications | Angielskie powiadomienia w prawym dolnym rogu; moduł Interface musi być włączony |
| Notification Duration | Czas wyświetlania: 1–8 sekund, domyślnie 3 sekundy |
| Island Profile | Profil gracza w Dynamic Island |
| Island Lyrics | Tekst piosenki w kapsule i panelu Music |
| Island Reactions | Reakcje wyspy na moduły, utwór, pauzę, obrażenia, niski poziom HP, lot i zmianę profilu |

Nowa instalacja ma włączony Interface i Notifications. Istniejące konfiguracje
zachowują zapisany stan Interface. Powiadomienia o modułach używają tekstów
`KillAura enabled`, `KillAura disabled`, `Module is now active` i
`Module has been turned off`. Kolejka mieści cztery komunikaty; ponowne
przełączenie tego samego modułu zastępuje jego poprzedni komunikat.
Wczytywanie konfiguracji nie powoduje serii powiadomień o każdym module.

## Dynamic Island

Moduł **Dynamic Island** znajduje się w **Render → Interface**. Zastępuje nazwę
**Song Island** w GUI. Stare konfiguracje z nazwą Song Island nadal są odczytywane.

Wyspa ma czarny kształt kapsuły w stylu iPhone'a, wysokość 32 jednostek GUI
i domyślną pozycję na środku przy górnej krawędzi. Ma własną projekcję 2D,
niezależną od macierzy renderowania świata. Również powiadomienia i Cursor
korzystają z poprawionej ścieżki renderowania.

Otwórz czat klawiszem **T**, kliknij kapsułę i wybierz **Profile** lub **Music**.
Kapsuła działa także w menu głównym; **Show in menus** wyłącza widok poza światem.
Bez świata profil pokazuje nazwę konta, domyślną głowę, wersję gry, FPS i czas.
Kliknięcie poza wyspą zwija ją i pozostawia obsługę kliknięcia ekranowi czatu.

- **Profile:** głowa aktualnego skina z warstwą nakrycia, nick, HP, pancerz,
  ping, pasek zdrowia, nazwa załadowanej konfiguracji, czas sesji, FPS i liczba
  włączonych modułów. Profil działa również bez odtwarzanej muzyki.
- **Music:** okładka, tytuł, wykonawca, postęp utworu, przyciski
  poprzedni/pauza/następny oraz zsynchronizowane karaoke. Brak dostępnego tekstu
  jest oznaczony komunikatem `Lyrics unavailable for this track`.
- **Reakcje:** chwilowa zmiana kapsuły, animowany znacznik, kolor stanu i pasek
  czasu. Najnowsze przełączenie modułu jest widoczne również podczas używania
  ClickGUI. Reakcję można zamknąć kliknięciem w wyspę w ekranie czatu.

Teksty są pobierane przez istniejący system muzyczny i mogą być niedostępne
dla danego utworu. Do ręcznej synchronizacji nadal służą Map lyrics i ustawienia
opóźnienia. Panel Spotify 3D zachowuje własny przełącznik Lyrics.
Pozycję zmienisz przez **Dynamic Island → Move island**.
**Reset position** przywraca położenie na górze ekranu. Klawisz F1 nadal ukrywa HUD
podczas gry.

## Menu główne i tapeta

**Main Menu** jest domyślnie włączone w **Render → Interface**.
Menu pokazuje napis **StormDLC**, oznaczenie **2.0**, bieżącą godzinę i datę.
Przy małej szerokości ekranu godzina trafia do podpisu pod logo.

W menu Minecrafta kliknij **Wallpaper**:

- **Windows wallpaper:** odczyt aktualnej tapety Windows przy wejściu do menu.
- **Custom image:** wklej pełną ścieżkę do PNG/JPG i kliknij **Use image**.
- **Storm wallpaper:** dołączona tapeta z białą Mazdą, wybrana z tego komputera.
- **Clock**, **Background dimming** i **Reload wallpaper** pozwalają dopasować widok.
- **Menu: Off** przywraca standardowe tło i logo Minecrafta.

Przyciski gry, światów, serwerów, Realms i opcji korzystają ze standardowej obsługi.
Obraz wypełnia ekran, zachowując proporcje i przycinając nadmiar przy krawędziach.
Ładowanie i odczyt tapety odbywają się poza wątkiem renderowania; tekstury są
zwalniane przy zmianie obrazu, wyłączeniu modułu i zamknięciu gry.
Obsługiwane są obrazy do 32 MB, 8192 pikseli na bok i 16 megapikseli łącznie.

## Cursor

W **Render → Interface → Cursor** włącz moduł i wybierz **Style: Duck / Bat**.
Animowany obraz znajduje się pod widocznym kursorem na ekranach GUI, czatu,
ekwipunku oraz menu. Nie przechwytuje kliknięć i nie zmienia działania myszy.

| Ustawienie | Domyślnie | Działanie |
| --- | --- | --- |
| Style | Duck | Kaczka albo nietoperz z dostarczonych GIF-ów |
| Size | 32 | Rozmiar w jednostkach GUI, zakres 12–72 |
| Offset X / Offset Y | 0 / 6 | Przesunięcie względem kursora |
| Animation Speed | 1 | Mnożnik tempa animacji, zakres 0,25–3 |
| Smooth Follow | wyłączone | Płynne podążanie za myszą |
| Click Pulse | włączone | Krótkie powiększenie po kliknięciu |

Pliki zachowują przezroczystość, proporcje i klatki GIF. Nie są zastępowane
statycznym obrazkiem. Zasoby graficzne są zwalniane przy wyłączeniu modułu
i przeładowaniu resource packów.

## Rotacje i stawianie bloków

- MineStar V1: szybki obrót z ograniczeniem prędkości, przyspieszenia i hamowaniem.
- MineStar V2: sprężyna z tłumieniem krytycznym i śledzeniem prędkości kątowej celu.
- Polar: kontrola przyspieszenia, tempa jego zmian i wspólnego limitu yaw/pitch.
- AC V2: adaptacyjne tempo, łagodne przejście do precyzyjnego celowania i śledzenie ruchu.
- HVH: wybór widocznych punktów AABB oraz sprawdzanie sąsiednich kątów na siatce GCD.
- Rotation speed działa we wszystkich trybach poza HVH. Wewnętrzny stan zachowuje
  ruchy mniejsze niż krok czułości myszy; dopiero wynik jest kwantyzowany.
- Stable Target ogranicza przeskakiwanie między przeciwnikami o zbliżonych wynikach
  sortowania. Zmiana celu zachowuje bieżący kąt modelu, a utrata celu lub wyłączenie
  modułu czyści jego stan.
- Model interpoluje kąty pomiędzy tikami. Kamera zachowuje własny kierunek.
- Atak wymaga zgodności promienia z bieżącą skrzynką celu. Przewidywana skrzynka
  pomaga prowadzić cel, ale nie jest traktowana jako potwierdzenie trafienia.
- Przy celu w zasięgu ataku wyprzedzony punkt jest ograniczany do aktualnej AABB.
  Korekta pakietu ataku nie resetuje wygładzania i nie zmienia widoku kamery.
  Polar i AC V2 są osobnymi presetami matematycznymi tego klienta.
- Target Strafe ma ustawienie Strafe Distance, zmienia stronę przy kolizji
  i sprawdza podłoże. Auto Sprint respektuje głód, skradanie i używanie przedmiotu.
- Firework Delay (ticks) steruje odstępem użycia fajerwerków przy Elytra Boost.
- AutoTrap i AutoWeb mają Silent Rotations, Use Aura Target oraz opcjonalne
  Inventory Swap. Podmiana z ekwipunku działa przy zamkniętym ekranie kontenera
  i pustym kursorze; wybrany slot oraz podmienione przedmioty są przywracane.
- AutoTrap buduje od dołu, dodaje podparcie dachu i opcjonalne Corners.
- AutoWeb ma Feet Web, Head Web, Only Grounded i opcjonalne Predict Movement.
  Pozycje stawiania uwzględniają rzeczywistą wysokość AABB, także przy elytrze.
- Wszystkie działania nadal wykluczają Friends i używają zwykłych interakcji gry.

## Budowanie

```powershell
.\gradlew.bat assemble -x test -x compileTestJava -x processTestResources
```

Plik wynikowy: `build/libs/storm-dlc-2.0.0+1.21.4.jar`.
Podczas tej aktualizacji nie uruchamiano testów ani klienta Minecraft.
