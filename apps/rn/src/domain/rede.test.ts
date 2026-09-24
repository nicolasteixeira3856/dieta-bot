import { PERGUNTA_FALHA, estimarComFalha } from "./rede";

describe("rede", () => {
  test('falha de rede vira confiança baixa e a pergunta "descreve em 1 linha"', async () => {
    const item = await estimarComFalha(async () => {
      throw new Error("down");
    });
    expect(item.confianca).toBe("baixa");
    expect(item.pergunta).toBe(PERGUNTA_FALHA);
    expect(item.pergunta).toBe("descreve em 1 linha");
  });

  test("confiança alta omite a pergunta", async () => {
    const item = await estimarComFalha(async () => ({
      kcal: 385,
      p: 18,
      c: 40,
      g: 10,
      confianca: "alto",
      pergunta: "sumir",
    }));
    expect(item.pergunta).toBeNull();
    expect(item.kcal).toBe(385);
  });
});
