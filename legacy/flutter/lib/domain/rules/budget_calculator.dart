/// Teto do dia e orçamento da janela. Dado puro, sem widget e sem rede.
///
/// Crédito de treino não tem cap de kcal. Treino ausente (`null`) é diferente
/// de treino informado como 0: os dois creditam 0, mas só o null significa
/// que o dia ainda não foi preenchido.
library;

/// Como o perfil guarda o teto. Cada variante carrega só os números que usa.
sealed class TetoPerfil {
  const TetoPerfil();

  /// Teto base da [data], antes do crédito de treino.
  int tetoNaData(DateTime data);
}

/// O mesmo teto em qualquer dia da semana.
final class TetoMesmoTodosOsDias extends TetoPerfil {
  const TetoMesmoTodosOsDias(this.kcal);

  final int kcal;

  @override
  int tetoNaData(DateTime data) => kcal;
}

/// Segunda–sexta contra sábado–domingo. [DateTime.weekday] 1–5 vs 6–7.
final class TetoUtilFds extends TetoPerfil {
  const TetoUtilFds({required this.util, required this.fds});

  final int util;
  final int fds;

  @override
  int tetoNaData(DateTime data) {
    final fimDeSemana = data.weekday >= DateTime.saturday;
    return fimDeSemana ? fds : util;
  }
}

/// Um teto por dia, de segunda a domingo. Não colapsa a semana num único slot.
final class TetoSeteDias extends TetoPerfil {
  const TetoSeteDias({
    required this.segunda,
    required this.terca,
    required this.quarta,
    required this.quinta,
    required this.sexta,
    required this.sabado,
    required this.domingo,
  });

  final int segunda;
  final int terca;
  final int quarta;
  final int quinta;
  final int sexta;
  final int sabado;
  final int domingo;

  @override
  int tetoNaData(DateTime data) {
    return switch (data.weekday) {
      DateTime.monday => segunda,
      DateTime.tuesday => terca,
      DateTime.wednesday => quarta,
      DateTime.thursday => quinta,
      DateTime.friday => sexta,
      DateTime.saturday => sabado,
      DateTime.sunday => domingo,
      _ => throw ArgumentError.value(
        data.weekday,
        'data',
        'dia da semana fora de 1..7',
      ),
    };
  }
}

/// Política de eat-back do treino. Parcial usa o percentual informado, sem clamp.
enum PoliticaCreditoTreino { zero, parcial, cem }

/// Entrada de um dia. [reservaProximas] já vem pronta — esta camada não soma refeições futuras.
final class EntradaOrcamento {
  const EntradaOrcamento({
    required this.data,
    required this.perfil,
    required this.politica,
    this.percentual,
    this.treinoKcal,
    required this.consumido,
    required this.reservaProximas,
  });

  final DateTime data;
  final TetoPerfil perfil;
  final PoliticaCreditoTreino politica;

  /// Percentual digitado (50 significa 50%). Só entra quando [politica] é parcial.
  final num? percentual;

  /// Kcal do treino do dia. Null = não informado.
  final num? treinoKcal;

  final num consumido;
  final num reservaProximas;
}

/// Resultado de um único cálculo: base, crédito, teto efetivo e orçamento da janela.
final class ResultadoOrcamento {
  const ResultadoOrcamento({
    required this.tetoBase,
    required this.creditoTreino,
    required this.tetoEfetivo,
    required this.orcamentoJanela,
  });

  final int tetoBase;
  final num creditoTreino;
  final num tetoEfetivo;
  final num orcamentoJanela;

  @override
  String toString() {
    return 'ResultadoOrcamento(tetoBase: $tetoBase, '
        'creditoTreino: $creditoTreino, tetoEfetivo: $tetoEfetivo, '
        'orcamentoJanela: $orcamentoJanela)';
  }
}

/// Calculadora sem estado. Cada chamada parte só da [entrada].
final class BudgetCalculator {
  const BudgetCalculator();

  ResultadoOrcamento calcular(EntradaOrcamento entrada) {
    final tetoBase = entrada.perfil.tetoNaData(entrada.data);
    final creditoTreino = _creditoTreino(
      politica: entrada.politica,
      percentual: entrada.percentual,
      treinoKcal: entrada.treinoKcal,
    );
    final tetoEfetivo = tetoBase + creditoTreino;
    final bruto = tetoEfetivo - entrada.consumido - entrada.reservaProximas;
    return ResultadoOrcamento(
      tetoBase: tetoBase,
      creditoTreino: creditoTreino,
      tetoEfetivo: tetoEfetivo,
      orcamentoJanela: bruto < 0 ? 0 : bruto,
    );
  }
}

/// Política 0, ou treino do dia ainda não informado, zera o crédito.
/// Parcial é o produto cru `treino * (pct / 100)`. 100% devolve o treino inteiro.
num _creditoTreino({
  required PoliticaCreditoTreino politica,
  required num? percentual,
  required num? treinoKcal,
}) {
  if (politica == PoliticaCreditoTreino.zero || treinoKcal == null) {
    return 0;
  }
  if (politica == PoliticaCreditoTreino.cem) {
    return treinoKcal;
  }
  if (percentual == null) {
    throw ArgumentError.notNull('percentual');
  }
  return treinoKcal * percentual / 100;
}
