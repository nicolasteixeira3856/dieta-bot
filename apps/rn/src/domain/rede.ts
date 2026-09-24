export const PERGUNTA_FALHA = "descreve em 1 linha";

export type Estimativa = {
  kcal: number;
  p: number;
  c: number;
  g: number;
  confianca: string;
  pergunta?: string | null;
};

/** Confiança alta não carrega pergunta. Qualquer outra confiança fica com uma linha. */
export function normalizarEstimativa(out: Estimativa): Estimativa {
  if (out.confianca === "alto") return { ...out, pergunta: null };
  const pergunta = out.pergunta?.trim();
  return { ...out, pergunta: pergunta ? pergunta : PERGUNTA_FALHA };
}

/** Falha de transporte vira confiança baixa e a pergunta fixa. */
export async function estimarComFalha(exec: () => Promise<Estimativa>): Promise<Estimativa> {
  try {
    return normalizarEstimativa(await exec());
  } catch {
    return { kcal: 0, p: 0, c: 0, g: 0, confianca: "baixa", pergunta: PERGUNTA_FALHA };
  }
}
