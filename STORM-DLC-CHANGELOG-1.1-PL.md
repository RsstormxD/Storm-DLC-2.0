# Storm DLC 2.0 — historia zmian 1.1 / Minecraft 1.21.4

Wersja oparta na Twoim ulepszonym folderze StormDLC. Własne niebieskie logo S znajduje się w GUI, HUD i metadanych moda.

## Instalacja

Java 21, Minecraft **1.21.4**, Fabric Loader 0.16.10 lub nowszy, Fabric API dla **1.21.4** (0.119.4+1.21.4).
W folderze mods zastąp poprzedni plik Storm DLC 2.0 nowym JAR-em. Nie instaluj jednocześnie osobnych Astolfo Visuals i Song Island; są połączone w tym modzie.

## Obsługa

- INSERT / prawy SHIFT: zwykłe GUI. Moduły GUI 3D, Spotify 3D i Song Island są w Render → Interface (także All).
- GUI zapamiętuje zakładkę, kategorię, wyszukiwanie i przewinięcie także po ponownym uruchomieniu gry.
- G w świecie: obsługa paneli 3D myszą. ESC lub G zamyka tryb obsługi. Kółko przewija ustawienia / tekst, przeciąganie obsługuje suwaki.
- **3D lock** w GUI 3D / Spotify 3D przypina pozycję i obrót do świata. Wyłączona blokada pozwala panelowi podążać za kamerą. „Place in front” ustawia go ponownie. Zmiana świata i ponowne włączenie modułu resetują kotwicę.
- Song Island → **Text in sky** wyświetla zsynchronizowany tekst piosenki wyżej i dalej w świecie. **3D lock** utrzymuje miejsce tekstu również po zmianie wersu. „Place text in front” ustawia nową kotwicę. Lyrics 3D włącza wariant bliżej gracza.
- Spotify 3D korzysta z ukrytego procesu pomocniczego, który izoluje dołączoną bibliotekę DLL od gry. Proces kończy się razem z modem. Panel korzysta z odtwarzacza uruchomionego w Windows, np. aplikacji Spotify. Ma przyciski poprzedni / pauza / następny i własny panel niezależny od wyspy HUD. Włącz odtwarzanie muzyki, aby pojawiły się informacje. Dostępność tekstu zależy od utworu; brak tekstu nie wyłącza sterowania.
- **Configs → Import config…**: wklej ścieżkę pliku JSON, przeciągnij jeden plik na ekran importu lub użyj Import clipboard dla JSON-a ze schowka. Następnie zaznacz profil i naciśnij Load. Pliki są w config/stormdlc/configs. Import nie nadpisuje istniejących profili, a błędny JSON pokazuje komunikat.

## Discord

Wbudowane ID aplikacji: **1552749614816305223**. Nie potrzeba Client Secret, tokenu bota ani tokenu użytkownika. Discord desktop musi działać, a udostępnianie aktywności być włączone.
Mod obsługuje odpowiedzi Discorda, ping/pong, ponowne połączenie oraz zamykanie połączenia bez blokowania gry.
RPC pokazuje nazwę „Storm DLC 2.0”. Opcja **Spinning logo** używa publicznego, animowanego GIF-a niebieskiego S. W config/stormdlc/discord-rpc.json pole **animatedLargeImage** określa adres animacji, a **largeImage** obraz statyczny. Aktualne instrukcje są w STORM-DLC-2.0-PL.md.

## Źródła

Standardowa kompilacja: gradlew.bat build. Test lokalny: gradlew.bat runClient -PstormDlcSmoke (oddzielny folder run-verification, testowy utwór i nowy świat). Kod testów nie wchodzi do zwykłego JAR-a.
Oryginalne informacje licencyjne i autorzy pozostają w LICENSE, LICENSE-Song-Island i THIRD_PARTY_NOTICES.md.
