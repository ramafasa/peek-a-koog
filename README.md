<img src="app/src/main/resources/kodee/kodee-peekaboo.svg" width="96" alt="Kodee covering its eyes">

# Peek-a-Koog

A Kotlin AI agent learns to play hide-and-seek, one failure at a time.

You hide Kodee, the Kotlin mascot, somewhere in a small house. Koog the detective, an agent built with
[Koog](https://github.com/JetBrains/koog), has to find it while the audience watches it walk from room to room. The first agent is hopeless. Every level of this repo fixes the failure the previous
level showed. This is the demo app for a KotlinConf talk proposal.

## The rule that matters most

The **engine is plain, deterministic Kotlin** (`engine/`). It knows where the toy is and it decides whether
the seeker found it. The LLM only chooses what to do next. A model that *says* it found Kodee proves nothing.

## Modules

| Module | What's inside |
|---|---|
| `engine` | The house, the rules and the event log. No AI, fully unit-tested. |
| `agent` | Koog seekers, one per `Level`, and the tools they use (`SeekerTools`). |
| `app` | Compose Desktop UI: a top-down floor plan where you hide Kodee and watch the detective search, plus the level picker and the engine's event log. |

## Levels

| Level | Idea | Expected failure |
|---|---|---|
| Baseline · Plain Kotlin | A hand-written search, no AI. Runs without an API key. | None: this is the bar every agent has to beat |
| 0 · Just a prompt | No tools at all | Claims to have found the toy without looking anywhere |
| 1 · Tools, in the dark | Tools + Koog's default strategy | Does odd things and you can't see why |
| 2 · Observable | Event handlers, tracing, OpenTelemetry | *planned* |
| 3 · Stateful | Beliefs as structured output, heatmap | *planned* |
| 4 · Graph | Custom strategy graph, move budget, history compression | *planned* |
| 5 · Guarded | The engine, not the model, decides who won | *planned* |
| 6 · Mature | Evals, long-term memory, persistence, model race | *planned* |

The commit that finishes a level gets the tag `level-N`.

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
