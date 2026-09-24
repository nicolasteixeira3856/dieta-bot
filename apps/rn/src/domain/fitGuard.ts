export type PratoOferta = { nome: string; kcal: number };

/** Nunca oferecer prato que explode o teto da janela. */
export function ofertasQueCabem(pratos: PratoOferta[], orcamentoKcal: number): PratoOferta[] {
  return pratos.filter((prato) => prato.nome.trim().length > 0 && prato.kcal <= orcamentoKcal);
}
