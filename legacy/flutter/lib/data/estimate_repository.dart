import 'dart:typed_data';

import 'package:result_dart/result_dart.dart';

import 'package:nutri/data/estimate.dart';
import 'package:nutri/data/estimate_service.dart';

class EstimateRepository {
  EstimateRepository(this._service);

  final EstimateService _service;

  Future<Result<Estimate>> estimar({
    required String texto,
    Uint8List? foto,
    required String janela,
  }) async {
    try {
      final estimate = await _service.estimar(
        texto: texto,
        foto: foto,
        janela: janela,
      );
      return Success(estimate);
    } on Exception catch (error) {
      return Failure(error);
    } catch (error) {
      return Failure(Exception(error.toString()));
    }
  }
}
