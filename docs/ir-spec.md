# IR-Spezifikation v1

Textformat auf Basis von S-Expressions. Ziel: eindeutig parsbar, diffbar, von Hand lesbar, unabhängig von der Zielsprache.

## 1. Grundprinzipien

1. **Aufgelöst:** Jeder Typ und jede Member-Referenz ist voll qualifiziert. Der Generator muss nie raten.
2. **Entzuckert:** Kein Syntax-Zucker. String-Interpolation, `?.`, `??=`, `using`-Kurzform, LINQ-Query-Syntax, Operatoren auf eigenen Typen und `+=` auf Events sind in explizite Knoten aufgelöst.
3. **Faithful bei Semantik:** Wertsemantik von `struct`, `checked`/`unchecked` und Coroutines (`yield`) bleiben erhalten. Das Zielbackend setzt sie um (Lint: SM001, SM003, PT003).
4. **Kanonisch:** Gleiche Eingabe erzeugt byte-identische IR (feste Reihenfolge von Mitgliedern und Attributen, siehe 8). Nötig für saubere Git-Diffs.
5. **Verlustfrei im Fehlerfall:** Was der Dekompiler nicht auflösen kann, steht als `(raw ...)` im Baum statt zu verschwinden.
6. **Zwei Ebenen (ADR-023):** Die normale, strukturierte IR (Abschnitte 2–9) ist das Ziel. Nativer Code, der sich nicht strukturiert heben lässt, steht im **nativen Dialekt** (Abschnitt 12) innerhalb derselben Dateien und Symbol-IDs. Beide Ebenen dürfen in einer Datei vorkommen, sogar im selben Typ.

## 2. Lexik

- Encoding UTF-8, Zeilenende `\n`.
- Whitespace trennt Tokens und ist sonst bedeutungslos.
- Kommentar: `;` bis Zeilenende.
- Klammern: `(` `)`.

```
ident      := [A-Za-z_][A-Za-z0-9_]*
qname      := ident { "." ident }
keyword    := ":" ident { "-" ident }           ; z. B. :base  :loc  :init
string     := '"' { zeichen | escape } '"'      ; Escapes: \\ \" \n \r \t \uXXXX
number     := ["-"] ziffern ["." ziffern] [exp] suffix
nat        := ziffern                                ; ohne Suffix, nur in :version, (char ...) und :size (ADR-024)
suffix     := "i8"|"u8"|"i16"|"u16"|"i32"|"u32"|"i64"|"u64"|"f32"|"f64"
bool       := "true" | "false"
null       := "null"
literal    := number | hexnum | string | bool | null | "(" "char" nat ")"
hexnum     := "0x" hexziffern suffix                 ; nur Ganzzahl-Suffixe, z. B. 0x7FFE0000u64
opsym      := "+" | "-" | "*" | "/" | "%" | "&" | "|" | "^" | "<<" | ">>"
            | "==" | "!=" | "<" | "<=" | ">" | ">=" | "&&" | "||"
```

Zahlen tragen **immer** ein Typsuffix (`5.0f32`, `0i32`, `7u64`). Einzige Ausnahme ist `nat` an genau drei Stellen: `:version`, `(char ...)` und `:size` (ADR-024). `char`-Literale stehen als `(char 97)` (UTF-16-Code-Unit als `nat`). Strings sind UTF-16-Text, `null` ist untypisiert.

## 3. Typen (Atome ohne Leerzeichen)

```
type   := prim | qname [ "<" type { "," type } ">" ] { suffix }
prim   := bool | char | string | object | void
        | i8 | u8 | i16 | u16 | i32 | u32 | i64 | u64 | f32 | f64
        | ptr                 ; nativer 64-Bit-Zeiger (Adresse im flachen Speicher, Abschnitt 12)
suffix := "[" { "," } "]"     ; Array, Rang = Kommas + 1
        | "?"                 ; Nullable<T> (nur für Werttypen)
        | "&"                 ; ref/out/in-Parameter (nur in Signaturen)
```

Beispiele: `i32`, `UnityEngine.Vector3`, `System.Collections.Generic.List<i32>`, `Dictionary<string,i32[]>`, `f32[,]`.
Generische Typparameter stehen als Name (`T`), Deklaration über `:generics`.

## 4. Member-Referenzen (Atome)

```
name  := ident | ".ctor" | ".cctor"
mref  := qname "::" name [ "<" type {"," type} ">" ] [ "#" [ type {"," type} ] ]
```

- Feld/Property: `Namespace.Typ::name`
- Methode: `Namespace.Typ::name#ParamTyp1,ParamTyp2` (Parameterliste leer: `#` alleine)
- Konstruktor: `Namespace.Typ::.ctor#f32,f32,f32`, statischer: `::.cctor#`
- Generische Methode: `Typ::Name<i32>#string`

Diese Referenz ist zugleich die **stabile Symbol-ID** für Index, Lint und `manual/`-Overrides.

## 5. Deklarationen

```
file    := header { decl }
header  := "(" "ir" ":version" nat ":assembly" string [":sha256" string] ")"

decl    := class | struct | interface | enum
class   := "(" "class"     qname typeattrs { member } ")"
struct  := "(" "struct"    qname typeattrs { member } ")"
interface := "(" "interface" qname typeattrs { member } ")"
enum    := "(" "enum" qname ":underlying" type { "(" ident number ")" } ")"

typeattrs := [":base" type] [":implements" "(" {type} ")"]
             [":generics" "(" {ident} ")"] [":mods" "(" {ident} ")"]
             [":attrs" "(" {attr} ")"]

member  := field | prop | method | ctor | event
field   := "(" "field"  ident type [":mods" mods] [":attrs" attrs] [":init" expr] ")"
prop    := "(" "prop"   ident type [":mods" mods] [":get" body] [":set" body] ")"
method  := "(" "method" ident type params [":mods" mods] [":generics" "(" {ident} ")"] (body | ":abstract") ")"
ctor    := "(" "ctor" params [":mods" mods] [":base-call" "(" {expr} ")"] body ")"
event   := "(" "event"  ident type [":mods" mods] ")"

params  := "(" { "(" ident type [":ref"|":out"|":in"] [":default" expr] ")" } ")"
attr    := "(" qname { expr } ")"
body    := stmt | cfg                    ; cfg nur im nativen Dialekt (Abschnitt 12)
mods    := "(" { ident } ")"
attrs   := "(" { attr } ")"
```

Modifier: `public private protected internal static abstract virtual override sealed readonly const`.
Im Setter heißt der Wert implizit `value`.
Serialisierte Unity-Felder erkennt man an `public` oder `[SerializeField]` in `:attrs`.

## 6. Statements

```
stmt :=
  "(" "block" { stmt } ")"
| "(" "local" ident type [expr] ")"                 ; gilt bis Blockende
| "(" "expr" expr ")"
| "(" "if" expr stmt [stmt] ")"
| "(" "while" expr stmt ")"
| "(" "do-while" stmt expr ")"
| "(" "for" "(" {stmt} ")" expr "(" {expr} ")" stmt ")"
| "(" "foreach" ident type expr stmt ")"
| "(" "switch" expr { "(" "case" "(" {expr} ")" {stmt} ")" } [ "(" "default" {stmt} ")" ] ")"
| "(" "break" ")" | "(" "continue" ")"
| "(" "return" [expr] ")"
| "(" "throw" expr ")"
| "(" "try" stmt { "(" "catch" ident type stmt ")" } [ "(" "finally" stmt ")" ] ")"
| "(" "yield-return" expr ")" | "(" "yield-break" ")"
| "(" "lock" expr stmt ")"
| "(" "using" ident type expr stmt ")"
| "(" "label" ident ")" | "(" "goto" ident ")"
| manual-node
```

`switch` hat **kein Durchfallen**: Jeder Zweig endet mit `break`, `return`, `throw` oder `continue`. Fallthrough löst der Dekompiler-Pass in `goto case` bzw. duplizierte Zweige auf.

## 7. Ausdrücke

```
expr :=
  literal | ident | "this" | "base"                  ; ident = Local oder Parameter
| "(" "field"  expr mref ")"       | "(" "sfield" mref ")"
| "(" "prop"   expr mref ")"       | "(" "sprop"  mref ")"
| "(" "call"   expr mref {expr} ")"| "(" "scall"  mref {expr} ")"
| "(" "new"    mref {expr} ")"                      ; mref = .ctor-Referenz
| "(" "new-array" type expr {expr} ")"              ; Längen je Dimension
| "(" "array-lit" type {expr} ")"
| "(" "index"  expr expr {expr} ")"                 ; Array-/Indexer-Zugriff
| "(" opsym expr expr ")"                           ; nur für Primitive
| "(" ( "neg" | "not" | "bnot" ) expr ")"
| "(" "checked" expr ")" | "(" "unchecked" expr ")"
| "(" "set" lvalue expr ")"
| "(" "set-op" opsym lvalue expr ")"
| "(" ( "pre-inc" | "pre-dec" | "post-inc" | "post-dec" ) lvalue ")"
| "(" "conv"  type expr ")"                         ; numerische Umwandlung
| "(" "cast"  type expr ")"                         ; Referenz-Cast, kann werfen
| "(" "as"    type expr ")"
| "(" "is"    type expr [ident] ")"
| "(" "cond"  expr expr expr ")"                    ; Ternär
| "(" "coalesce" expr expr ")"
| "(" "typeof" type ")" | "(" "default" type ")"
| "(" "lambda" params stmt ")"
| "(" "delegate" expr mref ")"                      ; Methodengruppe -> Delegate
| "(" "event-add" expr mref expr ")" | "(" "event-remove" expr mref expr ")"
| manual-node

lvalue := ident | "(" "field" expr mref ")" | "(" "sfield" mref ")"
        | "(" "prop" expr mref ")" | "(" "sprop" mref ")" | "(" "index" expr expr {expr} ")"
```

Regeln:
- Operatoren auf eigenen Typen (`Vector3 * float`) sind **immer** `scall ...::op_Multiply#...`. Nur Primitive nutzen `(* a b)`.
- Arithmetik ist standardmäßig `unchecked`. Überlaufgeprüfte Operationen stehen in `(checked ...)`.
- Zuweisung ist ein Ausdruck (`set`); als Statement steht `(expr (set ...))`.
- Wertsemantik: `struct`-Werte werden bei `set`, Argumentübergabe und `return` implizit kopiert (wie in C#). Ein optionaler Pass darf explizite `(copy expr)`-Knoten einfügen.

## 8. Attribute (Keywords an jedem Knoten)

Jeder Knoten darf am Ende Keyword-Paare tragen:

| Attribut | Bedeutung |
|---|---|
| `:loc "Datei.cs:12"` | Herkunft (optional, nur im Debug-Modus) |
| `:manual "Grund"` | Markiert Stelle als Handarbeit (siehe 9) |
| `:note "Text"` | Freitext für dich |

**Kanonische Reihenfolge** (für stabile Diffs):
1. Typen und Mitglieder alphabetisch nach stabiler ID, Felder vor Properties vor Konstruktoren vor Methoden.
2. Keyword-Attribute in der Reihenfolge der Grammatik oben, danach `:loc`, `:manual`, `:note`.
3. Einrückung: 2 Leerzeichen, jeder Kindknoten einer mehrzeiligen Liste auf eigener Zeile.

## 9. Unlösbares und Handarbeit

```
manual-node := "(" "raw" "cs" string ")"           ; Original-C#, nicht übersetzt
             | "(" "raw" "asm" string ")"          ; Disassembly-Text, nicht gehoben (Abschnitt 12)
             | "(" "manual" string ")"             ; Platzhalter mit Grund
```

- `raw` entsteht, wenn der Dekompiler scheitert. Der Generator erzeugt dafür `todo`-Code und der Lint meldet MN003/PT001.
- `:manual` am Member bedeutet: Der Generator überspringt den Rumpf, du lieferst ihn in `manual/`.

## 10. Beispiel

```lisp
(ir :version 1 :assembly "Assembly-CSharp" :sha256 "ab12...")

(class Mover :base UnityEngine.MonoBehaviour
  (field speed f32 :mods (public) :init 5.0f32)
  (method Update void () :mods (private)
    (block
      (local h f32 (scall UnityEngine.Input::GetAxis#string "Horizontal"))
      (expr
        (call (prop this UnityEngine.Component::transform)
              UnityEngine.Transform::Translate#UnityEngine.Vector3
              (scall UnityEngine.Vector3::op_Multiply#UnityEngine.Vector3,f32
                (scall UnityEngine.Vector3::op_Multiply#UnityEngine.Vector3,f32
                  (new UnityEngine.Vector3::.ctor#f32,f32,f32 h 0.0f32 0.0f32)
                  (field this Mover::speed))
                (sprop UnityEngine.Time::deltaTime)))))))
```

## 11. Versionierung

- `:version` ist eine ganze Zahl. Abwärtskompatible Erweiterungen (neue optionale Keywords) erhöhen sie nicht, neue Knotentypen oder geänderte Semantik schon.
- Parser müssen unbekannte Keywords ignorieren, unbekannte Knotennamen aber ablehnen (Exit-Code 2).
- Der native Dialekt (Abschnitt 12) gehört zu Version 1, weil noch keine Version veröffentlicht wurde. Ein Parser ohne Unterstützung dafür (M0 bis M10a) lehnt seine Knoten mit der Meldung „nativer Dialekt nicht unterstützt" ab. Ab M10b ist die Unterstützung Pflicht.

## 12. Nativer Dialekt (ADR-023)

Für Code, den `frontend-native` nicht in strukturierte IR heben kann (`lifted-low`). Er teilt Symbol-IDs, Index, Lint und Overlays mit der normalen IR.

### 12.1 Einbettung

Native Funktionen sind normale `method`-Deklarationen mit einem `cfg`-Rumpf und dem Keyword `:native`. Sie liegen in synthetischen Typen (`Native.<Modul>`), Daten als `field` mit `:native`:

```
method  := ... (body | ":abstract")          ; body darf ein cfg sein
nativeinfo := ":native" "(" ":arch" ident ":abi" ident [":addr" hexnum] [":size" nat]
                          [":status" ident] [":module" string] ")"
```

- `:arch`: `x86-64` oder `arm64`. `:abi`: `sysv64`, `win64`, `aapcs64`, `cdecl`, `internal` (Aufrufkonvention, soweit rekonstruiert).
- `:status`: `lifted-low` (Standard im Dialekt) oder `manual` (Rumpf kommt aus `manual/`).
- Felder: `(field gTable ptr :mods (static) :native (:arch x86-64 :abi cdecl :addr 0x1400A0000u64 :size 64) :init (bytes "00ff..."))`.
- Eine native Funktion hat die ID `Native.Modul::name#ParamTypen`, wie jede Methode. Parameter- und Rückgabetypen sind Primitive, `ptr` oder (wenn rekonstruiert) normale Typen.

### 12.2 Speichermodell

- Flacher, byteadressierter 64-Bit-Adressraum, **little endian**. `ptr` ist ein vorzeichenloser 64-Bit-Wert. Arithmetik auf `ptr` wie auf `u64`.
- Der Generator bildet den Speicher auf `NativeMemory` in `runtime-system` ab (ab M10b).
- Der Dialekt kennt keinen Garbage Collector. Speicher gehört dem nativen Code (Allokation über importierte Funktionen wie `malloc`).

### 12.3 Rumpf

```
cfg    := "(" "cfg" "(" { "(" "local" ident type ")" } ")" bb { bb } ")"
bb     := "(" "bb" ident { nstmt } term ")"
nstmt  := stmt                            ; normale Statements (expr, local entfällt, set usw.)
        | "(" "store" type expr expr ")"  ; (store T adresse wert)
term   :=
  "(" "br" ident ")"
| "(" "br-if" expr ident ident ")"        ; Bedingung, Ziel wahr, Ziel falsch
| "(" "br-switch" expr { "(" "case" number ident ")" } "(" "default" ident ")" ")"
| "(" "br-ind" expr { ident } ")"         ; indirekter Sprung, Zielliste wenn bekannt, sonst leer (NT001)
| "(" "ret" [expr] ")"
| "(" "unreachable" ")"
```

- Der erste `bb` ist der Eintrittspunkt. Locals gelten für die ganze Funktion (virtuelle Register), nicht blockweise.
- Jeder `bb` endet mit **genau einem** Terminator. Labels sind innerhalb der Funktion eindeutig.
- Normale Statements dürfen verwendet werden, aber keine strukturierten Sprünge (`if`, `while`, `for`, `switch`, `break`, `continue`, `goto`, `label`). Kontrollfluss läuft nur über Terminatoren. Gelingt Strukturrückgewinnung, wird die Funktion normal (nicht `cfg`).

### 12.4 Ausdrücke

```
expr  += "(" "load" type expr ")"                    ; (load T adresse)
       | "(" "ncall" ntarget type { expr } ")"        ; direkter nativer Aufruf, type = Rückgabetyp
       | "(" "ncall-ind" expr type "(" {type} ")" { expr } ")"
       | "(" "bitcast" type expr ")"                  ; gleiche Breite, Bits unverändert (i32<->f32 usw.)
       | "(" ( "zext" | "sext" | "trunc" ) type expr ")"   ; explizite Breitenänderung
       | "(" ( "rol" | "ror" ) expr expr ")"          ; Rotation
       | "(" "addr-of" mref ")"                       ; Adresse eines nativen Symbols oder Datums
       | "(" "intrin" string type { expr } ")"        ; Maschinen-Intrinsic (SIMD, Atomics)
ntarget := mref | "(" "import" string string ")"      ; (import "Bibliothek" "Funktion")
```

- Operatoren aus Abschnitt 7 gelten, die **Signedness kommt aus dem Operandentyp**: `(/ a b)` auf `u32` ist vorzeichenlos, auf `i32` vorzeichenbehaftet. `>>` ist auf `i*` arithmetisch, auf `u*` logisch. Überlauf wrappt (Zweierkomplement), `checked` ist im Dialekt nicht erlaubt.
- Operanden müssen gleiche Typen haben. Breitenwechsel sind immer explizit (`zext`, `sext`, `trunc`, `conv`).
- Gleitkomma nach IEEE 754. Intrinsics (`intrin`) haben keine Standardsemantik: Der Generator muss den Namen kennen, sonst erzeugt er `todo` (Lint NT003).
- `ncall` auf einen Import mit unbekanntem Namen im Adapter bzw. `runtime-system` ist ein `SH001`.

### 12.5 Beispiel

```lisp
(class Native.libmath
  (method clamp_add u32 ((a u32) (b u32)) :mods (static)
    :native (:arch x86-64 :abi sysv64 :addr 0x00401120u64 :size 24 :status lifted-low)
    (cfg ((local t u32))
      (bb entry
        (expr (set t (+ a b)))
        (br-if (< t a) sat done))
      (bb sat
        (expr (set t 0xFFFFFFFFu32))
        (br done))
      (bb done
        (ret t)))))
```

### 12.6 Validator-Regeln (Ergänzung)

| ID | Prüfung | Stufe |
|---|---|---|
| IR006 | `cfg`: Label doppelt oder Sprungziel existiert nicht | error |
| IR007 | `bb` ohne Terminator oder Terminator nicht am Ende | error |
| IR008 | Dialektknoten (`load`, `store`, `cfg`, ...) außerhalb einer Methode mit `:native` | error |
| IR009 | Operandentypen ungleich ohne explizite Umwandlung | error |
| IR010 | Nicht erreichbarer `bb` | warn |

Lint-Regeln NT001–NT003 stehen in `docs/cli-contract.md`.

### 12.7 Roh-Fallback

Funktionen, die nicht einmal in den Dialekt passen, stehen als `(method name ... :manual "Grund" :native (...) (raw asm "<Disassembly>"))`. Der Text dient nur der Dokumentation für Handarbeit und KI-Fix und wird nie ausgewertet.

