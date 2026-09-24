import 'package:flutter_test/flutter_test.dart';
import 'package:nutri/domain/rules/budget_calculator.dart';

void main() {
  // Semana de 21 a 27 de setembro de 2026: segunda a domingo.
  final segunda = DateTime(2026, 9, 21);
  final terca = DateTime(2026, 9, 22);
  final quarta = DateTime(2026, 9, 23);
  final quinta = DateTime(2026, 9, 24);
  final sexta = DateTime(2026, 9, 25);
  final sabado = DateTime(2026, 9, 26);
  final domingo = DateTime(2026, 9, 27);

  setUp(() {
    expect(segunda.weekday, DateTime.monday);
    expect(terca.weekday, DateTime.tuesday);
    expect(quarta.weekday, DateTime.wednesday);
    expect(quinta.weekday, DateTime.thursday);
    expect(sexta.weekday, DateTime.friday);
    expect(sabado.weekday, DateTime.saturday);
    expect(domingo.weekday, DateTime.sunday);
  });

  group('teto base, crédito travado em 0', () {
    const perfilFixo = TetoMesmoTodosOsDias(2000);

    test('mesmo todos os dias usa o mesmo teto na quarta e no domingo', () {
      final diaUtil = _calcular(
        data: quarta,
        perfil: perfilFixo,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
      );
      final fimDeSemana = _calcular(
        data: domingo,
        perfil: perfilFixo,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
      );

      expect(diaUtil.tetoBase, 2000);
      expect(fimDeSemana.tetoBase, 2000);
      expect(diaUtil.creditoTreino, 0);
      expect(fimDeSemana.creditoTreino, 0);
      expect(diaUtil.tetoEfetivo, 2000);
      expect(fimDeSemana.tetoEfetivo, 2000);
    });

    test('util/fds separa seg–sex de sáb e de dom', () {
      const perfil = TetoUtilFds(util: 2000, fds: 2300);

      final naSegunda = _calcular(
        data: segunda,
        perfil: perfil,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
      );
      final naSexta = _calcular(
        data: sexta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
      );
      final noSabado = _calcular(
        data: sabado,
        perfil: perfil,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
      );
      final noDomingo = _calcular(
        data: domingo,
        perfil: perfil,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
      );

      expect(naSegunda.tetoBase, 2000);
      expect(naSexta.tetoBase, 2000);
      expect(noSabado.tetoBase, 2300);
      expect(noDomingo.tetoBase, 2300);
      expect(naSegunda.tetoBase, isNot(noSabado.tetoBase));
      expect(naSexta.tetoBase, isNot(noDomingo.tetoBase));
      expect(noSabado.creditoTreino, 0);
      expect(noDomingo.tetoEfetivo, 2300);
    });

    test('7 dias escolhe o teto daquele weekday, sem colapsar a semana', () {
      const perfil = TetoSeteDias(
        segunda: 1900,
        terca: 2100,
        quarta: 1800,
        quinta: 2400,
        sexta: 2000,
        sabado: 2600,
        domingo: 1700,
      );
      final dias = [segunda, terca, quarta, quinta, sexta, sabado, domingo];
      final tetos = dias.map((data) {
        final resultado = _calcular(
          data: data,
          perfil: perfil,
          politica: PoliticaCreditoTreino.zero,
          treinoKcal: 1000,
        );
        expect(resultado.creditoTreino, 0);
        expect(resultado.tetoEfetivo, resultado.tetoBase);
        return resultado.tetoBase;
      }).toList();

      expect(tetos, [1900, 2100, 1800, 2400, 2000, 2600, 1700]);
      expect(tetos.toSet(), hasLength(7));
    });
  });

  group('crédito de treino', () {
    const perfil = TetoMesmoTodosOsDias(2000);

    test('política 0 com treino 1000 não mexe no teto', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
      );

      expect(resultado.creditoTreino, 0);
      expect(resultado.tetoBase, 2000);
      expect(resultado.tetoEfetivo, 2000);
    });

    test('treino não informado zera o crédito em 100% e no parcial', () {
      final integral = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.cem,
        percentual: 100,
      );
      final parcial = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.parcial,
        percentual: 50,
      );

      expect(integral.creditoTreino, 0);
      expect(integral.tetoEfetivo, 2000);
      expect(parcial.creditoTreino, 0);
      expect(parcial.tetoEfetivo, 2000);
    });

    test('treino informado como 0 em 100% credita 0', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.cem,
        treinoKcal: 0,
      );

      expect(resultado.creditoTreino, 0);
      expect(resultado.tetoEfetivo, 2000);
    });

    test('parcial 50% de 1000 credita 500, acima de 300', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.parcial,
        percentual: 50,
        treinoKcal: 1000,
      );

      expect(resultado.creditoTreino, 500);
      expect(resultado.tetoEfetivo, 2500);
      expect(resultado.creditoTreino, greaterThan(300));
    });

    test('parcial diferente de 50: 25% de 800 credita 200', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.parcial,
        percentual: 25,
        treinoKcal: 800,
      );

      expect(resultado.creditoTreino, 200);
      expect(resultado.tetoEfetivo, 2200);
    });

    test('100% de 1000 credita 1000, sem cap de 300', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.cem,
        treinoKcal: 1000,
      );

      expect(resultado.creditoTreino, 1000);
      expect(resultado.tetoBase, 2000);
      expect(resultado.tetoEfetivo, 3000);
      expect(resultado.creditoTreino, greaterThan(300));
    });
  });

  group('teto efetivo e orçamento da janela', () {
    const perfil = TetoMesmoTodosOsDias(2000);

    test('sobra positiva: 2000 - 400 - 250 = 1350', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
        consumido: 400,
        reservaProximas: 250,
      );

      expect(resultado.tetoEfetivo, 2000);
      expect(resultado.orcamentoJanela, 1350);
    });

    test('consumido + reserva acima do teto efetivo vira 0', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.zero,
        treinoKcal: 1000,
        consumido: 1500,
        reservaProximas: 700,
      );

      expect(resultado.tetoEfetivo, 2000);
      expect(1500 + 700, greaterThan(resultado.tetoEfetivo));
      expect(resultado.orcamentoJanela, 0);
    });

    test('a janela usa o teto efetivo, com o crédito dentro', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.parcial,
        percentual: 50,
        treinoKcal: 1000,
        consumido: 900,
        reservaProximas: 100,
      );

      expect(resultado.tetoBase, 2000);
      expect(resultado.creditoTreino, 500);
      expect(resultado.tetoEfetivo, 2500);
      expect(resultado.orcamentoJanela, 1500);
    });

    test('crédito grande ainda não impede o piso em 0', () {
      final resultado = _calcular(
        data: quarta,
        perfil: perfil,
        politica: PoliticaCreditoTreino.cem,
        treinoKcal: 1000,
        consumido: 2500,
        reservaProximas: 800,
      );

      expect(resultado.tetoEfetivo, 3000);
      expect(2500 + 800, greaterThan(resultado.tetoEfetivo));
      expect(resultado.orcamentoJanela, 0);
    });
  });
}

ResultadoOrcamento _calcular({
  required DateTime data,
  required TetoPerfil perfil,
  required PoliticaCreditoTreino politica,
  num? percentual,
  num? treinoKcal,
  num consumido = 0,
  num reservaProximas = 0,
}) {
  return const BudgetCalculator().calcular(
    EntradaOrcamento(
      data: data,
      perfil: perfil,
      politica: politica,
      percentual: percentual,
      treinoKcal: treinoKcal,
      consumido: consumido,
      reservaProximas: reservaProximas,
    ),
  );
}
