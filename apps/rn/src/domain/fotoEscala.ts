export const LADO_MAXIMO = 1280;
export const QUALIDADE_JPEG = 70;

export function dimensoesFoto(
  largura: number,
  altura: number,
  maxLado: number = LADO_MAXIMO,
): { largura: number; altura: number } {
  const maior = Math.max(largura, altura);
  if (maior <= maxLado || maior === 0) return { largura, altura };
  const fator = maxLado / maior;
  return {
    largura: Math.max(1, Math.trunc(largura * fator)),
    altura: Math.max(1, Math.trunc(altura * fator)),
  };
}
