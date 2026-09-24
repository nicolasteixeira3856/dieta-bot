import AsyncStorage from "@react-native-async-storage/async-storage";

const CHAVE = "nutri-dia-v1";

export type LogDia = {
  janela: string;
  kcal: number;
  p: number;
  c: number;
  g: number;
  estavel: boolean;
};

export type DiaSalvo = {
  onboardingFeito: boolean;
  diaApp: number;
  modo: "mesmo" | "util" | "sete";
  kcalUnico: string;
  campoUtil: string;
  campoFds: string;
  camposDia: string[];
  eat: "zero" | "parcial" | "cem";
  pct: string;
  treino: string;
  logs: LogDia[];
  removidas: string[];
  perguntadas: string[];
};

export const DIA_INICIAL: DiaSalvo = {
  onboardingFeito: false,
  diaApp: 1,
  modo: "mesmo",
  kcalUnico: "2000",
  campoUtil: "2000",
  campoFds: "2300",
  camposDia: ["1900", "2100", "1800", "2400", "2000", "2600", "1700"],
  eat: "parcial",
  pct: "50",
  treino: "",
  logs: [],
  removidas: [],
  perguntadas: [],
};

export async function lerDia(): Promise<DiaSalvo> {
  const cru = await AsyncStorage.getItem(CHAVE);
  if (!cru) return DIA_INICIAL;
  const parsed = JSON.parse(cru) as Partial<DiaSalvo>;
  return { ...DIA_INICIAL, ...parsed, logs: parsed.logs ?? [], camposDia: parsed.camposDia ?? DIA_INICIAL.camposDia };
}

export async function gravarDia(dia: DiaSalvo): Promise<void> {
  await AsyncStorage.setItem(CHAVE, JSON.stringify(dia));
}
