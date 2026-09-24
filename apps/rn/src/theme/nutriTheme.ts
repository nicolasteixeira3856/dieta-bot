import { MD3DarkTheme, type MD3Theme } from "react-native-paper";

/** MD3DarkTheme com override literal. Sem overlay branco e sem o dark default do Paper. */
export const nutriTheme: MD3Theme = {
  ...MD3DarkTheme,
  dark: true,
  roundness: 14,
  colors: {
    ...MD3DarkTheme.colors,
    primary: "#e8b86d",
    onPrimary: "#111111",
    primaryContainer: "#171b20",
    secondary: "#8b939c",
    background: "#0b0d10",
    surface: "#171b20",
    surfaceVariant: "#1e242b",
    onSurface: "#f3f5f7",
    onSurfaceVariant: "#8b939c",
    outline: "#2a3139",
    error: "#e07a6a",
    onError: "#0b0d10",
    tertiary: "#7dda9a",
    onBackground: "#f3f5f7",
    elevation: {
      level0: "transparent",
      level1: "#12151a",
      level2: "#171b20",
      level3: "#171b20",
      level4: "#171b20",
      level5: "#171b20",
    },
  },
};
