import "react-native-gesture-handler";

import { BottomSheetModalProvider } from "@gorhom/bottom-sheet";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { PaperProvider } from "react-native-paper";
import { SafeAreaProvider } from "react-native-safe-area-context";

import { DayProvider } from "../src/data/day";
import { nutriTheme } from "../src/theme/nutriTheme";
import { tokens } from "../src/theme/tokens";

const queryClient = new QueryClient();

export default function RootLayout() {
  return (
    <GestureHandlerRootView style={{ flex: 1, backgroundColor: tokens.bg }}>
      <SafeAreaProvider>
        <PaperProvider theme={nutriTheme}>
          <QueryClientProvider client={queryClient}>
            <BottomSheetModalProvider>
              <DayProvider>
                <StatusBar style="light" />
                <Stack
                  screenOptions={{
                    headerShown: false,
                    contentStyle: { backgroundColor: tokens.bg },
                  }}
                />
              </DayProvider>
            </BottomSheetModalProvider>
          </QueryClientProvider>
        </PaperProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
