import { calcular, dataSaoPaulo, tetoNaData, weekday, type DataCivil, type TetoPerfil } from "./budgetCalculator";

const segunda: DataCivil = { y: 2026, m: 9, d: 21 };
const terca: DataCivil = { y: 2026, m: 9, d: 22 };
const quarta: DataCivil = { y: 2026, m: 9, d: 23 };
const quinta: DataCivil = { y: 2026, m: 9, d: 24 };
const sexta: DataCivil = { y: 2026, m: 9, d: 25 };
const sabado: DataCivil = { y: 2026, m: 9, d: 26 };
const domingo: DataCivil = { y: 2026, m: 9, d: 27 };

const mesmo: TetoPerfil = { tipo: "mesmo", kcal: 2000 };

describe("budgetCalculator", () => {
  test("política 0 com treino 1000 credita 0", () => {
    const r = calcular({
      data: quarta,
      perfil: mesmo,
      politica: "zero",
      treinoKcal: 1000,
    });
    expect(r.creditoTreino).toBe(0);
    expect(r.tetoEfetivo).toBe(2000);
  });

  test("treino ausente com política 100% credita 0", () => {
    const integral = calcular({
      data: quarta,
      perfil: mesmo,
      politica: "cem",
      treinoKcal: null,
    });
    const parcial = calcular({
      data: quarta,
      perfil: mesmo,
      politica: "parcial",
      percentual: 50,
      treinoKcal: null,
    });
    expect(integral.creditoTreino).toBe(0);
    expect(parcial.creditoTreino).toBe(0);
  });

  test("50% de 480 credita 240", () => {
    const r = calcular({
      data: quarta,
      perfil: mesmo,
      politica: "parcial",
      percentual: 50,
      treinoKcal: 480,
    });
    expect(r.creditoTreino).toBe(240);
    expect(r.tetoEfetivo).toBe(2240);
  });

  test("100% de 480 credita 480", () => {
    const r = calcular({
      data: quarta,
      perfil: mesmo,
      politica: "cem",
      treinoKcal: 480,
    });
    expect(r.creditoTreino).toBe(480);
    expect(r.tetoEfetivo).toBe(2480);
  });

  test("sem cap: treino 2000 a 100% soma 2000 no teto", () => {
    const r = calcular({
      data: quarta,
      perfil: mesmo,
      politica: "cem",
      treinoKcal: 2000,
    });
    expect(r.creditoTreino).toBe(2000);
    expect(r.tetoEfetivo).toBe(4000);
  });

  test("orçamento_janela nunca negativo", () => {
    const r = calcular({
      data: quarta,
      perfil: mesmo,
      politica: "zero",
      consumido: 1500,
      reservaProximas: 700,
    });
    expect(r.tetoEfetivo).toBe(2000);
    expect(r.orcamentoJanela).toBe(0);
    expect(r.orcamentoJanela).toBeGreaterThanOrEqual(0);
  });

  test("teto base difere entre mesmo todos os dias, útil/fds e 7 dias", () => {
    expect(weekday(segunda)).toBe(1);
    expect(weekday(sabado)).toBe(6);
    expect(weekday(domingo)).toBe(7);

    expect(tetoNaData(mesmo, quarta)).toBe(2000);
    expect(tetoNaData(mesmo, domingo)).toBe(2000);

    const util: TetoPerfil = { tipo: "util", util: 2000, fds: 2300 };
    expect(tetoNaData(util, segunda)).toBe(2000);
    expect(tetoNaData(util, sexta)).toBe(2000);
    expect(tetoNaData(util, sabado)).toBe(2300);
    expect(tetoNaData(util, domingo)).toBe(2300);
    expect(tetoNaData(util, segunda)).not.toBe(tetoNaData(util, sabado));

    const sete: TetoPerfil = {
      tipo: "sete",
      segunda: 1900,
      terca: 2100,
      quarta: 1800,
      quinta: 2400,
      sexta: 2000,
      sabado: 2600,
      domingo: 1700,
    };
    const tetos = [segunda, terca, quarta, quinta, sexta, sabado, domingo].map((dia) =>
      tetoNaData(sete, dia),
    );
    expect(tetos).toEqual([1900, 2100, 1800, 2400, 2000, 2600, 1700]);
    expect(new Set(tetos).size).toBe(7);
  });

  test("weekday segue America/Sao_Paulo perto da meia-noite UTC", () => {
    const data = dataSaoPaulo("2026-09-27T02:30:00Z");
    expect(data).toEqual({ y: 2026, m: 9, d: 26 });
    expect(weekday(data)).toBe(6);
    const teto = tetoNaData({ tipo: "util", util: 2000, fds: 2300 }, data);
    expect(teto).toBe(2300);
  });
});
