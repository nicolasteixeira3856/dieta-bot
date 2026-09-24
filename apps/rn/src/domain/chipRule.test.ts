import { chipDaJanela, type LogEstavel } from "./chipRule";

describe("chipRule", () => {
  test("dia 1 não mostra chip mesmo com dois logs estáveis", () => {
    const chip = chipDaJanela(
      1,
      "cafe",
      [
        { janela: "cafe", estavel: true },
        { janela: "cafe", estavel: true },
      ],
      new Set(),
      new Set(),
    );
    expect(chip).toBeNull();
  });

  test("2º log estável da mesma janela pergunta uma vez e pode ser removido", () => {
    const logs: LogEstavel[] = [
      { janela: "cafe", estavel: true },
      { janela: "cafe", estavel: true },
      { janela: "almoco", estavel: true },
    ];
    expect(chipDaJanela(2, "cafe", logs, new Set(), new Set())).toEqual({
      janela: "cafe",
      pergunta: true,
    });
    expect(chipDaJanela(2, "cafe", logs, new Set(), new Set(["cafe"]))).toEqual({
      janela: "cafe",
      pergunta: false,
    });
    expect(chipDaJanela(2, "cafe", logs, new Set(["cafe"]), new Set(["cafe"]))).toBeNull();
  });
});
