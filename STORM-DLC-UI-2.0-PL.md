# Storm DLC 2.0 — menu, profil i ruch

Wydanie 2.0.1+1.21.4 zachowuje nazwę Storm DLC 2.0.

## Ładowanie i menu

Ekran ładowania jest czarny. StormDLC pojawia się z animacją liter, delikatną
falą i przesuwającym się rozświetleniem. Pasek przedstawia rzeczywisty postęp
ładowania zasobów. Standardowy cykl ładowania, obsługa błędów i przejścia
Minecrafta pozostają obsługiwane przez grę.

Menu ma godzinę HH:mm, datę, Dynamic Island oraz przyciski Liquid Glass.
Ich tło korzysta z obrazu ekranu, rozmycia, refrakcji na brzegu i refleksów.
Przyciski zachowują działanie świata, serwerów, Realms, ustawień, języka,
dostępności i wyjścia, wraz z obsługą klawiatury oraz opisami po najechaniu.

- Theme 1: obraz 1234dsza.jpg.
- Theme 2: czarny marmur z pliku z dopiskiem (1).
- Theme 3: czarny marmur z trzeciego przesłanego pliku.

Domyślnie wybrany jest Theme 1. Wybór zapisuje się po kliknięciu motywu.
Appearance pozwala ustawić własny PNG/JPG, tło Windows i przyciemnienie.
Duże obrazy są dekodowane w tle z ograniczeniem rozmiaru tekstury, a zmiana
tapety ma płynne przejście. Oryginalne dołączone pliki nie są modyfikowane.

## Dynamic Island i powiadomienia

Interface → Island Discord Profile używa tego samego nicku i avatara Discord,
które są widoczne w ClickGUI. Zielona kropka oznacza aktywne połączenie RPC;
szara oznacza ostatni znany profil bez aktywnego połączenia. Jeśli profil Discord
nie jest jeszcze dostępny, wyspa pokazuje profil Minecraft. Wyłączenie tej
opcji przełącza wyspę na Minecraft. Discord desktop i moduł Discord RPC
udostępniają dane profilu bez dodatkowego logowania.

Wyspa działa w menu i w grze. W menu pozostaje na środku u góry; pozycję w grze
nadal można ustawić przez Move island. Kliknięcie wyspy w menu lub czacie
rozwija Profile / Music. Angielskie powiadomienia mają animowane wejście,
wyjście, przesuwanie stosu i zmianę treści. Kolejka wyspy łączy powtarzające się
zdarzenia i nadaje pierwszeństwo ważnym komunikatom.

## KillAura — Movement

- Follow Target: podążanie za wybranym graczem, mobem lub zwierzęciem.
- Target Strafe: okrążanie celu.
- Off: wyłączenie sterowania ruchem.

Follow Distance określa dystans zatrzymania, ograniczony zasięgiem ataku.
Movement Prediction ustawia krótkie wyprzedzenie ruchu celu. Follow Auto Jump
umożliwia przechodzenie przez przeszkody wysokości jednego bloku przy dostępnej
przestrzeni nad graczem. Manual Movement Override oddaje sterowanie, gdy
naciskasz klawisze ruchu lub skradania. Auto Sprint działa, gdy pozwalają na to
warunki gracza. Rotation Mode nadal wybiera MineStar V1/V2, Polar, AC V2 lub HVH.

Planowanie korzysta z ograniczonego wyszukiwania A* w już załadowanych chunkach,
sprawdza kolizje, podłoże, granicę świata i niebezpieczne bloki. Każdy kierunek
wynikający z klawiszy jest ponownie sprawdzany przed ruchem. Podążanie wymaga
możliwej do przejścia trasy; przy braku drogi gracz się zatrzymuje. Mod nie
teleportuje gracza i korzysta z normalnych klawiszy oraz fizyki Minecrafta.
Lista Friends i filtry celów obowiązują również w ruchu. Wyłączenie modułu,
otwarcie ekranu, śmierć, utrata celu i zmiana świata zwalniają sterowanie.
