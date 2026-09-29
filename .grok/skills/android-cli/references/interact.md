Run `android layout --help`, `android screen capture --help` and
`android screen resolve --help` for the installed version. Resolve the executable
as described in [the CLI skill](../SKILL.md).

## Device coordination

Select an explicit device serial. Coordinate with any chat already using the
device before changing app state, navigation, theme, size/density or installed
packages. Layout and clean screenshots inspect the current state; instrumentation
installation by a first layout call is a separate device mutation. Use an idle
test device for initialization and journeys. Do not run init, install an APK,
reset data or navigate merely to demonstrate access to another chat's device.

## UI dump

`android layout --device <serial>` returns a JSON tree by default.
`--flat` returns a flat list; `--full` includes non-interactive and hidden
elements; `--output <file.json>` saves JSON without shell encoding ambiguity.
Use UTF-8 when parsing saved JSON.

In CLI 1.0.16457483, `--diff` is a deprecated no-op. It does not return
incremental changes or reduce the response. To inspect only relevant elements,
filter a fresh layout locally and compare saved captures when needed.
`--no-idle` avoids waiting for idle state; use it for a diagnosed idle-wait
problem, not as the default.

Each element may contain:

- `text`, `resourceId`, `contentDesc`.
- `interactions`: checkable, clickable, focusable, scrollable, long-clickable, password.
- `state`: checked, focused, selected.
- `bounds`: `[min X,min Y][max X,max Y]`; `center`: `[x,y]`.
- `off-screen`: the element exists in the hierarchy but may need scrolling to be visible.

Inspect JSON structure before assuming a flat list or optional field exists.
Layout helps locate elements and measure bounds; it does not prove color,
typography or visual fidelity. WebViews, animations or idle waits can prevent
a useful dump. Inspect a screenshot and report the limitation before changing
the device state.

## Clean screenshot

`android screen capture --device <serial> --output <file.png>` saves a PNG
of the current screen. Visually examine each PNG before relying on it.

Use screenshots for images, WebViews, visual appearance and the repository's
gold comparison. Required app captures belong to docs/qa/android/current/dark/
or light/; golds in docs/qa/stitch/ are separate read-only comparison inputs.
Diagnostic captures go in scratch output. Do not claim a screenshot alone
completes the written diff and iterative visual QA.

## Annotated screenshot

`android screen capture --device <serial> --annotate --output <file.png>`
adds numbered labels and bounding boxes. Visually inspect it. Keep annotated
images in scratch output; use clean images for gold comparisons.

In the verified CLI, use
`android screen resolve --screenshot <file.png> --string "tap #3"`.
The option is `--screenshot`, not `--screen`. The result substitutes the
label with its center coordinates. Inspect that result, confirm the current
screen still matches the annotated capture, then send the authorized input
to the selected device. Do not combine resolution with immediate execution of
uninspected shell text.

## Input

Use `adb -s <serial> shell input` for authorized interaction. Locate the
element's current center or bounds and check its available interactions.

```json
{
  "key": -248568265,
  "class": "android.widget.Button",
  "bounds": "[138,9][167,38]",
  "center": "[152,23]"
}
```

Tap this center with `adb -s <serial> shell input tap 152 23`.
For a scrollable list with bounds `[100,200][400,600]`, a slow upward swipe is
`adb -s <serial> shell input swipe 250 400 250 200 500` (500 ms).
Scroll only when the requested action permits it; inspect the result.

### Text input

Ensure the field is focused in its state list before input.
`adb -s <serial> shell input text "arroz%sfeijao"` illustrates ADB's
`%s` space convention. Shell quoting and special-character behavior differ
between PowerShell and POSIX; this command is not a general Unicode input API.
Use the project's existing Unicode-capable capture/test path when required.
Enter uses `adb -s <serial> shell input keyevent 66`.

After an authorized action, allow content to load and fetch a fresh layout.
Do not use `--diff` to wait for or prove a change.
