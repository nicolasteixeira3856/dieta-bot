export type LogEstavel = { janela: string; estavel: boolean };
export type Chip = { janela: string; pergunta: boolean };

/**
 * Chip nasce no 2º log estável da mesma janela.
 * Pergunta uma vez. Dá para remover.
 * Dia 1 do app não mostra chip.
 */
export function chipDaJanela(
  diaApp: number,
  janelaAtual: string,
  logs: LogEstavel[],
  removidas: ReadonlySet<string>,
  perguntadas: ReadonlySet<string>,
): Chip | null {
  if (diaApp <= 1) return null;
  const contagem = new Map<string, number>();
  for (const log of logs) {
    if (!log.estavel) continue;
    contagem.set(log.janela, (contagem.get(log.janela) ?? 0) + 1);
  }
  const candidatas = [...contagem.entries()]
    .filter(([janela, n]) => n >= 2 && !removidas.has(janela))
    .map(([janela]) => janela);
  if (candidatas.length === 0) return null;
  const janela = candidatas.includes(janelaAtual) ? janelaAtual : candidatas[0];
  return { janela, pergunta: !perguntadas.has(janela) };
}
