# 003 — Client React Native

O client mora em `apps/rn/`. Só Android. O kit de UI deste goal é Paper, não NativeWind e não React Native Reusables: o prompt do goal travou Paper 5 estável e proibiu NativeWind neste experimento. A constituição da raiz continua valendo para produto, LLM e telas.

## Versões

- Expo SDK 57 estável, pacote `expo` `~57.0.25`. Não é o SDK 58 beta.
- `react-native-paper` `5.15.3`. Não é a linha 6 alpha.
- `@gorhom/bottom-sheet` `5.2.14`.
- `react-native-reanimated` `4.5.1` com `react-native-worklets` `0.10.1`.
- Expo Router `~57.0.23`. TypeScript `strict`. `newArchEnabled: true`.
- Sem pasta `ios/` e sem chave `ios` no `app.json`.

## Tema

`src/theme/nutriTheme.ts` faz spread de `MD3DarkTheme` e substitui as cores que pintam tela: primary `#e8b86d`, onPrimary `#111111`, primaryContainer `#171b20`, secondary `#8b939c`, background `#0b0d10`, surface `#171b20`, surfaceVariant `#1e242b`, onSurface `#f3f5f7`, onSurfaceVariant `#8b939c`, outline `#2a3139`, error `#e07a6a`, onError `#0b0d10`, tertiary `#7dda9a`, onBackground `#f3f5f7`. Elevation level1 é `#12151a` e os níveis seguintes `#171b20`, sem o overlay branco do MD3. `roundness` 14. `dark` true. O `PaperProvider` recebe esse objeto.

## O que não veio do Paper

Saldo 34pt, composer, barra de 6px e cards da home são `StyleSheet`. O CTA sólido é fundo `#f3f5f7` e texto `#111`, não um `Button` contained dourado. T1 e T3 são bottom sheet do gorhom, raio 22 no topo e alça `#3a424c`. Não há Appbar, FAB, BottomNavigation nem Dialog centralizado.

## URL e persistência

`EXPO_PUBLIC_API_PUBLIC_URL` e `EXPO_PUBLIC_INVITE_CODE` vêm do ambiente. O exemplo commitado é `apps/rn/.env.example`: `http://127.0.0.1:8080` e `troca-isto`. Sem essas variáveis, o client usa o mesmo fallback. O header é `X-Invite`. O client não lê o `.env` da raiz e não embute chave OpenAI. No emulador, `adb reverse tcp:8080 tcp:8080` faz o `127.0.0.1` do aparelho chegar no host. O dia (perfil, eat-back, teto, logs) fica num JSON no AsyncStorage, chave `nutri-dia-v1`. Sem SQLite. A foto vai em base64 no POST e não é gravada.
