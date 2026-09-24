import 'dart:typed_data';

import 'package:flutter_bloc/flutter_bloc.dart';

import 'package:nutri/data/estimate.dart';
import 'package:nutri/data/estimate_repository.dart';
import 'package:nutri/data/foto_picker.dart';

enum RegistroFase { editando, enviando, card, erro }

class RegistroState {
  const RegistroState({
    this.texto = '',
    this.foto,
    this.fase = RegistroFase.editando,
    this.estimate,
    this.erro,
  });

  final String texto;
  final Uint8List? foto;
  final RegistroFase fase;
  final Estimate? estimate;
  final String? erro;

  RegistroState copyWith({
    String? texto,
    Uint8List? foto,
    bool limparFoto = false,
    RegistroFase? fase,
    Estimate? estimate,
    bool limparEstimate = false,
    String? erro,
    bool limparErro = false,
  }) {
    return RegistroState(
      texto: texto ?? this.texto,
      foto: limparFoto ? null : foto ?? this.foto,
      fase: fase ?? this.fase,
      estimate: limparEstimate ? null : estimate ?? this.estimate,
      erro: limparErro ? null : erro ?? this.erro,
    );
  }
}

class RegistroCubit extends Cubit<RegistroState> {
  RegistroCubit(this._repository, this._fotos) : super(const RegistroState());

  final EstimateRepository _repository;
  final FotoPicker _fotos;

  void texto(String valor) => emit(state.copyWith(texto: valor));

  Future<void> escolherFoto() async {
    final bytes = await _fotos.escolher();
    if (bytes == null || isClosed) {
      return;
    }
    emit(state.copyWith(foto: bytes));
  }

  Future<void> registrar() async {
    if (state.fase == RegistroFase.enviando) {
      return;
    }
    final texto = state.texto.trim();
    if (texto.isEmpty && state.foto == null) {
      emit(
        state.copyWith(fase: RegistroFase.erro, erro: 'Escreve ou manda foto.'),
      );
      return;
    }
    emit(state.copyWith(fase: RegistroFase.enviando, limparErro: true));
    final resultado = await _repository.estimar(
      texto: texto,
      foto: state.foto,
      janela: 'cafe',
    );
    if (isClosed) {
      return;
    }
    resultado.fold(
      (estimate) => emit(
        state.copyWith(
          fase: RegistroFase.card,
          estimate: estimate,
          limparErro: true,
        ),
      ),
      (_) => emit(
        state.copyWith(fase: RegistroFase.erro, erro: 'Não deu para estimar.'),
      ),
    );
  }
}
