import { QUALIDADE_JPEG, dimensoesFoto } from "./fotoEscala";

describe("fotoEscala", () => {
  test("lado maior da foto fica em 1280 e jpeg é 70", () => {
    expect(QUALIDADE_JPEG).toBe(70);
    expect(dimensoesFoto(2000, 1000)).toEqual({ largura: 1280, altura: 640 });
    expect(dimensoesFoto(800, 600)).toEqual({ largura: 800, altura: 600 });
    expect(dimensoesFoto(1000, 2000)).toEqual({ largura: 640, altura: 1280 });
  });
});
