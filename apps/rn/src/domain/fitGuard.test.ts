import { ofertasQueCabem } from "./fitGuard";

describe("fitGuard", () => {
  test("não oferece prato cuja kcal estoura o orçamento da janela", () => {
    const ofertas = ofertasQueCabem(
      [
        { nome: "hamburguer", kcal: 900 },
        { nome: "ovos", kcal: 380 },
        { nome: "", kcal: 100 },
      ],
      455,
    );
    expect(ofertas.map((p) => p.nome)).toEqual(["ovos"]);
    expect(ofertas.every((p) => p.kcal <= 455)).toBe(true);
  });
});
