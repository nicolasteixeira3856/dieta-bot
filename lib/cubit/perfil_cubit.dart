import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:nutri/domain/rules/budget_calculator.dart';

enum ModoTeto { mesmo, utilFds, seteDias }

enum PoliticaEatBack { zero, parcial, cem }

class PerfilState {
  const PerfilState({
    this.modo = ModoTeto.mesmo,
    this.kcalMesmo = 2000,
    this.kcalUtil = 2000,
    this.kcalFds = 2300,
    this.dias = const [2000, 2000, 2000, 2000, 2000, 2000, 2000],
    this.politica = PoliticaEatBack.zero,
    this.percentual = 50,
  });

  final ModoTeto modo;
  final int kcalMesmo;
  final int kcalUtil;
  final int kcalFds;
  final List<int> dias;
  final PoliticaEatBack politica;

  /// Percentual digitado do eat-back. O default é 50. Não existe cap de kcal.
  final int percentual;

  PerfilState copyWith({
    ModoTeto? modo,
    int? kcalMesmo,
    int? kcalUtil,
    int? kcalFds,
    List<int>? dias,
    PoliticaEatBack? politica,
    int? percentual,
  }) {
    return PerfilState(
      modo: modo ?? this.modo,
      kcalMesmo: kcalMesmo ?? this.kcalMesmo,
      kcalUtil: kcalUtil ?? this.kcalUtil,
      kcalFds: kcalFds ?? this.kcalFds,
      dias: dias ?? this.dias,
      politica: politica ?? this.politica,
      percentual: percentual ?? this.percentual,
    );
  }

  TetoPerfil get tetoPerfil {
    return switch (modo) {
      ModoTeto.mesmo => TetoMesmoTodosOsDias(kcalMesmo),
      ModoTeto.utilFds => TetoUtilFds(util: kcalUtil, fds: kcalFds),
      ModoTeto.seteDias => TetoSeteDias(
        segunda: dias[0],
        terca: dias[1],
        quarta: dias[2],
        quinta: dias[3],
        sexta: dias[4],
        sabado: dias[5],
        domingo: dias[6],
      ),
    };
  }

  PoliticaCreditoTreino get politicaTreino {
    return switch (politica) {
      PoliticaEatBack.zero => PoliticaCreditoTreino.zero,
      PoliticaEatBack.parcial => PoliticaCreditoTreino.parcial,
      PoliticaEatBack.cem => PoliticaCreditoTreino.cem,
    };
  }

  ResultadoOrcamento orcamentoDeHoje() {
    return const BudgetCalculator().calcular(
      EntradaOrcamento(
        data: DateTime.now(),
        perfil: tetoPerfil,
        politica: politicaTreino,
        percentual: percentual,
        consumido: 0,
        reservaProximas: 0,
      ),
    );
  }
}

class PerfilCubit extends Cubit<PerfilState> {
  PerfilCubit() : super(const PerfilState());

  void modo(ModoTeto modo) => emit(state.copyWith(modo: modo));

  void kcalMesmo(int kcal) => emit(state.copyWith(kcalMesmo: kcal));

  void kcalUtil(int kcal) => emit(state.copyWith(kcalUtil: kcal));

  void kcalFds(int kcal) => emit(state.copyWith(kcalFds: kcal));

  void politica(PoliticaEatBack politica) =>
      emit(state.copyWith(politica: politica));

  /// Limita o campo digitado a 1–100, como no wire. Isso não corta kcal.
  void percentual(int valor) {
    emit(state.copyWith(percentual: valor.clamp(1, 100)));
  }
}
