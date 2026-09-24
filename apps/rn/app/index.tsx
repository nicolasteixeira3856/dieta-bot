import { Redirect } from "expo-router";

import { useDia } from "../src/data/day";
import { Home } from "../src/features/Home";

export default function Index() {
  const { dia } = useDia();
  if (!dia.onboardingFeito) return <Redirect href="/onboarding/teto" />;
  return <Home />;
}
