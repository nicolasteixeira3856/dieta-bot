import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { View } from "react-native";

import { calcular, dataSaoPaulo, type TetoPerfil } from "../domain/budgetCalculator";
import { chipDaJanela, type Chip } from "../domain/chipRule";
import { janelaAgora } from "../domain/janela";
import { gravarDia, lerDia, type DiaSalvo, type LogDia } from "./storage";

type DiaCtx = {
  pronto: boolean;
  dia: DiaSalvo;
  patch: (parcial: Partial<DiaSalvo>) => void;
  confirmarLog: (log: LogDia) => void;
  removerChip: (janela: string) => void;
  responderChip: (janela: string, criar: boolean) => void;
};

const Ctx = createContext<DiaCtx | null>(null);

export function DayProvider({ children }: { children: ReactNode }) {
  const [dia, setDia] = useState<DiaSalvo | null>(null);

  useEffect(() => {
    let vivo = true;
    lerDia().then((salvo) => {
      if (vivo) setDia(salvo);
    });
    return () => {
      vivo = false;
    };
  }, []);

  const api = useMemo<DiaCtx | null>(() => {
    if (!dia) return null;
    const gravar = (proximo: DiaSalvo) => {
      setDia(proximo);
      void gravarDia(proximo);
    };
    return {
      pronto: true,
      dia,
      patch: (parcial) => gravar({ ...dia, ...parcial }),
      confirmarLog: (log) => gravar({ ...dia, logs: [...dia.logs, log] }),
      removerChip: (janela) =>
        gravar({ ...dia, removidas: [...new Set([...dia.removidas, janela])] }),
      responderChip: (janela, criar) =>
        gravar({
          ...dia,
          perguntadas: [...new Set([...dia.perguntadas, janela])],
          removidas: criar ? dia.removidas : [...new Set([...dia.removidas, janela])],
        }),
    };
  }, [dia]);

  if (!api) return <View style={{ flex: 1, backgroundColor: "#0b0d10" }} />;
  return <Ctx.Provider value={api}>{children}</Ctx.Provider>;
}

export function useDia(): DiaCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useDia fora do DayProvider");
  return ctx;
}

function numero(valor: string, fallback: number): number {
  const n = Number(valor);
  return Number.isFinite(n) ? n : fallback;
}

export function perfilDe(dia: DiaSalvo): TetoPerfil {
  if (dia.modo === "util") {
    return { tipo: "util", util: numero(dia.campoUtil, 2000), fds: numero(dia.campoFds, 2300) };
  }
  if (dia.modo === "sete") {
    const d = dia.camposDia;
    return {
      tipo: "sete",
      segunda: numero(d[0] ?? "", 1900),
      terca: numero(d[1] ?? "", 2100),
      quarta: numero(d[2] ?? "", 1800),
      quinta: numero(d[3] ?? "", 2400),
      sexta: numero(d[4] ?? "", 2000),
      sabado: numero(d[5] ?? "", 2600),
      domingo: numero(d[6] ?? "", 1700),
    };
  }
  return { tipo: "mesmo", kcal: numero(dia.kcalUnico, 2000) };
}

export function orcamentoDoDia(dia: DiaSalvo, agora: Date = new Date()) {
  const data = dataSaoPaulo(agora.toISOString());
  const politica = dia.eat === "zero" ? "zero" : dia.eat === "cem" ? "cem" : "parcial";
  const treinoInformado = dia.treino.trim().length > 0;
  const comido = dia.logs.reduce((soma, log) => soma + log.kcal, 0);
  const proteina = dia.logs.reduce((soma, log) => soma + log.p, 0);
  const resultado = calcular({
    data,
    perfil: perfilDe(dia),
    politica,
    percentual: numero(dia.pct, 50),
    treinoKcal: treinoInformado ? numero(dia.treino, 0) : null,
    consumido: comido,
    reservaProximas: 0,
  });
  const chip: Chip | null = chipDaJanela(
    dia.diaApp,
    janelaAgora(agora),
    dia.logs.map((log) => ({ janela: log.janela, estavel: log.estavel })),
    new Set(dia.removidas),
    new Set(dia.perguntadas),
  );
  return { ...resultado, comido, proteina, data, janela: janelaAgora(agora), chip };
}
