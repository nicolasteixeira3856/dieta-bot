/** Teto do dia e orçamento da janela. Dado puro, sem React e sem rede. */

export const ZONA = "America/Sao_Paulo";
const OFFSET_MS = 3 * 60 * 60 * 1000;

export type DataCivil = { y: number; m: number; d: number };

export type TetoPerfil =
  | { tipo: "mesmo"; kcal: number }
  | { tipo: "util"; util: number; fds: number }
  | {
      tipo: "sete";
      segunda: number;
      terca: number;
      quarta: number;
      quinta: number;
      sexta: number;
      sabado: number;
      domingo: number;
    };

export type PoliticaCredito = "zero" | "parcial" | "cem";

export type EntradaOrcamento = {
  data: DataCivil;
  perfil: TetoPerfil;
  politica: PoliticaCredito;
  percentual?: number | null;
  treinoKcal?: number | null;
  consumido?: number;
  reservaProximas?: number;
};

export type ResultadoOrcamento = {
  tetoBase: number;
  creditoTreino: number;
  tetoEfetivo: number;
  orcamentoJanela: number;
};

/** 1 = segunda … 7 = domingo. Meio-dia UTC evita a borda do fuso da máquina. */
export function weekday(data: DataCivil): number {
  const js = new Date(Date.UTC(data.y, data.m - 1, data.d, 12)).getUTCDay();
  return js === 0 ? 7 : js;
}

function deslocar(instant: Date): Date {
  return new Date(instant.getTime() - OFFSET_MS);
}

/** Instant UTC → data civil em America/Sao_Paulo (UTC−3, sem horário de verão). */
export function dataSaoPaulo(instantIso: string): DataCivil {
  const deslocado = deslocar(new Date(Date.parse(instantIso)));
  return {
    y: deslocado.getUTCFullYear(),
    m: deslocado.getUTCMonth() + 1,
    d: deslocado.getUTCDate(),
  };
}

export function horaSaoPaulo(instant: Date = new Date()): number {
  return deslocar(instant).getUTCHours();
}

/** ISO local com offset −03:00, para o POST de estimate/fit. */
export function localTimeSaoPaulo(instant: Date = new Date()): string {
  const sp = deslocar(instant);
  const p = (n: number) => String(n).padStart(2, "0");
  return `${sp.getUTCFullYear()}-${p(sp.getUTCMonth() + 1)}-${p(sp.getUTCDate())}T${p(sp.getUTCHours())}:${p(sp.getUTCMinutes())}:${p(sp.getUTCSeconds())}-03:00`;
}

export function tetoNaData(perfil: TetoPerfil, data: DataCivil): number {
  if (perfil.tipo === "mesmo") return perfil.kcal;
  const dia = weekday(data);
  if (perfil.tipo === "util") return dia >= 6 ? perfil.fds : perfil.util;
  switch (dia) {
    case 1:
      return perfil.segunda;
    case 2:
      return perfil.terca;
    case 3:
      return perfil.quarta;
    case 4:
      return perfil.quinta;
    case 5:
      return perfil.sexta;
    case 6:
      return perfil.sabado;
    default:
      return perfil.domingo;
  }
}

/** Política 0, ou treino ainda não informado, zera o crédito. Sem cap. */
export function creditoTreino(
  politica: PoliticaCredito,
  percentual: number | null | undefined,
  treinoKcal: number | null | undefined,
): number {
  if (politica === "zero" || treinoKcal == null) return 0;
  if (politica === "cem") return treinoKcal;
  if (percentual == null) throw new Error("percentual");
  return (treinoKcal * percentual) / 100;
}

export function calcular(entrada: EntradaOrcamento): ResultadoOrcamento {
  const tetoBase = tetoNaData(entrada.perfil, entrada.data);
  const credito = creditoTreino(entrada.politica, entrada.percentual, entrada.treinoKcal);
  const tetoEfetivo = tetoBase + credito;
  const bruto = tetoEfetivo - (entrada.consumido ?? 0) - (entrada.reservaProximas ?? 0);
  return {
    tetoBase,
    creditoTreino: credito,
    tetoEfetivo,
    orcamentoJanela: bruto < 0 ? 0 : bruto,
  };
}
