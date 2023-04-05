# cljs-atc

A [re-frame](https://github.com/Day8/re-frame)-powered game designed to simulate an Air Traffic Control station, with robust voice input support.

## Architecture

This section describes the full loop and how the different systems intersect,
so hopefully I don't totally forget if I don't work on this for a while.

1. The `atc.voice.parsing` namespace contains a description of the grammar for
   the voice commands we accept, which gets turned into a grammar parser via the
   `instaparse` library.
2. When we initialize the voice system via `[:voice/start!]`, we use that parser
   to generate a "grammar" that our speech recognizer (Kaldi) can understand—this is
   basically a bunch of example utterances used to tune the recognizer and restrict
   it from producing output that is not part of our expected grammar.
3. While the transmit key (spacebar) is held, we stream audio into the speech recognizer;
   when the speech recognizer emits a result, we enqueue it for processing. Note here that
   Kaldi may emit results before the user has actually finished speaking (IE:
   the transmit key is still held down). This is unfortunate, but luckily its grammar handles
   that fairly well
4. When the transmit key is released and we receive the final speech recognition result, we
   concat all of the results into a single string, and feed that into the grammar parser
   created in step 1 (the `atc.voice.process` namespace). We then transform
   this parsed result using `instaparse`'s "transform" utility into a series of
   "instructions" that get dispatched to the game engine as
   `[:game/command {:callsign, :instructions}]`.
5. The game engine (`atc.engine.core`) dispatches these instructions to the
   Aircraft in order, and emits any responses as `[:speech/enqueue {:message, :from}]`
6. When an aircraft receives an instruction, it turns those into "commands" that get
   stored in its internal state (`atc.engine.aircraft.instructions`). This is
   basically a big state machine.
7. At each tick of the game engine (via `[:game/tick]`) the aircraft "applies"
   the commands it has set to adjust course, speed, altitude, etc.
   (`atc.engine.aircraft.commands`) and then does some math based on its
   configured speed and heading to compute its new position.

To view the results of all this, we use `react-pixi` to a bunch of React components based on the state of all the aircraft, etc. in the re-frame database. Because the game tick is just a re-frame event, this makes the rendering quite natural---any updates to an Aircraft result in its component being re-rendered, resulting in the aircraft on screen getting updated.

To show the aircraft's previous locations, we actually keep a *complete* history of the last `N` game states. This is quite easy to do relatively efficiently due to clojure's immutable datastructures. This also means that, theoretically, we could support traveling backward in time by simply popping the history, since everything is stateless!
