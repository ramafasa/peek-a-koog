<img src="app/src/main/resources/kodee/kodee-peekaboo.svg" width="96" alt="Kodee covering its eyes">

# Peek-a-Koog

A Kotlin AI agent learns to play hide-and-seek, one failure at a time.

You hide Kodee, the Kotlin mascot, somewhere in a small house. Koog the detective, an agent built with
[Koog](https://github.com/JetBrains/koog), has to find it while the audience watches it walk from room to room. The first agent is hopeless. Every level of this repo fixes the failure the previous
level showed. This is the demo app for a KotlinConf talk proposal.

## The rule that matters most

The **engine is plain, deterministic Kotlin** (`engine/`). It knows where the toy is and it decides whether
the seeker found it. The LLM only chooses what to do next. A model that *says* it found Kodee proves nothing.

## The game is hard on purpose

A strong model finds Kodee in a small, fully described house almost every time, and then there is nothing
to show. So the house and the rules leave room for mistakes:

- **Nine rooms, 27 hiding spots, one long hall.** Every room opens onto the hall, except the laundry room
  (behind the bathroom) and the garage (outside: through the front door and across the front yard).
  From the hall almost everything sounds "warm", so listening there tells you little.

  ```
  bedroom | kids room | bathroom ┊ laundry room
  ========== hall ===============🚪 front yard
  living  |   study   | kitchen  ┊ garage
  ```
- **A tight move budget: 28.** The plain-Kotlin baseline needs up to 27 (`the move budget is tight` test keeps it that way).
  Walking, peeking and listening cost a move; looking around is free.
- **No map for the agents** (from Level 1 on): the prompt only says where they start. They have to look around
  and remember where they've been.
- **Footprints on the floor**: every stretch the detective walks again gets a new lane, brown → orange → red,
  and a spot peeked twice gets a red **×2**. You can see wasted moves even before Level 2 shows the reasons.

## Modules

| Module | What's inside |
|---|---|
| `engine` | The house, the rules and the event log. No AI, fully unit-tested. |
| `agent` | One package per level (`baseline`, `level0`, `level1`, `level2`, …), each a self-contained `Seeker`, plus a small harness shared by all (`Seeker`, `Llm`, `Thought`, the `play` referee). |
| `app` | Compose Desktop UI: a top-down floor plan where you hide Kodee and watch the detective search, plus the level picker and the engine's event log. |

## Levels

| Level | Idea | Expected failure |
|---|---|---|
| You · Human player | The engine hides Kodee and you search: click a room to walk (every move gives a free hot/cold hint), furniture to peek, or **Listen** where you stand for 1 move. Same engine calls as the agent's tools. Use it to explain the rules on stage. | Yours 🙂 |
| Baseline · Plain Kotlin | A hand-written search, no AI. Runs without an API key. | None: this is the bar every agent has to beat |
| 0 · Just a prompt | No tools at all | Claims to have found the toy without looking anywhere |
| 1 · Tools, in the dark | Tools + Koog's default strategy, no map | Wanders, re-checks spots, runs out of moves, and you can't see why |
| 2 · Lights on | The Level 1 agent unchanged, plus Koog's `EventHandler` (the **What Koog is thinking** panel and the detective's thought bubbles) and `OpenTelemetry` (one trace per game) | Now you can *see* why it fails: the next level's problem |
| 3 · Stateful | Beliefs as structured output, heatmap | *planned* |
| 4 · Graph | Custom strategy graph, move budget, history compression | *planned* |
| 5 · Guarded | The engine, not the model, decides who won | *planned* |
| 6 · Mature | Evals, long-term memory, persistence, model race | *planned* |

The commit that finishes a level gets the tag `level-N`.

**Levels never import each other.** A level that builds on the previous one copies its code (prompt, tools, …),
so every level reads on its own and `diff level1/ level2/` shows exactly what changed: for Level 2, the four lines
of `installFeatures`.

## Running

Requires JDK 17+. The seeker picks its LLM from the environment:

| Variable | Model |
|---|---|
| `ANTHROPIC_API_KEY` | Claude Sonnet 5 |
| `OPENAI_API_KEY` | GPT-5 mini |
| *(none)* | `llama3.2` via a local [Ollama](https://ollama.com) |

```bash
export ANTHROPIC_API_KEY=...
./gradlew :app:run
```

Hover over the furniture and click to hide Kodee, pick a level and press **Start seeking**.
The Baseline level needs no API key, so it is a quick way to see the whole show.
In **You · Human player** mode press **New game** and find Kodee yourself; **Show Kodee** reveals where it is hiding.

### Traces (Level 2+)

```bash
docker compose up -d                                   # Jaeger, UI at http://localhost:16686
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318
./gradlew :app:run                                     # play Level 2, then open Jaeger, service "peek-a-koog"
```

Langfuse works too: set `LANGFUSE_PUBLIC_KEY`, `LANGFUSE_SECRET_KEY` (and `LANGFUSE_HOST` if self-hosted).

```bash
./gradlew test   # engine rules, agent tests with a mocked LLM, and PNG snapshots of the house in app/build/snapshots
```

## Boo! journal

Every real failure goes into [`JOURNAL.md`](JOURNAL.md), with a date and a screenshot.
That journal is the script of the talk.

## Credits

Kodee by JetBrains s.r.o. is licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)
([Kotlin brand assets](https://kotlinlang.org/docs/kotlin-brand-assets.html)).
The logo (`kodee-peekaboo.svg`) is adapted from the official Kodee artwork: arms redrawn to cover the eyes, eyes closed.
The detective and the furniture are drawn in code.
