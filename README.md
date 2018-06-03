# [TIN] Botnet P2P - projekt wstępny

## Podział pracy i odpowiedzialność członków zespołu

### Skład zespołu
* Filip Konieczny
* Wojciech Gruszka
* Bartosz Wojciechowski
* Kacper Kamiński

### Podział pracy
* **Węzeł w TypeScript(Node.js)** - Filip Konieczny
* **Węzeł w GOLang** - Wojciech Gruszka
* **Węzeł w Java** - Bartosz Wojciechowski
* **Konsola operatorska w Python** - Kacper Kamiński

**Prace w ramach węzła**
* implementancja warstw aplikacji:
  * komunikacja po socketach
  * parsowanie wiadomości
  * obsługa wiadomości
* testy jednostkowe

**Prace w ramach konsoli operatorskiej**
* implementancja warstw aplikacji:
  * komunikacja po socketach
  * parsowanie wiadomości
  * obsługa wiadomości
* testy jednostkowe
* interfejs użytkownika

**Prace w ramach testu integracyjnego działania systemu**
* utworzenie lokalnych węzłów stworzonych w różnych językach, komunikujących się po różnych portach
* potwierdzenie otrzymania wiadomości i ich obsługi

## Opis funkcjonalności i wymagań

### Wymagania niefunkcjonalne
* Odporność projektowanego systemu na problemy łączności:
  - chwilowa lub trwała utrata łączności między węzłami
  - błędne pakiety
  - przekroczone limity czasowe
* Odporność systemu na błędy pojedynczych wątków.
* Kompatybilność z systemami z rodziny Linux.

### Wymagania funkcjonalne
* Sieć powinna mieć możliwość dołączenia nowych węzłów.
* Nowy węzeł powinien móc pobrać informacje o innych węzłach w sieci.
* Węzeł powinien mieć możliwość odbierania i dalszego przesyłania dowolnego typu pliku.
* Jeden z węzłów (Operator) powinien dostarczać użytkownikowi kontrolę nad całą siecią.
* Możliwość przesłania pliku do innych węzłów (propagacja).
* Operator powinien posiadać możliwość wydawania komend:
  - wylistowanie wszystkich węzłów
  - zakończenie pracy węzła operatorskiego
  - wylistowanie plików na danym węźle
  - wysłanie komunikatu o przesłaniu pliku z danego węzła do innego węzła
* Tylko węzeł konsoli operatorskiej powinien móc wydawać komendy.

## Analiza wymagań funkcjonalnych i niefunkcjonalnych
* Realizacja przesyłania plików po protokole TCP (zapewnienie integralności danych).
* Realizacja topologii sieci za pomocą algorytmu Kademlia.
* Komunikaty są znakowane UUID.
* Działanie węzła jest zrealizowane za pomocą wątków z rozdzieloną odpowiedzialnością zadań.
* Przypadki wielodostępu będziemy realizować za pomocą mutexów/kolejek wiadomości.

## Architektura realizacji

### Struktura sieci

#### Topologia
![](https://i.imgur.com/2QSnaGH.png)

Każdy węzeł w sieci posiada swój **GUID** (*Globally Unique ID*) o długości $N$ bitów. Możliwa liczba węzłów w sieci to zatem $2^N$.

#### Odległość węzłów

Odległość między dwoma węzłami (reprezentowanymi poprzez *GUID*) obliczana jest z użyciem funkcji $\oplus$ (XOR) na obu identyfikatorach. `distance(A, B) =  A XOR B`
Funkcja ta ma dwie kluczowe własności, wykorzystywane przy rutowaniu:
* $A \oplus B \equiv B \oplus A$
* Nie istnieje liczba $C$, taka że $A \oplus B \equiv A \oplus C$ dla każdego $A$ i $B$.

Adres $A$ jest bliższy adresowi $B$ niż adres $C$, jeżeli $A \oplus B < C \oplus B$.

#### Tablica routingu
![](https://upload.wikimedia.org/wikipedia/commons/thumb/6/63/Dht_example_SVG.svg/840px-Dht_example_SVG.svg.png)

##### Struktura tablicy routingu
Tablica routingowa składa się z $N$ *$k$-elementowych bucket'ów*. Każdy *k-bucket* może przechowywać do *k* wpisów. *N*-ty *k-bucket* zawiera wpisy o hostach, których *GUID* mają te same bity na poprzednich $N-1$ pozycjach.

Oznacza to że ostatni *k-bucket* zawiera adresy najbardziej odległe. Jest on także najłatwiejszy do zapełnienia, ponieważ może się w nim znaleźć $\frac{1}{2}$ wszystkich hostów w sieci (są to adresy z pierwszym bitem innym od bitu hosta). 
Analogicznie pierwszy *k-bucket* może przechowywać tylko jednego hosta - ten którego jedynie ostatni bit jest różny od bitu hosta.

##### Znajdowanie węzła
W przypadku kiedy w tablicy-routingowej hosta nie znajduje się wpis o hoście do którego chcemy wysłać wiadomość, odpytywane zostają węzły z *k-bucket*'a w którym znajdowałby się poszukiwany host. 
Jeżeli dany *k-bucket* jest pusty odpytywane są hosty z najbliższego *k-bucketa* (ponieważ znajdują się w nim najbliższe hosty do naszego poszukiwanego hosta). Standardowo odpytywane jest $\alpha$ hostów. Każdy z nich zwraca *k* najbliższych hostów do poszukiwanego. Następnie wybieramy z nich *k* najlepszych i zapełniamy nimi naszą tabelę routingową (wrzucamy je do naszych *k-bucket*'ów). Następnie powtarzamy iteracyjnie całą procedurę, odpytując za każdym razem zwracane hosty, które za każdym razem są bliższe hostowi którego chcemy znaleźć. Proces poszukiwania kończy się kiedy nie dostajemy już żadnych hostów znajdujących się bliżej do poszukiwanego hosta. W wyniku tego *k* najlepszych hostów, które otrzymaliśmy, znajduje się najbliżej w całej sieci do poszukiwanego hosta. 

Następnie możemy wysłać zapytanie o szukany węzeł do jednego z ostatnich zwróconych węzłów. Są to węzły najbliższe do tego szukanego, dlatego też jeżeli jest on podłączony do sieci, to jego wpis musi być przechowywany w tablicy routingowej jednego z nich. 

### Bootowanie i dołączanie do sieci

1. Nowy host tworzy swój GUID.
2. Nowy host wysyła do bootstrap node'a (musi znać wcześniej jego adres) wiadomość `FIND_NODE` podając własny `GUID` jako parametr.
3. Bootstrap node zwraca listę najbliższych węzłów, a także uaktualnia swoją tablicę routingową o adres nowego hosta.
4. Nowy host wysyła do każdego zwróconego węzła wiadomość `PING`.
5. Hosty, które otrzymały wiadomość dodają nowego hosta do swojej tablicy routingowej i wysyłają do niego wiadomość `PING_RESPONSE`.
6. Nowy host dodaje do swojej tablicy routingowej wszystkie węzły, które odpowiedziały zapytaniem `PING_RESPONSE` w zadanym czasie. Dzięki temu wiemy, które węzły wciąż żyją.

### Odłączanie się od sieci
![](https://i.imgur.com/Wh1Jut3.png)

Węzeł, który odłącza się od sieci P2P, wysyła żądanie `leave` do wszystkich węzłów, które ma w swojej tablicy routingu. Dzięki temu węzły mogą usunąć wspomniany węzeł ze swojej tablicy routingu.

### Crash węzła
![](https://i.imgur.com/vp9Aqty.png)

Wszystkie węzły w tablicy routingu danego węzła muszą być periodycznie `ping`owane, w celu ustalenia czy dany węzeł odpowiada. Jeśli nie, wysyłane jest zapytanie `find_node` z *GUID* ustawionym na nieodpowiadający węzeł. Dzięki temu znajdujemy najbliższy węzeł do nieodpowiadającego i możemy zastąpić nim pozycję w tablicy routingu.


### Uwagi implementacyjne
 * Routing realizowany jest za pomocą algorytmu Kademlia.
 * Tablica routingu zawiera krotki (GUID, IP, Port, isNAT).
 * Relay, czyli węzeł pośredniczący, akceptuje wiadomości od węzłów które chcą wysłać wiadomość do węzła który stoi za NATem.
 * Węzły stojące za NATem odpytują co jakiś czas węzły pośredniczące czy istnieją jakieś wiadomościo przeznaczone dla nich; jeżeli istnieją, to zestawiane jest połaczenie między nadawcą i odbiorcą przez węzeł pośredniczący - nadawca jest informowany o możliwości nadawania
 * Odłączenie i podłączenie się węzła jest realizowane zgodnie z algorytmem Kademlia, a same protokoły opisane są poniżej
 * Klucz prywatny Operatora (do weryfikacji podpisu) jest na stałe zapisany w pamięci klienta 

### Warstwy

![warstwy](https://i.imgur.com/iJJVQJC.png)
* **Warstwa aplikacyjna**
  - tworzy aplikację
  - dołącza się do sieci
  - wykonuje komunikaty
  - zapisuje/odczytuje pliki z dysku
  - w przypadku konsoli operatorskiej pozwala na wysyłanie komunikatów
* **Warstwa logiki biznesowej**
  - udostępnia API biznesowe, takie jak `joinNetwork`
  - nasłuchuje na typy wiadomości i odpowiada na nie
* **Warstwa P2P**
  - umożliwia tworzenie wiadomości
  - umożliwia nasłuchiwanie na wiadomości
* **Warstwa parsowania wiadomości**
  - parsuje otrzymane wiadomości
  - koduje wiadomości do wysłania
* **Warstwa komunikacji (sockety)**
  - tworzy połączenia
  - przyjmuje połączenia
  - zapełnia kolejkę odebranych wiadomości
  - wysyła wiadomości z kolejki wiadomości do wysłania


### Struktura wątków węzła 

- wątek aplikacyjny implementujący warstwę aplikacyjną systemu, startujący aplikację i inicjujący wszystkie jej moduły
- w przypadku gdy węzeł jest za NATem
    - wątki obsługi połączeń, działające przez węzły przekazujące:
        - wątek obsługi wiadomości aplikacyjnych - przekazuje sterowanie do wyższych warstw, po czym kończy życie
        - wątek obsługi wiadomości utrzymujących topologię sieci - zarządzanie tabelą routingu
    - wątek usypiany czasowo, sprawdzający żywotność węzłów z tablicy routingu 
    - wątek usypiany czasowo, odpytujący znane węzły przekazujące o dostępność nowych połączeń do nich
    - wątki przekazujące wiadomości dalej - tworzone zgodnie z potrzebą, w zależności od wywołań API


- w przypadku gdy węzeł nie jest za NATem
    - wątek słuchacza - nasłuchuje na zadanym porcie i oczekuje na połączenie od innego węzła; w przypadku połączenia nowe gniazdo przekazywane jest do nowego wątku który zajmuje się jego obsługą
    - wątki obsługi połączeń:
        - wątek obsługi wiadomości aplikacyjnych - przekazuje sterowanie do wyższych warstw, po czym kończy życie
        - wątek obsługi wiadomości utrzymujących topologię sieci - zarządzanie tabelą routingu
    - wątek usypiany czasowo, sprawdzający żywotność węzłów z tablicy routingu 
    - wątki przekazujące wiadomości dalej - tworzone zgodnie z potrzebą, w zależności od wywołań API
    - wątek obsługi przekazywania - oczekuje na połączenie z drugiej strony "omijania NATa", ma otwarte gniazdo do inicjatora połączenia

Między wątkami synchronizacjha prowadzona jest przez synchronizację mutexami/synchronized oraz kolejki wiadomości.

### Moduły i interfejsy

**API**
* `sendFileTo(file, peer)`
* `sendFileToAll(file)`
* `listFilesFrom(peer)`
* `listPeers()`
* `executeCommandOn(peer)`
* `onFile(file => callback(file))`
* `onCommand(command => callback(command))`

**Klasy**
> work in progress...

### Opis protokołów

Protokoły wykorzystują Google ProtoBuf jako bibliotekę do enkodowania i opakowania wiadomości. 

* przesłanie wiadomości
wiadomości mają maksymalny rozmiar 12KB.
Wiadomość posiada formę: 
```typescript
uuid: UUID,
type: MESSAGE_TYPE,
sender: GUID,
receiver: GUID,
propagation: boolean,
payload: PAYLOAD,
signature: SIGNATURE,    
```
Dopuszczalne Payloady (message type):
 - Command - komenda do wykonania
    ```typescript
    command: string
    sendResponse: boolean
    ```
 - Response - odpowiedź
    ```typescript
    text: string
    status: STATUS
    ```
 - FileChunk - fragment pliku, pliki przesyłane są we fragmentach, składane są po stronie klienta
    ```typescript
    path: string
    name: string
    chunkNumber: unsigned int
    allChunks: unsigned int
    data: byte[]
    chunkSize: unsigned int
    ```
 - Request Send To NAT - prośba relay'a o przesłanie danych dalej ( o zestawienie linku do danego peera)
    ```typescript
    target: GUID
    ```
 - Ask For Connections - zapytanie czy ktokolwiek chce się połączyć z danym peerem
    ```typescript
    source: GUID
    ```
 - Ping - sprawdza czy dany węzeł jest aktywny
    ```typescript
    target: GUID
    ```
 - Join
   ```typescript
    ip: string,
    port: unsigned int,
    isNAT: boolean
   ```
 - Leave
    ```typescript
    guid: GUID
   ```

Wiadomości przesyłane są po socketach TCP, ProtoBuf zapewnia niezależność od platformy i języka w którym pisany jest dany klient. 

W przypadku, gdy wiadomość ma prawdziwą flagę `propagation`, wiadomość jest wysyłana do każdego najbliższego węzła w sieci, w ten sposób propagując się po całej sieci. Pole `receiver` jest zmieniane przy każdym następnym węźle, a gdy będzie takie samo jak sender, to zakończyliśmy pętlę i kończymy propagację.

Wykrywanie czy jest się za NATem zostanie zrealizowane za pomocą zapytania do serwisu `https://www.ipify.org/` i porównaniu z adresem odczytamym z systemowej konfiguracji interfejsów sieciowych.

Przekazanie wiadomości przez węzeł pośredniczący polega na zestawieniu połaczenia z węzłem pośredniczącym, przekazaniem wiadomości `Request Send To NAT`. Gdy docelowy węzeł odpyta węzeł pośredniczący, pierwotny nadawca informowany jest wiadomością `Response` ze statusem `OK`. Węzeł nadający przesyła wiadomości które węzeł pośredniczący przekazuje do węzła docelowego.
W przypadku konieczności wielokrotnego przejścia przez węzły proces jest powielany. 

### Dodatkowe elementy implementacyjne

* parser wiadomości
  - zajmuje się budowaniem wiadomości, ich serializacją i podpisywaniem
  - deserializuje wiadomości, odzyskuje payload i sprawdza podpis

* możliwość przerwania działania wątku
  - cancellation points w C++
  - użycie `NonBlockingIO` w Javie

### Propozycje rozwiązania sytuacji krytycznych

* Sytuacja w której Operator zniknie z sieci, sieć w dalszym ciągu będzie działać - nie jest to sytuacja krytyczna.
   * węzły wykonują tylko polecenia podpisane właściwym kluczem
   * jeśli nie przychodzą żadne wiarygodne polecenie, węzły nie wykonują żadnych poleceń a jedynie wymieniają się danymi
* Zniknięcie z sieci wszelkich węzłów pośredniczących (które nie są za NATem) - hardlock, komunikacji w sieci nie da się odtworzyć 
 
### Scenariusze

#### Dołączanie się nowego hosta do sieci
![diagram](https://i.imgur.com/DLwHnNg.png)

#### Pobieranie listy plików (w przypadku gdy węzeł docelowy znajduje się już w tablicy routingowej)
![diagram](https://i.imgur.com/ENDsLSV.png)

#### Przesyłanie pliku (w przypadku gdy węzeł docelowy nie znajduje się w tablicy routingowej węzła źródłowego)
![diagram](https://i.imgur.com/tPEz1Mg.png)

#### Odłączanie się węzła
![diagram](https://i.imgur.com/jbcMKrB.png)

### Przypadki użycia
#### Konsola operatorska - zarządzający
![use_cases](https://i.imgur.com/XuPyWQN.png)

## Testowanie

Wstępnie testowanie zamierzamy przeprowadzić używając maszyn wirtualnych odgrywających role hostów. Przetestowane zostaną wszystkie podstawowe funkcje sieci które zostały wymienione powyżej.

### Główny scenariusz testowy
1. podłączenie do sieci 2 węzłów "klientów" i 1 "operatora"
2. wypisanie zawartości `/etc/` z obu klientów
3. pobranie z obu klientów  pliku `/etc/passwd` jeśli istnieje 
4. weryfikacja poprawności pobranych plików

Sytuacje do przetestowania (wybrane)
* próba wysłania wiadomości do węzła, który nie jest już połączony
* relay przestaje działać

Postaramy się przetestować także sytuacje krytyczne i pokazać, że system zachowuje się w przewidziany przez nas sposób.


---

ApplicationLayer
woła resztę

BusinessLogicLayer
`.joinNetwork(bootstrapNode, func onJoined())`
`.sendFileTo(file)`
`.sendFileToAll(file)`
`.onFileReceived(fileData)`
`.sendCommand(command)`
`.onCommand(command)`

P2PLayer
`routing table`
`.findNode(node): []nodes`
`.ping(func(result))`
`.sendFileChunk(bytes)`
`.onFileChunkReceived(func(bytes))`
`.sendCommand(command)`
`.onCommand(func(command))`
`.onPeerDisconnected(func(node))`

ProtobufLayer
`.assembleAndSendMessage(...)` \* dla każdego rodzaju wiadomości?
`.onMessageReceived(type, func(message)): Message`

MessageLayer

SocketLayer
`.sendMessage(bytes)`
`.onMessageReceived(bytes)`


```
def AL.launchClient(bootstrapNode: {host, port}):
-> var success = BLL.joinNetwork(bootstrapNode)
if(not success) {
    exit(error)
}
def BLL.joinNetwork():
-> P2PL.findNode(bootstrapNode, me)
P2PL.on('found_nodes', (nodes) => P2PL.routingTable.addNode(bootstrapNode), for each node -> P2Pl.ping(node), pingedNodes.append(node))
P2PL.on('ping_response', (node) => P2PL.routingTable.addNode(node), P2PL.findNode(node, randomIDforBucket), pingedNodes.remove(node))
P2PL.on('timeout', for each node in pingedNodes P2PL.routingTable.remove(node))
P2PL.on('ping', (node) => P2Pl.routingTable.addNode(node), 
Proto.create(pingResponseMessage, node, me))
pingedNodes = []
Proto.create -> createExactTypeMessage , ML.send(exactMessage))

ML.send -> messagesToSend$.next(message)

SL.on(messagesToSend$, for each msg -> send(msg))
P2PL.on('error', return error)

-> PBL.SendFindNodeMessage(bootstrapNode, me)
-> SL.sendMessage([]bytes, target(IP, Port)) <- if connection exists -> use it else  create and add to list 

```

new message
```
-> SL new message -> add to queue -> onMessageReceived(bytes)
```
