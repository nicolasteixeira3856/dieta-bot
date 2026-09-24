import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;

import 'package:nutri/data/estimate.dart';

/// Único leitor de API_PUBLIC_URL. Monta o POST /v1/estimate e não conhece a chave OpenAI.
class EstimateService {
  EstimateService(this._client);

  final http.Client _client;

  /// Sentinel do teste de fluxo quando o binário não recebe --dart-define.
  /// Um define explícito substitui os dois.
  static const baseUrl = String.fromEnvironment(
    'API_PUBLIC_URL',
    defaultValue: 'http://127.0.0.1:9',
  );
  static const inviteCode = String.fromEnvironment(
    'INVITE_CODE',
    defaultValue: 'convite-teste',
  );

  Future<Estimate> estimar({
    required String texto,
    Uint8List? foto,
    required String janela,
  }) async {
    final raiz = baseUrl.endsWith('/')
        ? baseUrl.substring(0, baseUrl.length - 1)
        : baseUrl;
    final uri = Uri.parse('$raiz/v1/estimate');
    final corpo = <String, Object?>{
      'local_time': DateTime.now().toIso8601String(),
      'janela': janela,
      'text': texto,
      'image_b64': foto == null ? null : base64Encode(foto),
      'assumptions': <Map<String, String>>[],
    };
    final resposta = await _client
        .post(
          uri,
          headers: {'Content-Type': 'application/json', 'X-Invite': inviteCode},
          body: jsonEncode(corpo),
        )
        .timeout(const Duration(seconds: 20));
    if (resposta.statusCode < 200 || resposta.statusCode >= 300) {
      throw EstimateException('resposta ${resposta.statusCode}');
    }
    final decoded = jsonDecode(resposta.body);
    if (decoded is! Map) {
      throw EstimateException('json invalido');
    }
    return Estimate.fromJson(Map<String, dynamic>.from(decoded));
  }
}

class EstimateException implements Exception {
  EstimateException(this.message);

  final String message;

  @override
  String toString() => message;
}
