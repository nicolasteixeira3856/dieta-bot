import { horaSaoPaulo } from "./budgetCalculator";

export function janelaNaHora(hora: number): string {
  if (hora >= 5 && hora <= 9) return "cafe";
  if (hora >= 10 && hora <= 11) return "lanche_manha";
  if (hora >= 12 && hora <= 14) return "almoco";
  if (hora >= 15 && hora <= 17) return "lanche_tarde";
  if (hora >= 18 && hora <= 21) return "janta";
  return "ceia";
}

export function janelaAgora(instant: Date = new Date()): string {
  return janelaNaHora(horaSaoPaulo(instant));
}

export function tituloJanela(id: string): string {
  switch (id) {
    case "cafe":
      return "Café";
    case "lanche_manha":
      return "Lanche manhã";
    case "almoco":
      return "Almoço";
    case "lanche_tarde":
      return "Lanche tarde";
    case "janta":
      return "Janta";
    default:
      return "Ceia";
  }
}

export const JANELAS = ["cafe", "lanche_manha", "almoco", "lanche_tarde", "janta", "ceia"] as const;
